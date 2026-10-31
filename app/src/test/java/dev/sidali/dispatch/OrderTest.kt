package dev.sidali.dispatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderTest {
    private val self = "dev.sidali.dispatch"
    private val signal = Target("org.thoughtcrime.securesms/.Share", "Signal", "Signal")
    private val igDm = Target("com.instagram.android/.DirectShare", "Messages", "Instagram")
    private val igFeed = Target("com.instagram.android/.Feed", "Feed", "Instagram")
    private val igStory = Target("com.instagram.android/.Story", "Stories", "Instagram")
    private val bluetooth = Target("com.android.bluetooth/.Opp", "bluetooth", "Bluetooth")
    private val me = Target("$self/.ShareActivity", "Dispatch", "Dispatch")
    private val all = listOf(signal, igDm, igFeed, igStory, bluetooth, me)

    @Test fun favoritesKeepUserOrderOthersByName() {
        val c = Choices().pin(igDm.key).pin(signal.key)
        val a = Arrange.arrange(all, self, c)
        assertEquals(listOf(igDm, signal), a.favorites)
        // Case-insensitive name order.
        assertEquals(listOf("bluetooth", "Feed", "Stories"), a.others.map { it.label })
    }

    @Test fun selfIsNeverListed() {
        val a = Arrange.arrange(all, self, Choices().pin(me.key))
        assertFalse(a.favorites.any { it.packageName == self })
        assertFalse(a.others.any { it.packageName == self })
    }

    @Test fun hiddenTargetsDisappear() {
        // Keep only Instagram Messages.
        val c = Choices().pin(igDm.key).hide(igFeed.key).hide(igStory.key)
        val a = Arrange.arrange(all, self, c)
        assertEquals(listOf(igDm), a.favorites)
        assertEquals(listOf(bluetooth, signal), a.others)
    }

    @Test fun pinnedButUnavailableIsSkippedNotForgotten() {
        val c = Choices().pin("gone.app/.X").pin(signal.key)
        val a = Arrange.arrange(listOf(signal), self, c)
        assertEquals(listOf(signal), a.favorites)
        assertEquals(listOf("gone.app/.X", signal.key), c.pinned)
    }

    @Test fun duplicatesFromSeveralQueriesMerged() {
        val a = Arrange.arrange(listOf(signal, signal, igDm, igDm), self, Choices().pin(signal.key))
        assertEquals(listOf(signal), a.favorites)
        assertEquals(listOf(igDm), a.others)
    }

    @Test fun pinHideAreExclusive() {
        var c = Choices().pin(signal.key).pin(signal.key)
        assertEquals(listOf(signal.key), c.pinned)
        c = c.hide(signal.key)
        assertEquals(emptyList<String>(), c.pinned); assertEquals(listOf(signal.key), c.hidden)
        c = c.pin(signal.key)
        assertEquals(listOf(signal.key), c.pinned); assertEquals(emptyList<String>(), c.hidden)
        assertEquals(Choices(), c.unpin(signal.key))
        assertEquals(Choices(), Choices().hide("a/b").unhide("a/b"))
    }

    @Test fun reorder() {
        val c = Choices(listOf("a/1", "b/2", "c/3", "d/4"))
        assertEquals(listOf("c/3", "a/1", "b/2", "d/4"), c.move(2, 0).pinned)
        assertEquals(listOf("b/2", "c/3", "d/4", "a/1"), c.move(0, 3).pinned)
        assertEquals(c, c.move(0, 9))
        assertEquals(c, c.move(-1, 0))
        assertEquals(listOf("b/2", "a/1", "c/3", "d/4"), c.moveKey("b/2", -1).pinned)
        assertEquals(c, c.moveKey("a/1", -1))
        assertEquals(listOf("a/1", "b/2", "d/4", "c/3"), c.moveKey("c/3", 1).pinned)
        assertEquals(c, c.moveKey("x/y", 1))
    }

    @Test fun reorderOnlyWhatIsVisible() {
        // "x/9" cannot receive images, so the share screen does not show it; it keeps its place.
        val c = Choices(listOf("a/1", "x/9", "b/2", "c/3"))
        assertEquals(listOf("c/3", "x/9", "a/1", "b/2"), c.reorderVisible(listOf("c/3", "a/1", "b/2")).pinned)
        assertEquals(c, c.reorderVisible(listOf("a/1", "b/2", "c/3")))
        // Unknown keys are ignored.
        assertEquals(listOf("b/2", "x/9", "a/1", "c/3"), c.reorderVisible(listOf("b/2", "zz/1", "a/1")).pinned)
    }

    @Test fun orderSurvivesStorage() {
        val c = Choices().pin("b/2").pin("a/1").pin("c/3").move(2, 0)
        val raw = Choices.encode(c.pinned)
        assertEquals(c.pinned, Choices.decode(raw))
        assertEquals(listOf("c/3", "b/2", "a/1"), Choices.decode(raw))
    }

    @Test fun storageIsTolerant() {
        assertEquals(emptyList<String>(), Choices.decode(null))
        assertEquals(emptyList<String>(), Choices.decode(""))
        assertEquals(listOf("a/1", "b/2"), Choices.decode("a/1\n\njunk\n/x\ny/\na/1\n b/2 \nc/d/e\nsp ace/x"))
        assertTrue(Choices.isKey("com.whatsapp/com.whatsapp.contact.ui.picker.ContactPicker"))
    }

    @Test fun lastUsedOnlyWhenAvailable() {
        assertEquals(signal, Arrange.arrange(all, self, Choices(), lastUsed = signal.key).last)
        assertNull(Arrange.arrange(all, self, Choices().hide(signal.key), lastUsed = signal.key).last)
        assertNull(Arrange.arrange(all, self, Choices(), lastUsed = me.key).last)
        assertNull(Arrange.arrange(all, self, Choices()).last)
    }

    @Test fun sameNamesToldApart() {
        val a = Target("com.google.android.youtube/com.google.android.apps.youtube.app.application.Shell_UploadActivity", "YouTube", "YouTube")
        val b = Target("com.google.android.youtube/com.google.android.youtube.ShareToShortsAlias", "YouTube", "YouTube")
        val out = Labels.disambiguate(listOf(a, b, signal, a))
        assertEquals(listOf("Shell Upload", "Share To Shorts", "Signal", "Shell Upload"), out.map { it.app })
        assertEquals(listOf(signal), Labels.disambiguate(listOf(signal)))
        assertEquals("Main", Labels.humanize("a/b.Main"))
        assertEquals("Share", Labels.humanize("a/b.ui.ShareIntentActivity"))
        assertEquals("Activity", Labels.humanize("a/b.Activity"))
    }
}
