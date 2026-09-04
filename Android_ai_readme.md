# Android AI Readme

## Android Projects

Android apps should use the workflows in the `workflows_examples` folder and modify them as needed to work for the specific project.

### GitHub Secrets for Workflows
The included workflows (e.g., `android_build.yml`, `branch_pr_alert.yml`) require the following secrets to be configured in your GitHub repository settings:

*   `PUSHOVER_APP_TOKEN`: Your Pushover Application Token.
*   `PUSHOVER_USER_KEY`: Your Pushover User Key.

These are used to send notifications about build status and repository activity.

## Workflow Examples

### android_build.yml

```yaml
name: Android Build

on:
  push:
    # Triggers on push to every branch EXCEPT 'main'
    branches-ignore: [ "main" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout
      uses: actions/checkout@v4

    - name: set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Create local.properties
      run: echo "sdk.dir=$ANDROID_HOME" > local.properties

    - name: Clean Build
      run: ./gradlew clean --no-daemon

    - name: Build with Gradle
      run: ./gradlew assembleDebug --no-daemon --no-build-cache --stacktrace 2>&1 | tee build.log

    - name: Upload Build Logs
      if: failure()
      id: log_upload
      uses: actions/upload-artifact@v4
      with:
        name: build-logs
        path: build.log

    - name: Rename APK
      run: |
        REPO_NAME=$(echo "${{ github.repository }}" | cut -d'/' -f2)
        APK_NAME="${REPO_NAME}-build-${{ github.run_number }}.apk"
        mv app/build/outputs/apk/debug/app-debug.apk "app/build/outputs/apk/debug/$APK_NAME"
        echo "APK_NAME=$APK_NAME" >> $GITHUB_ENV

    - name: Upload Debug APK
      id: apk_upload  # <--- Added ID to reference the link later
      uses: actions/upload-artifact@v4
      with:
        name: debug-apk
        path: app/build/outputs/apk/debug/${{ env.APK_NAME }}

    - name: Send Pushover Notification
      if: always() # Runs even if the build fails
      run: |
        # 1. Determine Title and Priority
        if [ "${{ job.status }}" == "success" ]; then
          PRIORITY=0
          TITLE="${{ github.workflow }}: Success"
          MESSAGE="Branch: ${{ github.ref_name }}
          Status: ${{ job.status }}

          Download APK: ${{ steps.apk_upload.outputs.artifact-url }}"
        else
          PRIORITY=1
          TITLE="${{ github.workflow }}: Failed"
          MESSAGE="Branch: ${{ github.ref_name }}
          Status: ${{ job.status }}

          Download Logs: ${{ steps.log_upload.outputs.artifact-url }}"
        fi

        # 2. Send Notification
        # We include the specific Artifact URL in the message body
        curl -s \
          --form-string "token=${{ secrets.PUSHOVER_APP_TOKEN }}" \
          --form-string "user=${{ secrets.PUSHOVER_USER_KEY }}" \
          --form-string "title=$TITLE" \
          --form-string "priority=$PRIORITY" \
          --form-string "message=$MESSAGE" \
          --form-string "url=${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}" \
          https://api.pushover.net/1/messages.json
```

### branch_pr_alert.yml

```yaml
name: New Branch/PR Alert
on:
  pull_request:
    types: [opened]
  create:

jobs:
  notify:
    runs-on: ubuntu-latest
    # Filter out tag creation so we only get Branch alerts (optional)
    if: github.event.ref_type != 'tag'

    steps:
      - name: Send Pushover Notification
        run: |
          # Determine the message based on the event
          if [[ "${{ github.event_name }}" == "pull_request" ]]; then
            MSG="New PR Opened: ${{ github.event.pull_request.title }} by ${{ github.actor }}"
            URL="${{ github.event.pull_request.html_url }}"
          else
            MSG="New Branch Created: ${{ github.ref_name }} by ${{ github.actor }}"
            URL="${{ github.server_url }}/${{ github.repository }}/tree/${{ github.ref_name }}"
          fi

          # Send the request
          curl -s \
            --form-string "token=${{ secrets.PUSHOVER_APP_TOKEN }}" \
            --form-string "user=${{ secrets.PUSHOVER_USER_KEY }}" \
            --form-string "title=GitHub Activity: ${{ github.repository }}" \
            --form-string "message=$MSG" \
            --form-string "url=$URL" \
            https://api.pushover.net/1/messages.json
```

### notify_new_work.yml

```yaml
name: Notify on New Work
on:
  pull_request:
    types: [opened]       # Triggers only when a PR is first created
  create:                 # Triggers when a Branch or Tag is created

jobs:
  notify:
    runs-on: ubuntu-latest
    steps:
      - name: Identify Event Type
        run: |
          if [[ "${{ github.event_name }}" == "pull_request" ]]; then
            echo "New Pull Request Detected: ${{ github.event.pull_request.title }}"
          elif [[ "${{ github.event_name }}" == "create" && "${{ github.event.ref_type }}" == "branch" ]]; then
            echo "New Branch Detected: ${{ github.ref_name }}"
          fi

      # Insert your Webhook/Pushover step here
      - name: Send Notification
        run: echo "Sending notification..."
```

## Robust Dark Mode Implementation

A guide and starter template for implementing a robust, accessible, and "eye-friendly" dark mode for web applications.

This implementation goes beyond simple color inversion. It focuses on reducing eye strain, preventing OLED smearing, ensuring accessibility compliance, and managing visual hierarchy through elevation.

