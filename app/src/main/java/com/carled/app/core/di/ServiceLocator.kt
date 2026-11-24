package com.carled.app.core.di

import android.content.Context
import com.carled.app.BuildConfig
import com.carled.app.data.ble.CarLedBleManager
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.CommissionChatSocket
import com.carled.app.data.repository.AuthRepositoryImpl
import com.carled.app.data.repository.BleDeviceRepositoryImpl
import com.carled.app.data.repository.DeviceClaimRepositoryImpl
import com.carled.app.data.repository.DeviceRepositoryImpl
import com.carled.app.data.repository.ProductRepositoryImpl
import com.carled.app.data.remote.dto.RefreshTokenRequest
import com.carled.app.data.repository.CommissionPricingRepositoryImpl
import com.carled.app.data.repository.CommissionRepositoryImpl
import com.carled.app.data.repository.MediaRepositoryImpl
import com.carled.app.domain.repository.AuthRepository
import com.carled.app.domain.repository.BleDeviceRepository
import com.carled.app.domain.repository.CommissionPricingRepository
import com.carled.app.domain.repository.CommissionRepository
import com.carled.app.domain.repository.DeviceClaimRepository
import com.carled.app.domain.repository.DeviceRepository
import com.carled.app.domain.repository.MediaRepository
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

    val plainHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val refreshApiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(plainHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    private val tokenAuthenticator: Authenticator by lazy {
        Authenticator { _, response ->
            if (response.responseCount() > 1) return@Authenticator null

            val refreshToken = userPreferences.getRefreshToken()
                ?: return@Authenticator run { authRepository.notifySessionExpired(); null }

            synchronized(this) {
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
                        return@runBlocking null
                    } catch (_: Exception) {
                        null
                    }
                }

                if (newData == null) {
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
            .addInterceptor { chain ->
                // OTA session responses contain a download capability.
                if (chain.request().url.encodedPath.endsWith("/ota/sessions")) {
                    chain.proceed(chain.request())
                } else {
                    logging.intercept(chain)
                }
            }
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
        BleDeviceRepositoryImpl(
            bleManager = bleManager,
            isMacOwned = deviceRepository::isMacOwned
        )
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

    val firmwareBleClient by lazy {
        com.carled.app.data.ble.FirmwareBleClient(bleManager)
    }

    val firmwareRepository by lazy {
        com.carled.app.data.repository.FirmwareRepository(
            apiService, userPreferences,
            requireContext().getSharedPreferences("firmware_ota", Context.MODE_PRIVATE), gson
        )
    }

    val commissionPricingRepository: CommissionPricingRepository by lazy {
        CommissionPricingRepositoryImpl(apiService)
    }
    val commissionRepository: CommissionRepository by lazy {
        CommissionRepositoryImpl(apiService, deviceAuthHeaderProvider, gson)
    }
    val commissionChatSocket: CommissionChatSocket by lazy {
        val wsBase = BuildConfig.BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/')
        CommissionChatSocket(okHttpClient, wsBase, gson)
    }
    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(requireContext(), apiService, plainHttpClient, deviceAuthHeaderProvider)
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

private fun okhttp3.Response.responseCount(): Int {
    var count = 1
    var prior = priorResponse
    while (prior != null) { count++; prior = prior.priorResponse }
    return count
}
