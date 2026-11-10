package com.carled.app.data.remote.iam.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Phản hồi sau khi tạo tài khoản khách hàng mới.
 */
data class RegisterResponseDto(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("email")
    val email: String,

    @SerializedName("accountType")
    val accountType: String? = null,

    @SerializedName("status")
    val status: String? = null
)
