package com.mfort.momsitter.application

import com.mfort.momsitter.api.dto.CreateReservationRequest
import com.mfort.momsitter.api.dto.CursorPageResponse
import com.mfort.momsitter.api.dto.ScheduledMessageResponse
import com.mfort.momsitter.application.exception.CreateReservationException
import com.mfort.momsitter.application.exception.ReservedMessageReadException
import com.mfort.momsitter.application.supporter.PaginationConstants.MAX_LIMIT
import com.mfort.momsitter.application.supporter.ReservationDueAtValidator
import com.mfort.momsitter.application.supporter.nextCursorOrNull
import com.mfort.momsitter.application.supporter.toInstantUtcOrThrow
import com.mfort.momsitter.application.supporter.toMessage
import com.mfort.momsitter.common.exception.ExceptionCode.MESSAGE_ID_NOT_FOUND
import com.mfort.momsitter.common.exception.ExceptionCode.RESERVATION_ID_NOT_FOUND
import com.mfort.momsitter.domain.*
import com.mfort.momsitter.domain.ReservationStatus.READY
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ReservationService(
    private val validator: ReservationDueAtValidator,
    private val messageRepository: MessageRepository,
    private val reservationRepository: ReservationRepository,
    private val reservationLoader: ReservationLoader,

    ) {
    @Transactional
    fun createReservation(request: CreateReservationRequest): Long {
        val dueAt = request.dueAt.toInstantUtcOrThrow()
        validator.validate(dueAt)

        val message = messageRepository.save(request.toMessage())
        val messageId = message.id ?: throw CreateReservationException(MESSAGE_ID_NOT_FOUND)

        val reservation = reservationRepository.save(generateReservation(messageId, dueAt))
        return reservation.id ?: throw CreateReservationException(RESERVATION_ID_NOT_FOUND)
    }

    private fun generateReservation(id: Long, dueAt: Instant): Reservation =
        Reservation(
            messageId = id,
            dueAt = dueAt,
            nextAttemptAt = dueAt,
            status = READY
        )

    @Transactional(readOnly = true)
    fun listByCursor(
        status: ReservationStatus,
        cursor: Long?,
        limit: Int
    ): CursorPageResponse<ScheduledMessageResponse> {
        val pageSize = minOf(MAX_LIMIT, maxOf(1, limit))
        val (reservations, messageMap) = reservationLoader.loadByCursor(status, cursor, pageSize)

        val items = getScheduledMessageResponses(reservations, messageMap)

        return CursorPageResponse(
            items = items,
            nextCursor = reservations.nextCursorOrNull(pageSize) {it.id}
        )
    }

    private fun getScheduledMessageResponses(
        reservations: List<Reservation>,
        messageMap: Map<Long, Message>
    ) = reservations.map { r ->
        val m = messageMap[r.messageId] ?: throw ReservedMessageReadException(MESSAGE_ID_NOT_FOUND)
        ScheduledMessageResponse.of(r, m.phoneNumber, m.title, m.contents)
    }
}