package com.carled.app.data.remote.iam.dto.request

import com.google.gson.annotations.SerializedName

/**
 * Payload đăng nhập Google OAuth2 thông qua Google ID Token.
 */
data class GoogleLoginRequestDto(
    @SerializedName("idToken")
    val idToken: String
)
