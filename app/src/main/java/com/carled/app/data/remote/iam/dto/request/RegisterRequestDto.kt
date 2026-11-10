package com.carled.app.data.remote.iam.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Payload đăng ký tài khoản khách hàng mới.
 */
data class RegisterRequestDto(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("confirmPassword")
    val confirmPassword: String
)
