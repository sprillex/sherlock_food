# `sherlock_food` API, CLI & Module Interface Reference

> See [ARCHITECTURE.md](ARCHITECTURE.md) for data flows/schemas and [README.md](README.md) for setup.

---

## 1. External REST Endpoints

### 1.1 Google Gemini API (`AiDiningClient.kt`)
- **Auth:** Query parameter `?key={apiKey}` (`PreferenceManager.getGeminiApiKey()` trimmed, else `BuildConfig.GEMINI_API_KEY`). Blank key aborts immediately (`null` / `Result.failure`).

#### Validate API Key
- **Request:** `GET https://generativelanguage.googleapis.com/v1beta/models?key={trimmedKey}` (connect/read timeout: `8000` ms)
- **Response:** HTTP `200..299` -> `Result.success(true)`; non-2xx -> `Result.failure(Exception("Validation failed (HTTP $responseCode)"))`

#### Fetch Grounded Dining Profile
- **Endpoint Fallback Chain (`CANDIDATE_ENDPOINTS`, tried in order, `10000` ms connect/read timeout):**
  1. `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent`
  2. `https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent`
  3. `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent`
  4. `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent`
  5. `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent`
- **Method & Headers:** `POST`, `Content-Type: application/json; charset=UTF-8`
- **Request Schema:**
```ts
interface GeminiRequest {
  systemInstruction: { parts: [{ text: string }] }; // AiDiningClient.SYSTEM_INSTRUCTION
  contents: [{ parts: [{ text: string }] }];         // AiDiningClient.buildUserPrompt(restaurant)
  tools: [{ googleSearch: {} }];
}
```
- **Extracted Inner JSON Schema (`AiDiningResponse`, `@Serializable`):**
```kotlin
data class AiDiningResponse(
    val found: Boolean,                  // @SerialName("found")
    val editorialSummary: String? = null,// @SerialName("editorial_summary")
    val signatureItems: List<String>? = null, // @SerialName("signature_items")
    val priceTier: String? = null,       // "$" | "$$" | "$$$" | "$$$$" | "unknown"
    val vibeTags: List<String>? = null,  // @SerialName("vibe_tags")
    val serviceModel: String? = null,    // "counter_service" | "full_table_service" | "bar_only" | "food_truck" | "unknown"
    val parking: String? = null,         // "dedicated_lot" | "street_only" | "none" | "unknown"
    val outdoorPatio: String? = null,    // "yes" | "no" | "unknown"
    val goodToKnow: String? = null       // @SerialName("good_to_know")
)
```
- **Rate-Limit & Fallback Semantics:**
  - **HTTP `200..299`:** Reads `candidates[0..n].content.parts[0..n].text`, strips prose/markdown fences via `sanitizeJsonString()`, decodes `AiDiningResponse`. Returns `null` if JSON decoding fails.
  - **HTTP `429`:** **Fail-fast** — logs error and immediately returns `null` without trying remaining candidate endpoints.
  - **HTTP `503` / other non-2xx / `Exception`:** Logs warning/error and advances to next endpoint in `CANDIDATE_ENDPOINTS`.
  - **`found == false`:** `RestaurantRepository.ensureDetailsEnriched` persists a `RestaurantDetailEntity` row with `editorialSummary = "No additional web dining notes found for this venue."` to avoid repeat queries.

### 1.2 Build Automation Server (`android_tools/server.py`)
- **Auth:** `Authorization: Bearer <API_KEY>` (constant-time `secrets.compare_digest`; missing/invalid returns HTTP `401`). Timeout per job: `900` s; output truncated to last `4000` chars.

| Method & Path | Request / Params | Response / Status |
| :--- | :--- | :--- |
| `POST /clone` | `{"repo": "<git-url>"}` | `202` `{"job_id", "status": "queued", "project"}`; `400` invalid repo/name |
| `POST /update` | `{"project": "<name>", "branch_flag": "-m" \| "-n" \| "<branch>"}` | `202` `{"job_id", "status": "queued", "project"}`; `400` bad input; `404` missing `update.sh` |
| `GET /status/{job_id}` | Path `job_id` | `200` `{"job_id", "status": "running"\|"success"\|"failed"\|"error", "project", "apk_filename"?, "nas_path"?, "download_url"?, "output"}`; `404` |
| `GET /logs/{job_id}` | Path `job_id` | `200` `text/plain` job output; `404` |
| `GET /projects` | — | `200` `{"projects": [{"name", "has_builds", "latest_apk"}]}` |
| `GET /projects/{project}/apks` | Path `project` | `200` `{"project", "apks": [{"filename", "size_bytes", "created_at", "nas_relative_path", "download_url"}]}`; `400`/`404` |
| `GET /download/{project}/{filename}` | Path `project`, `filename` | `200` `application/vnd.android.package-archive` (`FileResponse`); `400`/`404` |

---

