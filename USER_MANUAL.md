# Sprillex Restaurant Finder & Android Build Automation Platform - User Manual

## 1. Project Overview

The **Sprillex Restaurant Finder Ecosystem** is a multi-tier Android mobile application and build automation system designed for reliable, offline-first restaurant discovery and centralized mobile application lifecycle management.

### Key Application Features
- **Offline-First Spatial Restaurant Discovery:** Shipped with a pre-populated SQLite/Room database (`regional_restaurants.db`) derived from OpenStreetMap (OSM) spatial data. It enables instant, zero-latency searching, bounding-box filtering, and distance calculations even in offline or dead-zone environments.
- **AI-Powered Grounded Dining Enrichment:** Integrates Google's Gemini API (`gemini-2.5-flash`) with Google Search Grounding to dynamically fetch, deserialize, and cache rich venue profiles (including dietary tags, recommended dishes, price ranges, and current operating hours) directly into local Room database storage.
- **User Data & Favorites Management:** Features personal collection tracking, including favorite venues, custom wishlists, favorite dishes, and dish-level wishlists with zero initial cloud dependency.
- **Data Backup & Safety Strategy:** Built-in `BackupManager` allowing users to export and restore user preferences, favorites, and custom lists via JSON data files.
- **Eye-Friendly Dark Mode Standard:** Implements a `#121212` dark theme standard with elevation-based lightness scaling (`#1E1E1E` cards, `#2D2D2D` modals) and desaturated primary accents to eliminate OLED smearing and reduce eye strain.
- **Zero-Dependency Crash Reporting:** Features an integrated `CrashReporter` that captures unhandled fatal exceptions and broadcasts system-wide `ACTION_SEND` intents containing diagnostic payloads (`EXTRA_CRASH_PAYLOAD`, `EXTRA_STACK_TRACE`).
- **Automated Remote Build API (`android_tools`):** A standalone FastAPI background server and script suite that clones Android repositories, manages Git branches, executes Gradle debug builds, and delivers compiled APKs via HTTP streaming or SMB NAS network shares.

---

## 2. Prerequisites

### Android Mobile App Development & Execution
- **Android Operating System:** Android 8.0 (API Level 26) or higher.
- **Java Development Kit (JDK):** JDK 17 (Temurin or OpenJDK recommended).
- **Android SDK:** Android SDK with Platform 34 or higher and Android Build Tools.
- **Gradle:** Gradle 8.x (managed via included `gradlew` wrapper).
- **Gemini API Key:** Required for AI Grounded Dining search features (obtainable from Google AI Studio).

### Android Build Automation Server (`android_tools`)
- **Host OS:** Linux (Ubuntu 20.04/22.04 LTS or Debian 11/12 recommended).
- **Python Runtime:** Python 3.10+ with `pip` and `venv`.
- **System Tools:** `git`, `curl`, `bash`, `rsync` (for NAS sync), and `systemd`.
- **Port Availability:** TCP Port 8000 (configurable via environment file).

---

## 3. Installation & Setup

### 3.1 Building the Android Mobile App

#### Step 1: Clone the Repository
```bash
git clone https://github.com/sprillex/restaurantfinder.git
cd restaurantfinder
```

#### Step 2: Grant Execution Permissions
Ensure the Gradle wrapper script is executable:
```bash
chmod +x gradlew
```

#### Step 3: Configure `local.properties`
Create or update `local.properties` in the project root to point to your local Android SDK location:
```properties
sdk.dir=/path/to/android/sdk
```

#### Step 4: Compile the Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be output to:
`app/build/outputs/apk/debug/app-debug.apk`

---

### 3.2 Setting Up the Android Build Automation API Server (`android_tools`)

#### Step 1: Navigate to Build Automation Directory
```bash
cd android_tools
```

#### Step 2: Set Up Python Virtual Environment
```bash
python3 -m venv venv
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
```

#### Step 3: Configure Environment Variables
Copy the example environment configuration and update key settings:
```bash
cp builder-api.env.example builder-api.env
```

