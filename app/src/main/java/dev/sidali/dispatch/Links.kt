package dev.sidali.dispatch

/**
 * Removes tracking parameters from links before they go to the clipboard.
 * Pure Kotlin on purpose: no android.net.Uri, so it runs in plain unit tests and never re-encodes
 * the rest of the link. Only the query string is touched; the fragment and the other parameters
 * keep their order and spelling.
 */
object Links {
    /** Dropped on every site. */
    private val ALWAYS = setOf("fbclid", "gclid", "igshid", "igsh")
    private val PREFIXES = listOf("utm_", "mc_")
    /** `si` is a share id on YouTube and Spotify, but a real parameter elsewhere. */
    private val SI_HOSTS = listOf("youtube.com", "youtu.be", "spotify.com")

    private val URL = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)

    data class Result(val text: String, val removed: Int)

    fun isLink(text: String): Boolean = text.trim().let { t -> URL.matchEntire(t) != null }

    /** Cleans every link found in [text]; everything else is left as is. */
    fun cleanText(text: String): Result {
        var removed = 0
        val out = URL.replace(text) { m ->
            val r = clean(m.value)
            removed += r.removed
            r.text
        }
        return Result(out, removed)
    }

    fun clean(url: String): Result {
        val hashAt = url.indexOf('#')
        val beforeHash = if (hashAt >= 0) url.substring(0, hashAt) else url
        val fragment = if (hashAt >= 0) url.substring(hashAt) else ""
        val qAt = beforeHash.indexOf('?')
        if (qAt < 0) return Result(url, 0)
        val base = beforeHash.substring(0, qAt)
        val query = beforeHash.substring(qAt + 1)
        val host = host(base)
        val params = query.split('&')
        val kept = params.filter { p -> p.isNotEmpty() && !isTracker(p.substringBefore('=').lowercase(), host) }
        val removed = params.count { it.isNotEmpty() } - kept.size
        if (removed == 0) return Result(url, 0)
        val rebuilt = if (kept.isEmpty()) base else base + "?" + kept.joinToString("&")
        return Result(rebuilt + fragment, removed)
    }

    internal fun isTracker(name: String, host: String): Boolean {
        if (name in ALWAYS) return true
        if (PREFIXES.any { name.startsWith(it) }) return true
        if (name == "si" && SI_HOSTS.any { host == it || host.endsWith(".$it") }) return true
        return false
    }

    /** Host of an absolute http(s) URL, lowercased, without user info or port. */
    internal fun host(base: String): String {
        val afterScheme = base.substringAfter("://", "")
        val authority = afterScheme.substringBefore('/')
        return authority.substringAfterLast('@').substringBefore(':').lowercase()
    }
}
