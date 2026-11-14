package com.everfold.kotlin.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThemeColorTest {
    @Test
    fun parsesSixDigitRgbWithOptionalPrefixAndWhitespace() {
        listOf("#aB09ef", "aB09ef", "  #aB09ef  ").forEach { input ->
            assertEquals(0xFFAB09EF.toInt(), parseHexColor(input)?.toArgb(), input)
        }
    }

    @Test
    fun rejectsIncompleteNonHexAndAlphaInputs() {
        listOf("", "#", "#ABC", "12345", "1234567", "#FF123456", "#GG0000", "12 456", "##123456").forEach { input ->
            assertNull(parseHexColor(input), input)
        }
    }

    @Test
    fun formatsRgbAsSixUppercaseDigitsWithoutAlpha() {
        assertEquals("#000000", Color.Black.toHexColor())
        assertEquals("#FFFFFF", Color.White.toHexColor())
        assertEquals("#0012AB", Color(0x800012AB).toHexColor())
    }
}
