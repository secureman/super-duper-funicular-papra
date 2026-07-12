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

/**
 * Deterministic cache path for a document's file bytes, keyed by both the
 * document id and its updatedAt timestamp. If the document hasn't changed,
 * re-opening it finds this file already present and skips the network
 * entirely; if it HAS changed server-side, updatedAt differs, the filename
 * differs, and it re-downloads instead of serving a stale copy.
 */
fun cachedFileFor(context: Context, document: com.papra.mobile.data.remote.dto.DocumentDto): File {
    val versionKey = (document.updatedAt ?: document.createdAt ?: "").hashCode()
    val dir = File(context.cacheDir, "document_cache").apply { mkdirs() }
    val safeName = document.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return File(dir, "${document.id}_${versionKey}_$safeName")
}

/** Returns the cached file if it's already present and non-empty, downloading
 *  it via [download] only when there's no valid cache hit. */
suspend fun getOrDownload(
    context: Context,
    document: com.papra.mobile.data.remote.dto.DocumentDto,
    download: suspend (destination: File) -> File?,
): File? {
    val cached = cachedFileFor(context, document)
    if (cached.exists() && cached.length() > 0) return cached
    return download(cached)
}
