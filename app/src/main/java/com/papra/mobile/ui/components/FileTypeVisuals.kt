package com.papra.mobile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.papra.mobile.ui.theme.FileColorDoc
import com.papra.mobile.ui.theme.FileColorGeneric
import com.papra.mobile.ui.theme.FileColorImage
import com.papra.mobile.ui.theme.FileColorPdf
import com.papra.mobile.ui.theme.FileColorSpreadsheet

data class FileVisual(val icon: ImageVector, val color: Color)

/** Drive color-codes documents by type; we do the same for Papra documents. */
fun fileVisualFor(mimeType: String?): FileVisual = when {
    mimeType == null -> FileVisual(Icons.Filled.InsertDriveFile, FileColorGeneric)
    mimeType == "application/pdf" -> FileVisual(Icons.Filled.PictureAsPdf, FileColorPdf)
    mimeType.startsWith("image/") -> FileVisual(Icons.Filled.Image, FileColorImage)
    mimeType.contains("spreadsheet") || mimeType.contains("csv") ->
        FileVisual(Icons.Filled.TableChart, FileColorSpreadsheet)
    mimeType.contains("word") || mimeType.contains("document") ->
        FileVisual(Icons.Filled.Description, FileColorDoc)
    mimeType == "folder" -> FileVisual(Icons.Filled.Folder, FileColorGeneric)
    else -> FileVisual(Icons.Filled.InsertDriveFile, FileColorGeneric)
}
