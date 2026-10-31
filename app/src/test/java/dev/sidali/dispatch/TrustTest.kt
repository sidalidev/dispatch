package dev.sidali.dispatch

import org.junit.Assert.assertEquals
import org.junit.Test

class TrustTest {
    private val fp = "A1B2C3D4E5F60718293A4B5C6D7E8F90112233445566778899AABBCCDDEEFF01"

    @Test fun shortFingerprintKeepsFirstEightAndLastFour() {
        assertEquals("A1B2C3D4…FF01", Trust.short(fp))
    }

    @Test fun groupedFingerprintLooksLikeApksigner() {
        assertEquals("A1:B2:C3", Trust.grouped("A1B2C3"))
    }

    @Test fun hexIsUppercaseAndPadded() {
        assertEquals("000FFF", Trust.hex(byteArrayOf(0, 15, -1)))
    }

    @Test fun reviewDateInDayMonthYear() {
        assertEquals("24 Oct 2026", Trust.reviewDate("2026-10-24"))
        assertEquals("3 May 2027", Trust.reviewDate("2027-05-03"))
        assertEquals("soon", Trust.reviewDate("soon"))
    }

    @Test fun markdownBlocks() {
        val md = """
            # Changelog

            ## 0.3.0

            - **About** screen, see [SECURITY.md](SECURITY.md).
              Wrapped line.
            - `Erase everything`.

            A paragraph
            on two lines.
        """.trimIndent()
        assertEquals(
            listOf(
                Block.Title("Changelog"),
                Block.Heading("0.3.0"),
                Block.Bullet("About screen, see SECURITY.md. Wrapped line."),
                Block.Bullet("Erase everything."),
                Block.Para("A paragraph on two lines."),
            ),
            Block.parse(md),
        )
    }
}
