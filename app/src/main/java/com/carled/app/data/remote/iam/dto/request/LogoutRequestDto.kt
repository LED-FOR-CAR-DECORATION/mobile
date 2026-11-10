package com.carled.app.data.remote.iam.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Payload đăng xuất để thu hồi Refresh Token trên hệ thống.
 */
data class LogoutRequestDto(
    @SerializedName("refreshToken")
    val refreshToken: String
)
