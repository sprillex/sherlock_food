# `sherlock_food` Architecture Manifest (AI Context Spec)

`com.sprillex.restaurantfinder` | compile/targetSdk `34`, minSdk `24`, JVM `17` | Asset DB: `app/src/main/assets/regional_restaurants.db` (1,188 OSM venues, SE MI & NW OH).

## 1. Repository Map & Import Hierarchy

```text
build.gradle.kts / settings.gradle.kts / gradle.properties / gradle/libs.versions.toml # AGP 8.3.1, Kotlin 1.9.22, Compose BOM 2024.02.02, Room 2.6.1
app/build.gradle.kts # BuildConfig.GEMINI_API_KEY injection, Compose compiler 1.5.8, kapt
app/src/main/AndroidManifest.xml # FINE/COARSE_LOCATION, INTERNET; RestaurantFinderApp, MainActivity
app/src/main/assets/regional_restaurants.db # Pre-populated v3 SQLite (1,188 rows, 27 cols, 6 idx_restaurants_* indices)
app/src/main/java/com/sprillex/restaurantfinder/
  RestaurantFinderApp.kt # Application; calls CrashReporter.install(this) before super.onCreate()
  MainActivity.kt # Compose host, Room Flow collectors, FusedLocationProviderClient, dialog/sheet state
  data/
    AppDatabase.kt # Room v3 ("regional_restaurants.db", createFromAsset, StringListConverters)
    UserDatabase.kt # Room v5 ("user_data.db", isolated personal tables)
    Restaurant.kt / RestaurantDao.kt # @Entity("restaurants", 27 cols, 6 indices) + Flow/boundingBox/search/@Upsert DAO
    RestaurantDetailEntity.kt / RestaurantDetailDao.kt # @Entity("restaurant_details", FK->restaurants) + @Transaction DAO
    RestaurantWithDetails.kt # @Embedded Restaurant + @Relation RestaurantDetailEntity?
    StringListConverters.kt # @TypeConverter List<String>? <-> JSON via kotlinx.serialization
    Favorite.kt / FavoriteDao.kt # @Entity("user_favorites") PK restaurantId
    Wishlist.kt / WishlistDao.kt # @Entity("user_wishlist") PK restaurantId, notes, priority ("Low"|"Medium"|"High")
    DishWishlist.kt / DishWishlistDao.kt # @Entity("dish_wishlist") autoGenerate id, restaurantId, dishName, notes
    FavoriteDish.kt / FavoriteDishDao.kt # @Entity("favorite_dishes") autoGenerate id, restaurantId, dishName, notes
    PreferenceManager.kt # SharedPreferences("restaurant_finder_prefs"): key_anchor_index, key_gemini_api_key
    BackupManager.kt # JSON export/restore to context.filesDir/user_data_backup.json (regex parser)
    remote/AiDiningClient.kt # HttpURLConnection Gemini client (key check + generateContent fallback chain)
    remote/AiDiningResponse.kt # @Serializable DTO for Gemini JSON profile
    repository/RestaurantRepository.kt # Cache-first coordinator (RestaurantDetailDao + AiDiningClient)
  diagnostics/CrashReporter.kt # UncaughtExceptionHandler -> ACTION_SEND chooser (EXTRA_CRASH_PAYLOAD)
  location/LocationManager.kt # AnchorLocation + PRESET_ANCHORS (GPS, Toledo, Perrysburg, Monroe, Adrian)
  location/DistanceCalculator.kt # Haversine miles (R=3958.8) & formatDistance ("528 ft", "2.3 mi", "16 mi")
  utils/IntentHelper.kt # geo:0,0?q=... nav, tel: dialer, https:// browser, ACTION_SEND share
  ui/theme/Color.kt & Theme.kt # DarkColorScheme (#121212 bg, #1E1E1E L1, #2D2D2D L2, #8AB4F8 accent)
  ui/screens/MainScreen.kt # TopAppBar anchor menu, search bar, category FilterChips, sortAndFilterRestaurants()
  ui/screens/DetailBottomSheet.kt # ModalBottomSheet, auto-enrich LaunchedEffect, badges, AI card, dishes, actions
  ui/components/RestaurantCard.kt # L1 card, distance, fav/wishlist toggles, dish chips, quick actions
  ui/components/SettingsDialog.kt # API key Verify & Save + Export/Restore buttons
  ui/components/WishlistDialog.kt # Priority FilterChips + notes + remove
  ui/components/AddDishDialog.kt & AddFavoriteDishDialog.kt # DishWishlist / FavoriteDish creation dialogs
app/src/test/java/com/sprillex/restaurantfinder/ # JUnit 4 unit tests (data, remote, repository, diagnostics, location, utils)
test_results/venue_*.json # 20 captured Gemini API responses (success, found=false, 429) for unit tests
android_tools/ # FastAPI build server (server.py), systemd unit, clone_android.sh
universal_update_manager.sh / update.sh / init_repo.sh # Sprillex multi-repo build & subtree scripts
```

