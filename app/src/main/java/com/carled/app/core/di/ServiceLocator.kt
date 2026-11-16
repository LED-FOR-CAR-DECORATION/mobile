package com.carled.app.core.di

import android.content.Context
import com.carled.app.BuildConfig
import com.carled.app.data.ble.CarLedBleManager
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.remote.ApiService
import com.carled.app.data.repository.AuthRepositoryImpl
import com.carled.app.data.repository.BleDeviceRepositoryImpl
import com.carled.app.data.repository.DeviceClaimRepositoryImpl
import com.carled.app.data.repository.DeviceRepositoryImpl
import com.carled.app.data.repository.ProductRepositoryImpl
import com.carled.app.data.remote.dto.RefreshTokenRequest
import com.carled.app.domain.repository.AuthRepository
import com.carled.app.domain.repository.BleDeviceRepository
import com.carled.app.domain.repository.DeviceClaimRepository
import com.carled.app.domain.repository.DeviceRepository
import com.carled.app.domain.repository.ProductRepository
import com.carled.app.domain.usecase.auth.GoogleAuthUseCase
import com.carled.app.domain.usecase.ble.*
import com.carled.app.domain.usecase.auth.LoginUseCase
import com.carled.app.domain.usecase.auth.LogoutUseCase
import com.carled.app.domain.usecase.auth.RegisterUseCase
import com.carled.app.domain.usecase.auth.ResendVerificationUseCase
import com.carled.app.domain.usecase.auth.VerifyEmailUseCase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ServiceLocator {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun requireContext(): Context =
        appContext ?: throw IllegalStateException("ServiceLocator chưa được khởi tạo với Context")

    // -------------------------------------------------------------------------
    // Network
    // -------------------------------------------------------------------------

    val gson: Gson = GsonBuilder().create()

    /**
     * Lightweight client used exclusively for token refresh requests.
     * No authenticator attached — avoids the circular-dependency problem of
     * the main client calling itself, and prevents infinite 401 retry loops.
     */
    private val plainHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** Retrofit instance backed by plainHttpClient — only used inside tokenAuthenticator. */
    private val refreshApiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(plainHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    /**
     * OkHttp Authenticator that transparently refreshes an expired access token.
     *
     * Flow:
     *  1. Any request returns 401.
     *  2. OkHttp calls this authenticator on a background thread.
     *  3. We attempt one refresh via [refreshApiService] (separate client, no auth loop).
     *  4a. Success → persist new tokens, retry the original request with the new header.
     *  4b. Failure → clear the session so the app navigates to login on next UI action.
     *
     * [responseCount] guards against infinite loops: if we've already retried once
     * (prior response chain depth > 1) we give up rather than refreshing repeatedly.
     */
    private val tokenAuthenticator: Authenticator by lazy {
        Authenticator { _, response ->
            if (response.responseCount() > 1) return@Authenticator null

            val refreshToken = userPreferences.getRefreshToken()
                ?: return@Authenticator run { authRepository.notifySessionExpired(); null }

            synchronized(this) {
                // Another thread may have already refreshed while we waited for the lock.
                val currentStored = userPreferences.getAuthToken()
                val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
                if (currentStored != null && currentStored != requestToken) {
                    return@Authenticator response.request.newBuilder()
                        .header("Authorization", "Bearer $currentStored")
                        .build()
                }

                val newData = runBlocking {
                    try {
                        val r = refreshApiService.refreshToken(RefreshTokenRequest(refreshToken))
                        if (r.isSuccessful) r.body()?.data else null
                    } catch (e: java.io.IOException) {
                        return@runBlocking null // network blip — don't clear session
                    } catch (_: Exception) {
                        null
                    }
                }

                if (newData == null) {
                    // Only clear if we got an explicit rejection, not a network failure.
                    // (IOException path already returned null above without reaching here
                    //  via the runBlocking early-return; a null from a non-IOException
                    //  rejection still falls through to this clear.)
                    authRepository.notifySessionExpired()
                    return@Authenticator null
                }

                userPreferences.saveAuthToken(newData.accessToken)
                userPreferences.saveRefreshToken(newData.refreshToken)

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newData.accessToken}")
                    .build()
            }
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    val userPreferences: UserPreferences by lazy {
        UserPreferences(requireContext())
    }

    val bleManager: CarLedBleManager by lazy {
        CarLedBleManager(requireContext())
    }

    val bleDeviceRepository: BleDeviceRepository by lazy {
        BleDeviceRepositoryImpl(bleManager)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            apiService      = apiService,
            userPreferences = userPreferences,
            gson            = gson
        )
    }

    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(apiService, gson)
    }

    // Same token source + header format AuthRepositoryImpl uses for
    // apiService.getCurrentUserProfile("Bearer $token").
    private val deviceAuthHeaderProvider: suspend () -> String? = {
        userPreferences.getAuthToken()?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
    }

    val deviceClaimRepository: DeviceClaimRepository by lazy {
        DeviceClaimRepositoryImpl(api = apiService, authHeaderProvider = deviceAuthHeaderProvider)
    }

    val deviceRepository: DeviceRepository by lazy {
        DeviceRepositoryImpl(
            api                = apiService,
            userPreferences    = userPreferences,
            gson               = gson,
            authHeaderProvider = deviceAuthHeaderProvider
        )
    }

    val bleUseCases: BleUseCases by lazy {
        BleUseCases(
            renameDevice           = RenameDeviceUseCase(bleDeviceRepository),
            scanDevices            = ScanDevicesUseCase(bleDeviceRepository),
            stopScan               = StopScanUseCase(bleDeviceRepository),
            connectDevice          = ConnectDeviceUseCase(bleDeviceRepository),
            disconnectDevice       = DisconnectDeviceUseCase(bleDeviceRepository),
            getScannedDevices      = GetScannedDevicesUseCase(bleDeviceRepository),
            getConnectionState     = GetConnectionStateUseCase(bleDeviceRepository),
            getLedState            = GetLedStateUseCase(bleDeviceRepository),
            sendColorCommand       = SendColorCommandUseCase(bleDeviceRepository),
            sendModeCommand        = SendModeCommandUseCase(bleDeviceRepository),
            sendPowerCommand       = SendPowerCommandUseCase(bleDeviceRepository),
            sendBrightnessCommand  = SendBrightnessCommandUseCase(bleDeviceRepository),
            sendSensitivityCommand = SendSensitivityCommandUseCase(bleDeviceRepository),
            sendMarquee            = SendMarqueeUseCase(bleDeviceRepository),
            sendWifiConfig         = SendWifiConfigUseCase(bleDeviceRepository),
            clearCanvas            = ClearCanvasUseCase(bleDeviceRepository),
            reloadProjection       = ReloadProjectionUseCase(bleDeviceRepository),
            uploadProjection       = UploadProjectionUseCase(bleDeviceRepository),
            getTelemetryStatus     = GetTelemetryStatusUseCase(bleDeviceRepository),
            getTelemetryEvent      = GetTelemetryEventUseCase(bleDeviceRepository),
            sendSinglePixel        = SendSinglePixelUseCase(bleDeviceRepository),
            sendBatchDraw          = SendBatchDrawUseCase(bleDeviceRepository)
        )
    }

    val loginUseCase: LoginUseCase by lazy { LoginUseCase(authRepository) }
    val registerUseCase: RegisterUseCase by lazy { RegisterUseCase(authRepository) }
    val logoutUseCase: LogoutUseCase by lazy { LogoutUseCase(authRepository) }
    val verifyEmailUseCase: VerifyEmailUseCase by lazy { VerifyEmailUseCase(authRepository) }
    val resendVerificationUseCase: ResendVerificationUseCase by lazy { ResendVerificationUseCase(authRepository) }
    val googleAuthUseCase: GoogleAuthUseCase by lazy { GoogleAuthUseCase(authRepository) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns how many responses are chained before this one (1 = first attempt).
 * Used by [ServiceLocator.tokenAuthenticator] to avoid refreshing more than once
 * per original request.
 */
private fun okhttp3.Response.responseCount(): Int {
    var count = 1
    var prior = priorResponse
    while (prior != null) { count++; prior = prior.priorResponse }
    return count
}