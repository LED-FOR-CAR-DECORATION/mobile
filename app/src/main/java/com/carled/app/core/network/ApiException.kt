package com.carled.app.core.network

import com.carled.app.data.remote.iam.dto.response.ErrorDetailDto

/**
 * Ngoại lệ được phân tích có cấu trúc từ ErrorResponseDto của backend.
 */
class ApiException(
    val code: String?,
    val httpStatus: Int,
    override val message: String,
    val traceId: String? = null,
    val details: List<ErrorDetailDto> = emptyList()
) : Exception(message) {

    fun getFieldErrorMessage(fieldName: String): String? {
        return details.firstOrNull { it.field.equals(fieldName, ignoreCase = true) }?.message
    }
}
