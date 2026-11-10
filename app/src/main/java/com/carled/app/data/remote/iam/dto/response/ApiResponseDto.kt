package com.carled.app.data.remote.iam.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Envelope chuẩn cho toàn bộ phản hồi thành công từ backend.
 */
data class ApiResponseDto<T>(
    @SerializedName("data")
    val data: T? = null,

    @SerializedName("meta")
    val meta: Any? = null
)
