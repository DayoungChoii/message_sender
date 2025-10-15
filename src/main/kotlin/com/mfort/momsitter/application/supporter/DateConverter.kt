package com.mfort.momsitter.application.supporter

import com.mfort.momsitter.application.exception.DateParseException
import com.mfort.momsitter.common.exception.ExceptionCode.INVALID_DATE_PARSE

fun String.toInstantUtcOrThrow(): java.time.Instant = try {
    java.time.OffsetDateTime.parse(this).toInstant()
} catch (e: Exception) {
    throw DateParseException(INVALID_DATE_PARSE)
}
