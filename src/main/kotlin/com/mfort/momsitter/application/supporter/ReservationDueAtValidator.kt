package com.mfort.momsitter.application.supporter

import com.mfort.momsitter.application.exception.CreateReservationException
import com.mfort.momsitter.common.exception.ExceptionCode.INVALID_DUE_AT
import com.mfort.momsitter.common.validation.ParameterValidator
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit.HOURS

@Component
class ReservationDueAtValidator(
    private val clock: Clock
): ParameterValidator<Instant> {
    override fun validate(dueAt: Instant) {
        val now = clock.instant()
        ifDueAtIsValidated(now, dueAt)
    }

    private fun ifDueAtIsValidated(now: Instant, dueAt: Instant) {
        if (dueAt.isBefore(now) || dueAt.isAfter(now.plus(2, HOURS))) {
            throw CreateReservationException(INVALID_DUE_AT)
        }
    }
}