Edit `builder-api.env`:
```env
BUILDER_API_KEY=your_secure_bearer_token_here
PORT=8000
HOST=0.0.0.0
BUILD_ROOT_DIR=/var/sprillex/builds
SECONDARY_APK_DIR=/mnt/nas/projects
```

#### Step 4: Install Systemd Service (Optional but Recommended)
To run the server continuously in the background:
```bash
sudo cp android-builder.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable android-builder.service
sudo systemctl start android-builder.service
```

---

## 4. Configuration

### 4.1 Gemini API Key Configuration
The application accesses Gemini AI features using two fallback mechanisms:
1. **User Preference (In-App):** Configured via the Settings dialog in the app UI, which stores the key securely in `PreferenceManager`.
2. **Build Configuration Fallback:** Injected at compile time via `BuildConfig.GEMINI_API_KEY` or `local.properties`:
   ```properties
   GEMINI_API_KEY=AIzaSyYourActualGeminiApiKeyHere
   ```

### 4.2 GitHub Actions Workflows Configuration
To enable build status alerts via Pushover for GitHub CI/CD workflows (`android_build.yml`, `branch_pr_alert.yml`), configure the following secrets in your GitHub repository (**Settings > Secrets and variables > Actions**):

- `PUSHOVER_APP_TOKEN`: Your Pushover Application Token.
- `PUSHOVER_USER_KEY`: Your Pushover User Key.

### 4.3 App Design & Dark Mode Standard
All UI elements follow strict dark theme color properties (`ui/theme/Color.kt` and `ui/theme/Theme.kt`):
- Base Background (`Surface`): `#121212`
- Card Surface (`Level 1 Elevation`): `#1E1E1E`
- Dialog/Sheet Surface (`Level 2 Elevation`): `#2D2D2D`
- Primary Accent: Desaturated `#8AB4F8`

---

## 5. Usage Guide

### 5.1 Using the Android Application

1. **Launch App:** Open **Sprillex Restaurant Finder** on your Android device.
2. **Browse & Filter:** Use top category chips (`All`, `Restaurant`, `Fast Food`, `Cafe`, `Bar`, `Pub`) or type keywords in the search bar to filter by cuisine or municipality.
3. **View Detail Bottom Sheet:** Tap on any venue card to view full venue metadata, distance, phone contact, and website link.
4. **AI Dining Insights:** Tap **"AI Dining Profile"** on a venue detail screen to trigger the Gemini AI client. The app fetches grounded web information (recommended dishes, dietary suitability) and caches it locally.
5. **Manage Favorites & Wishlists:** Tap the star icon to add a venue to Favorites, or tap the Bookmark icon to organize into custom Wishlists.
6. **Export/Import Backup:** Access Settings to export your personal data to JSON or restore from an existing JSON backup file.

---

### 5.2 Interacting with the Build Automation REST API (`android_tools`)

All API calls require HTTP Bearer Token authorization.

#### 1. Trigger Repository Clone & Build
```bash
curl -X POST "http://localhost:8000/clone" \
  -H "Authorization: Bearer your_secure_bearer_token_here" \
  -H "Content-Type: application/json" \
  -d '{"repo": "https://github.com/sprillex/restaurantfinder.git"}'
```

#### 2. Trigger Branch Build / Update
```bash
curl -X POST "http://localhost:8000/update" \
  -H "Authorization: Bearer your_secure_bearer_token_here" \
  -H "Content-Type: application/json" \
  -d '{"project": "restaurantfinder", "branch_flag": "-m"}'
```

#### 3. Check Job Status
```bash
curl -X GET "http://localhost:8000/status/<JOB_ID>" \
  -H "Authorization: Bearer your_secure_bearer_token_here"
```

#### 4. List All Compiled APKs
```bash
curl -X GET "http://localhost:8000/projects/restaurantfinder/apks" \
  -H "Authorization: Bearer your_secure_bearer_token_here"
```

