package com.tom.fourhourbody.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Progress photos are copied into the app's own storage and referenced by path. A picked
 * gallery URI only grants temporary access, so keeping the URI would leave broken photos
 * behind a few days later.
 */
object PhotoStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    fun newPhotoFile(context: Context): File = File(dir(context), "${UUID.randomUUID()}.jpg")

    fun shareUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copies a picked image in and returns the stored path, or null if it could not be read. */
    fun importFrom(context: Context, source: Uri): String? = runCatching {
        val target = newPhotoFile(context)
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.absolutePath
    }.getOrNull()

    /** Decodes a down-sampled bitmap — full-resolution photos do not belong in a list row. */
    fun loadThumbnail(path: String, maxPx: Int = 512): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxPx || bounds.outHeight / sample > maxPx) {
            sample *= 2
        }
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()
}
