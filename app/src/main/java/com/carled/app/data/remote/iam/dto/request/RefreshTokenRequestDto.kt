package com.carled.app.data.remote.iam.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Payload yêu cầu cấp lại Access Token qua Refresh Token.
 */
data class RefreshTokenRequestDto(
    @SerializedName("refreshToken")
    val refreshToken: String
)
