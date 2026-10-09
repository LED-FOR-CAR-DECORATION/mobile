package com.carled.app.core.di

import android.content.Context
import com.carled.app.data.ble.CarLedBleManager
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.MockApiService
import com.carled.app.data.repository.AuthRepositoryImpl
import com.carled.app.data.repository.BleDeviceRepositoryImpl
import com.carled.app.data.repository.ProductRepositoryImpl
import com.carled.app.domain.repository.AuthRepository
import com.carled.app.domain.repository.BleDeviceRepository
import com.carled.app.domain.repository.ProductRepository
import com.carled.app.domain.usecase.auth.LoginUseCase
import com.carled.app.domain.usecase.ble.BleUseCases
import com.carled.app.domain.usecase.ble.ConnectDeviceUseCase
import com.carled.app.domain.usecase.ble.DisconnectDeviceUseCase
import com.carled.app.domain.usecase.ble.ScanDevicesUseCase
import com.carled.app.domain.usecase.ble.SendBrightnessCommandUseCase
import com.carled.app.domain.usecase.ble.SendColorCommandUseCase
import com.carled.app.domain.usecase.ble.SendModeCommandUseCase
import com.carled.app.domain.usecase.ble.SendPowerCommandUseCase
import com.carled.app.domain.usecase.ble.StopScanUseCase

/**
 * ServiceLocator quản lý phụ thuộc (Dependency Injection) độc lập, gọn nhẹ.
 * Dễ dàng thay thế hoặc tích hợp cùng Hilt / Koin khi mở rộng dự án.
 */
object ServiceLocator {
    private var appContext: Context? = null

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

    val bleUseCases: BleUseCases by lazy {
        BleUseCases(
            scanDevices = ScanDevicesUseCase(bleDeviceRepository),
            stopScan = StopScanUseCase(bleDeviceRepository),
            connectDevice = ConnectDeviceUseCase(bleDeviceRepository),
            disconnectDevice = DisconnectDeviceUseCase(bleDeviceRepository),
            sendColorCommand = SendColorCommandUseCase(bleDeviceRepository),
            sendModeCommand = SendModeCommandUseCase(bleDeviceRepository),
            sendPowerCommand = SendPowerCommandUseCase(bleDeviceRepository),
            sendBrightnessCommand = SendBrightnessCommandUseCase(bleDeviceRepository)
        )
    }

    val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(authRepository)
    }
}
