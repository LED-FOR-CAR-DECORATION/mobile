package com.carled.app.core.common

/**
 * Lớp bọc trạng thái của dữ liệu hoặc kết quả tác vụ bất đồng bộ trong kiến trúc MVVM.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
