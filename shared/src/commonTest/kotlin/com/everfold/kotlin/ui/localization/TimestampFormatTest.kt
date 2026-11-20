package com.everfold.kotlin.ui.localization

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

class TimestampFormatTest {
    @Test
    fun localTimestampUsesExpectedDateTimePattern() {
        val pattern = Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$""")
        listOf(
            Instant.fromEpochMilliseconds(0L),
            Instant.parse("2026-06-15T12:34:56Z"),
        ).forEach { instant ->
            val formatted = formatLocalTimestamp(instant)
            assertTrue(pattern.matches(formatted), "Unexpected format: $formatted")
        }
    }
}
