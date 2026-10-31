package dev.sidali.sharecheck

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.TextView

open class Main : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uris = buildList {
            val clip = intent.clipData
            if (clip != null) for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let { add(it) }
        }
        val files = uris.joinToString(" ") { u -> describe(u) }
        val msg = "SHARE_OK target=${javaClass.simpleName} action=${intent.action} type=${intent.type} " +
            "text=${intent.getStringExtra(Intent.EXTRA_TEXT)} files=${uris.size} $files"
        Log.i("ShareCheck", msg)
        setContentView(TextView(this).apply { text = msg; textSize = 18f; setPadding(40, 200, 40, 40) })
    }

    private fun describe(u: Uri): String = try {
        val bytes = contentResolver.openInputStream(u)?.use { it.readBytes() } ?: ByteArray(0)
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        "[readable bytes=${bytes.size} image=${o.outWidth}x${o.outHeight}]"
    } catch (t: Throwable) { "[UNREADABLE $t]" }
}

class Other : Main()

class Paste : Activity() {
    override fun onWindowFocusChanged(has: Boolean) {
        super.onWindowFocusChanged(has); if (!has) return
        val msg = try {
            val clip = getSystemService(android.content.ClipboardManager::class.java).primaryClip
            val item = clip?.getItemAt(0)
            val uri = item?.uri
            val bmp = uri?.let { contentResolver.openInputStream(it)?.use { s -> BitmapFactory.decodeStream(s) } }
            "PASTE_OK mime=${clip?.description?.getMimeType(0)} text=${item?.text} uri=$uri image=${bmp?.width}x${bmp?.height}"
        } catch (t: Throwable) { "PASTE_FAIL $t" }
        Log.i("ShareCheck", msg)
        setContentView(TextView(this).apply { text = msg; textSize = 18f; setPadding(40, 200, 40, 40) })
    }
}
