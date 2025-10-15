package com.mfort.momsitter.fixture

import com.appmattus.kotlinfixture.kotlinFixture
import com.mfort.momsitter.domain.Message
import com.mfort.momsitter.domain.Reservation
import com.mfort.momsitter.domain.ReservationStatus.READY
import java.time.Instant
import java.time.temporal.ChronoUnit


class Fixture {
    companion object {
        private val fixture = kotlinFixture()

        fun getMessage(id: Long): Message =
            fixture<Message> {
                property(Message::phoneNumber) { "01012345678" }
                property(Message::title) { "예약 알림" }
                property(Message::contents) { "오늘 5시에 픽업 예약이 있습니다." }
            }.also {
                val field = Message::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(it, id)
            }

        fun getReservation(messageId: Long, reservationId: Long): Reservation =
            fixture<Reservation> {
                property(Reservation::messageId) { messageId }
                property(Reservation::dueAt) { Instant.now().plus(1, ChronoUnit.HOURS) }
                property(Reservation::nextAttemptAt) { Instant.now().plus(1, ChronoUnit.HOURS) }
                property(Reservation::status) { READY }
                property(Reservation::retryCount) { 0 }
            }.also {
                val field = Reservation::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(it, reservationId) // 테스트용 예약 id
            }
    }
}

