package dev.sidali.dispatch

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Size

/** What was shared with Dispatch, read once from the incoming intent. */
data class Payload(
    val action: String,
    val type: String,
    val text: String?,
    val subject: String?,
    val uris: List<Uri>,
) {
    val isEmpty get() = text.isNullOrBlank() && uris.isEmpty()
    val isLink get() = uris.isEmpty() && text != null && Links.isLink(text)
    val single: Uri? get() = uris.singleOrNull()
    val isImage get() = single != null && type.startsWith("image/")
    val isVideo get() = single != null && type.startsWith("video/")

    companion object {
        fun from(intent: Intent): Payload {
            val action = intent.action ?: Intent.ACTION_SEND
            val uris = LinkedHashSet<Uri>()
            if (action == Intent.ACTION_SEND_MULTIPLE) {
                intent.streams()?.let { uris += it }
            } else {
                intent.stream()?.let { uris += it }
            }
            val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            val clip = intent.clipData
            if (clip != null) for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let { uris += it }
            val clipText = if (text == null && uris.isEmpty() && clip != null && clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null
            val type = intent.type ?: if (uris.isEmpty()) "text/plain" else "*/*"
            return Payload(action, type, text ?: clipText, intent.getStringExtra(Intent.EXTRA_SUBJECT), uris.toList())
        }

        @Suppress("DEPRECATION")
        private fun Intent.stream(): Uri? =
            if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else getParcelableExtra(Intent.EXTRA_STREAM)

        @Suppress("DEPRECATION")
        private fun Intent.streams(): List<Uri>? =
            if (Build.VERSION.SDK_INT >= 33) getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) else getParcelableArrayListExtra(Intent.EXTRA_STREAM)
    }
}

/** Name and size of a shared file, when its provider tells. */
data class FileInfo(val name: String?, val size: Long?)

fun ContentResolver.info(uri: Uri): FileInfo = try {
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (!c.moveToFirst()) return@use FileInfo(null, null)
        val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { c.getString(it) }
        val s = c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }?.let { c.getLong(it) }
        FileInfo(n, s)
    } ?: FileInfo(null, null)
} catch (_: Exception) { FileInfo(null, null) }

fun ContentResolver.thumbnail(uri: Uri, px: Int): Bitmap? = try {
    loadThumbnail(uri, Size(px, px), null)
} catch (_: Exception) { null }
