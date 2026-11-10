package com.carled.app.data.remote.iam.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Phản hồi chứa cặp mã truy cập (access token) và refresh token sau khi đăng nhập thành công.
 */
data class LoginResponseDto(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String,

    @SerializedName("tokenType")
    val tokenType: String = "Bearer",

    @SerializedName("expiresIn")
    val expiresIn: Long = 900L
)
