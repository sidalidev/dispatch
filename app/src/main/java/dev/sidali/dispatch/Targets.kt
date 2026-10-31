package dev.sidali.dispatch

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap

/**
 * Finds the apps that can receive a share, the same way the system share menu does: one
 * queryIntentActivities() call with the very intent that was shared. Visibility comes from the
 * <queries> SEND intents in the manifest, not from QUERY_ALL_PACKAGES.
 */
object Targets {

    /** Targets for this exact share (same action, type and data as the system menu would use). */
    fun forShare(ctx: Context, shared: Intent): List<Target> {
        val probe = Intent(shared).apply {
            component = null; `package` = null; selector = null
            if (type == null && action == Intent.ACTION_SEND) type = "text/plain"
        }
        return Labels.disambiguate(resolve(ctx, probe))
    }

    /** Every target that can receive at least one common kind of content, for the settings screen. */
    fun all(ctx: Context): List<Target> {
        val probes = listOf(
            Intent.ACTION_SEND to "text/plain", Intent.ACTION_SEND to "image/*", Intent.ACTION_SEND to "video/*",
            Intent.ACTION_SEND to "*/*", Intent.ACTION_SEND_MULTIPLE to "image/*", Intent.ACTION_SEND_MULTIPLE to "*/*",
        )
        val seen = LinkedHashMap<String, Target>()
        for ((action, type) in probes) for (t in resolve(ctx, Intent(action).setType(type))) seen.putIfAbsent(t.key, t)
        return Labels.disambiguate(seen.values.toList())
    }

    private fun resolve(ctx: Context, intent: Intent): List<Target> {
        val pm = ctx.packageManager
        val infos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION") pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return infos.mapNotNull { ri ->
            val act = ri.activityInfo ?: return@mapNotNull null
            if (act.packageName == ctx.packageName || !act.exported) return@mapNotNull null
            // Some targets require a permission Dispatch does not hold: launching them would fail.
            if (act.permission != null && ctx.checkSelfPermission(act.permission) != PackageManager.PERMISSION_GRANTED) return@mapNotNull null
            val key = ComponentName(act.packageName, act.name).flattenToString()
            val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty()
            val app = act.applicationInfo.loadLabel(pm).toString().trim()
            Target(key, label.ifEmpty { app }, app)
        }
    }

    private val icons = ConcurrentHashMap<String, ImageBitmap>()

    fun cachedIcon(key: String): ImageBitmap? = icons[key]

    /** The activity's own icon (Instagram Stories and Instagram Messages may differ), cached in memory. */
    fun icon(ctx: Context, key: String, px: Int): ImageBitmap? {
        icons[key]?.let { return it }
        val cn = ComponentName.unflattenFromString(key) ?: return null
        val d = try { ctx.packageManager.getActivityIcon(cn) } catch (_: PackageManager.NameNotFoundException) { return null }
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, px, px)
        d.draw(Canvas(bmp))
        return bmp.asImageBitmap().also { icons[key] = it }
    }

    /**
     * Sends the original share to [key]: same action, type, extras and ClipData, with read access to
     * the shared files passed on. Dispatch can grant it because it holds that access itself.
     */
    fun launch(act: Activity, shared: Intent, key: String): Boolean {
        val cn = ComponentName.unflattenFromString(key) ?: return false
        val grants = shared.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val out = Intent(shared).apply {
            component = cn; `package` = null; selector = null
            flags = grants or Intent.FLAG_GRANT_READ_URI_PERMISSION
            // When Dispatch was opened in a task of its own, the target gets its own task too, so it
            // is not stuck in a task that is hidden from recents.
            if (act.isTaskRoot) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            act.startActivity(out); true
        } catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }
    }
}
