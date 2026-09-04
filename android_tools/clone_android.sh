#!/bin/bash

# --- Usage: ./clone_android.sh <GITHUB_URL> ---

GITHUB_URL=$1
BASE_DIR="$HOME/sprillex/android"
TEMPLATE_SOURCE="$BASE_DIR/foodscan/local.properties" # Uses foodscan as the "gold standard" for SDK location

# 1. Input Validation
if [ -z "$GITHUB_URL" ]; then
    echo "❌ Error: Please provide a GitHub URL."
    echo "Usage: ./clone_android.sh https://github.com/user/repo"
    exit 1
fi

# 2. Clone Repository
echo "⬇️  Cloning repository..."
cd "$BASE_DIR"
# Extract project name from URL (e.g., 'map' from '.../map.git')
PROJECT_NAME=$(basename "$GITHUB_URL" .git)
git clone "$GITHUB_URL"

if [ ! -d "$PROJECT_NAME" ]; then
    echo "❌ Error: Clone failed. Directory '$PROJECT_NAME' not found."
    exit 1
fi

cd "$PROJECT_NAME"
echo "📂 Entered directory: $PROJECT_NAME"

# 3. Setup Gradle Wrapper (The Fixer)
echo "🔧 Configuring Gradle Wrapper..."
# If gradlew doesn't exist, generate it using system gradle
if [ ! -f "gradlew" ]; then
    gradle wrapper
fi
chmod +x gradlew

# Force update gradle-wrapper.properties to version 8.7
WRAPPER_FILE="gradle/wrapper/gradle-wrapper.properties"
if [ -f "$WRAPPER_FILE" ]; then
    # regex replace distributionUrl line
    sed -i 's|distributionUrl=.*|distributionUrl=https\\://services.gradle.org/distributions/gradle-8.7-bin.zip|' "$WRAPPER_FILE"
    echo "✅ Gradle version pinned to 8.7"
else
    echo "⚠️  Warning: $WRAPPER_FILE not found."
fi

# 4. Configure SDK Location
echo "⚙️  Setting up local.properties..."
if [ -f "$TEMPLATE_SOURCE" ]; then
    cp "$TEMPLATE_SOURCE" "local.properties"
    echo "✅ Copied SDK path from foodscan."
else
    # Fallback if foodscan is missing
    echo "sdk.dir=$HOME/Android/Sdk" > local.properties
    echo "✅ Created generic local.properties."
fi

# 5. Hide Local Configs from Git
echo "🙈 Hiding local configurations from Git..."
git update-index --skip-worktree "$WRAPPER_FILE" 2>/dev/null
echo "gradlew" >> .git/info/exclude
echo "gradlew.bat" >> .git/info/exclude
echo "gradle/wrapper/gradle-wrapper.jar" >> .git/info/exclude

# 6. Generate the Update Script
SCRIPT_NAME="update_${PROJECT_NAME}.sh"
echo "🚀 Generating automation script: $SCRIPT_NAME..."

# Capitalize first letter for APK naming (e.g. map -> Map)
CAP_NAME="${PROJECT_NAME^}"

cat <<EOF > "$SCRIPT_NAME"
#!/bin/bash
PROJECT_DIR="$BASE_DIR/$PROJECT_NAME"
SOURCE_APK="\$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
DEST_DIR="\$PROJECT_DIR/builds"

set -e 

cd "\$PROJECT_DIR"
mkdir -p "\$DEST_DIR"

echo "⬇️  Pulling latest changes..."
git pull

echo "🔨 Building APK..."
./gradlew assembleDebug

TIMESTAMP=\$(date +%Y-%d-%H-%M)
NEW_FILENAME="${CAP_NAME}-B\${TIMESTAMP}.apk"
FINAL_PATH="\$DEST_DIR/\$NEW_FILENAME"

if [ -f "\$SOURCE_APK" ]; then
    echo "wm  Renaming to: \$NEW_FILENAME"
    cp "\$SOURCE_APK" "\$FINAL_PATH"
else
    echo "❌ Error: APK not found."
    exit 1
fi

echo "📱 Installing..."
if adb get-state 1>/dev/null 2>&1; then
    adb install -r "\$FINAL_PATH"
    echo "✅ Installed \$NEW_FILENAME"
else
    echo "⚠️  Device not found. Saved to builds/"
fi
EOF

chmod +x "$SCRIPT_NAME"

echo "🎉 Setup Complete! You can now update this app anytime by running:"
echo "   $BASE_DIR/$PROJECT_NAME/$SCRIPT_NAME"
