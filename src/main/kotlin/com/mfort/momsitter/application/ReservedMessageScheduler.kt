package com.mfort.momsitter.application

import com.mfort.momsitter.application.supporter.ReservedScheduleConstants.SCHEDULER_INTERVAL_SECONDS
import com.mfort.momsitter.application.supporter.ReservedScheduleConstants.SCHEDULER_PICK_UP_LIMIT
import com.mfort.momsitter.application.supporter.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class ReservedMessageScheduler(
    private val loader: ReservationLoader,
    private val dispatcher: SendTaskDispatcher,
    private val applier: SendResultApplier
) {
    private val log = logger()

    @Scheduled(fixedDelay = SCHEDULER_INTERVAL_SECONDS * 1000)
    @Transactional
    fun processReservations() {
        log.info("[Scheduler] Started at {}", Instant.now())


        val (reservations, messageMap) = loader.loadWithLock(SCHEDULER_PICK_UP_LIMIT)
        if (reservations.isEmpty()) return

        log.info("[Scheduler] Loaded {} reservations", reservations.size)

        val futures = dispatcher.dispatch(reservations, messageMap)
        val results = futures.map { it.get() }
        applier.apply(reservations, results)

        log.info("[Scheduler] Finished cycle. Processed {} results", results.size)
    }
}
