package com.papra.mobile.ui.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders a local PDF file page-by-page using Android's built-in
 * android.graphics.pdf.PdfRenderer -- no third-party PDF library, no extra
 * APK size. Pages render lazily as they scroll into view and are cached in
 * memory for the composable's lifetime.
 */
@Composable
fun PdfPagesView(file: File, modifier: Modifier = Modifier) {
    var pageCount by remember(file) { mutableStateOf(0) }
    var loadError by remember(file) { mutableStateOf<String?>(null) }
    var renderer by remember(file) { mutableStateOf<PdfRenderer?>(null) }

    DisposableEffect(file) {
        var pfd: ParcelFileDescriptor? = null
        var localRenderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            localRenderer = PdfRenderer(pfd)
            renderer = localRenderer
            pageCount = localRenderer.pageCount
        } catch (e: Exception) {
            loadError = e.message ?: "Could not open PDF"
        }
        onDispose {
            localRenderer?.close()
            pfd?.close()
        }
    }

    when {
        loadError != null -> {
            Text(
                loadError ?: "Could not open PDF",
                color = MaterialTheme.colorScheme.error,
                modifier = modifier.padding(16.dp),
            )
        }
        pageCount == 0 -> {
            Box(modifier = modifier.height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            LazyColumn(modifier = modifier) {
                items(pageCount) { index ->
                    PdfPage(renderer = renderer, index = index)
                }
            }
        }
    }
}

@Composable
private fun PdfPage(renderer: PdfRenderer?, index: Int) {
    var bitmap by remember(index) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(index, renderer) {
        val r = renderer ?: return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            // PdfRenderer isn't safe for concurrent page access, so pages render
            // one at a time even though LazyColumn may compose several at once.
            synchronized(r) {
                try {
                    val page = r.openPage(index)
                    // 2x scale for a sharper render on high-density screens.
                    val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bmp
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(bitmap = bmp.asImageBitmap(), contentDescription = "Page ${index + 1}", modifier = Modifier.fillMaxWidth())
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
