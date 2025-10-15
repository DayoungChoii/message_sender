package com.mfort.momsitter.application

import com.mfort.momsitter.application.supporter.ReservedScheduleConstants.MAX_RETRY
import com.mfort.momsitter.application.supporter.ReservedScheduleConstants.RETRY_DELAY_SECONDS
import com.mfort.momsitter.application.supporter.logger
import com.mfort.momsitter.domain.Reservation
import com.mfort.momsitter.domain.ReservationRepository
import com.mfort.momsitter.domain.ReservationStatus.*
import com.mfort.momsitter.infra.SendResult
import org.springframework.stereotype.Component
import java.time.Instant.now

@Component
class SendResultApplier(
    private val reservationRepository: ReservationRepository
) {
    private val log = logger()

    fun apply(reservations: List<Reservation>, results: List<SendResult>) {
        results.forEach { result ->
            val reservation = reservations.find { it.id == result.reservationId } ?: return@forEach
            when (result) {
                is SendResult.Success -> {
                    reservation.status = DONE
                }
                is SendResult.Failure -> {
                    if (result.retryable) {
                        if (reservation.retryCount >= MAX_RETRY) {
                            reservation.status = FAILED
                        } else {
                            reservation.retryCount++
                            reservation.nextAttemptAt = now().plusSeconds(RETRY_DELAY_SECONDS)
                            reservation.status = RETRY_READY
                        }
                    } else {
                        reservation.status = FAILED
                    }
                }
            }
            log.info(
                "[Applier] ReservationId={} MessageId={} status={} (retryCount={})",
                reservation.id, reservation.messageId, reservation.status, reservation.retryCount
            )

            reservationRepository.save(reservation)
        }
    }
}
