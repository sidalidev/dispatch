package dev.sidali.dispatch

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Serves the private copies made by "Copy image", read-only. Not exported: other apps only get
 * access through the one-off grant the clipboard gives to the app that reads the clip.
 */
class ClipProvider : ContentProvider() {
    override fun onCreate() = true

    private fun file(uri: Uri): File {
        val ctx = context ?: throw FileNotFoundException()
        val name = uri.pathSegments.singleOrNull()?.takeIf { Clips.isSafeName(it) } ?: throw FileNotFoundException()
        val f = File(Clips.dir(ctx), name)
        if (!f.isFile) throw FileNotFoundException(name)
        return f
    }

    override fun getType(uri: Uri): String? = uri.lastPathSegment?.takeIf { Clips.isSafeName(it) }?.let { Clips.mimeFor(it) }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("read-only")
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor {
        val f = file(uri)
        val cols = (projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
            .filter { it == OpenableColumns.DISPLAY_NAME || it == OpenableColumns.SIZE }.toTypedArray()
        return MatrixCursor(cols, 1).apply { addRow(cols.map { if (it == OpenableColumns.SIZE) f.length() else f.name }) }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, s: String?, a: Array<out String>?): Int = throw UnsupportedOperationException()
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = throw UnsupportedOperationException()
}
