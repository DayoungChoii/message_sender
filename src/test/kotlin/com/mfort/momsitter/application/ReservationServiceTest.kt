package com.mfort.momsitter.application

import com.mfort.momsitter.api.dto.CreateReservationRequest
import com.mfort.momsitter.api.dto.CursorPageResponse
import com.mfort.momsitter.api.dto.ScheduledMessageResponse
import com.mfort.momsitter.application.exception.CreateReservationException
import com.mfort.momsitter.application.supporter.ReservationDueAtValidator
import com.mfort.momsitter.application.supporter.toInstantUtcOrThrow
import com.mfort.momsitter.domain.*
import com.mfort.momsitter.fixture.Fixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import java.time.Instant
import java.time.temporal.ChronoUnit

class ReservationServiceTest {

    private val validator: ReservationDueAtValidator = mock()
    private val messageRepository: MessageRepository = mock()
    private val reservationRepository: ReservationRepository = mock()
    private val reservationLoader: ReservationLoader = mock()

    private val reservationService = ReservationService(
        validator,
        messageRepository,
        reservationRepository,
        reservationLoader
    )

    private val messageId1 = 1L
    private val messageId2 = 2L
    private val reservationId1 = 10L
    private val reservationId2 = 20L

    @Test
    fun `예약 생성 성공`() {
        // given
        val dueAt = Instant.now().plus(10, ChronoUnit.MINUTES)
        val request = CreateReservationRequest(
            phoneNumber = "01012345678",
            title = "예약 알림",
            contents = "오늘 5시에 픽업 예약이 있습니다.",
            dueAt = dueAt.toString()
        )

        val message = Fixture.getMessage(messageId1)
        val reservation = Fixture.getReservation(messageId1, reservationId1)

        `when`(messageRepository.save(any())).thenReturn(message)
        `when`(reservationRepository.save(any())).thenReturn(reservation)

        // when
        val result = reservationService.createReservation(request)

        // then
        assertThat(result).isEqualTo(reservationId1)

        verify(validator).validate(request.dueAt.toInstantUtcOrThrow())
        verify(messageRepository).save(any())
        verify(reservationRepository).save(any())
    }

    @Test
    fun `메시지 저장 후 id가 null이면 예외 발생`() {
        // given
        val dueAt = Instant.now().plus(10, ChronoUnit.MINUTES)
        val request = CreateReservationRequest(
            phoneNumber = "01012345678",
            title = "예약 알림",
            contents = "오늘 5시에 픽업 예약이 있습니다.",
            dueAt = dueAt.toString()
        )

        val messageWithoutId = Fixture.getMessage(id = 0L).apply {
            val field = this::class.java.getDeclaredField("id")
            field.isAccessible = true
            field.set(this, null)
        }

        `when`(messageRepository.save(any())).thenReturn(messageWithoutId)

        // when & then
        assertThrows<CreateReservationException> {
            reservationService.createReservation(request)
        }

        verify(messageRepository).save(any())
        verify(reservationRepository, never()).save(any())
    }

    @Test
    fun `커서 조회 - 데이터가 있을 때 nextCursor 생성`() {
        // given
        val reservations = listOf(
            Fixture.getReservation(messageId1, reservationId1),
            Fixture.getReservation(messageId2, reservationId2)
        )
        val messages = mapOf(
            1L to Fixture.getMessage(1L),
            2L to Fixture.getMessage(2L)
        )

        `when`(reservationLoader.loadByCursor(ReservationStatus.READY, null, 2))
            .thenReturn(reservations to messages)

        // when
        val result: CursorPageResponse<ScheduledMessageResponse> =
            reservationService.listByCursor(ReservationStatus.READY, null, 2)

        // then
        assertThat(result.items).hasSize(2)
        assertThat(result.nextCursor).isNotNull
    }

    @Test
    fun `커서 조회 - 데이터가 없을 때 빈 결과`() {
        // given
        `when`(reservationLoader.loadByCursor(ReservationStatus.READY, null, 2))
            .thenReturn(emptyList<Reservation>() to emptyMap<Long, Message>())

        // when
        val result = reservationService.listByCursor(ReservationStatus.READY, null, 2)

        // then
        assertThat(result.items).isEmpty()
        assertThat(result.nextCursor).isNull()
    }
}