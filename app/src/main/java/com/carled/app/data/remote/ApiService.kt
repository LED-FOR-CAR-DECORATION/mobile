package com.carled.app.data.remote

import com.carled.app.data.remote.dto.ClaimDeviceRequest
import com.carled.app.data.remote.dto.ClaimDeviceResponse
import com.carled.app.data.remote.dto.ForgotPasswordRequest
import com.carled.app.data.remote.dto.GoogleAuthRequest
import com.carled.app.data.remote.dto.LoginRequest
import com.carled.app.data.remote.dto.LoginResponse
import com.carled.app.data.remote.dto.ProductDto
import com.carled.app.data.remote.dto.RefreshTokenRequest
import com.carled.app.data.remote.dto.RegisterRequest
import com.carled.app.data.remote.dto.RegisterResponse
import com.carled.app.data.remote.dto.ResendVerificationRequest
import com.carled.app.data.remote.dto.ResetPasswordRequest
import com.carled.app.data.remote.dto.UserProfileResponse
import com.carled.app.data.remote.dto.VerifyEmailRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Returns 201 + RegisterResponse (no token — user must verify email first)
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<Unit>

    @POST("api/auth/resend-verification")
    suspend fun resendVerification(@Body request: ResendVerificationRequest): Response<Unit>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Unit>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>

    @POST("api/auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    // -------------------------------------------------------------------------
    // Users
    // -------------------------------------------------------------------------

    // No auth interceptor is configured on OkHttpClient (see ServiceLocator),
    // so the bearer token has to be passed explicitly on every call that needs it.
    @GET("api/users/me")
    suspend fun getCurrentUserProfile(
        @Header("Authorization") authorization: String
    ): Response<UserProfileResponse>

    // -------------------------------------------------------------------------
    // Products
    // -------------------------------------------------------------------------

    @GET("api/products")
    suspend fun getProducts(): Response<List<ProductDto>>

    @GET("api/products/{id}")
    suspend fun getProductById(@Path("id") id: String): Response<ProductDto>

    // -------------------------------------------------------------------------
    // Devices
    // -------------------------------------------------------------------------

    // Verifies the signed QR payload (sn/mac/sig) and attaches the device to
    // whichever account the bearer token identifies.
    @POST("api/devices/claim")
    suspend fun claimDevice(
        @Header("Authorization") authorization: String,
        @Body request: ClaimDeviceRequest
    ): Response<ClaimDeviceResponse>
}