#!/usr/bin/env bash
set -e
set -o pipefail

# ==============================================================================
# Sprillex General Repository Initializer & Subtree Setup Tool
#
# DESCRIPTION:
#   Initializes a new or existing repository into the Sprillex ecosystem.
#   - Handles blank repo initial commits to prevent git subtree merge failures.
#   - Embeds central sprillex_tools via Git Subtree.
#   - Generates starter boilerplate for blank repositories.
#   - Creates a root uniform update.sh wrapper and AGENTS.md symlink.
# ==============================================================================

# --- Configurable Directories & Repositories (Change as needed) ---
SPRILLEX_PYTHON_HOME="${SPRILLEX_PYTHON_HOME:-$HOME/sprillex/virtual}"
SPRILLEX_DOCKER_HOME="${SPRILLEX_DOCKER_HOME:-$HOME/sprillex/docker}"
SPRILLEX_ANDROID_HOME="${SPRILLEX_ANDROID_HOME:-$HOME/sprillex/android}"
SPRILLEX_GENERIC_HOME="${SPRILLEX_GENERIC_HOME:-$HOME/sprillex/projects}"

DEFAULT_REPO_BASE="git@github.com:sprillex"
TOOLS_REPO_URL="${TOOLS_REPO_URL:-git@github.com:sprillex/tools.git}"

# Strip trailing slash from default base
DEFAULT_REPO_BASE="${DEFAULT_REPO_BASE%/}"

# Defaults
PROJECT_TYPE="python"
NON_INTERACTIVE=false
INPUT=""
GITHUB_URL=""

show_help() {
    echo "Usage: $0 <PROJECT_NAME | GITHUB_URL> [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -t, --type TYPE   Project type: python (default), docker, android, generic"
    echo "  -y, --yes         Run non-interactively (bypass prompts)"
    echo "  -h, --help        Display this help message"
}

# Parse Arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            PROJECT_TYPE="${2,,}"
            shift 2
            ;;
        -y|--yes)
            NON_INTERACTIVE=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        -*)
            echo "Unknown flag: $1"
            show_help
            exit 1
            ;;
        *)
            if [ -z "$INPUT" ]; then
                INPUT="$1"
            else
                echo "Warning: Extra positional argument ignored: $1"
            fi
            shift
            ;;
    esac
done

if [ -z "$INPUT" ]; then
    if [ "$NON_INTERACTIVE" = true ]; then
        echo "Error: PROJECT_NAME or GITHUB_URL is required in non-interactive mode."
        exit 1
    fi
    echo "=== Sprillex Repository Initializer ==="
    read -p "Enter Project Name or GitHub URL (e.g. net-monitor or https://github.com/user/repo.git): " INPUT
fi

if [ -z "$INPUT" ]; then
    echo "Error: No project name or URL provided."
    exit 1
fi

if [[ "$NON_INTERACTIVE" = false ]] && [ -z "$PROJECT_TYPE" ]; then
    echo "Select Project Type:"
    echo "1) Python"
    echo "2) Docker"
    echo "3) Android"
    echo "4) Generic"
    read -p "Selection [1]: " TYPE_OPT
    case "$TYPE_OPT" in
        2) PROJECT_TYPE="docker" ;;
        3) PROJECT_TYPE="android" ;;
        4) PROJECT_TYPE="generic" ;;
        *) PROJECT_TYPE="python" ;;
    esac
fi

INPUT="${INPUT%/}"