**Import Rules:** `MainActivity` (`lifecycleScope`) -> `RestaurantRepository` -> (`RestaurantDetailDao`, `AiDiningClient`). `ui.*` composables are stateless/callback-driven and MUST NOT import Room DAOs, `AiDiningClient`, or `java.net.*`. There is **no** `ViewModel` class; `MainActivity` owns Compose state and collects Room `Flow`s.

---

## 2. Database Schema & State Machines

### `AppDatabase` (`regional_restaurants.db`, Room `version = 3`, `exportSchema = false`)
Asset DB: 1,188 rows (522 `restaurant`, 460 `fast_food`, 85 `cafe`, 81 `bar`, 40 `pub`; bounds `41.2500921..42.0997607`, `-84.4326343..-83.2800916`). Legacy root `regional_restaurants.db` (13 cols, `INTEGER PK`) is unused; `regional_restaurants2.db` matches v3.

```sql
CREATE TABLE restaurants (
  id TEXT NOT NULL PRIMARY KEY,           -- "{osm_type}/{osm_id}", e.g. "node/30455276"
  osm_type TEXT NOT NULL, osm_id INTEGER NOT NULL, name TEXT NOT NULL, amenity TEXT NOT NULL,
  cuisine TEXT, street TEXT, housenumber TEXT, postcode TEXT, city TEXT,
  phone TEXT, website TEXT, opening_hours TEXT, takeaway TEXT, delivery TEXT,
  outdoor_seating TEXT, drive_through TEXT, wheelchair TEXT,
  brand TEXT, brand_wikidata TEXT, wikidata TEXT,
  diet_vegetarian TEXT, diet_vegan TEXT, diet_gluten_free TEXT,
  latitude REAL NOT NULL, longitude REAL NOT NULL, last_updated INTEGER NOT NULL
);
CREATE INDEX idx_restaurants_lat_lon ON restaurants (latitude, longitude);
CREATE INDEX idx_restaurants_amenity ON restaurants (amenity);
CREATE INDEX idx_restaurants_cuisine ON restaurants (cuisine);
CREATE INDEX idx_restaurants_city ON restaurants (city);
CREATE INDEX idx_restaurants_brand ON restaurants (brand);
CREATE INDEX idx_restaurants_delivery_takeaway ON restaurants (delivery, takeaway);

CREATE TABLE restaurant_details (
  restaurant_id TEXT NOT NULL PRIMARY KEY,
  editorial_summary TEXT, signature_items TEXT, -- List<String>? JSON via StringListConverters
  price_tier TEXT, vibe_tags TEXT,              -- vibe_tags: List<String>? JSON
  service_model TEXT, parking TEXT, outdoor_patio TEXT, good_to_know TEXT,
  fetched_at INTEGER NOT NULL,
  FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
);
```

### `UserDatabase` (`user_data.db`, Room `version = 5`, `exportSchema = false`)
```sql
CREATE TABLE user_favorites (restaurantId TEXT NOT NULL PRIMARY KEY, addedAt INTEGER NOT NULL);
CREATE TABLE user_wishlist (restaurantId TEXT NOT NULL PRIMARY KEY, notes TEXT NOT NULL, priority TEXT NOT NULL, addedAt INTEGER NOT NULL);
CREATE TABLE dish_wishlist (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, restaurantId TEXT NOT NULL, dishName TEXT NOT NULL, notes TEXT NOT NULL, addedAt INTEGER NOT NULL);
CREATE TABLE favorite_dishes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, restaurantId TEXT NOT NULL, dishName TEXT NOT NULL, notes TEXT NOT NULL, addedAt INTEGER NOT NULL);
```

### Venue Search & Filtering (`MainScreen.sortAndFilterRestaurants`)
| State Input | Transition / Rule | Output |
| :--- | :--- | :--- |
| `searchQuery` (`""`) | Substring match (`ignoreCase = true`) | Matches `name`, `cuisine`, or `city` |
| `selectedAmenity` (`null`) | `"All"`/`null` -> all; `"Favorites"` -> `id in favoriteIds`; `"Wishlist"` -> `id in wishlistMap` | Else `amenity.equals(selectedAmenity, true)` |
| `effectiveAnchor` | `if (selectedAnchor.isGps && gpsLocationState != null) gpsLocationState!! else selectedAnchor` | Haversine distance computed & sorted ascending |