### 🎨 Best Practices & Design Principles

#### 1. The Foundation: Dark Grey, Not Black
* **Avoid `#000000`:** Pure black causes "smearing" on OLED screens when pixels turn on/off and creates high-contrast "halation" (blurring) around text.
* **Use `#121212`:** This standard dark grey reduces eye strain and allows for shadows to be visible.

#### 2. Typography & Contrast
* **Opacity over Color:** Instead of specific hex codes for text, use white with reduced opacity to blend naturally with different background elevations.
    * *High Emphasis:* 87% opacity
    * *Medium Emphasis:* 60% opacity
    * *Disabled:* 38% opacity
* **Avoid Pure White Text:** Pure white (`#FFFFFF`) on dark backgrounds can be visually vibrating.

#### 3. Color Desaturation
* **Desaturate Accents:** Bright brand colors (like deep blue) often vibrate or become unreadable against dark backgrounds.
* **Solution:** Desaturate and lighten accent colors (e.g., turn Deep Blue `#0055ff` into Pastel Blue `#8ab4f8`) to meet WCAG contrast standards.

#### 4. Depth via Elevation
* **Lightness = Closeness:** Since you cannot cast a shadow on a black void, use lightness to indicate depth. The "closer" an element is to the user (like a modal or card), the lighter the grey background should be.
    * *Background:* Level 0 (`#121212`)
    * *Card:* Level 1 (`#1e1e1e`)
    * *Modal:* Level 2 (`#2d2d2d`)

---

### 🚀 Starter Template

This template uses **CSS Custom Properties (Variables)** for theming and includes a blocking script to prevent the "Flash of Light" (FOUC) on page load.

#### `index.html`

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Robust Dark Mode Template</title>
    <style>
        /* 1. Define the Color System */
        :root {
            /* LIGHT THEME DEFAULTS */
            --bg-body: #ffffff;
            --bg-surface-1: #f7f7f7; /* Cards */
            --bg-surface-2: #eeeeee; /* Modals/Dropdowns */

            --text-primary: #121212;
            --text-secondary: #5f6368;

            --brand-color: #0055ff;
            --error-color: #d32f2f;

            --border-color: #e0e0e0;
            --shadow-color: rgba(0, 0, 0, 0.1);
        }

        /* DARK THEME OVERRIDES */
        /* Applied when data-theme="dark" is on <html> OR system pref is dark */
        [data-theme="dark"] {
            /* Foundation: Dark Grey, not Black */
            --bg-body: #121212;

            /* Elevation via Lightness (lighter = closer to user) */
            --bg-surface-1: #1e1e1e;
            --bg-surface-2: #2d2d2d;

            /* Typography: Off-white and Opacity */
            --text-primary: rgba(255, 255, 255, 0.87);
            --text-secondary: rgba(255, 255, 255, 0.60);

            /* Desaturated Accents */
            --brand-color: #8ab4f8; /* Lighter, pastel blue */
            --error-color: #f28b82; /* Lighter red/pink */

            --border-color: #333333;
            --shadow-color: rgba(0, 0, 0, 0.5);
        }

        /* 2. Global Styles */
        body {
            background-color: var(--bg-body);
            color: var(--text-primary);
            font-family: system-ui, -apple-system, sans-serif;
            margin: 0;
            padding: 2rem;
            line-height: 1.6;
            transition: background-color 0.3s ease, color 0.3s ease;
        }

        /* 3. Component Examples */
        .card {
            background-color: var(--bg-surface-1);
            padding: 2rem;
            border-radius: 8px;
            border: 1px solid var(--border-color);
            box-shadow: 0 4px 6px var(--shadow-color);
            max-width: 600px;
            margin-bottom: 2rem;
        }

        h1, h2 {
            margin-top: 0;
        }

        .secondary-text {
            color: var(--text-secondary);
            font-size: 0.9rem;
        }

        button {
            background-color: var(--brand-color);
            color: #121212; /* Keep text dark on brand buttons for contrast */
            border: none;
            padding: 10px 20px;
            border-radius: 4px;
            font-weight: 600;
            cursor: pointer;
        }

        /* Image handling for dark mode to reduce glare */
        [data-theme="dark"] img {
            filter: brightness(0.8) contrast(1.2);
        }
    </style>

    <script>
        // Check local storage or system preference immediately
        const savedTheme = localStorage.getItem('theme');
        const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

        if (savedTheme === 'dark' || (!savedTheme && systemPrefersDark)) {
            document.documentElement.setAttribute('data-theme', 'dark');
        }
    </script>
</head>
<body>

    <h1>Robust Dark Mode</h1>

    <div class="card">
        <h2>Surface Level 1</h2>
        <p>This card sits on top of the background. In dark mode, it is slightly lighter (#1e1e1e) than the body (#121212) to create elevation without relying solely on shadows.</p>
        <p class="secondary-text">This is secondary text. It uses opacity rather than a dark grey hex code, ensuring it blends correctly with any background color.</p>
        <br>
        <button id="theme-toggle">Toggle Theme</button>
    </div>

    <script>
        const toggleBtn = document.getElementById('theme-toggle');
        const html = document.documentElement;

        toggleBtn.addEventListener('click', () => {
            const currentTheme = html.getAttribute('data-theme');
            let newTheme = 'light';

            if (currentTheme !== 'dark') {
                newTheme = 'dark';
            }

            html.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
        });
    </script>
</body>
</html>
