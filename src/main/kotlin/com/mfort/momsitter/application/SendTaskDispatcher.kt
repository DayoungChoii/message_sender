package com.mfort.momsitter.application

import com.mfort.momsitter.application.supporter.logger
import com.mfort.momsitter.domain.Message
import com.mfort.momsitter.domain.Reservation
import com.mfort.momsitter.infra.NotificationClient
import org.springframework.stereotype.Component

@Component
class SendTaskDispatcher(
    private val notificationClient: NotificationClient
) {
    private val log = logger()

    fun dispatch(reservations: List<Reservation>, messageMap: Map<Long, Message>) =
        reservations.mapNotNull { res ->
            val message = messageMap[res.messageId]
                ?: run {
                    log.warn("Message not found for reservationId=${res.id}, messageId=${res.messageId}")
                    return@mapNotNull null
                }
            notificationClient.sendAsync(res, message)
        }
}