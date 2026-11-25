package com.sprillex.restaurantfinder.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Manages copying, EXIF-orienting, downscaling, resolving, and deleting user restaurant photos
 * inside `context.filesDir/restaurant_photos`.
 */
object PhotoStorageManager {
    const val PHOTOS_DIR_NAME = "restaurant_photos"
    const val CAMERA_CACHE_DIR_NAME = "camera_captures"
    const val MAX_EDGE_PX = 1600
    const val JPEG_QUALITY = 85

    fun getPhotosDirectory(context: Context): File {
        val dir = File(context.filesDir, PHOTOS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createTempCameraUri(context: Context): Uri {
        val cacheDir = File(context.cacheDir, CAMERA_CACHE_DIR_NAME)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val tempFile = File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    fun saveImageFromUri(
        context: Context,
        sourceUri: Uri,
        restaurantId: String
    ): String? {
        return try {
            val resolver = context.contentResolver

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOpts)
            }
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
                return null
            }

            val sampleSize = calculateInSampleSize(
                boundsOpts.outWidth,
                boundsOpts.outHeight,
                MAX_EDGE_PX,
                MAX_EDGE_PX
            )
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOpts)
            } ?: return null

            val rotationDegrees = try {
                resolver.openInputStream(sourceUri)?.use { input ->
                    when (ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (_: Exception) {
                0f
            }

            val oriented = if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                    if (it != decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val finalBitmap = downscaleIfNeeded(oriented, MAX_EDGE_PX)
            if (finalBitmap != oriented) {
                oriented.recycle()
            }

            val safePrefix = restaurantId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "${safePrefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val destFile = File(getPhotosDirectory(context), fileName)

            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            finalBitmap.recycle()

            fileName
        } catch (_: Exception) {
            null
        }
    }

    fun resolvePhotoFile(context: Context, relativePath: String): File {
        val photosDir = getPhotosDirectory(context)
        val cleaned = relativePath.removePrefix("$PHOTOS_DIR_NAME/").trimStart('/')
        val candidate = File(photosDir, cleaned)
        return try {
            val baseCanonical = photosDir.canonicalPath
            val targetCanonical = candidate.canonicalPath
            if (targetCanonical.startsWith(baseCanonical + File.separator) || targetCanonical == baseCanonical) {
                candidate
            } else {
                File(photosDir, candidate.name)
            }
        } catch (_: Exception) {
            File(photosDir, File(cleaned).name)
        }
    }

    fun deletePhotoFile(context: Context, relativePath: String): Boolean {
        val file = resolvePhotoFile(context, relativePath)
        return if (file.exists()) file.delete() else false
    }

    fun loadScaledBitmap(file: File, maxEdgePx: Int = 800): Bitmap? {
        if (!file.exists()) return null
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

            val sampleSize = calculateInSampleSize(
                boundsOpts.outWidth,
                boundsOpts.outHeight,
                maxEdgePx,
                maxEdgePx
            )
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
        } catch (_: Exception) {
            null
        }
    }

    private fun downscaleIfNeeded(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val currentMax = max(bitmap.width, bitmap.height)
        if (currentMax <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / currentMax.toFloat()
        val targetWidth = max(1, (bitmap.width * scale).roundToInt())
        val targetHeight = max(1, (bitmap.height * scale).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
