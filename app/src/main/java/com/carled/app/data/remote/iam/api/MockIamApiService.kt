package com.carled.app.data.remote.iam.api

import com.carled.app.data.remote.iam.dto.request.*
import com.carled.app.data.remote.iam.dto.response.*
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/**
 * Mock implementation của IamApiService dùng cho chế độ offline hoặc môi trường kiểm thử.
 */
class MockIamApiService(
    private val simulatedDelayMs: Long = 400L,
    private val simulateNetworkFailure: Boolean = false
) : IamApiService {

    override suspend fun login(request: LoginRequestDto): Response<ApiResponseDto<LoginResponseDto>> {
        delay(simulatedDelayMs)
        if (simulateNetworkFailure) {
            return makeErrorResponse(401, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không chính xác")
        }
        val data = LoginResponseDto(
            accessToken = "mock-access-token-${request.email.hashCode()}-${System.currentTimeMillis()}",
            refreshToken = "mock-refresh-token-${request.email.hashCode()}-${System.currentTimeMillis()}",
            tokenType = "Bearer",
            expiresIn = 900L
        )
        return Response.success(ApiResponseDto(data = data))
    }

    override suspend fun register(request: RegisterRequestDto): Response<ApiResponseDto<RegisterResponseDto>> {
        delay(simulatedDelayMs)
        if (simulateNetworkFailure) {
            return makeErrorResponse(400, "EMAIL_ALREADY_EXISTS", "Email đã tồn tại trên hệ thống")
        }
        val data = RegisterResponseDto(
            id = (System.currentTimeMillis() % 10000),
            email = request.email,
            accountType = "CUSTOMER",
            status = "PENDING_VERIFICATION"
        )
        return Response.success(201, ApiResponseDto(data = data))
    }

    override suspend fun verifyEmail(request: VerifyEmailRequestDto): Response<ApiResponseDto<String>> {
        delay(simulatedDelayMs)
        if (request.otp != "123456" && simulateNetworkFailure) {
            return makeErrorResponse(400, "INVALID_OTP", "Mã OTP không hợp lệ hoặc đã hết hạn")
        }
        return Response.success(ApiResponseDto(data = "Email verified successfully"))
    }

    override suspend fun resendVerification(request: ResendVerificationRequestDto): Response<Unit> {
        delay(simulatedDelayMs)
        return Response.success(202, Unit)
    }

    override suspend fun googleLogin(request: GoogleLoginRequestDto): Response<ApiResponseDto<LoginResponseDto>> {
        delay(simulatedDelayMs)
        val data = LoginResponseDto(
            accessToken = "mock-google-access-token-${System.currentTimeMillis()}",
            refreshToken = "mock-google-refresh-token-${System.currentTimeMillis()}",
            tokenType = "Bearer",
            expiresIn = 900L
        )
        return Response.success(ApiResponseDto(data = data))
    }

    override suspend fun refreshToken(request: RefreshTokenRequestDto): Response<ApiResponseDto<LoginResponseDto>> {
        delay(simulatedDelayMs)
        val data = LoginResponseDto(
            accessToken = "mock-refreshed-access-token-${System.currentTimeMillis()}",
            refreshToken = "mock-refreshed-refresh-token-${System.currentTimeMillis()}",
            tokenType = "Bearer",
            expiresIn = 900L
        )
        return Response.success(ApiResponseDto(data = data))
    }

    override suspend fun logout(authHeader: String, request: LogoutRequestDto): Response<Unit> {
        delay(simulatedDelayMs)
        return Response.success(204, Unit)
    }

    override suspend fun forgotPassword(request: ForgotPasswordRequestDto): Response<ApiResponseDto<ForgotPasswordResponseDto>> {
        delay(simulatedDelayMs)
        val data = ForgotPasswordResponseDto(
            message = "If the email is registered, a password reset code has been sent."
        )
        return Response.success(ApiResponseDto(data = data))
    }

    override suspend fun resetPassword(request: ResetPasswordRequestDto): Response<ApiResponseDto<ResetPasswordResponseDto>> {
        delay(simulatedDelayMs)
        val data = ResetPasswordResponseDto(
            message = "Password has been reset successfully. Please log in with your new password."
        )
        return Response.success(ApiResponseDto(data = data))
    }

    private fun <T> makeErrorResponse(code: Int, errorCode: String, message: String): Response<T> {
        val errorJson = """
            {
                "code": "$errorCode",
                "message": "$message",
                "status": $code,
                "timestamp": "2026-11-10T00:00:00Z",
                "traceId": "mock-trace-1234",
                "details": []
            }
        """.trimIndent()
        val body = errorJson.toResponseBody("application/json".toMediaTypeOrNull())
        return Response.error(code, body)
    }
}
