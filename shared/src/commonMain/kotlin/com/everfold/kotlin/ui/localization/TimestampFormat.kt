package com.everfold.kotlin.ui.localization

import kotlin.time.Instant

/** 将时间戳格式化为本地时区可读的日期时间字符串。 */
internal expect fun formatLocalTimestamp(instant: Instant): String
