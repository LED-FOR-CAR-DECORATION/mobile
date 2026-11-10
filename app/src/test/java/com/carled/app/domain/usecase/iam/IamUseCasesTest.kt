package com.carled.app.domain.usecase.iam

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.iam.AuthTokens
import com.carled.app.domain.model.iam.IamUser
import com.carled.app.domain.model.iam.PasswordResetResult
import com.carled.app.domain.model.iam.RegisterResult
import com.carled.app.domain.repository.iam.IamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IamUseCasesTest {

    private lateinit var fakeRepository: FakeIamRepository
    private lateinit var iamUseCases: IamUseCases

    @Before
    fun setUp() {
        fakeRepository = FakeIamRepository()
        iamUseCases = IamUseCases(
            login = LoginUseCase(fakeRepository),
            register = RegisterUseCase(fakeRepository),
            verifyEmail = VerifyEmailUseCase(fakeRepository),
            resendVerification = ResendVerificationUseCase(fakeRepository),
            googleLogin = GoogleLoginUseCase(fakeRepository),
            refreshToken = RefreshTokenUseCase(fakeRepository),
            logout = LogoutUseCase(fakeRepository),
            forgotPassword = ForgotPasswordUseCase(fakeRepository),
            resetPassword = ResetPasswordUseCase(fakeRepository)
        )
    }

    @Test
    fun `login with empty email returns error`() = runBlocking {
        val result = iamUseCases.login("", "password123")
        assertTrue(result is Resource.Error)
        assertEquals("Email không được để trống", (result as Resource.Error).message)
    }

    @Test
    fun `login with empty password returns error`() = runBlocking {
        val result = iamUseCases.login("test@example.com", "")
        assertTrue(result is Resource.Error)
        assertEquals("Mật khẩu không được để trống", (result as Resource.Error).message)
    }

    @Test
    fun `login with valid credentials returns success tokens`() = runBlocking {
        val result = iamUseCases.login("test@example.com", "P@ssword123")
        assertTrue(result is Resource.Success)
        val tokens = (result as Resource.Success).data
        assertEquals("valid-access-token", tokens.accessToken)
        assertEquals("valid-refresh-token", tokens.refreshToken)
    }

    @Test
    fun `register with mismatched password returns error`() = runBlocking {
        val result = iamUseCases.register("test@example.com", "Password123", "Mismatch123")
        assertTrue(result is Resource.Error)
        assertEquals("Mật khẩu xác nhận không khớp", (result as Resource.Error).message)
    }

    @Test
    fun `register with short password returns error`() = runBlocking {
        val result = iamUseCases.register("test@example.com", "short", "short")
        assertTrue(result is Resource.Error)
        assertEquals("Mật khẩu phải chứa ít nhất 8 ký tự", (result as Resource.Error).message)
    }

    @Test
    fun `register with valid info returns register result`() = runBlocking {
        val result = iamUseCases.register("test@example.com", "Password123", "Password123")
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals("test@example.com", data.email)
        assertEquals("PENDING_VERIFICATION", data.status)
    }

    @Test
    fun `verifyEmail with invalid otp length returns error`() = runBlocking {
        val result = iamUseCases.verifyEmail("test@example.com", "123")
        assertTrue(result is Resource.Error)
        assertEquals("Mã OTP phải có đúng 6 chữ số", (result as Resource.Error).message)
    }

    @Test
    fun `verifyEmail with valid 6 digit otp returns success`() = runBlocking {
        val result = iamUseCases.verifyEmail("test@example.com", "123456")
        assertTrue(result is Resource.Success)
        assertEquals("Email verified successfully", (result as Resource.Success).data)
    }

    @Test
    fun `resendVerification with empty email returns error`() = runBlocking {
        val result = iamUseCases.resendVerification("   ")
        assertTrue(result is Resource.Error)
        assertEquals("Email không được để trống", (result as Resource.Error).message)
    }

    @Test
    fun `googleLogin with blank token returns error`() = runBlocking {
        val result = iamUseCases.googleLogin("   ")
        assertTrue(result is Resource.Error)
        assertEquals("Google ID Token không được để trống", (result as Resource.Error).message)
    }

    @Test
    fun `forgotPassword with empty email returns error`() = runBlocking {
        val result = iamUseCases.forgotPassword("")
        assertTrue(result is Resource.Error)
        assertEquals("Email không được để trống", (result as Resource.Error).message)
    }

    @Test
    fun `resetPassword with mismatched confirmation returns error`() = runBlocking {
        val result = iamUseCases.resetPassword(
            email = "user@example.com",
            otp = "123456",
            newPassword = "NewPassword123",
            confirmPassword = "DifferentPassword123"
        )
        assertTrue(result is Resource.Error)
        assertEquals("Mật khẩu xác nhận không khớp", (result as Resource.Error).message)
    }

    @Test
    fun `resetPassword with valid data returns success message`() = runBlocking {
        val result = iamUseCases.resetPassword(
            email = "user@example.com",
            otp = "123456",
            newPassword = "NewPassword123",
            confirmPassword = "NewPassword123"
        )
        assertTrue(result is Resource.Success)
        assertEquals("Password reset successful", (result as Resource.Success).data.message)
    }

    // Fake Repository for testing
    private class FakeIamRepository : IamRepository {
        var loggedIn = false

        override fun getCurrentUser(): Flow<IamUser?> = flowOf(null)
        override fun getTokens(): Flow<AuthTokens?> = flowOf(null)

        override suspend fun login(email: String, password: String): Resource<AuthTokens> {
            loggedIn = true
            return Resource.Success(
                AuthTokens(
                    accessToken = "valid-access-token",
                    refreshToken = "valid-refresh-token",
                    tokenType = "Bearer",
                    expiresIn = 900L
                )
            )
        }

        override suspend fun register(
            email: String,
            password: String,
            confirmPassword: String
        ): Resource<RegisterResult> {
            return Resource.Success(
                RegisterResult(
                    id = 101L,
                    email = email,
                    accountType = "CUSTOMER",
                    status = "PENDING_VERIFICATION"
                )
            )
        }

        override suspend fun verifyEmail(email: String, otp: String): Resource<String> {
            return Resource.Success("Email verified successfully")
        }

        override suspend fun resendVerification(email: String): Resource<Unit> {
            return Resource.Success(Unit)
        }

        override suspend fun googleLogin(idToken: String): Resource<AuthTokens> {
            return Resource.Success(
                AuthTokens(
                    accessToken = "google-access-token",
                    refreshToken = "google-refresh-token",
                    tokenType = "Bearer",
                    expiresIn = 900L
                )
            )
        }

        override suspend fun refreshToken(): Resource<AuthTokens> {
            return Resource.Success(
                AuthTokens(
                    accessToken = "refreshed-access-token",
                    refreshToken = "refreshed-refresh-token",
                    tokenType = "Bearer",
                    expiresIn = 900L
                )
            )
        }

        override suspend fun logout(): Resource<Unit> {
            loggedIn = false
            return Resource.Success(Unit)
        }

        override suspend fun forgotPassword(email: String): Resource<PasswordResetResult> {
            return Resource.Success(PasswordResetResult("OTP sent"))
        }

        override suspend fun resetPassword(
            email: String,
            otp: String,
            newPassword: String,
            confirmPassword: String?
        ): Resource<PasswordResetResult> {
            return Resource.Success(PasswordResetResult("Password reset successful"))
        }

        override suspend fun isLoggedIn(): Boolean = loggedIn
    }
}
