package com.mfort.momsitter.application

import com.mfort.momsitter.domain.*
import com.mfort.momsitter.domain.ReservationStatus.*
import com.mfort.momsitter.infra.NotificationClient
import com.mfort.momsitter.infra.SendResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest
class ReservedMessageSchedulerIntegrationTest @Autowired constructor(
    private val scheduler: ReservedMessageScheduler,
    private val reservationRepository: ReservationRepository,
    private val messageRepository: MessageRepository
) {

    @MockitoBean
    private lateinit var notificationClient: NotificationClient

    @Test
    @Transactional
    fun `예약이 성공적으로 처리되면 DONE 상태가 된다`() {
        // given
        val message = messageRepository.save(
            Message(phoneNumber = "01012345678", title = "테스트 알림", contents = "예약 메시지입니다")
        )

        val reservation = reservationRepository.save(
            Reservation(
                messageId = message.id!!,
                dueAt = Instant.now().plusSeconds(60),
                nextAttemptAt = Instant.now(),
                status = READY
            )
        )

        // mock NotificationClient
        `when`(notificationClient.sendAsync(reservation, message))
            .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(SendResult.Success(reservation.id!!)))

        // when
        scheduler.processReservations()

        // then
        val updated = reservationRepository.findById(reservation.id!!).get()
        assertThat(updated.status).isEqualTo(DONE)
    }

    @Test
    @Transactional
    fun `500 오류 발생 시 RETRY_READY 상태로 전환되고 retryCount 증가`() {
        // given
        val message = messageRepository.save(
            Message("01098765432", "테스트 알림", "재시도 메시지입니다")
        )

        val reservation = reservationRepository.save(
            Reservation(
                messageId = message.id!!,
                dueAt = Instant.now(),
                nextAttemptAt = Instant.now(),
                status = READY
            )
        )

        `when`(notificationClient.sendAsync(reservation, message))
            .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(
                SendResult.Failure(reservation.id!!, retryable = true)
            ))

        // when
        scheduler.processReservations()

        // then
        val updated = reservationRepository.findById(reservation.id!!).get()
        assertThat(updated.status).isEqualTo(RETRY_READY)
        assertThat(updated.retryCount).isEqualTo(1)
        assertThat(updated.nextAttemptAt).isAfter(Instant.now()) // +30초 이후
    }

    @Test
    @Transactional
    fun `재시도 불가능 오류 발생 시 FAILED 상태가 된다`() {
        // given
        val message = messageRepository.save(
            Message("01055556666", "테스트 알림", "실패 메시지입니다")
        )

        val reservation = reservationRepository.save(
            Reservation(
                messageId = message.id!!,
                dueAt = Instant.now(),
                nextAttemptAt = Instant.now(),
                status = READY
            )
        )

        `when`(notificationClient.sendAsync(reservation, message))
            .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(
                SendResult.Failure(reservation.id!!, retryable = false)
            ))

        // when
        scheduler.processReservations()

        // then
        val updated = reservationRepository.findById(reservation.id!!).get()
        assertThat(updated.status).isEqualTo(FAILED)
    }
}