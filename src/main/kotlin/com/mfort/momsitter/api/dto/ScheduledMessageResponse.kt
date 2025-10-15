package com.mfort.momsitter.api.dto

import com.mfort.momsitter.domain.Reservation
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ScheduledMessageResponse(
    val reservationId: Long,
    val messageId: Long,
    val phoneNumber: String,
    val title: String,
    val contents: String,
    val dueAtKst: String,
    val status: String,
    val updatedAtKst: String
) {
    companion object {
        private val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        private val kst = ZoneId.of("Asia/Seoul")

        fun of(r: Reservation, phone: String, title: String, contents: String): ScheduledMessageResponse =
            ScheduledMessageResponse(
                reservationId = r.id!!,
                messageId = r.messageId,
                phoneNumber = phone,
                title = title,
                contents = contents,
                dueAtKst = r.dueAt.atZone(kst).format(fmt),
                status = r.status.name,
                updatedAtKst = r.updatedAt.atZone(kst).format(fmt)
            )
    }
}