## 2. CLI Interfaces & Build Scripts

| Script / Binary | Flags & Arguments | Defaults / Env Overrides | Exit Codes |
| :--- | :--- | :--- | :--- |
| `./gradlew` | `assembleDebug`, `test`, `clean`, `--no-daemon`, `--no-build-cache`, `--stacktrace`, `-PGEMINI_API_KEY=...` | Gradle `8.8`, JVM `-Xmx2048m`; reads `local.properties` | `0` success; non-zero on build/test failure |
| `./update.sh` | `-m`\|`--main` (checkout/pull `main`), `-n`\|`--newest` (newest remote branch), `-u`\|`--update` (`git fetch origin`) | Builds current checked-out branch with `./gradlew assembleDebug --no-daemon` | `0` success; `set -e -o pipefail` propagates git/gradle errors |
| `./universal_update_manager.sh` | `[FOLDER_NAME]`, `-u`\|`--update`, `-m`\|`--main`, `-n`\|`--newest`, `-c`\|`--settings`, `-s`\|`--status`, `-l`\|`--logs`, `-r`\|`--restart`, `-y`\|`--yes`, `-h`\|`--help` | `TARGET_DIR="${TARGET_DIR:-$HOME}"` (scans maxdepth 3 for `update.sh`) | `0` help/success; `1` unknown flag, missing `TARGET_DIR`, or no `update.sh` found |
| `./init_repo.sh` | `<PROJECT_NAME \| GITHUB_URL>`, `-t`\|`--type` (`python`\|`docker`\|`android`\|`generic`), `-y`\|`--yes`, `-h`\|`--help` | `PROJECT_TYPE="python"`, `DEFAULT_REPO_BASE="git@github.com:sprillex"`, `SPRILLEX_*_HOME` | `0` help/success; `1` unknown flag or missing input in `-y` mode |
| `./android_tools/clone_android.sh` | `<PROJECT_NAME \| GITHUB_URL>` (positional `$1`) | `SPRILLEX_ANDROID_HOME="$HOME/sprillex/android"`, `DEFAULT_REPO_BASE="https://github.com/sprillex"`, `SECONDARY_APK_DIR=""` | `0` success; `1` missing `$1` or clone dir failure; `set -e` |

---

## 3. Public Kotlin Module Interfaces

### 3.1 Room DAOs (`AppDatabase` & `UserDatabase`)
```kotlin
// AppDatabase (regional_restaurants.db, version = 3)
interface RestaurantDao {
    fun getAllRestaurants(): Flow<List<Restaurant>>                   // Streams all 1,188+ cached venues
    suspend fun getRestaurantById(id: String): Restaurant?            // Lookup by "{osm_type}/{osm_id}"
    fun getRestaurantsInBoundingBox(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<Restaurant>>
    fun searchRestaurants(query: String): Flow<List<Restaurant>>      // SQLite LIKE on name, cuisine, city
    @Upsert suspend fun upsertRestaurants(restaurants: List<Restaurant>) // Safe OTA upsert without touching UserDatabase
}
interface RestaurantDetailDao {
    @Transaction fun getRestaurantWithDetailsFlow(restaurantId: String): Flow<RestaurantWithDetails?>
    suspend fun getCachedDetails(restaurantId: String): RestaurantDetailEntity?
    @Insert(onConflict = REPLACE) suspend fun upsertDetails(details: RestaurantDetailEntity)
    @Query("DELETE ...") suspend fun deleteDetails(restaurantId: String)
}

// UserDatabase (user_data.db, version = 5)
interface FavoriteDao {
    fun getAllFavorites(): Flow<List<Favorite>>
    suspend fun addFavorite(favorite: Favorite)
    suspend fun removeFavorite(favorite: Favorite)
    fun isFavorite(restaurantId: String): Flow<Boolean>
}
interface WishlistDao {
    fun getAllWishlistItems(): Flow<List<Wishlist>>
    suspend fun getWishlistItem(restaurantId: String): Wishlist?
    suspend fun addOrUpdateWishlist(wishlist: Wishlist)
    suspend fun removeWishlist(wishlist: Wishlist)
    suspend fun removeWishlistByRestaurantId(restaurantId: String)
    fun isInWishlist(restaurantId: String): Flow<Boolean>
}
interface DishWishlistDao {
    fun getDishesForRestaurant(restaurantId: String): Flow<List<DishWishlist>>
    fun getAllDishWishlistItems(): Flow<List<DishWishlist>>
    suspend fun addOrUpdateDish(dish: DishWishlist)
    suspend fun removeDish(dish: DishWishlist)
    suspend fun removeDishById(dishId: Long)
}
interface FavoriteDishDao {
    fun getFavoriteDishesForRestaurant(restaurantId: String): Flow<List<FavoriteDish>>
    fun getAllFavoriteDishes(): Flow<List<FavoriteDish>>
    suspend fun addOrUpdateFavoriteDish(favoriteDish: FavoriteDish)
    suspend fun removeFavoriteDish(favoriteDish: FavoriteDish)
    suspend fun removeFavoriteDishById(dishId: Long)
}
```

