package com.mfort.momsitter.api.dto

data class CursorPageResponse<T>(
    val items: List<T>,
    val nextCursor: String?
)