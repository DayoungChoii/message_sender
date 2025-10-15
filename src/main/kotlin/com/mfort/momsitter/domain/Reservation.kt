package com.mfort.momsitter.domain

import com.mfort.momsitter.domain.ReservationStatus.*
import jakarta.persistence.*
import jakarta.persistence.EnumType.*
import jakarta.persistence.GenerationType.*
import java.time.Instant

@Entity
@Table(
    name = "reservation",
    indexes = [
        Index(name = "idx_res_pick", columnList = "status,next_attempt_at,id"),
        Index(name = "idx_res_msgid", columnList = "message_id")
    ]
)
class Reservation(
    @Column(name = "message_id", nullable = false)
    val messageId: Long,

    @Column(name = "due_at", nullable = false)
    val dueAt: Instant,

    @Column(name = "next_attempt_at", nullable = false)
    var nextAttemptAt: Instant,

    @Enumerated(STRING)
    @Column(name = "status", length = 20, nullable = false)
    var status: ReservationStatus = ReservationStatus.READY,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,
): BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long? = null

    @Column(name = "canceled_at", nullable = true)
    var canceledAt: Instant? = null

    fun markSending() {
        status = SENDING
    }

    fun markSuccess() {
        status = DONE
    }

    fun markRetry() {
        status = RETRY_READY
        retryCount++
        nextAttemptAt = Instant.now().plusSeconds(60) // 1분 후 재시도
    }
}