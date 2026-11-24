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
import com.carled.app.domain.model.CreateSignedUploadUrlRequest
import com.carled.app.domain.model.CustomerOwnedCharacterResponse
import com.carled.app.domain.model.commission.ApproveDeliverableCardResponse
import com.carled.app.domain.model.commission.CommissionPricingResponse
import com.carled.app.domain.model.commission.CommissionResponse
import com.carled.app.domain.model.commission.CreateCommissionRequest
import com.carled.app.domain.model.commission.CommissionMessageResponse
import com.carled.app.domain.model.commission.CommissionSummaryResponse
import com.carled.app.domain.model.commission.ConfirmRevisionCardRequest
import com.carled.app.domain.model.commission.ConfirmRevisionCardResponse
import com.carled.app.domain.model.commission.CreateCommissionMessageRequest

import com.carled.app.domain.model.PaymentSessionResponse
import com.carled.app.domain.model.SignedUploadUrlResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface ApiService {

    @GET("api/devices/{id}/ota/check")
    suspend fun checkFirmware(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Query("currentVersion") currentVersion: String?
    ): Response<ApiResponse<com.carled.app.data.remote.dto.FirmwareCheck>>

    @POST("api/devices/{id}/ota/sessions")
    suspend fun createFirmwareSession(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body request: com.carled.app.data.remote.dto.CreateOtaSessionRequest
    ): Response<ApiResponse<com.carled.app.data.remote.dto.FirmwareSession>>

    @retrofit2.http.PATCH("api/devices/{id}/firmware")
    suspend fun reportFirmware(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body request: com.carled.app.data.remote.dto.ReportFirmwareRequest
    ): Response<com.google.gson.JsonObject>

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

    @GET("api/devices")
    suspend fun getDevices(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<List<DeviceDto>>>

    @GET("api/devices/{id}")
    suspend fun getDeviceById(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long
    ): Response<ApiResponse<DeviceDetailDto>>

    // -------------------------------------------------------------------------
    // Commissions
    // -------------------------------------------------------------------------

    @GET("api/commissions/pricing")
    suspend fun getCommissionPricing(): Response<ApiResponse<CommissionPricingResponse>>

    @POST("api/commissions")
    suspend fun createCommission(
        @Header("Authorization") authorization: String,
        @Body request: CreateCommissionRequest
    ): Response<ApiResponse<CommissionResponse>>

    @GET("api/commissions/my")
    suspend fun getMyCommissions(
        @Header("Authorization") authorization: String,
        @Query("status") status: String? = null,
        @Query("sortDirection") sortDirection: String = "DESC",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<List<CommissionSummaryResponse>>>

    @GET("api/commissions/{id}")
    suspend fun getCommissionDetail(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long
    ): Response<ApiResponse<CommissionResponse>>

    @GET("api/commissions/{id}/messages")
    suspend fun getCommissionMessages(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long
    ): Response<ApiResponse<List<CommissionMessageResponse>>>

    @POST("api/commissions/{id}/messages")
    suspend fun sendCommissionMessage(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body request: CreateCommissionMessageRequest
    ): Response<ApiResponse<CommissionMessageResponse>>

    // -------------------------------------------------------------------------
    // Media (MED-01) — ASSUMPTION: path/shape not given in any spec seen so
    // far. Follows this backend's multipart conventions; confirm against the
    // real MED-01 spec once available.
    // -------------------------------------------------------------------------

    @POST("api/media/upload-url")
    suspend fun getMediaUploadUrl(
        @Header("Authorization") authorization: String,
        @Body request: CreateSignedUploadUrlRequest
    ): Response<ApiResponse<SignedUploadUrlResponse>>
    @GET("api/payments/{paymentCode}")
    suspend fun getPaymentSession(
        @Header("Authorization") authorization: String,
        @Path("paymentCode") paymentCode: String
    ): Response<ApiResponse<PaymentSessionResponse>>

    /** Staging-only test trigger — simulates a successful transfer without a real bank. */
    @POST("api/payments/mock-success/{paymentCode}")
    suspend fun mockPaymentSuccess(
        @Header("Authorization") authorization: String,
        @Path("paymentCode") paymentCode: String
    ): Response<ApiResponse<PaymentSessionResponse>>

    @POST("api/commissions/{id}/deliverable-card/approve")
    suspend fun approveDeliverableCard(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long
    ): Response<ApproveDeliverableCardResponse> // confirmed unwrapped, same as POST /api/commissions

    @POST("api/commissions/{id}/revision-card/confirm")
    suspend fun confirmRevisionCard(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body request: ConfirmRevisionCardRequest
    ): Response<ApiResponse<ConfirmRevisionCardResponse>>

    @POST("api/commissions/{id}/revision-card/cancel")
    suspend fun cancelRevisionCard(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long
    ): Response<Unit>
}

    @GET("api/customer/characters/owned")
    suspend fun getOwnedCharacters(
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<List<CustomerOwnedCharacterResponse>>>
}
