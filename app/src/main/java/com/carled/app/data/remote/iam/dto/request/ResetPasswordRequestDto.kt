package com.carled.app.data.remote.iam.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Payload đặt lại mật khẩu bằng mã OTP 6 số.
 */
data class ResetPasswordRequestDto(
    @SerializedName("email")
    val email: String,

    @SerializedName("otp")
    val otp: String,

    @SerializedName("newPassword")
    val newPassword: String,

    @SerializedName("confirmPassword")
    val confirmPassword: String? = null
)
