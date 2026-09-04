#!/bin/bash
set -e

# --- Configuration ---
DEFAULT_REPO_BASE="https://github.com/sprillex/"
BASE_DIR="${SPRILLEX_ANDROID_HOME:-$HOME/sprillex/android}"
SECONDARY_APK_DIR="${SECONDARY_APK_DIR:-}"

# Strip any trailing slash from the default base URL
DEFAULT_REPO_BASE="${DEFAULT_REPO_BASE%/}"

# --- Usage: ./clone_android.sh <PROJECT_NAME | GITHUB_URL> ---
INPUT=$1

# 1. Input Validation & URL Formatting
if [ -z "$INPUT" ]; then
    echo "❌ Error: Please provide a project name or GitHub URL."
    echo "Usage: ./clone_android.sh <project-name>"
    echo "   or: ./clone_android.sh https://github.com/user/repo.git"
    exit 1
fi

# Strip trailing slash from input if present
INPUT="${INPUT%/}"

if [[ "$INPUT" =~ ^(https?://|git@) ]]; then
    GITHUB_URL="$INPUT"
    if [[ ! "$GITHUB_URL" =~ \.git$ ]]; then
        GITHUB_URL="${GITHUB_URL}.git"
    fi
else
    CLEAN_NAME=$(basename "$INPUT" .git)
    GITHUB_URL="${DEFAULT_REPO_BASE}/${CLEAN_NAME}.git"
fi

PROJECT_NAME=$(basename "$GITHUB_URL" .git)
PROJECT_DIR="$BASE_DIR/$PROJECT_NAME"
SRC_DIR="$PROJECT_DIR/src"
BUILDS_DIR="$PROJECT_DIR/builds"

TEMPLATE_SOURCE=$(find "$BASE_DIR" -maxdepth 3 -name "local.properties" -print -quit 2>/dev/null || true)

# 2. Setup Project Wrapper & Clone Repository
echo "📁 Setting up project structure at: $PROJECT_DIR"
mkdir -p "$PROJECT_DIR"
mkdir -p "$BUILDS_DIR"

echo "⬇️  Cloning repository into: $SRC_DIR"
git clone "$GITHUB_URL" "$SRC_DIR"

if [ ! -d "$SRC_DIR" ]; then
    echo "❌ Error: Clone failed. Directory '$SRC_DIR' not found."
    exit 1
fi

cd "$SRC_DIR"

# Locate build root
SETTINGS_FILE=$(find . -maxdepth 3 -name "settings.gradle" -o -name "settings.gradle.kts" | head -n 1)
if [ -n "$SETTINGS_FILE" ]; then
    BUILD_ROOT="$(cd "$(dirname "$SETTINGS_FILE")" && pwd)"
else
    BUILD_ROOT="$SRC_DIR"
fi
cd "$BUILD_ROOT"

# 3. Setup Gradle Wrapper
echo "🔧 Configuring Gradle Wrapper..."
if [ ! -f "gradlew" ]; then
    gradle wrapper || true
fi
chmod +x gradlew 2>/dev/null || true

WRAPPER_FILE="gradle/wrapper/gradle-wrapper.properties"
if [ -f "$WRAPPER_FILE" ]; then
    sed -i 's|distributionUrl=.*|distributionUrl=https\\://services.gradle.org/distributions/gradle-8.7-bin.zip|' "$WRAPPER_FILE"
    echo "✅ Gradle version pinned to 8.7"
else
    echo "⚠️  Warning: $WRAPPER_FILE not found."
fi

# 4. Configure SDK Location
echo "⚙️  Setting up local.properties..."
if [ -n "$TEMPLATE_SOURCE" ] && [ -f "$TEMPLATE_SOURCE" ]; then
    cp "$TEMPLATE_SOURCE" "local.properties"
    echo "✅ Copied SDK path from existing project."
else
    echo "sdk.dir=$HOME/Android/Sdk" > local.properties
    echo "✅ Created generic local.properties."
fi

# 5. Hide Local Configs from Git
echo "🙈 Hiding local configurations from Git..."
cd "$SRC_DIR"
if [ -f "$BUILD_ROOT/$WRAPPER_FILE" ]; then
    REL_WRAPPER=$(realpath --relative-to="$SRC_DIR" "$BUILD_ROOT/$WRAPPER_FILE" 2>/dev/null || echo "$WRAPPER_FILE")
    git update-index --skip-worktree "$REL_WRAPPER" 2>/dev/null || true
fi

mkdir -p .git/info
touch .git/info/exclude
grep -qxF "gradlew" .git/info/exclude || echo "gradlew" >> .git/info/exclude
grep -qxF "gradlew.bat" .git/info/exclude || echo "gradlew.bat" >> .git/info/exclude
grep -qxF "gradle/wrapper/gradle-wrapper.jar" .git/info/exclude || echo "gradle/wrapper/gradle-wrapper.jar" >> .git/info/exclude
grep -qxF "local.properties" .git/info/exclude || echo "local.properties" >> .git/info/exclude

# 6. Generate the Update Script in Project Root
SCRIPT_PATH="$PROJECT_DIR/update.sh"
echo "🚀 Generating automation script: $SCRIPT_PATH..."

CAP_NAME="${PROJECT_NAME^}"

cat <<EOF > "$SCRIPT_PATH"
#!/bin/bash
set -e

SECONDARY_APK_DIR="\${SECONDARY_APK_DIR:-$SECONDARY_APK_DIR}"
SKIP_ADB_INSTALL="\${SKIP_ADB_INSTALL:-false}"
EOF

cat <<'EOF' >> "$SCRIPT_PATH"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
DEST_DIR="$SCRIPT_DIR/builds"
BASE_DIR="$HOME/sprillex/android"
PROJECT_NAME="$(basename "$SCRIPT_DIR")"
CAP_NAME="${PROJECT_NAME^}"

mkdir -p "$DEST_DIR"
cd "$SRC_DIR"

echo "🧹 Resetting local Git repository state..."
git reset --hard HEAD
git clean -fd -e "local.properties" -e "**/local.properties" -e "gradle/wrapper/" -e "**/gradle/wrapper/"

echo "⬇️  Fetching remote branches..."
git fetch --all --prune

mapfile -t branches < <(git for-each-ref --format='%(refname:lstrip=3)' refs/remotes/origin | grep -v '^HEAD$' || true)

if [ ${#branches[@]} -eq 0 ]; then
    echo "❌ Error: No remote branches found."
    exit 1
fi

TARGET_BRANCH=""
ARG=$1

if [ -n "$ARG" ]; then
    case "$ARG" in
        -m|--main)
            if printf '%s\n' "${branches[@]}" | grep -qx "main"; then
                TARGET_BRANCH="main"
            elif printf '%s\n' "${branches[@]}" | grep -qx "master"; then
                TARGET_BRANCH="master"
            else
                TARGET_BRANCH="main"
            fi
            ;;
        -n|--newest)
            TARGET_BRANCH=$(git for-each-ref --sort=-committerdate --format='%(refname:lstrip=3)' refs/remotes/origin | grep -v '^HEAD$' | head -n 1)
            ;;
        *)
            if printf '%s\n' "${branches[@]}" | grep -qx "$ARG"; then
                TARGET_BRANCH="$ARG"
            else
                echo "⚠️  Branch '$ARG' not found on remote."
            fi
            ;;
    esac
fi

if [ -z "$TARGET_BRANCH" ]; then
    echo ""
    echo "Available remote branches:"
    for i in "${!branches[@]}"; do
        echo "$((i+1)). ${branches[$i]}"
    done
    echo ""

    read -p "Select a branch to build [1-${#branches[@]}] (Default: 1): " selection
    if [ -z "$selection" ]; then
        selection=1
    fi

    if [[ "$selection" =~ ^[0-9]+$ ]] && [ "$selection" -ge 1 ] && [ "$selection" -le "${#branches[@]}" ]; then
        TARGET_BRANCH="${branches[$((selection-1))]}"
    elif printf '%s\n' "${branches[@]}" | grep -qx "$selection"; then
        TARGET_BRANCH="$selection"
    else
        echo "❌ Invalid selection. Defaulting to first branch: ${branches[0]}"
        TARGET_BRANCH="${branches[0]}"
    fi
fi

echo "🌿 Switching to branch: $TARGET_BRANCH"
git checkout -B "$TARGET_BRANCH" "origin/$TARGET_BRANCH"
git reset --hard "origin/$TARGET_BRANCH"

# Locate Gradle project directory dynamically
SETTINGS_FILE=$(find "$SRC_DIR" -maxdepth 3 \( -name "settings.gradle" -o -name "settings.gradle.kts" \) | head -n 1)
if [ -n "$SETTINGS_FILE" ]; then
    BUILD_ROOT="$(cd "$(dirname "$SETTINGS_FILE")" && pwd)"
else
    BUILD_ROOT="$SRC_DIR"
fi

cd "$BUILD_ROOT"
WRAPPER_FILE="gradle/wrapper/gradle-wrapper.properties"

# Ensure Gradle Wrapper and properties exist
if [ ! -f "gradlew" ] || [ ! -f "$WRAPPER_FILE" ]; then
    echo "🔧 Restoring Gradle Wrapper in $BUILD_ROOT..."
    gradle wrapper || true
    chmod +x gradlew 2>/dev/null || true
    if [ -f "$WRAPPER_FILE" ]; then
        sed -i 's|distributionUrl=.*|distributionUrl=https\\://services.gradle.org/distributions/gradle-8.7-bin.zip|' "$WRAPPER_FILE"
    fi
fi

# Ensure local.properties exists
if [ ! -f "local.properties" ]; then
    echo "⚙️  Restoring local.properties in $BUILD_ROOT..."
    TEMPLATE_SOURCE=$(find "$BASE_DIR" -maxdepth 3 -name "local.properties" -print -quit 2>/dev/null || true)
    if [ -n "$TEMPLATE_SOURCE" ] && [ -f "$TEMPLATE_SOURCE" ]; then
        cp "$TEMPLATE_SOURCE" "local.properties"
    else
        echo "sdk.dir=$HOME/Android/Sdk" > local.properties
    fi
fi

echo "🔨 Building APK..."
./gradlew assembleDebug

TIMESTAMP=$(date +%Y-%m-%d-%H-%M)
NEW_FILENAME="${CAP_NAME}-B${TIMESTAMP}.apk"
FINAL_PATH="$DEST_DIR/$NEW_FILENAME"

# Locate the built APK
FOUND_APK=$(find "$BUILD_ROOT" -type f -path "*/build/outputs/apk/debug/*.apk" | head -n 1)
if [ -z "$FOUND_APK" ]; then
    FOUND_APK=$(find "$BUILD_ROOT" -type f -name "*debug*.apk" | head -n 1)
fi

if [ -n "$FOUND_APK" ] && [ -f "$FOUND_APK" ]; then
    echo "💾 Renaming to: $NEW_FILENAME"
    cp "$FOUND_APK" "$FINAL_PATH"
else
    echo "❌ Error: Could not locate compiled debug APK in $BUILD_ROOT"
    exit 1
fi

if [ -n "$SECONDARY_APK_DIR" ]; then
    SEC_BASE="${SECONDARY_APK_DIR%/}"
    if [ ! -d "$SEC_BASE" ]; then
        echo "❌ Error: Secondary directory '$SEC_BASE' does not exist."
    else
        TARGET_SUBDIR="$SEC_BASE/projects/$PROJECT_NAME/apk"
        if mkdir -p "$TARGET_SUBDIR" 2>/dev/null; then
            SEC_PATH="$TARGET_SUBDIR/$NEW_FILENAME"
            if cp "$FINAL_PATH" "$SEC_PATH"; then
                echo "💾 Secondary copy saved to: $SEC_PATH"
            else
                echo "❌ Error: Failed to copy APK to secondary location: $SEC_PATH"
            fi
        else
            echo "❌ Error: Failed to create secondary directory: $TARGET_SUBDIR"
        fi
    fi
fi

echo "📱 Checking device installation options..."
if [ "${SKIP_ADB_INSTALL:-false}" = "true" ]; then
    echo "ℹ️  SKIP_ADB_INSTALL is active. Skipping ADB device installation."
elif adb get-state 1>/dev/null 2>&1; then
    adb install -r "$FINAL_PATH"
    echo "✅ Installed $NEW_FILENAME"
else
    echo "⚠️  ADB device not found. Output APK saved to: $FINAL_PATH"
fi
EOF

chmod +x "$SCRIPT_PATH"

echo ""
echo "🎉 Setup Complete!"
echo "   Project Root:  $PROJECT_DIR"
echo "   Git Tree:      $SRC_DIR"
echo "   Update Script: $SCRIPT_PATH"
echo ""

# 7. Transition User into the Project Root Directory
cd "$PROJECT_DIR"
if [ "$SHLVL" -gt 1 ] && [ -t 0 ]; then
    echo "🚀 Entering project shell ($PROJECT_DIR)... (type 'exit' when done)"
    exec "$SHELL"
fi
