# Sprillex Restaurant Finder (`sherlock_food`) — Operator & Setup Manual

> **Documentation Index:** [ARCHITECTURE.md](ARCHITECTURE.md) (AI Context & Schemas) · [API.md](API.md) (REST, CLI & Kotlin Contracts) · [LIBRARIES.md](LIBRARIES.md) (OSV SBOM) · [USER_MANUAL.md](USER_MANUAL.md) · [roadmap.md](roadmap.md) · [dark_mode.md](dark_mode.md)

---

## 1. Overview & Primary Features

**Sprillex Restaurant Finder** (`com.sprillex.restaurantfinder`) is an offline-first Android Jetpack Compose application for discovering food and drink venues across **Southeast Michigan (Monroe & Lenawee counties)** and **Northwest Ohio (Lucas & Wood counties)**, paired with optional remote build automation (`android_tools/`).

- **Offline OSM Spatial Discovery:** Ships with `app/src/main/assets/regional_restaurants.db` (1,188 OpenStreetMap venues: 522 `restaurant`, 460 `fast_food`, 85 `cafe`, 81 `bar`, 40 `pub`; bounding box `41.2500921..42.0997607`, `-84.4326343..-83.2800916`), indexed on `(latitude, longitude)`, `amenity`, `cuisine`, `city`, `brand`, and `(delivery, takeaway)`.
- **Dual Room Persistence:** Separates read-mostly venue/AI cache data (`AppDatabase`, `regional_restaurants.db`, v3) from personal user data (`UserDatabase`, `user_data.db`, v5) so OTA venue `@Upsert` syncs cannot wipe user records.
- **On-Demand Gemini Enrichment:** Queries Google Gemini (`gemini-2.5-flash` with fallback models) and Google Search Grounding (`tools: [{"googleSearch": {}}]`) to cache editorial summaries, signature items, price tier, vibe tags, service model, parking, and patio availability in `restaurant_details`.
- **Personal Collections & Backup:** Favorites, restaurant wishlists (`Low`/`Medium`/`High` priority + notes), dish wishlists, favorite dishes, and JSON export/restore (`user_data_backup.json`).
- **Location & External Intents:** Fused GPS location (`ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`) or preset municipal anchors (`Toledo, OH`, `Perrysburg, OH`, `Monroe, MI`, `Adrian, MI`), Haversine distance sorting, `geo:` navigation, `tel:` dialer, browser launch, and `ACTION_SEND` sharing.
- **Robust Dark Mode & Crash Reporting:** Enforces `#121212` dark surfaces (`#1E1E1E` cards, `#2D2D2D` modals, `#8AB4F8` accent) and installs `CrashReporter` in `RestaurantFinderApp.onCreate()`.

---

## 2. Prerequisites

| Component | Requirement (Verified from Build Config) |
| :--- | :--- |
| **Target Build Host OS** | Linux Mint / Ubuntu 20.04+ / Debian 11+ (x86_64) |
| **JDK** | OpenJDK / Temurin **JDK 17** (`sourceCompatibility = VERSION_17`, `jvmTarget = "17"`) |
| **Android SDK** | `compileSdk = 34`, `targetSdk = 34`, `minSdk = 24`, Android SDK Build-Tools 34.x, Platform-Tools (`adb`) |
| **Gradle / AGP / Kotlin** | Gradle `8.8` (`./gradlew`), AGP `8.3.1`, Kotlin `1.9.22`, Compose Compiler `1.5.8` |
| **Runtime CLI Binaries** | `git`, `bash`, `curl`, `adb`, `./gradlew` |
| **Gemini API Key** | Optional for offline browsing; required for **AI Dining Insights** (Google AI Studio key) |
| **Build Server (`android_tools/`, optional)** | Python `3.9+` (`fastapi>=0.100.0`, `uvicorn>=0.22.0`, `pydantic>=2.0.0`), `systemd` |

---

## 3. Installation & Setup

### 3.1 Host Prerequisites (Linux Mint / Ubuntu, Non-Interactive)
```bash
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jdk git curl unzip adb sqlite3
```

Ensure `ANDROID_HOME` is configured (or install Android command-line tools and accept SDK licenses non-interactively):
```bash
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 3.2 Configure `local.properties` & Build Debug APK
```bash
chmod +x gradlew
printf "sdk.dir=%s\n" "${ANDROID_HOME:-$HOME/Android/Sdk}" > local.properties
# Optional compile-time Gemini API key injection:
# printf "GEMINI_API_KEY=AIzaSyYourKeyHere\n" >> local.properties

./gradlew assembleDebug --no-daemon
```
Output APK path: `app/build/outputs/apk/debug/app-debug.apk`.

### 3.3 Automated Build via `universal_update_manager.sh` or `update.sh`
```bash
# Non-interactive build in current repo wrapper:
./update.sh -m

