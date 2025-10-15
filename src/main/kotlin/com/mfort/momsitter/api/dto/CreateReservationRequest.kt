package com.mfort.momsitter.api.dto

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size


data class CreateReservationRequest(
    @field:Pattern(regexp = "^01\\d{8,9}$") val phoneNumber: String,
    @field:Size(min = 1, max = 100) val title: String,
    @field:Size(min = 1, max = 500) val contents: String,
    val dueAt: String
)