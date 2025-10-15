package com.mfort.momsitter.application.supporter

import com.mfort.momsitter.application.exception.CreateReservationException
import com.mfort.momsitter.common.exception.ExceptionCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class ReservationDueAtValidatorTest {

    private val fixedNow = Instant.parse("2025-10-02T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
    private val validator = ReservationDueAtValidator(clock)

    @Test
    fun `정상 - 현재 시간`() {
        validator.validate(fixedNow) // 예외 없어야 함
    }

    @Test
    fun `정상 - 1시간 뒤`() {
        validator.validate(fixedNow.plus(1, ChronoUnit.HOURS)) // 예외 없어야 함
    }

    @Test
    fun `비정상 - 과거 시간`() {
        val past = fixedNow.minusSeconds(1)
        val ex = assertThrows<CreateReservationException> {
            validator.validate(past)
        }
        assert(ex.exceptionCode == ExceptionCode.INVALID_DUE_AT)
    }

    @Test
    fun `비정상 - 3시간 뒤`() {
        val future = fixedNow.plus(3, ChronoUnit.HOURS)
        val ex = assertThrows<CreateReservationException> {
            validator.validate(future)
        }
        assert(ex.exceptionCode == ExceptionCode.INVALID_DUE_AT)
    }
}