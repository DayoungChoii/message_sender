package dev.designpattern.adapt.support.error

import com.mfort.momsitter.common.exception.ExceptionCode

open class CustomException(
    val exceptionCode: ExceptionCode,
    override val message: String? = exceptionCode.message
) : RuntimeException(message)
