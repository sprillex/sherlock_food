package com.sprillex.restaurantfinder

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.sprillex.restaurantfinder.data.*
import com.sprillex.restaurantfinder.data.remote.AiDiningClient
import com.sprillex.restaurantfinder.data.repository.RestaurantRepository
import com.sprillex.restaurantfinder.location.AnchorLocation
import com.sprillex.restaurantfinder.location.LocationManager
import com.sprillex.restaurantfinder.ui.components.AddDishDialog
import com.sprillex.restaurantfinder.ui.components.AddFavoriteDishDialog
import com.sprillex.restaurantfinder.ui.components.AddPhotoMetadataDialog
import com.sprillex.restaurantfinder.ui.components.AddVisitLogDialog
import com.sprillex.restaurantfinder.ui.components.EditRestaurantDialog
import com.sprillex.restaurantfinder.ui.components.ManualInsightDialog
import com.sprillex.restaurantfinder.ui.components.SettingsDialog
import com.sprillex.restaurantfinder.ui.components.WishlistDialog
import com.sprillex.restaurantfinder.ui.screens.DetailBottomSheet
import com.sprillex.restaurantfinder.ui.screens.MainScreen
import com.sprillex.restaurantfinder.ui.theme.RestaurantFinderTheme
import com.sprillex.restaurantfinder.utils.IntentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var gpsLocationState by mutableStateOf<AnchorLocation?>(null)

    private var pendingPhotoRestaurantId by mutableStateOf<String?>(null)
    private var pendingCameraCaptureUri by mutableStateOf<Uri?>(null)
    private var capturedPhotoUriForMetadata by mutableStateOf<Pair<String, Uri>?>(null)

    private lateinit var userDbInstance: UserDatabase

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            startGpsLocationUpdates()
        }
    }

    private val pickGalleryPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val resId = pendingPhotoRestaurantId
        pendingPhotoRestaurantId = null
        if (uri != null && resId != null) {
            capturedPhotoUriForMetadata = resId to uri
        }
    }

    private val takeCameraPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val resId = pendingPhotoRestaurantId
        val captureUri = pendingCameraCaptureUri
        pendingPhotoRestaurantId = null
        pendingCameraCaptureUri = null
        if (success && resId != null && captureUri != null) {
            capturedPhotoUriForMetadata = resId to captureUri
        }
    }

    private val exportZipLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            lifecycleScope.launch {
                val ok = BackupManager.exportBackupArchive(applicationContext, userDbInstance, uri)
                val msg = if (ok) "ZIP archive exported successfully" else "Failed to export ZIP archive"
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private val restoreZipLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            lifecycleScope.launch {
                val ok = BackupManager.restoreBackupArchive(applicationContext, userDbInstance, uri)
                val msg = if (ok) "ZIP archive restored successfully" else "Invalid or unreadable backup archive"
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkAndRequestLocationPermissions()

        setContent {
            RestaurantFinderTheme {
                Surface {
                    val preferenceManager = remember { PreferenceManager(applicationContext) }
                    val initialAnchorIndex = remember { preferenceManager.getSelectedAnchorIndex().coerceIn(0, LocationManager.PRESET_ANCHORS.lastIndex) }
                    var userApiKey by remember { mutableStateOf(preferenceManager.getGeminiApiKey()) }
                    var isSettingsOpen by remember { mutableStateOf(false) }

                    val appDb = remember { AppDatabase.getDatabase(applicationContext) }
                    val userDb = remember {
                        UserDatabase.getDatabase(applicationContext).also { userDbInstance = it }
                    }

                    val aiClient = remember { AiDiningClient() }
                    val restaurantRepository = remember {
                        RestaurantRepository(
                            appDb.restaurantDetailDao(),
                            aiClient
                        )
                    }

                    val restaurantsFlow = remember { appDb.restaurantDao().getAllRestaurants() }
                    val restaurants by restaurantsFlow.collectAsState(initial = emptyList())
                    val baseRestaurantsById = remember(restaurants) { restaurants.associateBy { it.id } }

                    val favoritesFlow = remember { userDb.favoriteDao().getAllFavorites() }
                    val favorites by favoritesFlow.collectAsState(initial = emptyList())
                    val favoriteIds = remember(favorites) { favorites.map { it.restaurantId }.toSet() }

                    val wishlistFlow = remember { userDb.wishlistDao().getAllWishlistItems() }
                    val wishlistItems by wishlistFlow.collectAsState(initial = emptyList())
                    val wishlistMap = remember(wishlistItems) { wishlistItems.associateBy { it.restaurantId } }

                    val dishWishlistFlow = remember { userDb.dishWishlistDao().getAllDishWishlistItems() }
                    val dishWishlistItems by dishWishlistFlow.collectAsState(initial = emptyList())
                    val dishWishlistMap = remember(dishWishlistItems) { dishWishlistItems.groupBy { it.restaurantId } }

                    val favoriteDishFlow = remember { userDb.favoriteDishDao().getAllFavoriteDishes() }
                    val favoriteDishItems by favoriteDishFlow.collectAsState(initial = emptyList())
                    val favoriteDishMap = remember(favoriteDishItems) { favoriteDishItems.groupBy { it.restaurantId } }

                    val overridesFlow = remember { userDb.restaurantOverrideDao().getAllOverrides() }
                    val overrideItems by overridesFlow.collectAsState(initial = emptyList())
                    val overridesMap = remember(overrideItems) { overrideItems.associateBy { it.restaurantId } }

                    val manualInsightsFlow = remember { userDb.manualInsightDao().getAllManualInsights() }
                    val manualInsightItems by manualInsightsFlow.collectAsState(initial = emptyList())
                    val manualInsightsMap = remember(manualInsightItems) { manualInsightItems.associateBy { it.restaurantId } }

                    val photosFlow = remember { userDb.restaurantPhotoDao().getAllPhotos() }
                    val photoItems by photosFlow.collectAsState(initial = emptyList())
                    val photosMap = remember(photoItems) { photoItems.groupBy { it.restaurantId } }

                    val visitLogsFlow = remember { userDb.visitLogDao().getAllVisitLogs() }
                    val visitLogItems by visitLogsFlow.collectAsState(initial = emptyList())
                    val visitLogsMap = remember(visitLogItems) { visitLogItems.groupBy { it.restaurantId } }

                    var selectedAnchor by remember {
                        mutableStateOf(LocationManager.PRESET_ANCHORS[initialAnchorIndex])
                    }

                    val effectiveAnchor = remember(selectedAnchor, gpsLocationState) {
                        if (selectedAnchor.isGps && gpsLocationState != null) {
                            gpsLocationState!!
                        } else {
                            selectedAnchor
                        }
                    }

                    var selectedRestaurantIdForDetail by remember { mutableStateOf<String?>(null) }
                    var selectedRestaurantForWishlist by remember { mutableStateOf<Restaurant?>(null) }
                    var selectedRestaurantForAddDish by remember { mutableStateOf<Restaurant?>(null) }
                    var selectedRestaurantForAddFavDish by remember { mutableStateOf<Restaurant?>(null) }
                    var selectedRestaurantIdForEdit by remember { mutableStateOf<String?>(null) }
                    var selectedRestaurantForManualInsight by remember { mutableStateOf<Restaurant?>(null) }
                    var selectedRestaurantForVisitLog by remember { mutableStateOf<Restaurant?>(null) }

                    val toggleFavorite: (String) -> Unit = { restaurantId ->
                        lifecycleScope.launch {
                            if (favoriteIds.contains(restaurantId)) {
                                userDb.favoriteDao().removeFavorite(Favorite(restaurantId))
                            } else {
                                userDb.favoriteDao().addFavorite(Favorite(restaurantId))
                            }
                        }
                    }

                    MainScreen(
                        restaurants = restaurants,
                        favoriteIds = favoriteIds,
                        wishlistMap = wishlistMap,
                        dishWishlistMap = dishWishlistMap,
                        favoriteDishMap = favoriteDishMap,
                        overridesMap = overridesMap,
                        photosMap = photosMap,
                        visitLogsMap = visitLogsMap,
                        selectedAnchor = effectiveAnchor,
                        onAnchorSelected = { anchor ->
                            selectedAnchor = anchor
                            val newIndex = LocationManager.PRESET_ANCHORS.indexOf(anchor)
                            if (newIndex >= 0) {
                                preferenceManager.setSelectedAnchorIndex(newIndex)
                            }
                            if (anchor.isGps) {
                                checkAndRequestLocationPermissions()
                            }
                        },
                        onFavoriteToggle = toggleFavorite,
                        onWishlistClick = { selectedRestaurantForWishlist = it },
                        onSettingsClick = { isSettingsOpen = true },
                        onRestaurantClick = { selectedRestaurantIdForDetail = it.id },
                        onNavigateClick = { IntentHelper.launchNavigation(this, it) },
                        onCallClick = { r -> r.phone?.let { IntentHelper.launchDialer(this, it) } },
                        onWebsiteClick = { r -> r.website?.let { IntentHelper.launchBrowser(this, it) } },
                        onShareClick = { IntentHelper.shareRestaurant(this, it) }
                    )

                    if (isSettingsOpen) {
                        SettingsDialog(
                            currentApiKey = userApiKey,
                            onValidateKey = { key -> aiClient.validateApiKey(key) },
                            onSaveApiKey = { newKey ->
                                preferenceManager.setGeminiApiKey(newKey)
                                userApiKey = newKey
                            },
                            onBackupClick = {
                                lifecycleScope.launch {
                                    val path = BackupManager.exportUserData(applicationContext, userDb)
                                    Toast.makeText(applicationContext, "Backup saved: $path", Toast.LENGTH_LONG).show()
                                }
                            },
                            onRestoreClick = {
                                lifecycleScope.launch {
                                    val backupFile = File(filesDir, BackupManager.BACKUP_JSON_FILENAME)
                                    val success = BackupManager.restoreUserData(applicationContext, userDb, backupFile)
                                    val msg = if (success) "User data restored successfully" else "No backup file found"
                                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onExportZipClick = {
                                exportZipLauncher.launch("sherlock_food_backup.zip")
                            },
                            onRestoreZipClick = {
                                restoreZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            },
                            onDismiss = { isSettingsOpen = false }
                        )
                    }

                    selectedRestaurantIdForDetail?.let { restaurantId ->
                        val baseRestaurant = baseRestaurantsById[restaurantId]
                        if (baseRestaurant != null) {
                            val currentOverride = overridesMap[restaurantId]
                            val effectiveRestaurant = remember(baseRestaurant, currentOverride) {
                                baseRestaurant.applyOverride(currentOverride)
                            }
                            val restaurantWithDetailsFlow = remember(restaurantId) {
                                restaurantRepository.observeRestaurant(restaurantId)
                            }
                            val restaurantWithDetails by restaurantWithDetailsFlow.collectAsState(initial = null)
                            var isLoadingEnrichment by remember(restaurantId) { mutableStateOf(false) }

                            DetailBottomSheet(
                                restaurant = effectiveRestaurant,
                                details = restaurantWithDetails?.details,
                                isLoadingEnrichment = isLoadingEnrichment,
                                onEnrich = {
                                    if (restaurantWithDetails?.details == null && !isLoadingEnrichment) {
                                        isLoadingEnrichment = true
                                        lifecycleScope.launch {
                                            try {
                                                restaurantRepository.ensureDetailsEnriched(
                                                    restaurant = effectiveRestaurant,
                                                    apiKeyOverride = userApiKey
                                                )
                                            } finally {
                                                isLoadingEnrichment = false
                                            }
                                        }
                                    }
                                },
                                onRefreshEnrich = {
                                    if (!isLoadingEnrichment) {
                                        isLoadingEnrichment = true
                                        lifecycleScope.launch {
                                            try {
                                                restaurantRepository.ensureDetailsEnriched(
                                                    restaurant = effectiveRestaurant,
                                                    apiKeyOverride = userApiKey,
                                                    forceRefresh = true
                                                )
                                            } finally {
                                                isLoadingEnrichment = false
                                            }
                                        }
                                    }
                                },
                                hasOverride = currentOverride != null,
                                isClosedOverride = currentOverride?.isClosed == true,
                                manualInsight = manualInsightsMap[restaurantId],
                                photos = photosMap[restaurantId] ?: emptyList(),
                                visitLogs = visitLogsMap[restaurantId] ?: emptyList(),
                                onEditRestaurantClick = { selectedRestaurantIdForEdit = restaurantId },
                                onEditManualInsightClick = { selectedRestaurantForManualInsight = effectiveRestaurant },
                                onAddPhotoCameraClick = {
                                    pendingPhotoRestaurantId = restaurantId
                                    val captureUri = PhotoStorageManager.createTempCameraUri(applicationContext)
                                    pendingCameraCaptureUri = captureUri
                                    takeCameraPhotoLauncher.launch(captureUri)
                                },
                                onAddPhotoGalleryClick = {
                                    pendingPhotoRestaurantId = restaurantId
                                    pickGalleryPhotoLauncher.launch("image/*")
                                },
                                onDeletePhotoClick = { photo ->
                                    lifecycleScope.launch {
                                        userDb.restaurantPhotoDao().deletePhoto(photo)
                                        withContext(Dispatchers.IO) {
                                            PhotoStorageManager.deletePhotoFile(applicationContext, photo.relativePath)
                                        }
                                    }
                                },
                                onAddVisitLogClick = { selectedRestaurantForVisitLog = effectiveRestaurant },
                                onDeleteVisitLogClick = { visit ->
                                    lifecycleScope.launch {
                                        userDb.visitLogDao().deleteVisitLog(visit)
                                    }
                                },
                                isFavorite = favoriteIds.contains(restaurantId),
                                wishlist = wishlistMap[restaurantId],
                                dishes = dishWishlistMap[restaurantId] ?: emptyList(),
                                favoriteDishes = favoriteDishMap[restaurantId] ?: emptyList(),
                                onFavoriteToggle = { toggleFavorite(restaurantId) },
                                onWishlistClick = {
                                    selectedRestaurantForWishlist = effectiveRestaurant
                                },
                                onAddDishClick = {
                                    selectedRestaurantForAddDish = effectiveRestaurant
                                },
                                onDeleteDishClick = { dish ->
                                    lifecycleScope.launch {
                                        userDb.dishWishlistDao().removeDish(dish)
                                    }
                                },
                                onAddFavoriteDishClick = {
                                    selectedRestaurantForAddFavDish = effectiveRestaurant
                                },
                                onDeleteFavoriteDishClick = { favDish ->
                                    lifecycleScope.launch {
                                        userDb.favoriteDishDao().removeFavoriteDish(favDish)
                                    }
                                },
                                onDismiss = { selectedRestaurantIdForDetail = null },
                                onNavigateClick = { IntentHelper.launchNavigation(this, effectiveRestaurant) },
                                onCallClick = { effectiveRestaurant.phone?.let { IntentHelper.launchDialer(this, it) } },
                                onWebsiteClick = { effectiveRestaurant.website?.let { IntentHelper.launchBrowser(this, it) } },
                                onShareClick = { IntentHelper.shareRestaurant(this, effectiveRestaurant) }
                            )
                        }
                    }

                    selectedRestaurantIdForEdit?.let { editId ->
                        val baseRestaurant = baseRestaurantsById[editId]
                        if (baseRestaurant != null) {
                            EditRestaurantDialog(
                                baseRestaurant = baseRestaurant,
                                currentOverride = overridesMap[editId],
                                onSave = { override ->
                                    lifecycleScope.launch {
                                        userDb.restaurantOverrideDao().upsertOverride(override)
                                        selectedRestaurantIdForEdit = null
                                    }
                                },
                                onResetToDefaults = {
                                    lifecycleScope.launch {
                                        userDb.restaurantOverrideDao().deleteOverride(editId)
                                        selectedRestaurantIdForEdit = null
                                    }
                                },
                                onDismiss = { selectedRestaurantIdForEdit = null }
                            )
                        }
                    }

                    selectedRestaurantForManualInsight?.let { restaurant ->
                        ManualInsightDialog(
                            restaurant = restaurant,
                            currentInsight = manualInsightsMap[restaurant.id],
                            onSave = { insight ->
                                lifecycleScope.launch {
                                    userDb.manualInsightDao().upsertManualInsight(insight)
                                    selectedRestaurantForManualInsight = null
                                }
                            },
                            onDelete = {
                                lifecycleScope.launch {
                                    userDb.manualInsightDao().deleteManualInsight(restaurant.id)
                                    selectedRestaurantForManualInsight = null
                                }
                            },
                            onDismiss = { selectedRestaurantForManualInsight = null }
                        )
                    }

                    selectedRestaurantForVisitLog?.let { restaurant ->
                        AddVisitLogDialog(
                            restaurant = restaurant,
                            onSave = { rating, orderedDishes, notes ->
                                lifecycleScope.launch {
                                    userDb.visitLogDao().upsertVisitLog(
                                        VisitLog(
                                            restaurantId = restaurant.id,
                                            rating = rating,
                                            orderedDishes = orderedDishes,
                                            notes = notes
                                        )
                                    )
                                    selectedRestaurantForVisitLog = null
                                }
                            },
                            onDismiss = { selectedRestaurantForVisitLog = null }
                        )
                    }

                    capturedPhotoUriForMetadata?.let { (resId, photoUri) ->
                        val base = baseRestaurantsById[resId]
                        if (base != null) {
                            val effective = base.applyOverride(overridesMap[resId])
                            AddPhotoMetadataDialog(
                                restaurant = effective,
                                onSave = { category, caption ->
                                    lifecycleScope.launch {
                                        val relPath = withContext(Dispatchers.IO) {
                                            PhotoStorageManager.saveImageFromUri(applicationContext, photoUri, resId)
                                        }
                                        if (relPath != null) {
                                            userDb.restaurantPhotoDao().insertPhoto(
                                                RestaurantPhoto(
                                                    restaurantId = resId,
                                                    relativePath = relPath,
                                                    category = category,
                                                    caption = caption
                                                )
                                            )
                                        } else {
                                            Toast.makeText(applicationContext, "Could not save selected photo", Toast.LENGTH_SHORT).show()
                                        }
                                        capturedPhotoUriForMetadata = null
                                    }
                                },
                                onDismiss = { capturedPhotoUriForMetadata = null }
                            )
                        }
                    }

                    selectedRestaurantForAddFavDish?.let { restaurant ->
                        AddFavoriteDishDialog(
                            restaurant = restaurant,
                            onSave = { dishName, notes ->
                                lifecycleScope.launch {
                                    userDb.favoriteDishDao().addOrUpdateFavoriteDish(
                                        FavoriteDish(
                                            restaurantId = restaurant.id,
                                            dishName = dishName,
                                            notes = notes
                                        )
                                    )
                                    selectedRestaurantForAddFavDish = null
                                }
                            },
                            onDismiss = { selectedRestaurantForAddFavDish = null }
                        )
                    }

                    selectedRestaurantForAddDish?.let { restaurant ->
                        AddDishDialog(
                            restaurant = restaurant,
                            onSave = { dishName, notes ->
                                lifecycleScope.launch {
                                    userDb.dishWishlistDao().addOrUpdateDish(
                                        DishWishlist(
                                            restaurantId = restaurant.id,
                                            dishName = dishName,
                                            notes = notes
                                        )
                                    )
                                    selectedRestaurantForAddDish = null
                                }
                            },
                            onDismiss = { selectedRestaurantForAddDish = null }
                        )
                    }

                    selectedRestaurantForWishlist?.let { restaurant ->
                        WishlistDialog(
                            restaurant = restaurant,
                            currentWishlist = wishlistMap[restaurant.id],
                            onSave = { notes, priority ->
                                lifecycleScope.launch {
                                    userDb.wishlistDao().addOrUpdateWishlist(
                                        Wishlist(
                                            restaurantId = restaurant.id,
                                            notes = notes,
                                            priority = priority
                                        )
                                    )
                                    selectedRestaurantForWishlist = null
                                }
                            },
                            onDelete = {
                                lifecycleScope.launch {
                                    userDb.wishlistDao().removeWishlistByRestaurantId(restaurant.id)
                                    selectedRestaurantForWishlist = null
                                }
                            },
                            onDismiss = { selectedRestaurantForWishlist = null }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestLocationPermissions() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            startGpsLocationUpdates()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startGpsLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                gpsLocationState = AnchorLocation("Current Location (GPS)", loc.latitude, loc.longitude, isGps = true)
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                gpsLocationState = AnchorLocation("Current Location (GPS)", loc.latitude, loc.longitude, isGps = true)
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }
}
