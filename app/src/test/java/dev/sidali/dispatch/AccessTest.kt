package dev.sidali.dispatch

import dev.sidali.dispatch.Access.Action
import dev.sidali.dispatch.Access.Kind
import dev.sidali.dispatch.Access.Row
import dev.sidali.dispatch.Access.Snapshot
import dev.sidali.dispatch.Access.Status
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessTest {
    private val fresh = Snapshot(grantedPermissions = 0, requestedPermissions = 0, notifications = false, unrestricted = false)

    @Test fun freshInstallAsksForNothing() {
        assertEquals(
            listOf(
                Row(Kind.Permissions, Status.None, Action.CheckInSettings),
                Row(Kind.Notifications, Status.Off, Action.TurnOnInSettings),
                Row(Kind.Battery, Status.Optimized, Action.TurnOnInSettings),
            ),
            Access.rows(fresh),
        )
    }

    @Test fun whatIsOnCanBeTurnedOff() {
        val rows = Access.rows(fresh.copy(notifications = true, unrestricted = true))
        assertEquals(Row(Kind.Notifications, Status.On, Action.TurnOffInSettings), rows[1])
        assertEquals(Row(Kind.Battery, Status.Unrestricted, Action.TurnOffInSettings), rows[2])
    }

    @Test fun grantedPermissionsAreCountedAndCanBeRemoved() {
        assertEquals(Row(Kind.Permissions, Status.Some, Action.TurnOffInSettings, 2), Access.rows(fresh.copy(grantedPermissions = 2, requestedPermissions = 3))[0])
        assertEquals(Row(Kind.Permissions, Status.Some, Action.CheckInSettings, 0), Access.rows(fresh.copy(requestedPermissions = 1))[0])
    }

    @Test fun everyRowAlwaysHasAWayToSettings() {
        listOf(true, false).forEach { n -> listOf(true, false).forEach { b ->
            assertEquals(3, Access.rows(fresh.copy(notifications = n, unrestricted = b)).size)
        } }
    }
}
