package dev.sidali.dispatch

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * What Android lets Dispatch do, read live from the system every time the screen comes back.
 * Dispatch asks for no permission, so every row says so honestly, and each one opens the exact
 * Settings page where it can be checked, turned on or turned off. Both directions are one tap.
 */
object Access {
    enum class Kind { Permissions, Notifications, Battery }

    /** What the row shows on the right, and what a tap does, said out loud. */
    enum class Status { None, Some, On, Off, Optimized, Unrestricted }
    enum class Action { CheckInSettings, TurnOnInSettings, TurnOffInSettings }

    data class Row(val kind: Kind, val status: Status, val action: Action, val granted: Int = 0)

    /** The system's view of the app, as plain values so the rows can be tested without a phone. */
    data class Snapshot(val grantedPermissions: Int, val requestedPermissions: Int, val notifications: Boolean, val unrestricted: Boolean)

    fun rows(s: Snapshot): List<Row> = listOf(
        if (s.requestedPermissions == 0) Row(Kind.Permissions, Status.None, Action.CheckInSettings)
        else Row(Kind.Permissions, Status.Some, if (s.grantedPermissions > 0) Action.TurnOffInSettings else Action.CheckInSettings, s.grantedPermissions),
        if (s.notifications) Row(Kind.Notifications, Status.On, Action.TurnOffInSettings)
        else Row(Kind.Notifications, Status.Off, Action.TurnOnInSettings),
        if (s.unrestricted) Row(Kind.Battery, Status.Unrestricted, Action.TurnOffInSettings)
        else Row(Kind.Battery, Status.Optimized, Action.TurnOnInSettings),
    )

    /** Runtime permissions the installed app requests, and how many of them are granted right now. */
    fun snapshot(ctx: Context): Snapshot {
        val info: PackageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS)
        val names = info.requestedPermissions.orEmpty()
        val flags = info.requestedPermissionsFlags ?: IntArray(names.size)
        val granted = flags.count { it and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 }
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val pm = ctx.getSystemService(PowerManager::class.java)
        return Snapshot(
            grantedPermissions = granted,
            requestedPermissions = names.size,
            notifications = nm?.areNotificationsEnabled() == true,
            unrestricted = pm?.isIgnoringBatteryOptimizations(ctx.packageName) == true,
        )
    }

    /** The exact Settings page for each row; the app's own page when a phone has no narrower one. */
    fun open(ctx: Context, kind: Kind) {
        val app = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
        val exact = when (kind) {
            Kind.Notifications -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            Kind.Permissions, Kind.Battery -> app
        }
        try { ctx.startActivity(exact) } catch (_: ActivityNotFoundException) {
            try { ctx.startActivity(app) } catch (_: ActivityNotFoundException) { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }
    }
}
