#!/usr/bin/env bash
set -e
set -o pipefail

# ==============================================================================
# Sprillex Uniform Lifecycle Wrapper for Android
# ==============================================================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

BRANCH="main"
BUILD_MAIN=false
BUILD_NEWEST=false

for arg in "$@"; do
    case "$arg" in
        -m|--main)
            BUILD_MAIN=true
            ;;
        -n|--newest)
            BUILD_NEWEST=true
            ;;
        -u|--update)
            echo "[INFO] Updating repository..."
            git fetch origin
            ;;
    esac
done

if [ "$BUILD_NEWEST" = true ]; then
    echo "[INFO] Fetching and checking out newest remote branch..."
    git fetch origin
    NEWEST_BRANCH=$(git branch -r --sort=-committerdate | head -n 1 | sed 's/origin\///' | tr -d ' ')
    if [ -n "$NEWEST_BRANCH" ]; then
        echo "[INFO] Switching to newest branch: $NEWEST_BRANCH"
        git checkout "$NEWEST_BRANCH" || true
        git pull origin "$NEWEST_BRANCH" || true
    fi
elif [ "$BUILD_MAIN" = true ]; then
    echo "[INFO] Switching to main branch..."
    git checkout main || true
    git pull origin main || true
fi

echo "[INFO] Granting permissions and building Android APK..."
chmod +x gradlew
./gradlew assembleDebug --no-daemon

echo "[SUCCESS] Android Build complete."
