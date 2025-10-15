package com.mfort.momsitter.application.supporter

fun <T> List<T>.nextCursorOrNull(limit: Int, idSelector: (T) -> Long?): String? {
    if (this.size < limit) return null
    return this.lastOrNull()?.let { idSelector(it)?.toString() }
}
