# Android Restaurant Finder App - Comprehensive Technical Roadmap

This roadmap outlines the complete architecture, UI design standards, location mechanics, data management, CI/CD integration, and testing strategy for building the offline-first Android restaurant finder application.

---

## Phase 1: Local Data Layer & Asset Ingestion

1. **[x] Room Entity & Asset Pre-population**
   - Define `@Entity(tableName = "restaurants")` matching the pre-built `regional_restaurants.db` schema:
     - `id`: `Long` (`@PrimaryKey`, OSM stable 64-bit ID)
     - `name`: `String`
     - `amenity`: `String` (e.g., `restaurant`, `fast_food`, `cafe`, `bar`, `pub`)
     - `cuisine`: `String?`
     - `street`: `String?`
     - `housenumber`: `String?`
     - `postcode`: `String?`
     - `city`: `String?`
     - `phone`: `String?`
     - `website`: `String?`
     - `latitude`: `Double`
     - `longitude`: `Double`
     - `last_updated`: `Long`
   - Configure Room Database builder to pre-populate from asset: `createFromAsset("regional_restaurants.db")`.

2. **[x] DAO Query Design & Spatial Indexing**
   - Leverage SQLite B-tree indices (`index_restaurants_latitude_longitude`, `index_restaurants_amenity`, `index_restaurants_cuisine`, `index_restaurants_city`).
   - Implement bounding-box queries for map and proximity searches:
     `WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng`
   - Dynamic category/amenity filters (`WHERE amenity IN (:amenityList)`).
   - Text search query across `name`, `cuisine`, and `city` fields using SQLite `LIKE`.

3. **[x] User Data Separation**
   - Maintain user state (e.g., `user_favorites`, `visited_logs`) in separate Room tables referencing `restaurant_id` as a foreign key.
   - Ensures OTA database updates using `@Upsert` on `restaurants` will never overwrite or erase user personal data.

---

## Phase 2: Location Engine & Geographic Calculation

1. **[x] Device Proximity & Location Provider**
   - Implement runtime permission handler for `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`.
   - Utilize FusedLocationProviderClient to obtain last-known/single-fix device location on launch (avoiding continuous battery-draining GPS polling).

2. **[x] Manual Anchor Point & Area Override**
   - Support manual municipality selection (e.g., Toledo, Perrysburg, Monroe, Adrian) or custom map pin anchor coordinates.
   - Active location state toggle: "Use GPS Location" vs. "Use Selected Anchor Area".

3. **[x] Client-Side Distance Calculation & Sorting**
   - Calculate straight-line distance using Haversine formula or `Location.distanceBetween` from active anchor point to candidate venues.
   - Format and expose formatted distance strings (e.g., "0.4 mi", "1.2 mi", "850 ft") in the UI.
   - Enable ascending distance sorting for candidate feeds.

---

## Phase 3: Core UI Architecture & Robust Dark Mode Standards

1. **[x] Screen Layouts**
   - **Main Directory Screen:**
     - Top App Bar with active anchor indicator ("Near Current Location" or "Near Adrian, MI") and area override picker.
     - Search bar for quick text filtering by venue name or cuisine tag.
     - Dynamic filter chips (Sit-Down, Fast Food, Cafe, Bar/Pub) and distance radius slider (2 mi, 5 mi, 10 mi, 25 mi).
     - LazyColumn / RecyclerView showing restaurant cards with distance, cuisine badge, and quick actions.
   - **Restaurant Detail View (or Bottom Sheet):**
     - Complete venue address, category, cuisine tags, phone number, and website URL.
     - Quick action bar: Navigate (Map Intent), Call (Dialer Intent), Visit Web (Browser Intent).

2. **[x] Robust Dark Mode Implementation (Ecosystem Mandate)**
   - **Base Palette:**
     - Pure black (`#000000`) is strictly avoided to prevent OLED smearing and visual halation.
     - Base background (`--bg-body`): `#121212`.
   - **Elevation Hierarchy:**
     - Level 0 (Body Background): `#121212`
     - Level 1 (Cards, Containers): `#1e1e1e`
     - Level 2 (Modals, Dialogs, Bottom Sheets): `#2d2d2d`
   - **Typography & Opacity:**
     - High Emphasis Text: `rgba(255, 255, 255, 0.87)`
     - Medium Emphasis Text: `rgba(255, 255, 255, 0.60)`
     - Disabled Text: `rgba(255, 255, 255, 0.38)`
     - Pure white (`#FFFFFF`) on dark background is avoided to eliminate visual vibration.
   - **Desaturated Accent Colors:**
     - Lightened, desaturated accent palette for dark mode (e.g., Pastel Blue `#8AB4F8` instead of Deep Blue `#0055FF`) to pass WCAG AAA/AA contrast checks.

