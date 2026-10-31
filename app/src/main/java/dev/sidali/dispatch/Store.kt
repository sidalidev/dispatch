package dev.sidali.dispatch

import android.annotation.SuppressLint
import android.content.Context

/**
 * Everything Dispatch remembers: pinned targets in order, hidden targets, and one switch.
 * The last used target is stored only while "Remember last used" is on. No counters, no history.
 */
object Store {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("dispatch", Context.MODE_PRIVATE)

    fun choices(ctx: Context): Choices = prefs(ctx).let {
        Choices(Choices.decode(it.getString("pinned", null)), Choices.decode(it.getString("hidden", null)))
    }

    fun setChoices(ctx: Context, c: Choices) {
        prefs(ctx).edit()
            .putString("pinned", Choices.encode(c.pinned))
            .putString("hidden", Choices.encode(c.hidden))
            .apply()
    }

    fun rememberLast(ctx: Context): Boolean = prefs(ctx).getBoolean("remember_last", false)

    fun setRememberLast(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean("remember_last", on).apply { if (!on) remove("last") }.apply()
    }

    fun last(ctx: Context): String? = if (rememberLast(ctx)) prefs(ctx).getString("last", null) else null

    fun used(ctx: Context, key: String) {
        if (rememberLast(ctx)) prefs(ctx).edit().putString("last", key).apply()
    }

    /**
     * Back to first launch: no favorites, nothing hidden, switch off. Cleared in memory and on disk
     * right away (commit, not apply), then the file itself is deleted.
     */
    @SuppressLint("ApplySharedPref")
    fun erase(ctx: Context) {
        prefs(ctx).edit().clear().commit()
        ctx.deleteSharedPreferences("dispatch")
    }
}
