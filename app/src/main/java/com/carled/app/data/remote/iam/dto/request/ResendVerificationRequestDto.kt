package com.carled.app.data.remote.iam.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Payload yêu cầu gửi lại mã OTP xác thực email.
 */
data class ResendVerificationRequestDto(
    @SerializedName("email")
    val email: String
)
