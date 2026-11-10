package com.carled.app.data.remote.iam.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Envelope lỗi chuẩn phản ánh kiến trúc backend Lumintra.
 */
data class ErrorResponseDto(
    @SerializedName("code")
    val code: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("status")
    val status: Int? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null,

    @SerializedName("traceId")
    val traceId: String? = null,

    @SerializedName("details")
    val details: List<ErrorDetailDto>? = null
)

data class ErrorDetailDto(
    @SerializedName("field")
    val field: String? = null,

    @SerializedName("message")
    val message: String? = null
)
