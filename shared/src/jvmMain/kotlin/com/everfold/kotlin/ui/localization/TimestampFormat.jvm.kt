package com.everfold.kotlin.ui.localization

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant

private val LocalTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)

internal actual fun formatLocalTimestamp(instant: Instant): String =
    LocalTimestampFormatter
        .withZone(ZoneId.systemDefault())
        .format(java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()))
