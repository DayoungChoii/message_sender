package com.mfort.momsitter.application

import com.mfort.momsitter.domain.MessageRepository
import com.mfort.momsitter.domain.ReservationRepository
import com.mfort.momsitter.fixture.Fixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.Mockito.*
import org.springframework.data.domain.PageRequest



class ReservationLoaderTest {

    private val reservationRepository = mock(ReservationRepository::class.java)
    private val messageRepository = mock(MessageRepository::class.java)

    private val loader = ReservationLoader(reservationRepository, messageRepository)

    @Test
    fun `빈 결과일 때 empty 반환`() {
        // given
        `when`(reservationRepository.findReadyReservationsWithLock(any(), any())).thenReturn(emptyList())

        // when
        val (reservations, messageMap) = loader.loadWithLock(10)

        // then
        assertThat(reservations).isEmpty()
        assertThat(messageMap).isEmpty()
        verify(messageRepository, never()).findAllById(any())
    }

    @Test
    fun `예약과 메시지를 정상적으로 로드`() {
        // given
        val messageId = 1L
        val reservationId = 10L
        val message = Fixture.getMessage(messageId)
        val reservation = Fixture.getReservation(messageId, reservationId)

        val pageable = PageRequest.of(0, 10)

        `when`(reservationRepository.findReadyReservationsWithLock(any(), any()))
            .thenReturn(listOf(reservation))

        `when`(messageRepository.findAllById(listOf(1L)))
            .thenReturn(listOf(message))

        // when
        val (reservations, messageMap) = loader.loadWithLock(10)

        // then
        assertThat(reservations).hasSize(1)
        assertThat(messageMap).hasSize(1)
        assertThat(messageMap[1L]).isEqualTo(message)
        verify(reservationRepository).findReadyReservationsWithLock(any(), any())
        verify(messageRepository).findAllById(listOf(1L))
    }
}