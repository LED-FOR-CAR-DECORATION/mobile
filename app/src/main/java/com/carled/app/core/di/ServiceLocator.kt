package com.carled.app.core.di

import android.content.Context
import com.carled.app.core.network.NetworkConfig
import com.carled.app.core.network.RetrofitClient
import com.carled.app.data.ble.CarLedBleManager
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.MockApiService
import com.carled.app.data.remote.iam.api.IamApiService
import com.carled.app.data.remote.iam.api.MockIamApiService
import com.carled.app.data.repository.AuthRepositoryImpl
import com.carled.app.data.repository.BleDeviceRepositoryImpl
import com.carled.app.data.repository.ProductRepositoryImpl
import com.carled.app.data.repository.iam.IamRepositoryImpl
import com.carled.app.domain.repository.AuthRepository
import com.carled.app.domain.repository.BleDeviceRepository
import com.carled.app.domain.repository.ProductRepository
import com.carled.app.domain.repository.iam.IamRepository
import com.carled.app.domain.usecase.ble.*
import com.carled.app.domain.usecase.auth.LoginUseCase
import com.carled.app.domain.usecase.auth.LogoutUseCase
import com.carled.app.domain.usecase.auth.RegisterUseCase
import com.carled.app.domain.usecase.iam.IamUseCases

/**
 * ServiceLocator quản lý phụ thuộc (Dependency Injection) độc lập, gọn nhẹ.
 * Dễ dàng thay thế hoặc tích hợp cùng Hilt / Koin khi mở rộng dự án.
 */
object ServiceLocator {
    private var appContext: Context? = null

    /**
     * Cờ cho phép chuyển đổi nhanh giữa Retrofit Client thật kết nối backend và Mock API.
     */
    var useMockNetwork: Boolean = false

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun requireContext(): Context {
        return appContext ?: throw IllegalStateException("ServiceLocator chưa được khởi tạo với Context")
    }

    val userPreferences: UserPreferences by lazy {
        UserPreferences(requireContext())
    }

    val bleManager: CarLedBleManager by lazy {
        CarLedBleManager(requireContext())
    }

    val apiService: ApiService by lazy {
        MockApiService()
    }

    val bleDeviceRepository: BleDeviceRepository by lazy {
        BleDeviceRepositoryImpl(bleManager)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(apiService, userPreferences)
    }

    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(apiService)
    }

    // IAM Network & Repository
    val iamApiService: IamApiService by lazy {
        if (useMockNetwork) {
            MockIamApiService()
        } else {
            RetrofitClient.createIamApiService(
                baseUrl = NetworkConfig.activeBaseUrl,
                tokenProvider = { userPreferences.getAccessToken() }
            )
        }
    }

    val iamRepository: IamRepository by lazy {
        IamRepositoryImpl(iamApiService, userPreferences)
    }

    val iamUseCases: IamUseCases by lazy {
        IamUseCases(
            login = com.carled.app.domain.usecase.iam.LoginUseCase(iamRepository),
            register = com.carled.app.domain.usecase.iam.RegisterUseCase(iamRepository),
            verifyEmail = com.carled.app.domain.usecase.iam.VerifyEmailUseCase(iamRepository),
            resendVerification = com.carled.app.domain.usecase.iam.ResendVerificationUseCase(iamRepository),
            googleLogin = com.carled.app.domain.usecase.iam.GoogleLoginUseCase(iamRepository),
            refreshToken = com.carled.app.domain.usecase.iam.RefreshTokenUseCase(iamRepository),
            logout = com.carled.app.domain.usecase.iam.LogoutUseCase(iamRepository),
            forgotPassword = com.carled.app.domain.usecase.iam.ForgotPasswordUseCase(iamRepository),
            resetPassword = com.carled.app.domain.usecase.iam.ResetPasswordUseCase(iamRepository)
        )
    }

    val bleUseCases: BleUseCases by lazy {
        BleUseCases(
            renameDevice = RenameDeviceUseCase(bleDeviceRepository),
            // Scan / Connect
            scanDevices          = ScanDevicesUseCase(bleDeviceRepository),
            stopScan             = StopScanUseCase(bleDeviceRepository),
            connectDevice        = ConnectDeviceUseCase(bleDeviceRepository),
            disconnectDevice     = DisconnectDeviceUseCase(bleDeviceRepository),
            getScannedDevices    = GetScannedDevicesUseCase(bleDeviceRepository),
            getConnectionState   = GetConnectionStateUseCase(bleDeviceRepository),
            getLedState          = GetLedStateUseCase(bleDeviceRepository),
            // Basic LED
            sendColorCommand     = SendColorCommandUseCase(bleDeviceRepository),
            sendModeCommand      = SendModeCommandUseCase(bleDeviceRepository),
            sendPowerCommand     = SendPowerCommandUseCase(bleDeviceRepository),
            sendBrightnessCommand = SendBrightnessCommandUseCase(bleDeviceRepository),
            sendSensitivityCommand = SendSensitivityCommandUseCase(bleDeviceRepository),
            // Advanced features
            sendMarquee          = SendMarqueeUseCase(bleDeviceRepository),
            sendWifiConfig       = SendWifiConfigUseCase(bleDeviceRepository),
            clearCanvas          = ClearCanvasUseCase(bleDeviceRepository),
            reloadProjection     = ReloadProjectionUseCase(bleDeviceRepository),
            uploadProjection     = UploadProjectionUseCase(bleDeviceRepository),
            // Telemetry
            getTelemetryStatus   = GetTelemetryStatusUseCase(bleDeviceRepository),
            getTelemetryEvent    = GetTelemetryEventUseCase(bleDeviceRepository),
            // Canvas draw
            sendSinglePixel      = SendSinglePixelUseCase(bleDeviceRepository),
            sendBatchDraw        = SendBatchDrawUseCase(bleDeviceRepository)
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
}
