package com.carled.app.data.remote.iam.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Phản hồi xác nhận đặt lại mật khẩu thành công.
 */
data class ResetPasswordResponseDto(
    @SerializedName("message")
    val message: String
)