---

## Phase 4: System Integrations & External Launchers

1. **[x] External Navigation Intent**
   - Tap on "Navigate" or restaurant map pin dispatches an implicit `geo:` intent or Universal Navigation URI:
     `geo:0,0?q=latitude,longitude(Restaurant+Name)`
   - Hands off turn-by-turn routing directly to the user's default navigation application (Google Maps, OsmAnd, Organic Maps).

2. **[x] Browser & Contact Intents**
   - Web browser launcher using `Intent.ACTION_VIEW` for venues with web URLs.
   - Dialer launcher using `Intent.ACTION_DIAL` (`tel:$phone`) for venues with listed phone numbers.

---

## Phase 5: Preferences Persistence & Data Safety

1. **[x] User Preference Persistence**
   - Store filter states (e.g., excluding Fast Food, active search radius) using Jetpack DataStore.
   - Remember last selected manual location override when GPS is disabled or inactive.

2. **[x] Data Safety & Backup Strategy (Ecosystem Mandate)**
   - Implement an automated export/backup and restore procedure for app settings, user preferences, and user database tables (`user_favorites`, notes).
   - Ensure backups are outputted in a portable format (JSON / SQLite export) for easy restoration across app reinstalls or updates.

---

## Phase 6: Sync Pipeline, Build Automation & CI/CD Integration

1. **[x] OTA Background Refresh / Sync Engine**
   - Background update task (WorkManager or manual "Check for Updates" action in settings).
   - Fetches updated Overpass bounding-box JSON data.
   - Performs batch `@Upsert` into Room database without disturbing user favorites or preferences.

2. **[x] CI/CD Pipeline (`android_build.yml`)**
   - Configured GitHub Actions workflow triggering on non-main branches:
     - Sets up JDK 17 environment (Temurin distribution).
     - Resolves `local.properties` SDK directory.
     - Executes `./gradlew assembleDebug --no-daemon`.
     - Renames output APK using run number (`<repo>-build-<run_number>.apk`).
     - Uploads debug APK and build log artifacts.
     - Triggers Pushover notifications (`PUSHOVER_APP_TOKEN` and `PUSHOVER_USER_KEY`) with build status and artifact download links.

3. **[x] Sprillex Tools & Universal Update Manager Compatibility**
   - Maintenance scripts generated via `clone_android.sh` and `init_repo.sh`.
   - Root `update.sh` wrapper integration compatible with `universal_update_manager.sh` (`-m` / `--main`, `-n` / `--newest` flags supported).

---

## Phase 7: Testing, Verification & Pre-Release Quality Assurance

1. **[x] DAO & Database Verification**
   - Unit tests verifying Room pre-population from asset database `regional_restaurants.db`.
   - Spatial bounding-box query verification and multi-field keyword search tests.

2. **[x] Distance & Location Unit Tests**
   - Unit tests for Haversine distance calculations and candidate sorting algorithms.

3. **[x] UI & Dark Mode Compliance Audit**
   - Verify dark theme contrast ratios and elevation levels (`#121212`, `#1e1e1e`, `#2d2d2d`).
   - Validate intent handling for navigation, web, and dialer actions.

---

## Phase 8: Future Feature Pipeline & AI Integration

1. **[x] Restaurants Wishlist ("Restaurants I Want to Try")**
   - Support marking venues as "Want to Try" with custom notes and priority tags.

2. **[x] Social Sharing (System Share Sheet)**
   - Dispatch Android `ACTION_SEND` intents with formatted text and location links for venue sharing.

3. **[ ] Dynamic Location Awareness & Proximity Sorting**
   - Continuous device GPS position monitoring and real-time auto-sorting by distance.

4. **[ ] AI Integration**
   - Intelligent natural language venue search and personalized dining recommendations.

5. **[ ] Favorite Menu Items**
   - Saved personal list of favorite dishes associated with specific restaurants.

6. **[ ] Dish Wishlist ("Items I Want to Try")**
   - Track specific menu items the user wants to order on future visits.

7. **[ ] Smart Menu Integration**
   - Digital menu parsing, OCR, and highlighting popular/dietary-compliant menu items.
