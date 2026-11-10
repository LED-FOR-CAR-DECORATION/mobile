package com.carled.app.data.repository.iam

import com.carled.app.core.common.Resource
import com.carled.app.core.network.ApiException
import com.carled.app.core.network.RetrofitClient
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.remote.iam.api.IamApiService
import com.carled.app.data.remote.iam.dto.request.*
import com.carled.app.data.remote.iam.dto.response.*
import com.carled.app.domain.model.iam.AuthTokens
import com.carled.app.domain.model.iam.IamUser
import com.carled.app.domain.model.iam.PasswordResetResult
import com.carled.app.domain.model.iam.RegisterResult
import com.carled.app.domain.repository.iam.IamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response

class IamRepositoryImpl(
    private val apiService: IamApiService,
    private val userPreferences: UserPreferences
) : IamRepository {

    private val _currentUser = MutableStateFlow<IamUser?>(loadCachedUser())
    private val _tokens = MutableStateFlow<AuthTokens?>(loadCachedTokens())

    override fun getCurrentUser(): Flow<IamUser?> = _currentUser.asStateFlow()
    override fun getTokens(): Flow<AuthTokens?> = _tokens.asStateFlow()

    override suspend fun login(email: String, password: String): Resource<AuthTokens> {
        return safeApiCall {
            val response = apiService.login(LoginRequestDto(email, password))
            val loginData = extractData(response)

            // Lưu trữ phiên đăng nhập vào SharedPreferences
            userPreferences.saveTokens(
                accessToken = loginData.accessToken,
                refreshToken = loginData.refreshToken,
                tokenType = loginData.tokenType,
                expiresInSeconds = loginData.expiresIn
            )
            userPreferences.saveUser(
                id = email.hashCode().toString(),
                email = email
            )

            val authTokens = AuthTokens(
                accessToken = loginData.accessToken,
                refreshToken = loginData.refreshToken,
                tokenType = loginData.tokenType,
                expiresIn = loginData.expiresIn
            )
            val user = IamUser(
                id = email.hashCode().toString(),
                email = email,
                fullName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            )

            _tokens.value = authTokens
            _currentUser.value = user

            authTokens
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        confirmPassword: String
    ): Resource<RegisterResult> {
        return safeApiCall {
            val response = apiService.register(
                RegisterRequestDto(email, password, confirmPassword)
            )
            val registerData = extractData(response)
            RegisterResult(
                id = registerData.id,
                email = registerData.email,
                accountType = registerData.accountType ?: "CUSTOMER",
                status = registerData.status ?: "PENDING_VERIFICATION"
            )
        }
    }

    override suspend fun verifyEmail(email: String, otp: String): Resource<String> {
        return safeApiCall {
            val response = apiService.verifyEmail(VerifyEmailRequestDto(email, otp))
            extractData(response)
        }
    }

    override suspend fun resendVerification(email: String): Resource<Unit> {
        return safeApiCall {
            val response = apiService.resendVerification(ResendVerificationRequestDto(email))
            if (!response.isSuccessful) {
                throw parseError(response)
            }
            Unit
        }
    }

    override suspend fun googleLogin(idToken: String): Resource<AuthTokens> {
        return safeApiCall {
            val response = apiService.googleLogin(GoogleLoginRequestDto(idToken))
            val loginData = extractData(response)

            userPreferences.saveTokens(
                accessToken = loginData.accessToken,
                refreshToken = loginData.refreshToken,
                tokenType = loginData.tokenType,
                expiresInSeconds = loginData.expiresIn
            )

            val authTokens = AuthTokens(
                accessToken = loginData.accessToken,
                refreshToken = loginData.refreshToken,
                tokenType = loginData.tokenType,
                expiresIn = loginData.expiresIn
            )

            _tokens.value = authTokens
            authTokens
        }
    }

    override suspend fun refreshToken(): Resource<AuthTokens> {
        val currentRefreshToken = userPreferences.getRefreshToken()
            ?: return Resource.Error("Không tìm thấy refresh token trong bộ nhớ")

        return safeApiCall {
            val response = apiService.refreshToken(RefreshTokenRequestDto(currentRefreshToken))
            val loginData = extractData(response)

            userPreferences.saveTokens(
                accessToken = loginData.accessToken,
                refreshToken = loginData.refreshToken,
                tokenType = loginData.tokenType,
                expiresInSeconds = loginData.expiresIn
            )

            val authTokens = AuthTokens(
                accessToken = loginData.accessToken,
                refreshToken = loginData.refreshToken,
                tokenType = loginData.tokenType,
                expiresIn = loginData.expiresIn
            )

            _tokens.value = authTokens
            authTokens
        }
    }

    override suspend fun logout(): Resource<Unit> {
        val currentRefreshToken = userPreferences.getRefreshToken() ?: ""
        val token = userPreferences.getAccessToken() ?: ""
        val authHeader = if (token.isNotBlank()) "Bearer $token" else ""

        try {
            if (currentRefreshToken.isNotBlank()) {
                apiService.logout(authHeader, LogoutRequestDto(currentRefreshToken))
            }
        } catch (_: Exception) {
            // Ngay cả khi backend trả lỗi hoặc mất mạng, ta vẫn xóa sạch session local
        } finally {
            userPreferences.clear()
            _currentUser.value = null
            _tokens.value = null
        }
        return Resource.Success(Unit)
    }

    override suspend fun forgotPassword(email: String): Resource<PasswordResetResult> {
        return safeApiCall {
            val response = apiService.forgotPassword(ForgotPasswordRequestDto(email))
            val data = extractData(response)
            PasswordResetResult(data.message)
        }
    }

    override suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String?
    ): Resource<PasswordResetResult> {
        return safeApiCall {
            val response = apiService.resetPassword(
                ResetPasswordRequestDto(
                    email = email,
                    otp = otp,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )
            )
            val data = extractData(response)
            PasswordResetResult(data.message)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return !userPreferences.getAccessToken().isNullOrBlank()
    }

    // Helper functions
    private fun loadCachedUser(): IamUser? {
        val email = userPreferences.getUserEmail() ?: return null
        val id = userPreferences.getUserId() ?: email.hashCode().toString()
        val accountType = userPreferences.getAccountType() ?: "CUSTOMER"
        val status = userPreferences.getAccountStatus() ?: "ACTIVE"
        return IamUser(
            id = id,
            email = email,
            accountType = accountType,
            status = status,
            fullName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        )
    }

    private fun loadCachedTokens(): AuthTokens? {
        val accessToken = userPreferences.getAccessToken() ?: return null
        val refreshToken = userPreferences.getRefreshToken() ?: return null
        val tokenType = userPreferences.getTokenType()
        val expiresAt = userPreferences.getTokenExpiresAt()
        val expiresInSeconds = ((expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType,
            expiresIn = expiresInSeconds
        )
    }

    private fun <T> extractData(response: Response<ApiResponseDto<T>>): T {
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.data != null) {
                return body.data
            }
            throw ApiException(
                code = "EMPTY_DATA",
                httpStatus = response.code(),
                message = "Phản hồi từ máy chủ không có nội dung dữ liệu"
            )
        }
        throw parseError(response)
    }

    private fun parseError(response: Response<*>): ApiException {
        val errorBody = response.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
            try {
                val errorDto = RetrofitClient.gson.fromJson(errorBody, ErrorResponseDto::class.java)
                return ApiException(
                    code = errorDto.code,
                    httpStatus = errorDto.status ?: response.code(),
                    message = errorDto.message ?: "Yêu cầu thất bại (${response.code()})",
                    traceId = errorDto.traceId,
                    details = errorDto.details ?: emptyList()
                )
            } catch (_: Exception) {
                // Fallback nếu không parse được JSON chuẩn
            }
        }
        return ApiException(
            code = "HTTP_${response.code()}",
            httpStatus = response.code(),
            message = "Lỗi kết nối máy chủ (${response.code()}): ${response.message()}"
        )
    }

    private inline fun <T> safeApiCall(block: () -> T): Resource<T> {
        return try {
            Resource.Success(block())
        } catch (e: ApiException) {
            Resource.Error(e.message, e)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Đã xảy ra lỗi không xác định", e)
        }
    }
}
