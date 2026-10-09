package com.carled.app

import android.app.Application
import com.carled.app.core.di.ServiceLocator

class CarLedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo ServiceLocator cho Dependency Injection toàn ứng dụng
        ServiceLocator.initialize(this)
    }
}