### 3.2 Repository, Network, Storage & Utility Contracts
| Type | Method Signature | Behavioral Contract |
| :--- | :--- | :--- |
| `RestaurantRepository` | `observeRestaurant(restaurantId: String): Flow<RestaurantWithDetails?>` | Emits cached `RestaurantWithDetails` and re-emits on Room table updates. |
| `RestaurantRepository` | `suspend fun ensureDetailsEnriched(restaurant: Restaurant, apiKeyOverride: String? = null)` | Returns early if cached; otherwise queries `AiDiningClient` and upserts result (or `"found": false` note). |
| `AiDiningClient` | `suspend fun validateApiKey(keyToTest: String): Result<Boolean>` | Calls `GET /v1beta/models?key=...` on `Dispatchers.IO`; fails on blank key or non-2xx. |
| `AiDiningClient` | `suspend fun queryDiningProfile(restaurant: Restaurant, overrideApiKey: String? = null): AiDiningResponse?` | Posts grounded prompt across `CANDIDATE_ENDPOINTS`; fails fast on 429, retries 503. |
| `AiDiningClient` | `buildUserPrompt(restaurant)`, `sanitizeJsonString(rawText)`, `parseGeminiResponseText(responseText)` | Builds MI/OH anchored prompt; strips markdown fences; extracts first non-blank candidate text part. |
| `PreferenceManager` | `getSelectedAnchorIndex(): Int` / `setSelectedAnchorIndex(index: Int)` | Reads/writes `key_anchor_index` (default `0`) in `restaurant_finder_prefs`. |
| `PreferenceManager` | `getGeminiApiKey(): String` / `setGeminiApiKey(apiKey: String)` | Reads/writes `key_gemini_api_key` (default `""`) in `restaurant_finder_prefs`. |
| `BackupManager` | `suspend fun exportUserData(context: Context, userDatabase: UserDatabase): String` | Writes `filesDir/user_data_backup.json` and returns absolute path. |
| `BackupManager` | `suspend fun restoreUserData(context: Context, userDatabase: UserDatabase, backupFile: File): Boolean` | Returns `false` if missing; regex-parses sections and upserts into `UserDatabase` + prefs. |
| `DistanceCalculator` | `calculateDistanceMiles(lat1, lon1, lat2, lon2): Double` | Haversine distance in miles (`EARTH_RADIUS_MILES = 3958.8`). |
| `DistanceCalculator` | `formatDistance(distanceMiles: Double): String` | `< 0.2` mi -> `"<feet> ft"`, `< 10.0` -> `"%.1f mi"`, else `"%.0f mi"`. |
| `LocationManager` | `PRESET_ANCHORS: List<AnchorLocation>`, `GPS_ANCHOR`, `DEFAULT_ANCHOR` | `[GPS(41.6528,-83.5379), Toledo OH, Perrysburg OH, Monroe MI, Adrian MI]`. |
| `IntentHelper` | `launchNavigation(ctx, r)`, `launchDialer(ctx, phone)`, `launchBrowser(ctx, url)`, `shareRestaurant(ctx, r)`, `formatShareText(r)` | Fires implicit `geo:0,0?q=...`, `tel:`, `https://`, or `ACTION_SEND` chooser; shows `Toast` on failure. |
| `CrashReporter` | `install(app: Application)`, `buildReportBody(...): String` | Installs global `UncaughtExceptionHandler` launching `ACTION_SEND` (`EXTRA_CRASH_PAYLOAD`, `EXTRA_STACK_TRACE`). |

### 3.3 UI State & Event Contracts (`ui.screens`)
- `sortAndFilterRestaurants(restaurants, favoriteIds, wishlistMap, searchQuery, selectedAmenity, selectedAnchor): List<Pair<Restaurant, Double>>`: Pure filter + Haversine distance sort ascending.
- `MainScreen(...)`: Renders top anchor dropdown, search field, category `FilterChip` row (`All`, `Favorites`, `Wishlist`, `restaurant`, `fast_food`, `cafe`, `bar`, `pub`), and `LazyColumn` of `RestaurantCard`s; emits user intents via callbacks (`onAnchorSelected`, `onFavoriteToggle`, `onWishlistClick`, `onSettingsClick`, `onRestaurantClick`, `onNavigateClick`, `onCallClick`, `onWebsiteClick`, `onShareClick`).
- `DetailBottomSheet(...)`: Fires `LaunchedEffect(restaurant.id) { onEnrich() }` on open; displays OSM tags, AI Dining Insights card (`DarkSurfaceLevel1`), `FavoriteDish` list, `DishWishlist` list, `Wishlist` card, and action buttons inside `ModalBottomSheet` (`DarkSurfaceLevel2`).