#### 5. Download Compiled APK Binary
```bash
curl -O -J "http://localhost:8000/download/restaurantfinder/<APK_FILENAME>" \
  -H "Authorization: Bearer your_secure_bearer_token_here"
```

---

## 6. Architecture & Logic

### 6.1 Android Client Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Jetpack Compose UI                       │
│  (MainActivity, MainScreen, DetailBottomSheet, Component UI) │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
                ▼                             ▼
┌─────────────────────────────┐  ┌────────────────────────────┐
│    RestaurantRepository     │  │       AiDiningClient       │
│  (Data Access Aggregator)   │  │   (Gemini 2.5 Flash API)   │
└───────────────┬─────────────┘  └────────────┬───────────────┘
                │                             │
                ▼                             ▼
┌─────────────────────────────────────────────────────────────┐
│                   Room Database Layer                       │
│  (AppDatabase: Offline OSM DB) / (UserDatabase: Favorites)  │
└─────────────────────────────────────────────────────────────┘
```

- **AppDatabase (`regional_restaurants.db`):** Pre-populated Room database containing OSM restaurant nodes and polygons. Optimized with spatial B-tree indices on `(latitude, longitude)`, `amenity`, `cuisine`, `city`, and `(delivery, takeaway)`.
- **UserDatabase:** Stores user personal data (`Favorite`, `Wishlist`, `FavoriteDish`, `DishWishlist`, `RestaurantDetailEntity`).
- **AiDiningClient:** Sends REST payloads to Gemini 2.5 Flash with Google Search Grounding enabled (`tools: [{"googleSearch": {}}]`). Validates user API keys against `GET /v1beta/models` before requesting structured JSON profiles.

---

### 6.2 Build Automation Architecture (`android_tools`)

```
┌─────────────────────────────────────────────────────────────┐
│                 FastAPI Service (server.py)                 │
│               Port 8000 | Bearer Auth Middle                │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
                ▼                             ▼
┌─────────────────────────────┐  ┌────────────────────────────┐
│       Bash Automation       │  │     Secondary Storage      │
│ (clone_android.sh/update.sh)│  │    (NAS SMB Share Layout)  │
└─────────────────────────────┘  └────────────────────────────┘
```

- **Execution Queue:** Async job runner tracks background tasks via unique Job UUIDs.
- **Log Management:** Streams compilation logs to `/logs/{job_id}` for remote debugging.
- **NAS SMB Synchronization:** Mirrors build outputs to `<SECONDARY_APK_DIR>/projects/<project>/apk/` for direct mobile client indexing.

---

## 7. Troubleshooting

### Common Errors & Solutions

| Error / Issue | Root Cause | Resolution |
| :--- | :--- | :--- |
| **HTTP 429 Too Many Requests** in AI Dining View | Gemini API quota or rate limit exceeded. | The app handles HTTP 429 by failing fast to protect quota. Wait a few minutes or switch to a paid/higher-quota API key in Settings. |
| **"found": false in AI Dining Response** | Venue not indexed or recognized in Google Search grounding. | Handled gracefully as an unlisted venue profile rather than a parsing error. No action needed. |
| **Room Pre-populated Schema Verification Failure** | Index definition or primary key mismatch in Room entity vs `regional_restaurants.db`. | Ensure `Restaurant` entity includes composite primary key `'{osm_type}/{osm_id}'` and explicitly declares all six indices matching SQLite schema. |
| **Build Server HTTP 401 Unauthorized** | Missing or incorrect Bearer token in request header. | Supply the valid API key configured in `BUILDER_API_KEY` via `Authorization: Bearer <KEY>`. |
| **Gradle Build Fails with `sdk.dir` Error** | Missing `local.properties` file in Android project root. | Create `local.properties` containing `sdk.dir=/path/to/android-sdk`. |
| **App Crash on Unhandled Exception** | Uncaught runtime error. | `CrashReporter` captures the stack trace and opens a system share dialog. Share the crash payload with the engineering team for analysis. |

---
