package com.mfort.momsitter.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

enum class ExceptionCode(
    val code: String,
    val message: String,
    val httpStatus: HttpStatus
) {
    INVALID_DATE_PARSE("INVALID_DATE_PARSE", "fail to parse string to instant", BAD_REQUEST),
    MESSAGE_ID_NOT_FOUND("MESSAGE_ID_NOT_FOUND", "messageId not found", INTERNAL_SERVER_ERROR),
    RESERVATION_ID_NOT_FOUND("RESERVATION_ID_NOT_FOUND", "reservationId not found", INTERNAL_SERVER_ERROR),
    INVALID_DUE_AT("INVALID_DUE_AT", "dueAt is invalid", BAD_REQUEST),
}
