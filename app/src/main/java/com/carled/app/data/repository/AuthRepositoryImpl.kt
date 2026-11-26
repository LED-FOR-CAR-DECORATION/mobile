package com.carled.app.data.repository

import com.carled.app.core.common.JwtUtils
import com.carled.app.core.common.Resource
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.dto.ApiErrorResponse
import com.carled.app.data.remote.dto.ForgotPasswordRequest
import com.carled.app.data.remote.dto.GoogleAuthRequest
import com.carled.app.data.remote.dto.LoginData
import com.carled.app.data.remote.dto.LoginRequest
import com.carled.app.data.remote.dto.LoginResponse
import com.carled.app.data.remote.dto.LogoutRequest
import com.carled.app.data.remote.dto.RefreshTokenRequest
import com.carled.app.data.remote.dto.RegisterRequest
import com.carled.app.data.remote.dto.ResendVerificationRequest
import com.carled.app.data.remote.dto.ResetPasswordRequest
import com.carled.app.data.remote.dto.UpdateProfileRequest
import com.carled.app.data.remote.dto.UserProfileData
import com.carled.app.data.remote.dto.VerifyEmailRequest
import com.carled.app.domain.model.User
import com.carled.app.domain.repository.AuthRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences,
    private val gson: Gson = Gson()
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(buildRestoredUser())

    private fun buildRestoredUser(): User? {
        val token = userPreferences.getAuthToken()?.takeIf { it.isNotBlank() } ?: return null
        val id    = userPreferences.getUserId()?.takeIf    { it.isNotBlank() } ?: return null
        val email = userPreferences.getUserEmail()?.takeIf { it.isNotBlank() } ?: return null
        return User(id = id, email = email, fullName = "", role = "", token = token)
    }

    override fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Resource<User> =
        safeAuthCall(defaultError = "Đăng nhập không thành công") {
            apiService.login(LoginRequest(email, password))
        }

    /**
    - Registers a new account. The server returns 201 + the pending user data
    - (no token). Returns the registered email so the UI can navigate directly
    - to the email-verification screen.
     */
    override suspend fun register(
        email: String,
        password: String,
        confirmPassword: String
    ): Resource<String> = try {
        val response = apiService.register(RegisterRequest(email, password, confirmPassword))
        if (response.isSuccessful) {
            val body = response.body()
                ?: return Resource.Error("Phản hồi từ máy chủ trống")
            Resource.Success(body.data.email)
        } else {
            Resource.Error(parseErrorBody(response, "Đăng ký không thành công"))
        }
    } catch (e: CancellationException) { throw e }catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng", e)
    }

    override suspend fun verifyEmail(email: String, otp: String): Resource<Unit> =
        safeUnitCall(defaultError = "Xác thực email thất bại") {
            apiService.verifyEmail(VerifyEmailRequest(email, otp))
        }

    override suspend fun resendVerification(email: String): Resource<Unit> =
        safeUnitCall(defaultError = "Không thể gửi lại mã xác thực") {
            apiService.resendVerification(ResendVerificationRequest(email))
        }

    override suspend fun forgotPassword(email: String): Resource<Unit> =
        safeUnitCall(defaultError = "Không thể gửi yêu cầu đặt lại mật khẩu") {
            apiService.forgotPassword(ForgotPasswordRequest(email))
        }

    override suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String
    ): Resource<Unit> =
        safeUnitCall(defaultError = "Đặt lại mật khẩu thất bại") {
            apiService.resetPassword(
                ResetPasswordRequest(
                    email           = email,
                    otp             = otp,
                    newPassword     = newPassword,
                    confirmPassword = confirmPassword
                )
            )
        }

    override suspend fun refreshToken(refreshToken: String): Resource<User> =
        safeAuthCall(defaultError = "Làm mới phiên đăng nhập thất bại") {
            apiService.refreshToken(RefreshTokenRequest(refreshToken))
        }

    override suspend fun googleAuth(idToken: String): Resource<User> =
        safeAuthCall(defaultError = "Đăng nhập Google thất bại") {
            apiService.googleAuth(GoogleAuthRequest(idToken))
        }

    override suspend fun getProfile(): Resource<User> {
        val token = userPreferences.getAuthToken()?.takeIf { it.isNotBlank() }
            ?: return Resource.Error("Chưa đăng nhập")

        return try {
            val response = apiService.getCurrentUserProfile("Bearer $token")
            if (response.isSuccessful) {
                val profile = response.body()?.data
                    ?: return Resource.Error("Phản hồi từ máy chủ trống")
                // Keep the id the session already established (from the JWT's
                // `sub` claim at login) rather than switching to
                // profile.accountId.toString(). Those two aren't guaranteed to
                // be the same string, and per-user local caches keyed by this
                // id (e.g. UserPreferences' birth date) would silently stop
                // resolving if it changed out from under them.
                val id = _currentUser.value?.id ?: userPreferences.getUserId() ?: profile.accountId.toString()
                val user = User(
                    id       = id,
                    email    = profile.email,
                    fullName = profile.name ?: "New User",
                    role     = profile.roles.firstOrNull()?.removePrefix("ROLE_") ?: "",
                    token    = token,
                    phone    = profile.phone
                )
                _currentUser.value = user
                userPreferences.saveUser(user.id, user.email)
                Resource.Success(user)
            } else {
                Resource.Error(parseErrorBody(response, "Không tải được thông tin tài khoản"))
            }
        } catch (e: CancellationException) { throw e }catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng", e)
        }
    }

    override suspend fun updateProfile(name: String, phone: String?): Resource<User> {
        val token = userPreferences.getAuthToken()?.takeIf { it.isNotBlank() }
            ?: return Resource.Error("Chưa đăng nhập")

        return try {
            val response = apiService.updateCurrentUserProfile(
                "Bearer $token",
                UpdateProfileRequest(name = name, phone = phone)
            )
            if (response.isSuccessful) {
                val profile = response.body()?.data
                    ?: return Resource.Error("Phản hồi từ máy chủ trống")
                // Same reasoning as getProfile(): don't let a save silently
                // re-key the session's id, or per-user local data (birth
                // date) saved under the old id becomes unreachable.
                val id = _currentUser.value?.id ?: userPreferences.getUserId() ?: profile.accountId.toString()
                val user = User(
                    id       = id,
                    email    = profile.email,
                    fullName = profile.name ?: "New User",
                    role     = profile.roles.firstOrNull()?.removePrefix("ROLE_") ?: "",
                    token    = token,
                    phone    = profile.phone
                )
                _currentUser.value = user
                userPreferences.saveUser(user.id, user.email)
                Resource.Success(user)
            } else {
                Resource.Error(parseErrorBody(response, "Không thể cập nhật thông tin tài khoản"))
            }
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng", e)
        }
    }

    override suspend fun logout(): Resource<Unit> {
        val token        = userPreferences.getAuthToken()?.takeIf { it.isNotBlank() }
        val refreshToken = userPreferences.getRefreshToken()?.takeIf { it.isNotBlank() }
        clearSession()

        if (token != null && refreshToken != null) {
            try {
                apiService.logout("Bearer $token", LogoutRequest(refreshToken))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* silent — local logout is already complete */ }
        }

        return Resource.Success(Unit)
    }

    override suspend fun isLoggedIn(): Boolean =
        !userPreferences.getAuthToken().isNullOrBlank()

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
    - The response carries only {accessToken, refreshToken, expiresIn, tokenType} —
    - no user object — so the User is rebuilt entirely from the accessToken's
    - own JWT claims (sub -> id, email, roles -> role).
     */
    private suspend fun safeAuthCall(
        defaultError: String,
        block: suspend () -> Response<LoginResponse>
    ): Resource<User> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                val data = response.body()?.data
                    ?: return Resource.Error("Phản hồi từ máy chủ trống")
                val claims = JwtUtils.decodeClaims(data.accessToken)
                    ?: return Resource.Error("Không đọc được thông tin phiên đăng nhập")
                val id = claims.sub
                val email = claims.email
                if (id.isNullOrBlank() || email.isNullOrBlank()) {
                    return Resource.Error("Không đọc được thông tin phiên đăng nhập")
                }

                val baseUser = User(
                    id       = id,
                    email    = email,
                    fullName = "",
                    role     = claims.primaryRole(),
                    token    = data.accessToken
                )
                // Best-effort: the JWT alone has no display name. If /me fails for
                // any reason (400/404/500/network), fall back to the JWT-only user
                // rather than failing a login that already succeeded server-side.
                val user = fetchProfile(data.accessToken)?.let { profile ->
                    User(
                        id       = baseUser.id,
                        email    = profile.email.ifBlank { baseUser.email },
                        fullName = profile.name ?: "New User",
                        role     = profile.roles.firstOrNull()?.removePrefix("ROLE_") ?: baseUser.role,
                        token    = baseUser.token,
                        phone    = profile.phone
                    )
                } ?: baseUser

                persistSession(data, user)
                _currentUser.value = user
                Resource.Success(user)
            } else {
                Resource.Error(parseErrorBody(response, defaultError))
            }
        } catch (e: CancellationException) { throw e }catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng", e)
        }
    }

    private suspend fun safeUnitCall(
        defaultError: String,
        block: suspend () -> Response<Unit>
    ): Resource<Unit> = try {
        val response = block()
        if (response.isSuccessful) Resource.Success(Unit)
        else Resource.Error(parseErrorBody(response, defaultError))
    } catch (e: CancellationException) { throw e }catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng", e)
    }

    /** Best-effort GET /api/users/me for the just-issued [accessToken]. Returns
    - null on any failure (400/404/500/network) — callers fall back to the
    - JWT-only user rather than failing a login that already succeeded. */

    private suspend fun fetchProfile(accessToken: String): UserProfileData? = try {
        val response = apiService.getCurrentUserProfile("Bearer $accessToken")
        if (response.isSuccessful) response.body()?.data else null
    } catch (e: CancellationException) { throw e }catch (e: Exception) {
        null
    }

    private fun persistSession(data: LoginData, user: User) {
        userPreferences.saveAuthToken(data.accessToken)
        userPreferences.saveRefreshToken(data.refreshToken)
        userPreferences.saveUser(user.id, user.email)
    }

    private fun clearSession() {
        userPreferences.clear()
        _currentUser.value = null
    }

    private fun parseErrorBody(response: Response<*>, defaultMessage: String): String {
        return try {
            response.errorBody()
                ?.string()
                ?.takeIf { it.isNotBlank() }
                ?.let { gson.fromJson(it, ApiErrorResponse::class.java).resolvedMessage() }
                ?: defaultMessage
        } catch (e: CancellationException) { throw e }catch (e: Exception) {
            defaultMessage
        }
    }
    override fun notifySessionExpired() = clearSession()
}