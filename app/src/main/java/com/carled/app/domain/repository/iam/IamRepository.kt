package com.carled.app.domain.repository.iam

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.iam.AuthTokens
import com.carled.app.domain.model.iam.IamUser
import com.carled.app.domain.model.iam.PasswordResetResult
import com.carled.app.domain.model.iam.RegisterResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface định nghĩa các nghiệp vụ liên quan đến IAM (Identity & Access Management).
 */
interface IamRepository {
    fun getCurrentUser(): Flow<IamUser?>
    fun getTokens(): Flow<AuthTokens?>

    suspend fun login(email: String, password: String): Resource<AuthTokens>
    suspend fun register(email: String, password: String, confirmPassword: String): Resource<RegisterResult>
    suspend fun verifyEmail(email: String, otp: String): Resource<String>
    suspend fun resendVerification(email: String): Resource<Unit>
    suspend fun googleLogin(idToken: String): Resource<AuthTokens>
    suspend fun refreshToken(): Resource<AuthTokens>
    suspend fun logout(): Resource<Unit>
    suspend fun forgotPassword(email: String): Resource<PasswordResetResult>
    suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String? = null
    ): Resource<PasswordResetResult>

    suspend fun isLoggedIn(): Boolean
}
