package com.mfort.momsitter.application

import com.mfort.momsitter.domain.ReservationRepository
import com.mfort.momsitter.domain.ReservationStatus.*
import com.mfort.momsitter.fixture.Fixture
import com.mfort.momsitter.infra.SendResult.Failure
import com.mfort.momsitter.infra.SendResult.Success
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.Instant.now

class SendResultApplierTest {

    private val reservationRepository: ReservationRepository = mock()
    private val applier = SendResultApplier(reservationRepository)

    private val messageId = 1L
    private val reservationId = 10L

    @Test
    fun `성공 결과는 DONE 상태로 변경된다`() {
        // given
        val reservation = Fixture.getReservation(messageId, reservationId)
        val results = listOf(Success(reservation.id!!))

        // when
        applier.apply(listOf(reservation), results)

        // then
        assertThat(reservation.status).isEqualTo(DONE)
        verify(reservationRepository).save(reservation)
    }

    @Test
    fun `재시도 가능한 실패는 RETRY_READY로 변경되고 retryCount 증가`() {
        //given
        val reservation = Fixture.getReservation(messageId, reservationId)
        val results = listOf(Failure(reservation.id!!, retryable = true))

        //when
        applier.apply(listOf(reservation), results)

        //then
        assertThat(reservation.status).isEqualTo(RETRY_READY)
        assertThat(reservation.retryCount).isEqualTo(1)
        assertThat(reservation.nextAttemptAt).isAfter(now())
        verify(reservationRepository).save(reservation)
    }

    @Test
    fun `재시도 불가능한 실패는 FAILED 상태로 변경된다`() {
        //given
        val reservation = Fixture.getReservation(messageId, reservationId)
        val results = listOf(Failure(reservation.id!!, retryable = false))

        //when
        applier.apply(listOf(reservation), results)

        //then
        assertThat(reservation.status).isEqualTo(FAILED)
        verify(reservationRepository).save(reservation)
    }
}