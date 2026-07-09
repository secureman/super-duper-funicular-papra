package com.papra.mobile.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/** Result of resolving a content Uri to an actual on-disk file for upload. */
data class ResolvedFile(val file: File, val mimeType: String)

/**
 * Multipart upload needs a real File, but pickers/camera intents hand back
 * content:// Uris. This copies the Uri's bytes into the app cache dir under
 * its original display name (falling back to a generic name) and resolves
 * its mime type via the ContentResolver.
 */
fun resolveContentUriToFile(context: Context, uri: Uri): ResolvedFile {
    val resolver = context.contentResolver
    val displayName = queryDisplayName(resolver, uri) ?: "upload_${System.currentTimeMillis()}"
    val mimeType = resolver.getType(uri) ?: "application/octet-stream"

    val outFile = File(context.cacheDir, displayName)
    resolver.openInputStream(uri)?.use { input ->
        outFile.outputStream().use { output -> input.copyTo(output) }
    } ?: error("Could not open picked file")

    return ResolvedFile(outFile, mimeType)
}

private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
    val cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return it.getString(index)
        }
    }
    return null
}

/** Creates an empty jpg in the cache dir for the camera intent to write into. */
fun createCameraCaptureFile(context: Context): File =
    File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
