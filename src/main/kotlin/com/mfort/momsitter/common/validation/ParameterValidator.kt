package com.mfort.momsitter.common.validation

interface ParameterValidator<T> {
    /** 유효하면 아무 것도 리턴하지 않고, 실패하면 예외를 던진다. */
    fun validate(target: T)
}