if [[ "$INPUT" =~ ^(https?://|git@|file://|/|\./|\.\./) ]]; then
    GITHUB_URL="$INPUT"
    if [[ "$GITHUB_URL" =~ ^(https?://|git@) ]] && [[ ! "$GITHUB_URL" =~ \.git$ ]]; then
        GITHUB_URL="${GITHUB_URL}.git"
    fi
else
    CLEAN_NAME=$(basename "$INPUT" .git)
    GITHUB_URL="${DEFAULT_REPO_BASE}/${CLEAN_NAME}.git"
fi

PROJECT_NAME=$(basename "$GITHUB_URL" .git)

# Determine Target Base Path based on Project Type
case "$PROJECT_TYPE" in
    python)
        BASE_DIR="$SPRILLEX_PYTHON_HOME"
        PROJECT_DIR="$BASE_DIR/$PROJECT_NAME"
        SRC_DIR="$PROJECT_DIR/src"
        ;;
    docker)
        BASE_DIR="$SPRILLEX_DOCKER_HOME"
        PROJECT_DIR="$BASE_DIR/$PROJECT_NAME"
        SRC_DIR="$PROJECT_DIR"
        ;;
    android)
        BASE_DIR="$SPRILLEX_ANDROID_HOME"
        PROJECT_DIR="$BASE_DIR/$PROJECT_NAME"
        SRC_DIR="$PROJECT_DIR/src"
        ;;
    *)
        BASE_DIR="$SPRILLEX_GENERIC_HOME"
        PROJECT_DIR="$BASE_DIR/$PROJECT_NAME"
        SRC_DIR="$PROJECT_DIR"
        ;;
esac

echo "------------------------------------------"
echo "Project Name: $PROJECT_NAME"
echo "Project Type: $PROJECT_TYPE"
echo "Project Dir:  $PROJECT_DIR"
echo "Source Dir:   $SRC_DIR"
echo "GitHub URL:   $GITHUB_URL"
echo "------------------------------------------"

mkdir -p "$BASE_DIR"
mkdir -p "$PROJECT_DIR"
mkdir -p "$SRC_DIR"

cd "$SRC_DIR"

export GIT_TERMINAL_PROMPT=0

# Step 1: Git Clone or Init
if [ ! -d ".git" ]; then
    echo "⬇️  Cloning or initializing repository in $SRC_DIR..."
    git clone -c net.gitFetchWithCli=true --config core.askPass= "$GITHUB_URL" . 2>/dev/null || {
        echo "ℹ️  Remote repository clone failed or repo is empty. Initializing local git repo..."
        git init
        git remote add origin "$GITHUB_URL" 2>/dev/null || true
    }
fi

# Step 2: Handle Empty Repository (Prevent Git Subtree Merge Crash)
if [ -z "$(git rev-parse --quiet --verify HEAD 2>/dev/null || true)" ]; then
    echo "🌱 Blank repository detected. Creating initial commit..."
    echo "# $PROJECT_NAME" > README.md
    git add README.md
    git commit -m "Initial commit"
fi

# Step 3: Embed Central Tools via Git Subtree
echo "📦 Embedding Sprillex tools via Git Subtree..."
if [ ! -d "sprillex_tools" ]; then
    git remote add sprillex-tools "$TOOLS_REPO_URL" 2>/dev/null || true
    git fetch sprillex-tools --depth=1 2>/dev/null || true
    git subtree add --prefix=sprillex_tools sprillex-tools main --squash 2>/dev/null || {
        echo "⚠️ Subtree add encountered warning/error (remote unreachable or missing branch). Continuing with fallback directory setup..."
    }
else
    echo "ℹ️  'sprillex_tools' subtree directory already exists. Pulling latest..."
    git subtree pull --prefix=sprillex_tools "$TOOLS_REPO_URL" main --squash 2>/dev/null || true
fi

# Step 4: Scaffold Starter Templates if Repo is Blank
case "$PROJECT_TYPE" in
    python)
        if [ ! -f "services.yaml" ] && [ ! -f "services.json" ]; then
            echo "📄 Creating starter services.yaml..."
            cat << EOF > services.yaml
services:
  - name: $PROJECT_NAME
    description: $PROJECT_NAME Python Service
    type: simple
    script: main.py
    default_port: 5000
EOF
        fi

        if [ ! -f "main.py" ]; then
            echo "🐍 Creating starter main.py..."
            cat << EOF > main.py
#!/usr/bin/env python3
import time

def main():
    print("Starting $PROJECT_NAME service...")
    while True:
        time.sleep(10)

if __name__ == "__main__":
    main()
EOF
        fi

        if [ ! -f "requirements.txt" ]; then
            touch requirements.txt
        fi

        if [ ! -f ".env.example" ]; then
            cat << EOF > .env.example
# Service Configuration Template
PORT=5000
EOF
        fi
        ;;

    docker)
        if [ ! -f "docker-compose.yml" ]; then
            echo "🐳 Creating starter docker-compose.yml..."
            cat << EOF > docker-compose.yml
