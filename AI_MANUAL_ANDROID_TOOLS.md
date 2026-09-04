# AI Coder Instruction Manual: Android Tools

## 1. System Context & Purpose
This document provides AI agents with the necessary context and technical constraints for managing, cloning, and building Android applications within the ecosystem. The ecosystem relies heavily on continuous integration workflows and automated dependency resolution tools for Android development.

### Key Components:
- `android_tools/clone_android.sh`: A terminal utility specifically designed for cloning and setting up Android application repositories.
- `workflows_examples/`: A directory containing pre-configured GitHub Actions workflows for automated builds (`android_build.yml`) and notifications (`branch_pr_alert.yml`, `notify_new_work.yml`).
- `Android_ai_readme.md`: Documentation containing workflow schemas and GitHub Secret requirements.

## 2. Strict Constraints & Operational Rules
When generating code or configuring Android projects within this ecosystem, AI must adhere strictly to the following constraints:
- **READ-ONLY Mode Acknowledgment:** You must not alter the core logic of `clone_android.sh` without explicit user instruction.
- **Git URL Validation:** When invoking clone operations, the `GITHUB_URL` string must end with `.git`. The internal script utilizes regular expressions (`=~ \.git$`) to validate this and prevent malformed directory creation.
- **Idempotent Operations:** File appends (such as excluding auto-generated files in `.git/info/exclude`) are managed idempotently using `grep -qxF` to prevent duplicate entries and bloat.

## 3. Integration & Setup Steps

### 3.1. Project Initialization & Android SDK Resolution
To clone a repository:
```bash
./android_tools/clone_android.sh https://github.com/user/android-project.git
```
**SDK Path Resolution (`local.properties`):**
During initialization, the tool dynamically searches for an existing `local.properties` file using a `find` command (max depth 2) to use as a template. If none is found, it falls back to a hardcoded default path to ensure the Gradle build system knows where the Android SDK is located.

### 3.2. GitHub Actions Integration
Android apps must integrate the provided GitHub Action workflows located in `workflows_examples`.

**Required GitHub Secrets:**
If utilizing the notification workflows (which push APK build statuses and PR alerts), the following secrets must be injected into the repository:
- `PUSHOVER_APP_TOKEN`
- `PUSHOVER_USER_KEY`

**Workflow Implementation:**
When configuring the repository, copy the desired workflow (e.g., `android_build.yml`) to `.github/workflows/`. AI should note that the build workflow explicitly handles JDK 17 setup, grants execute permissions to `gradlew`, builds the `assembleDebug` APK, and orchestrates Pushover API calls for success/failure notifications.

### 3.3. Update Manager Compatibility
The `clone_android.sh` script automatically generates an `update.sh` script in the repository root.
This generated script is fully compliant with the Sprillex standard and seamlessly integrates with `universal_update_manager.sh`. It specifically supports branch selection flags (`-m` / `--main` and `-n` / `--newest`), allowing automated synchronization and building of the latest Android commits.

## 4. State Modifications & Data Handling
- **Authentication Handing:** `clone_android.sh` supports cloning private repositories by providing instructions for Personal Access Token (PAT) authentication.
- **File Hierarchy:** The host categorizes Android source builds structurally under the `android` folder umbrella.
- **Decoupled Architecture:** The Android application source code has been extracted to a separate repository. This current repository retains only the build documentation, workflows, and tools. AI should refer to this documentation when configuring external Android codebases.
