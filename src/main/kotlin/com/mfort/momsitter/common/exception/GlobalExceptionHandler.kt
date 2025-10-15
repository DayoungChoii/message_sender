package com.mfort.momsitter.common.exception

import com.mfort.momsitter.application.supporter.logger
import dev.designpattern.adapt.support.error.CustomException
import dev.designpattern.adapt.support.error.ExceptionResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = logger()

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ExceptionResponse> {
        val safeMessage = e.message ?: "Unexpected error occurred"

        log.error(
            "[${e::class.simpleName}] 발생: code=${e.exceptionCode.code}, " +
                    "httpStatus=${e.exceptionCode.httpStatus}, message=$safeMessage", e
        )

        val response = ExceptionResponse(
            code = e.exceptionCode.code,
            message = safeMessage
        )
        return ResponseEntity.status(e.exceptionCode.httpStatus).body(response)
    }
}
