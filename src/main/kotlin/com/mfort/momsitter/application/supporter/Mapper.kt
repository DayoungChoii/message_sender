package com.mfort.momsitter.application.supporter

import com.mfort.momsitter.api.dto.CreateReservationRequest
import com.mfort.momsitter.domain.Message

fun CreateReservationRequest.toMessage(): Message =
    Message(
        phoneNumber = this.phoneNumber,
        title = this.title,
        contents = this.contents
    )