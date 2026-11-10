package com.carled.app.data.remote.iam.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Phản hồi xác nhận yêu cầu gửi mã OTP quên mật khẩu.
 */
data class ForgotPasswordResponseDto(
    @SerializedName("message")
    val message: String
)
