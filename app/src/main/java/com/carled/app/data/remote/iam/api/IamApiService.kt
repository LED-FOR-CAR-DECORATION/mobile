package com.carled.app.data.remote.iam.api

import com.carled.app.data.remote.iam.dto.request.*
import com.carled.app.data.remote.iam.dto.response.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Interface Retrofit định nghĩa toàn bộ 9 Endpoint API của module IAM backend:
 * - /api/auth/login
 * - /api/auth/register
 * - /api/auth/verify-email
 * - /api/auth/resend-verification
 * - /api/auth/google
 * - /api/auth/refresh
 * - /api/auth/logout
 * - /api/auth/forgot-password
 * - /api/auth/reset-password
 */
interface IamApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<ApiResponseDto<LoginResponseDto>>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): Response<ApiResponseDto<RegisterResponseDto>>

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(
        @Body request: VerifyEmailRequestDto
    ): Response<ApiResponseDto<String>>

    @POST("api/auth/resend-verification")
    suspend fun resendVerification(
        @Body request: ResendVerificationRequestDto
    ): Response<Unit>

    @POST("api/auth/google")
    suspend fun googleLogin(
        @Body request: GoogleLoginRequestDto
    ): Response<ApiResponseDto<LoginResponseDto>>

    @POST("api/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequestDto
    ): Response<ApiResponseDto<LoginResponseDto>>

    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") authHeader: String,
        @Body request: LogoutRequestDto
    ): Response<Unit>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequestDto
    ): Response<ApiResponseDto<ForgotPasswordResponseDto>>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequestDto
    ): Response<ApiResponseDto<ResetPasswordResponseDto>>
}
