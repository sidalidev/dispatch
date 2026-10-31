package dev.sidali.dispatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinksTest {
    private fun clean(u: String) = Links.clean(u).text

    @Test fun utmRemovedOtherParamsKept() {
        assertEquals("https://example.com/?id=3", clean("https://example.com/?utm_source=x&id=3"))
        assertEquals("https://example.com/a?b=1&c=2", clean("https://example.com/a?utm_medium=m&b=1&utm_campaign=z&c=2"))
        assertEquals(2, Links.clean("https://example.com/a?utm_medium=m&b=1&utm_campaign=z").removed)
    }

    @Test fun questionMarkDroppedWhenNothingLeft() {
        assertEquals("https://example.com/page", clean("https://example.com/page?utm_source=x&fbclid=abc"))
    }

    @Test fun clickIds() {
        assertEquals("https://shop.example/p", clean("https://shop.example/p?gclid=1"))
        assertEquals("https://example.com/", clean("https://example.com/?fbclid=IwAR0"))
        assertEquals("https://www.instagram.com/p/xyz/", clean("https://www.instagram.com/p/xyz/?igshid=abc"))
        assertEquals("https://www.instagram.com/p/xyz/", clean("https://www.instagram.com/p/xyz/?igsh=abc"))
        assertEquals("https://news.example/a", clean("https://news.example/a?mc_cid=1&mc_eid=2"))
    }

    @Test fun siOnlyForYoutubeAndSpotify() {
        assertEquals("https://youtu.be/dQw4w9WgXcQ", clean("https://youtu.be/dQw4w9WgXcQ?si=abc"))
        assertEquals("https://www.youtube.com/watch?v=1", clean("https://www.youtube.com/watch?v=1&si=abc"))
        assertEquals("https://music.youtube.com/watch?v=1", clean("https://music.youtube.com/watch?v=1&si=x"))
        assertEquals("https://open.spotify.com/track/1", clean("https://open.spotify.com/track/1?si=abc"))
        // Elsewhere `si` may be a real parameter.
        assertEquals("https://example.com/?si=1", clean("https://example.com/?si=1"))
        assertEquals("https://notyoutube.com/?si=1", clean("https://notyoutube.com/?si=1"))
    }

    @Test fun caseAndFragmentAndEncoding() {
        assertEquals("https://example.com/?q=a%20b#top", clean("https://example.com/?UTM_Source=x&q=a%20b#top"))
        assertEquals("https://example.com/#utm_source=x", clean("https://example.com/#utm_source=x"))
        assertEquals("https://example.com/", Links.clean("https://example.com/").text)
        assertEquals(0, Links.clean("https://example.com/?id=3").removed)
    }

    @Test fun lookalikesKept() {
        assertEquals("https://example.com/?utm=1&gclid_x=2", clean("https://example.com/?utm=1&gclid_x=2"))
    }

    @Test fun linksInsideText() {
        val r = Links.cleanText("Look at this https://example.com/?utm_source=x&id=3 and https://youtu.be/a?si=z please")
        assertEquals("Look at this https://example.com/?id=3 and https://youtu.be/a please", r.text)
        assertEquals(2, r.removed)
        assertEquals(Links.Result("No links here", 0), Links.cleanText("No links here"))
    }

    @Test fun host() {
        assertEquals("www.youtube.com", Links.host("https://user@WWW.YouTube.com:443/watch"))
        assertTrue(Links.isLink(" https://example.com/x "))
        assertFalse(Links.isLink("hello https://example.com"))
    }
}
