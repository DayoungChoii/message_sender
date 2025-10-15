package com.mfort.momsitter

import com.mfort.momsitter.application.supporter.logger
import com.mfort.momsitter.domain.*
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class DataInitializer(
    private val messageRepository: MessageRepository,
    private val reservationRepository: ReservationRepository
) : CommandLineRunner {
    private val log = logger()

    override fun run(vararg args: String?) {
        if (messageRepository.count() > 0 || reservationRepository.count() > 0) {
            return
        }

        val now = Instant.now()

        // 메시지 100건 생성
        val messages = (1..100).map { i ->
            Message(
                phoneNumber = "0100000${"%04d".format(i)}",
                title = "테스트 메시지 $i",
                contents = "이것은 $i 번째 메시지입니다."
            )
        }

        val savedMessages = messageRepository.saveAll(messages)

        // 예약 100건 생성 (메시지와 1:1 매핑)
        val reservations = savedMessages.mapIndexed { idx, msg ->
            Reservation(
                messageId = msg.id!!,
                dueAt = now.plus(idx + 1L, ChronoUnit.SECONDS), // 1분 간격으로 dueAt 생성
                nextAttemptAt = now.plus(idx + 1L, ChronoUnit.SECONDS),
                status = ReservationStatus.READY
            )
        }

        reservationRepository.saveAll(reservations)

        val minDueAt = reservations.minOf { it.dueAt }
        val maxDueAt = reservations.maxOf { it.dueAt }

        log.info(
            "[DataInitializer] 예약 ${reservations.size}건 저장 완료 (dueAt 범위: $minDueAt ~ $maxDueAt)"
        )
        log.info("[DataInitializer] 초기 데이터 세팅 완료")
    }
}