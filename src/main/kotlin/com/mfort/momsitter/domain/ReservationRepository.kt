package com.mfort.momsitter.domain

import jakarta.persistence.LockModeType.*
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant


interface ReservationRepository : JpaRepository<Reservation, Long> {

    @Lock(PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT r
          FROM Reservation r
         WHERE r.status IN ('READY', 'RETRY_READY')
           AND r.nextAttemptAt <= :now
         ORDER BY r.nextAttemptAt ASC
        """
    )
    fun findReadyReservationsWithLock(
        @Param("now") now: Instant,
        pageable: Pageable
    ): List<Reservation>

    @Query(
        """
        SELECT r FROM Reservation r
         WHERE r.status = :status
           AND (:cursor IS NULL OR r.id > :cursor)
         ORDER BY r.id ASC
        """
    )
    fun findByCursor(
        @Param("status") status: ReservationStatus?,
        @Param("cursor") cursor: Long?,
        pageable: Pageable
    ): List<Reservation>
}