package dev.sidali.dispatch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import java.io.File

/** Copy to the clipboard. Text goes as is (links cleaned); images go through a private copy. */
object Clips {
    const val AUTHORITY = "dev.sidali.dispatch.clips"
    private const val KEEP = 3
    private val SAFE = Regex("""clip-\d{1,19}\.(png|jpg|webp|gif|heic|avif)""")
    private val EXT = mapOf(
        "image/png" to "png", "image/jpeg" to "jpg", "image/webp" to "webp",
        "image/gif" to "gif", "image/heic" to "heic", "image/avif" to "avif",
    )

    fun isSafeName(name: String) = SAFE.matches(name)

    fun mimeFor(name: String): String? = EXT.entries.firstOrNull { name.endsWith("." + it.value) }?.key

    fun dir(ctx: Context) = File(ctx.noBackupFilesDir, "clips").apply { mkdirs() }

    fun copyText(ctx: Context, label: String, text: String) {
        ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
    }

    /**
     * Copies the shared image into private storage and puts a content:// URI to that copy on the
     * clipboard. The app that pastes gets one-off read access through the clipboard grant. The grant
     * Dispatch received for the original ends with this screen, so the original URI cannot be used.
     */
    fun copyImage(ctx: Context, label: String, source: Uri, mime: String): Boolean {
        val resolved = ctx.contentResolver.getType(source) ?: mime
        val ext = EXT[resolved] ?: EXT[mime] ?: "png"
        val dir = dir(ctx)
        val f = File(dir, "clip-${System.currentTimeMillis()}.$ext")
        val ok = try {
            ctx.contentResolver.openInputStream(source)?.use { input -> f.outputStream().use { input.copyTo(it) } } != null
        } catch (_: Exception) { false }
        if (!ok || f.length() == 0L) { f.delete(); return false }
        dir.listFiles { x -> isSafeName(x.name) }?.sortedByDescending { it.name.substringAfter('-').substringBefore('.').toLongOrNull() ?: 0 }
            ?.drop(KEEP)?.forEach { it.delete() }
        val uri = Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(f.name).build()
        ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newUri(ctx.contentResolver, label, uri))
        return true
    }
}
