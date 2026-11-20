package com.everfold.kotlin.ui.localization

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import kotlin.time.Instant

private const val MILLIS_PER_SECOND = 1000.0

internal actual fun formatLocalTimestamp(instant: Instant): String {
    val formatter =
        NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd HH:mm:ss"
            locale = NSLocale(localeIdentifier = "en_US_POSIX")
            timeZone = NSTimeZone.localTimeZone
        }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(instant.toEpochMilliseconds() / MILLIS_PER_SECOND))
}
