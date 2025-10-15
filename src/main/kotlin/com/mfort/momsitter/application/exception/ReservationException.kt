package com.mfort.momsitter.application.exception

import com.mfort.momsitter.common.exception.ExceptionCode
import dev.designpattern.adapt.support.error.CustomException

class DateParseException(exceptionCode: ExceptionCode) : CustomException(exceptionCode)
class CreateReservationException(exceptionCode: ExceptionCode) : CustomException(exceptionCode)
class ReservedMessageReadException(exceptionCode: ExceptionCode) : CustomException(exceptionCode)