version: '3.8'

services:
  app:
    image: nginx:alpine
    ports:
      - "\${SERVICE_PORT:-8080}:80"
    volumes:
      - \${SPRILLEX_DATA_PATH:-./data}/config:/etc/nginx/conf.d
    restart: unless-stopped
EOF
        fi

        if [ ! -f ".env.example" ]; then
            cat << EOF > .env.example
SERVICE_PORT=8080
SPRILLEX_DATA_PATH=./data
EOF
        fi
        ;;
esac

# Step 5: Generate Root Uniform update.sh Wrapper
WRAPPER_PATH="$PROJECT_DIR/update.sh"
echo "🚀 Generating Uniform update.sh wrapper at $WRAPPER_PATH..."

cat << 'EOF_WRAPPER' > "$WRAPPER_PATH"
#!/usr/bin/env bash
set -e
set -o pipefail

# ==============================================================================
# Sprillex Uniform Lifecycle Wrapper
# Auto-generated by init_repo.sh
# ==============================================================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLS_DIR="$PROJECT_DIR/src/sprillex_tools"
TOOLS_REPO="${TOOLS_REPO_URL:-git@github.com:sprillex/tools.git}"

if [ ! -d "$TOOLS_DIR" ]; then
    TOOLS_DIR="$PROJECT_DIR/sprillex_tools"
fi

# Detect Delegate Script Path
if [ -f "$TOOLS_DIR/python_tools/update.sh" ] && ([ -d "$PROJECT_DIR/src" ] || [ -f "$PROJECT_DIR/main.py" ]); then
    DELEGATE_SCRIPT="$TOOLS_DIR/python_tools/update.sh"
elif [ -f "$TOOLS_DIR/docker_tools/update.sh" ] && [ -f "$PROJECT_DIR/docker-compose.yml" ]; then
    DELEGATE_SCRIPT="$TOOLS_DIR/docker_tools/update.sh"
elif [ -f "$TOOLS_DIR/android_tools/update.sh" ]; then
    DELEGATE_SCRIPT="$TOOLS_DIR/android_tools/update.sh"
else
    DELEGATE_SCRIPT="$TOOLS_DIR/python_tools/update.sh"
fi

sync_tools() {
    echo "[INFO] Syncing central Sprillex tools via Git Subtree..."
    cd "$PROJECT_DIR"
    if [ -d "src" ]; then cd src; fi
    git subtree pull --prefix=sprillex_tools "$TOOLS_REPO" main --squash || true
}

for arg in "$@"; do
    if [ "$arg" == "--self-update" ] || [ "$arg" == "--sync-tools" ]; then
        sync_tools
        exit 0
    fi
done

if [ ! -f "$DELEGATE_SCRIPT" ]; then
    echo "[ERROR] Lifecycle delegate script not found at: $DELEGATE_SCRIPT"
    exit 1
fi

exec "$DELEGATE_SCRIPT" "$@"
EOF_WRAPPER

chmod +x "$WRAPPER_PATH"

# Symlink AGENTS.md to Root if available
if [ -f "$SRC_DIR/sprillex_tools/python_tools/AGENTS.md" ] && [ ! -f "$PROJECT_DIR/AGENTS.md" ]; then
    ln -sf "$SRC_DIR/sprillex_tools/python_tools/AGENTS.md" "$PROJECT_DIR/AGENTS.md"
fi

echo ""
echo "🎉 Setup Complete!"
echo "   Project Name:      $PROJECT_NAME"
echo "   Project Type:      $PROJECT_TYPE"
echo "   Project Directory: $PROJECT_DIR"
echo "   Source Directory:  $SRC_DIR"
echo "   Uniform Manager:   $WRAPPER_PATH"
echo ""
