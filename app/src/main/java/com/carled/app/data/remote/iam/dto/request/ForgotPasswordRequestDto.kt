package com.carled.app.data.remote.iam.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Payload yêu cầu mã OTP để khôi phục mật khẩu.
 */
data class ForgotPasswordRequestDto(
    @SerializedName("email")
    val email: String
)