### Gemini Enrichment State Machine (`DetailBottomSheet` -> `RestaurantRepository` -> `AiDiningClient`)
```text
Sheet open (LaunchedEffect(restaurant.id)) -> if (details == null && !isLoadingEnrichment):
  detailDao.getCachedDetails(id)
    ├─ Hit  -> return (Flow<RestaurantWithDetails?> already renders cached entity)
    └─ Miss -> resolve key (overrideApiKey.trim() ?: BuildConfig.GEMINI_API_KEY.trim())
                ├─ Blank -> logW, return null (no cache write)
                └─ Set   -> loop CANDIDATE_ENDPOINTS (POST, 10s connect/read timeout):
                             ├─ HTTP 2xx -> parseGeminiResponseText -> sanitizeJsonString -> decode AiDiningResponse
                             │    ├─ found=true  -> upsertDetails(editorialSummary) -> Flow auto-updates UI
                             │    └─ found=false -> upsertDetails("No additional web dining notes found for this venue.")
                             ├─ HTTP 429 -> FAIL FAST: return null immediately (stops fallback loop, no cache write)
                             └─ HTTP 503 / other / Exception -> try next endpoint; all fail -> return null
```

---

## 3. UI, Entrypoints & Config Injection

- **App & Activity:** `RestaurantFinderApp.onCreate()` installs `CrashReporter` before `super.onCreate()`. `MainActivity` (`ACTION_MAIN`/`LAUNCHER`, no deep links) requests `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`, fetches `lastLocation`, and subscribes to `LocationRequest` (`PRIORITY_BALANCED_POWER_ACCURACY`, 10s/5s, removed in `onDestroy()`).
- **Reactive State:** `MainActivity` collects `getAllRestaurants()`, `getAllFavorites()`, `getAllWishlistItems()`, `getAllDishWishlistItems()`, and `getAllFavoriteDishes()` via `collectAsState(emptyList())`; `DetailBottomSheet` collects `restaurantRepository.observeRestaurant(id)`. Modals use local `mutableStateOf` (`isSettingsOpen`, `selectedRestaurantForDetail`, `selectedRestaurantForWishlist`, `selectedRestaurantForAddDish`, `selectedRestaurantForAddFavDish`).
- **Config / Key Resolution:** Compile-time `BuildConfig.GEMINI_API_KEY` (`local.properties`/Gradle `-PGEMINI_API_KEY` -> env `GEMINI_API_KEY` -> `""`). Runtime override in `PreferenceManager` (`"restaurant_finder_prefs"`, `"key_gemini_api_key"`), verified via `AiDiningClient.validateApiKey()` (`GET .../v1beta/models?key=...`).

---

## 4. Testing & Mocking Conventions

Tests under `app/src/test/` are **pure JVM JUnit 4** (no Robolectric or `androidTest` suites yet):
- **DAO Fakes:** `FakeDetailDao` (`mutableMapOf` + `flowOf`) implements `RestaurantDetailDao` (`RestaurantRepositoryTest`).
- **Context/Prefs Fakes:** `InMemorySharedPreferences` + `TestContext : ContextWrapper(null)` (`PreferenceManagerTest`).
- **Offline Network Guard:** `AiDiningClient(apiKey = "")` short-circuits before `HttpURLConnection`; `logW`/`logE` catch `android.util.Log` stub errors (`AiDiningClientTest`).
- **Golden Fixtures:** `AiDiningClientTest.testParseAllTestResultsFixtures` parses `test_results/*.json` (`632337651` -> `found=false`; `429`/`error` -> `null` text).

## 5. Engineering Invariants (Do-Not-Break Rules)

1. **Room Schema Parity:** `Restaurant` columns, types, `NOT NULL` defaults (`osm_type=""`, `osm_id=0`, `last_updated=0`), `id` (`"{osm_type}/{osm_id}"`), and all 6 `idx_restaurants_*` index names MUST match `app/src/main/assets/regional_restaurants.db` or `createFromAsset` crashes at launch.
2. **DB Separation:** Keep `UserDatabase` (`user_data.db`) free of FKs to `AppDatabase` so `@Upsert` refreshes never wipe user data.
3. **Rate-Limit Fail-Fast & Negative Cache:** HTTP `429` MUST abort `CANDIDATE_ENDPOINTS` immediately and return `null`; `found: false` MUST cache `"No additional web dining notes found for this venue."`.
4. **Dark Mode (`dark_mode.md`):** Never use `#000000` or `#FFFFFF`; use `#121212` (`DarkBgBody`), `#1E1E1E` (`DarkSurfaceLevel1`), `#2D2D2D` (`DarkSurfaceLevel2`), `0xDEFFFFFF` (`TextPrimary`), `0x99FFFFFF` (`TextSecondary`), `#8AB4F8` (`BrandPastelBlue`).
5. **CrashReporter Ordering:** `CrashReporter.install(this)` MUST run before `super.onCreate()` in `RestaurantFinderApp`.
6. **Backup Regex Contract:** `BackupManager.restoreUserData` uses positional regexes; keep JSON key order/escaping in `exportUserData` synchronized with `restoreUserData`.
