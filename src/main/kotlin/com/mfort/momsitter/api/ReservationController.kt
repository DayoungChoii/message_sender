package com.mfort.momsitter.api

import com.mfort.momsitter.api.dto.CreateReservationRequest
import com.mfort.momsitter.api.dto.CursorPageResponse
import com.mfort.momsitter.api.dto.ScheduledMessageResponse
import com.mfort.momsitter.application.ReservationService
import com.mfort.momsitter.application.supporter.PaginationConstants
import com.mfort.momsitter.domain.ReservationStatus
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/reservations")
class ReservationController(
    private val reservationService: ReservationService,
) {
    @PostMapping
    fun create(@Valid @RequestBody req: CreateReservationRequest) =
        ResponseEntity.ok(reservationService.createReservation(req))

    @GetMapping
    fun list(
        @RequestParam(required = true) status: ReservationStatus,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = PaginationConstants.DEFAULT_LIMIT.toString()) limit: Int
    ): ResponseEntity<CursorPageResponse<ScheduledMessageResponse>> =
        ResponseEntity.ok(reservationService.listByCursor(status, cursor?.toLongOrNull(), limit))

}