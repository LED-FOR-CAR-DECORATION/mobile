package com.carled.app.data.remote.iam.api

import com.carled.app.data.remote.iam.dto.request.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockIamApiServiceTest {

    private lateinit var apiService: MockIamApiService

    @Before
    fun setUp() {
        apiService = MockIamApiService(simulatedDelayMs = 0L, simulateNetworkFailure = false)
    }

    @Test
    fun `login returns success response with access and refresh tokens`() = runBlocking {
        val response = apiService.login(LoginRequestDto("driver@carled.vn", "P@ssword123"))
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertNotNull(body?.data?.accessToken)
        assertNotNull(body?.data?.refreshToken)
        assertEquals("Bearer", body?.data?.tokenType)
    }

    @Test
    fun `register returns created account with customer status`() = runBlocking {
        val response = apiService.register(
            RegisterRequestDto("newdriver@carled.vn", "P@ssword123", "P@ssword123")
        )
        assertTrue(response.isSuccessful)
        val data = response.body()?.data
        assertNotNull(data)
        assertEquals("newdriver@carled.vn", data?.email)
        assertEquals("CUSTOMER", data?.accountType)
        assertEquals("PENDING_VERIFICATION", data?.status)
    }

    @Test
    fun `verifyEmail returns success message`() = runBlocking {
        val response = apiService.verifyEmail(
            VerifyEmailRequestDto("driver@carled.vn", "123456")
        )
        assertTrue(response.isSuccessful)
        assertEquals("Email verified successfully", response.body()?.data)
    }

    @Test
    fun `resendVerification returns accepted 202`() = runBlocking {
        val response = apiService.resendVerification(
            ResendVerificationRequestDto("driver@carled.vn")
        )
        assertTrue(response.isSuccessful)
        assertEquals(202, response.code())
    }

    @Test
    fun `googleLogin returns valid token session`() = runBlocking {
        val response = apiService.googleLogin(GoogleLoginRequestDto("mock-google-id-token"))
        assertTrue(response.isSuccessful)
        assertNotNull(response.body()?.data?.accessToken)
    }

    @Test
    fun `refreshToken returns renewed access token`() = runBlocking {
        val response = apiService.refreshToken(RefreshTokenRequestDto("mock-refresh-token"))
        assertTrue(response.isSuccessful)
        assertNotNull(response.body()?.data?.accessToken)
    }

    @Test
    fun `logout returns 204 no content`() = runBlocking {
        val response = apiService.logout("Bearer mock-token", LogoutRequestDto("mock-refresh-token"))
        assertTrue(response.isSuccessful)
        assertEquals(204, response.code())
    }

    @Test
    fun `forgotPassword returns generic acknowledgment message`() = runBlocking {
        val response = apiService.forgotPassword(ForgotPasswordRequestDto("driver@carled.vn"))
        assertTrue(response.isSuccessful)
        assertNotNull(response.body()?.data?.message)
    }

    @Test
    fun `resetPassword returns success acknowledgment`() = runBlocking {
        val response = apiService.resetPassword(
            ResetPasswordRequestDto("driver@carled.vn", "123456", "NewPassword123")
        )
        assertTrue(response.isSuccessful)
        assertNotNull(response.body()?.data?.message)
    }
}
