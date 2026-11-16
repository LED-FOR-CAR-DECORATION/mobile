package com.carled.app.data.remote

import com.carled.app.data.remote.dto.ClaimDeviceRequest
import com.carled.app.data.remote.dto.ClaimDeviceResponse
import com.carled.app.data.remote.dto.ApiResponse
import com.carled.app.data.remote.dto.DeviceDto
import com.carled.app.data.remote.dto.DeviceDetailDto
import com.carled.app.data.remote.dto.ForgotPasswordRequest
import com.carled.app.data.remote.dto.GoogleAuthRequest
import com.carled.app.data.remote.dto.LoginRequest
import com.carled.app.data.remote.dto.LoginResponse
import com.carled.app.data.remote.dto.LogoutRequest
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
import retrofit2.http.Query

interface ApiService {

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

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

    /**
     * Logout — always send the refresh token so the server can revoke the session.
     * The previous parameterless overload was removed: it never invalidated the
     * refresh token server-side, leaving the session live until natural expiry.
     *
     * AuthRepositoryImpl / LogoutUseCase must supply both the Authorization header
     * and the stored refresh token in the body.
     */
    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String,
        @Body request: LogoutRequest
    ): Response<Unit>

    // -------------------------------------------------------------------------
    // Users
    // -------------------------------------------------------------------------

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

    @POST("api/devices/claim")
    suspend fun claimDevice(
        @Header("Authorization") authorization: String,
        @Body request: ClaimDeviceRequest
    ): Response<ClaimDeviceResponse>

    /**
     * GET /api/devices
     * Retrieves the paginated list of bound devices.
     * Default page is set to 0 to satisfy server validation: page >= 0.
     */
    @GET("api/devices")
    suspend fun getDevices(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<List<DeviceDto>>>

    /**
     * GET /api/devices/{id}
     * Retrieves detailed information for a specific device by its numeric ID (> 0).
     */
    @GET("api/devices/{id}")
    suspend fun getDeviceById(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long
    ): Response<ApiResponse<DeviceDetailDto>>
}