package com.carled.app.core.di

import android.content.Context
import com.carled.app.BuildConfig
import com.carled.app.data.ble.CarLedBleManager
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.remote.ApiService
import com.carled.app.data.repository.AuthRepositoryImpl
import com.carled.app.data.repository.BleDeviceRepositoryImpl
import com.carled.app.data.repository.DeviceClaimRepositoryImpl
import com.carled.app.data.repository.ProductRepositoryImpl
import com.carled.app.domain.repository.AuthRepository
import com.carled.app.domain.repository.BleDeviceRepository
import com.carled.app.domain.repository.DeviceClaimRepository
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

    private fun requireContext(): Context {
        return appContext ?: throw IllegalStateException("ServiceLocator chưa được khởi tạo với Context")
    }

    // -------------------------------------------------------------------------
    // Network
    // -------------------------------------------------------------------------

    val gson: Gson = GsonBuilder().create()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
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

    val deviceClaimRepository: DeviceClaimRepository by lazy {
        DeviceClaimRepositoryImpl(
            api = apiService,
            // Same token source + header format AuthRepositoryImpl uses for
            // apiService.getCurrentUserProfile("Bearer $token").
            authHeaderProvider = {
                userPreferences.getAuthToken()?.takeIf { it.isNotBlank() }?.let { token -> "Bearer $token" }
            }
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

    val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(authRepository)
    }

    val registerUseCase: RegisterUseCase by lazy {
        RegisterUseCase(authRepository)
    }

    val logoutUseCase: LogoutUseCase by lazy {
        LogoutUseCase(authRepository)
    }

    val verifyEmailUseCase: VerifyEmailUseCase by lazy {
        VerifyEmailUseCase(authRepository)
    }

    val resendVerificationUseCase: ResendVerificationUseCase by lazy {
        ResendVerificationUseCase(authRepository)
    }
    val googleAuthUseCase: GoogleAuthUseCase by lazy {
        GoogleAuthUseCase(authRepository)
    }
}