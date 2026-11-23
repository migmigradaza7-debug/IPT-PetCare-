package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PetPhotoStorage {
  private const val DIR_NAME = "pet_photos"
  private const val MAX_DIM_PX = 720

  fun getPhotoFile(context: Context, petId: String): File? {
    if (petId.isBlank()) return null
    val file = File(File(context.applicationContext.filesDir, DIR_NAME), "$petId.jpg")
    return if (file.exists() && file.length() > 0L) file else null
  }

  suspend fun savePhotoFromUri(context: Context, petId: String, sourceUri: Uri): Boolean =
    withContext(Dispatchers.IO) {
      if (petId.isBlank()) return@withContext false
      val appContext = context.applicationContext
      val dir = File(appContext.filesDir, DIR_NAME).apply { mkdirs() }
      val destFile = File(dir, "$petId.jpg")
      try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(sourceUri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val sampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, MAX_DIM_PX)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = appContext.contentResolver.openInputStream(sourceUri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return@withContext false

        FileOutputStream(destFile).use { out ->
          bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
        true
      } catch (_: Exception) {
        false
      }
    }

  fun deletePhoto(context: Context, petId: String) {
    if (petId.isBlank()) return
    val file = File(File(context.applicationContext.filesDir, DIR_NAME), "$petId.jpg")
    if (file.exists()) {
      file.delete()
    }
  }

  private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
    var inSampleSize = 1
    if (width > maxDim || height > maxDim) {
      val halfWidth = width / 2
      val halfHeight = height / 2
      while (halfWidth / inSampleSize >= maxDim && halfHeight / inSampleSize >= maxDim) {
        inSampleSize *= 2
      }
    }
    return inSampleSize
  }
}
