package com.mfort.momsitter.application

import com.mfort.momsitter.domain.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ReservationLoader(
    private val reservationRepository: ReservationRepository,
    private val messageRepository: MessageRepository
) {
    fun loadWithLock(limit: Int): Pair<List<Reservation>, Map<Long, Message>> {
        val now = Instant.now()
        val pageable = PageRequest.of(0, limit)
        val reservations = reservationRepository.findReadyReservationsWithLock(now, pageable)
        return loadMessagesFor(reservations)
    }

    fun loadByCursor(status: ReservationStatus, cursor: Long?, limit: Int): Pair<List<Reservation>, Map<Long, Message>> {
        val reservations = reservationRepository.findByCursor(status, cursor, PageRequest.of(0, limit))
        return loadMessagesFor(reservations)
    }

    private fun loadMessagesFor(reservations: List<Reservation>): Pair<List<Reservation>, Map<Long, Message>> {
        if (reservations.isEmpty()) return emptyList<Reservation>() to emptyMap()

        val messageIds = reservations.map { it.messageId }
        val messageMap = messageRepository.findAllById(messageIds).associateBy { it.id!! }
        return reservations to messageMap
    }
}
