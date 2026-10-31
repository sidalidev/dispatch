package dev.sidali.dispatch

import android.content.Context
import android.content.pm.PackageManager
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the About screen shows so anyone can check it: signing certificate, last review, documents. */
object Trust {
    const val CONTACT = "contact@sidali.dev"
    const val SITE = "https://sidali.dev"

    /** SHA-256 of the certificate that signed the installed APK, read from the system at run time. */
    fun signingSha256(ctx: Context): String? = try {
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val si = info.signingInfo
        // The current signer is the last entry of the history; a multi-signer APK lists its signers instead.
        val cert = when {
            si == null -> null
            si.hasMultipleSigners() -> si.apkContentsSigners.firstOrNull()
            else -> si.signingCertificateHistory.lastOrNull()
        }
        cert?.let { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }
    } catch (_: Exception) { null }

    fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02X".format(it) }

    /** First 8 and last 4 characters, enough to compare at a glance with SECURITY.md. */
    fun short(fingerprint: String) =
        if (fingerprint.length <= 12) fingerprint else fingerprint.take(8) + "…" + fingerprint.takeLast(4)

    /** Pairs separated by colons, the way keytool and apksigner print it. */
    fun grouped(fingerprint: String) = fingerprint.chunked(2).joinToString(":")

    /** "2026-10-24" becomes "24 Oct 2026". */
    fun reviewDate(iso: String): String = try {
        LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    } catch (_: Exception) { iso }

    /** CHANGELOG.md and PRIVACY.md, bundled from the repository at build time. */
    fun document(ctx: Context, name: String): String =
        try { ctx.assets.open(name).bufferedReader().use { it.readText() } } catch (_: Exception) { "" }

    /**
     * Erase everything: favorites, hidden apps, settings and the private copies made by Copy image.
     * Dispatch holds no key and no account, so this is all there is.
     */
    fun eraseEverything(ctx: Context) {
        Store.erase(ctx)
        listOf(ctx.filesDir, ctx.noBackupFilesDir, ctx.cacheDir).forEach { dir ->
            dir.listFiles()?.forEach(File::deleteRecursively)
        }
    }
}

/** A tiny reader for the Markdown of CHANGELOG.md and PRIVACY.md: headings, bullets, paragraphs. */
sealed interface Block {
    data class Title(val text: String) : Block
    data class Heading(val text: String) : Block
    data class Bullet(val text: String) : Block
    data class Para(val text: String) : Block

    companion object {
        private val LINK = Regex("""\[([^\]]+)]\([^)]*\)""")

        private fun inline(s: String) = LINK.replace(s) { it.groupValues[1] }.replace("**", "").replace("`", "").trim()

        fun parse(md: String): List<Block> {
            val out = mutableListOf<Block>()
            val para = StringBuilder()
            fun flush() { if (para.isNotBlank()) out += Para(inline(para.toString())); para.clear() }
            for (raw in md.lines()) {
                val line = raw.trimEnd()
                when {
                    line.isBlank() -> flush()
                    line.startsWith("## ") -> { flush(); out += Heading(inline(line.removePrefix("## "))) }
                    line.startsWith("# ") -> { flush(); out += Title(inline(line.removePrefix("# "))) }
                    line.startsWith("- ") -> { flush(); out += Bullet(inline(line.removePrefix("- "))) }
                    // A wrapped bullet continues the previous one.
                    line.startsWith("  ") && out.lastOrNull() is Bullet && para.isEmpty() ->
                        out[out.lastIndex] = Bullet((out.last() as Bullet).text + " " + inline(line))
                    else -> { if (para.isNotEmpty()) para.append(' '); para.append(line.trim()) }
                }
            }
            flush()
            return out
        }
    }
}
