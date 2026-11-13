package com.carled.app.data.remote.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val confirmPassword: String
)

/**
 * Matches the server's actual response for login/refresh/google-auth:
 *   {"data":{"accessToken":"...","expiresIn":900,"refreshToken":"...","tokenType":"Bearer"}}
 * There is no user object in this payload at all — id/email/role are only
 * available by decoding the JWT claims in accessToken (see JwtUtils).
 */
data class LoginResponse(
    val data: LoginData
)

data class LoginData(
    val accessToken: String,
    val expiresIn: Long,
    val refreshToken: String,
    val tokenType: String
)

// No longer populated by any real response — kept only in case something
// else in the codebase still references it. Safe to delete once confirmed unused.
data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String
)

/**
 * GET /api/users/me — {"data":{accountId,name,email,phone,avatar,roles},"meta":"string"}.
 * This is the only place `name` (a real display name, vs. the JWT's bare email/role
 * claims) is available. phone/avatar are captured here for completeness even though
 * nothing downstream consumes them yet.
 */
data class UserProfileResponse(
    val data: UserProfileData,
    val meta: String? = null
)

data class UserProfileData(
    val accountId: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val avatar: String? = null,
    val roles: List<String> = emptyList()
)

data class RegisterResponse(
    val data: RegisterData
)

data class RegisterData(
    val id: Int,
    val email: String,
    val accountType: String,
    val status: String
)

data class ProductDto(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val stockQuantity: Int,
    val category: String
)

data class VerifyEmailRequest(
    val email: String,
    val otp: String
)

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String,
    val confirmPassword: String
)

data class ResendVerificationRequest(
    val email: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class GoogleAuthRequest(
    val idToken: String
)

data class ApiErrorResponse(
    val code: String? = null,
    val message: String? = null,
    val status: Int? = null,
    val details: List<ValidationDetail>? = null,
    val timestamp: String? = null,
    val traceId: String? = null
) {
    /**
     * Returns the most specific error message:
     * - First validation detail when present (field-level errors)
     * - Top-level message otherwise
     */
    fun resolvedMessage(): String? =
        details?.firstOrNull()?.message?.takeIf { it.isNotBlank() }
            ?: message?.takeIf { it.isNotBlank() }
}

data class ValidationDetail(
    val field: String? = null,
    val message: String? = null
)