package com.carled.app.data.remote.iam.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Payload xác thực email bằng mã OTP 6 số.
 */
data class VerifyEmailRequestDto(
    @SerializedName("email")
    val email: String,

    @SerializedName("otp")
    val otp: String
)
