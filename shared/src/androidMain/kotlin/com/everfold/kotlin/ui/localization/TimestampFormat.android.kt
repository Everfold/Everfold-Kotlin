package com.everfold.kotlin.ui.localization

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Instant

internal actual fun formatLocalTimestamp(instant: Instant): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(instant.toEpochMilliseconds()))