# Multi-project host manager (non-interactive directory scan):
TARGET_DIR="$HOME/sprillex/android" ./universal_update_manager.sh sherlock_food -y -m
```
For setting up the optional FastAPI build server (`android_tools/server.py`), see [android_tools/install.md](android_tools/install.md).

---

## 4. Configuration

| Source | Key / Variable | Default / Value | Purpose |
| :--- | :--- | :--- | :--- |
| `local.properties` | `sdk.dir` | *(required locally)* | Path to Android SDK for Gradle |
| `local.properties` / `-P` / Env | `GEMINI_API_KEY` | `""` | Injected into `BuildConfig.GEMINI_API_KEY` at compile time |
| `gradle.properties` | `org.gradle.jvmargs` | `-Xmx2048m -Dfile.encoding=UTF-8` | Gradle daemon JVM heap & UTF-8 encoding |
| `gradle.properties` | `android.useAndroidX` | `true` | Enables AndroidX artifacts |
| `gradle.properties` | `android.nonTransitiveRclass` | `true` | Scopes R classes per module |
| `SharedPreferences` (`restaurant_finder_prefs`) | `key_anchor_index` | `0` (`Current Location (GPS)`) | Persisted index into `LocationManager.PRESET_ANCHORS` |
| `SharedPreferences` (`restaurant_finder_prefs`) | `key_gemini_api_key` | `""` | In-app Gemini API key override (takes precedence over `BuildConfig`) |
| App Assets | `app/src/main/assets/regional_restaurants.db` | Bundled v3 SQLite (`1,188` rows) | Pre-populated asset copied by `AppDatabase` (`createFromAsset`) |
| App Files Dir | `context.filesDir/user_data_backup.json` | Created on export | JSON backup read/written by `BackupManager` |
| `android_tools` Env | `API_KEY`, `API_HOST`, `API_PORT` | `CHANGE_ME...`, `0.0.0.0`, `8080` | FastAPI Bearer auth & Uvicorn listen address |
| `android_tools` Env | `SPRILLEX_ANDROID_HOME`, `ANDROID_TOOLS_DIR`, `SECONDARY_APK_DIR`, `SKIP_ADB_INSTALL` | `~/sprillex/android`, `/opt/julebot/bin`, `""`, `"true"` | Build workspace, script dir, NAS mirror, ADB install toggle |

---

## 5. Usage Guide

### 5.1 Build, Test & ADB Commands
```bash
# 1. Run all JVM unit tests
./gradlew test --no-daemon
# Expected: BUILD SUCCESSFUL (executes AiDiningClientTest, DistanceCalculatorTest, etc.)

# 2. Assemble debug APK
./gradlew assembleDebug --no-daemon
# Expected: app/build/outputs/apk/debug/app-debug.apk created; BUILD SUCCESSFUL

# 3. Verify connected Android device & install
adb devices
# Expected: <serial>    device
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Expected: Performing Streamed Install
#           Success

# 4. Launch app and tail logs
adb shell am start -n com.sprillex.restaurantfinder/.MainActivity
# Expected: Starting: Intent { cmp=com.sprillex.restaurantfinder/.MainActivity }
adb logcat -s AiDiningClient:D AndroidRuntime:E
```

### 5.2 App Navigation Summary
1. **Anchor Selection:** Tap the pin icon in the top bar (`Near Current Location (GPS)`) to switch between live GPS and preset cities (`Toledo, OH`, `Perrysburg, OH`, `Monroe, MI`, `Adrian, MI`).
2. **Search & Filter:** Filter by name, cuisine, or city in the search field; tap category chips (`All`, `Favorites`, `Wishlist`, `Restaurant`, `Fast food`, `Cafe`, `Bar`, `Pub`). Results auto-sort by Haversine distance ascending.
3. **Venue Card & Detail Sheet:** Tap favorite/bookmark icons or quick actions (`Navigate`, `Call`, `Website`, `Share`) directly on a card, or tap the card to open `DetailBottomSheet` (triggers on-demand Gemini enrichment if uncached, displays OSM attributes, **Favorite Menu Items**, and **Dishes I Want to Try**).
4. **Settings & Backup:** Tap the gear icon to validate (`GET /v1beta/models`) & save a Gemini API key, or **Export** / **Restore** `user_data_backup.json`.

---

## 6. Troubleshooting

| Error / HTTP Code | Root Cause | Fix |
| :--- | :--- | :--- |
| `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local.properties` | Missing `local.properties` or unset `ANDROID_HOME`. | Run `printf "sdk.dir=%s\n" "$ANDROID_HOME" > local.properties`. |
| `Pre-packaged database has an invalid schema: restaurants(...)` | `Restaurant.kt` entity columns/indices diverged from `app/src/main/assets/regional_restaurants.db`. | Keep `id: String` (`osm_type/osm_id`), all 27 columns, and all 6 `idx_restaurants_*` indices aligned; increment `AppDatabase` version only with matching asset DB. |
| `Gemini API quota exceeded (HTTP 429). Failing fast` | Free-tier/project Gemini RPM or RPD quota exhausted. | Client intentionally fails fast without cycling fallback models. Wait for quota reset or save a higher-tier key in **Settings**. |
| `Gemini API service unavailable (HTTP 503)` | Transient upstream model capacity/overload. | Client automatically retries remaining entries in `CANDIDATE_ENDPOINTS` (`gemini-2.5-flash` -> `gemini-flash-latest` -> `gemini-2.5-pro` -> `gemini-2.5-flash-lite` -> `gemini-3.5-flash`). |
| `"found": false` / `"No additional web dining notes found for this venue."` | Google Search Grounding could not verify the specific MI/OH venue. | Expected fallback cached in `restaurant_details` to prevent repeated API calls. |
| `Validation failed (HTTP 400)` / `HTTP 403` in Settings | Invalid or restricted Gemini API key on `GET /v1beta/models`. | Generate/unrestrict a Generative Language API key in Google AI Studio and re-verify. |
| `HTTP 401 Invalid or missing Authorization header` (`android_tools`) | Request to `server.py` lacks matching `Authorization: Bearer <API_KEY>`. | Set `API_KEY` in `/opt/builder-api/.env` and pass matching Bearer header. |
| System Share Sheet opens on crash (`=== APP CRASH REPORT ===`) | Uncaught exception intercepted by `CrashReporter`. | Share or inspect `EXTRA_STACK_TRACE` payload; check `adb logcat -s AndroidRuntime:E`. |
