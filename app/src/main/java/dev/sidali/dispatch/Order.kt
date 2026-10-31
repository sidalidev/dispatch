package dev.sidali.dispatch

import java.text.Collator

/**
 * A share target: one activity of one app. [key] is the flattened ComponentName ("package/class"),
 * so "Instagram Messages" and "Instagram Stories" are two different targets.
 */
data class Target(val key: String, val label: String, val app: String) {
    val packageName: String get() = key.substringBefore('/')
}

/** What the share screen shows, in order. */
data class Arranged(val favorites: List<Target>, val others: List<Target>, val last: Target?)

/**
 * The user's choices: pinned targets in their order, and hidden targets.
 * A target is never both pinned and hidden. Immutable: every change returns a new value.
 */
data class Choices(val pinned: List<String> = emptyList(), val hidden: List<String> = emptyList()) {

    fun pin(key: String) = copy(pinned = if (key in pinned) pinned else pinned + key, hidden = hidden - key)
    fun unpin(key: String) = copy(pinned = pinned - key)
    fun hide(key: String) = copy(pinned = pinned - key, hidden = if (key in hidden) hidden else hidden + key)
    fun unhide(key: String) = copy(hidden = hidden - key)

    /** Moves the pinned target at [from] to [to]; out of range indexes are ignored. */
    fun move(from: Int, to: Int): Choices {
        if (from !in pinned.indices || to !in pinned.indices || from == to) return this
        val list = pinned.toMutableList()
        list.add(to, list.removeAt(from))
        return copy(pinned = list)
    }

    /** Moves the pinned [key] by [by] places (negative = earlier), stopping at both ends. */
    fun moveKey(key: String, by: Int): Choices {
        val i = pinned.indexOf(key)
        return if (i < 0) this else move(i, (i + by).coerceIn(0, pinned.size - 1))
    }

    /**
     * Applies a new order of the pinned targets the user can see right now. Pinned targets that are
     * not visible (an app that cannot receive this kind of content) keep their place.
     */
    fun reorderVisible(visibleInNewOrder: List<String>): Choices {
        val visible = visibleInNewOrder.filter { it in pinned }.distinct()
        val set = visible.toSet()
        if (set.size != pinned.count { it in set }) return this
        val next = visible.iterator()
        return copy(pinned = pinned.map { if (it in set) next.next() else it })
    }

    companion object {
        /** One flattened ComponentName per line: they never contain a line break. */
        fun encode(keys: List<String>): String = keys.joinToString("\n")

        fun decode(raw: String?): List<String> =
            raw.orEmpty().split('\n').map { it.trim() }.filter { isKey(it) }.distinct()

        /** "package/class", nothing else. Anything else in storage is dropped. */
        fun isKey(s: String): Boolean {
            val slash = s.indexOf('/')
            return slash > 0 && slash < s.length - 1 && s.indexOf('/', slash + 1) < 0 && s.none { it.isWhitespace() }
        }
    }
}

object Arrange {
    /**
     * Splits the targets that can receive the current share into the user's favorites (their order)
     * and the others (by name). Dispatch itself and hidden targets never appear; duplicates returned
     * by several queries are merged.
     */
    fun arrange(
        candidates: List<Target>,
        selfPackage: String,
        choices: Choices,
        lastUsed: String? = null,
        collator: Comparator<String> = defaultCollator(),
    ): Arranged {
        val byKey = LinkedHashMap<String, Target>()
        for (t in candidates) if (t.packageName != selfPackage && t.key !in choices.hidden) byKey.putIfAbsent(t.key, t)
        val favorites = choices.pinned.mapNotNull { byKey[it] }
        val pinnedSet = choices.pinned.toSet()
        val others = byKey.values.filter { it.key !in pinnedSet }
            .sortedWith(compareBy(collator) { t: Target -> t.label }.thenBy(collator) { it.app }.thenBy { it.key })
        val last = lastUsed?.let { byKey[it] }
        return Arranged(favorites, others, last)
    }

    fun defaultCollator(): Comparator<String> = Collator.getInstance().apply { strength = Collator.SECONDARY }
        .let { c -> Comparator { a, b -> c.compare(a, b) } }
}

/** Two targets with the same name in the same app (YouTube has two "YouTube") get told apart. */
object Labels {
    fun disambiguate(targets: List<Target>): List<Target> {
        val counts = targets.distinctBy { it.key }.groupingBy { it.label to it.app }.eachCount()
        return targets.map { t -> if ((counts[t.label to t.app] ?: 1) > 1) t.copy(app = humanize(t.key)) else t }
    }

    /** "pkg/com.example.ui.Shell_UploadActivity" becomes "Shell Upload". */
    fun humanize(key: String): String {
        val simple = key.substringAfter('/').substringAfterLast('.').substringAfterLast('$')
        val trimmed = listOf("Activity", "Alias", "Launcher", "Handler", "Intent").fold(simple) { s, suffix -> s.removeSuffix(suffix) }
            .ifEmpty { simple }
        return trimmed.replace('_', ' ')
            .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
            .split(' ').filter { it.isNotBlank() }.joinToString(" ")
    }
}
