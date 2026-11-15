package com.carled.app.core.di

import android.content.Context
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.repository.AuthRepositoryImpl
import com.carled.app.domain.repository.AuthRepository
import com.carled.app.domain.usecase.auth.RegisterUseCase
import com.carled.app.domain.usecase.auth.ResendVerificationUseCase
import com.carled.app.domain.usecase.auth.VerifyEmailUseCase

class AppModule(context: Context) {

    val userPreferences = UserPreferences(context)

    val authRepository: AuthRepository = AuthRepositoryImpl(
        apiService      = ServiceLocator.apiService,
        userPreferences = userPreferences,
        gson            = ServiceLocator.gson
    )

    // Use cases
    val registerUseCase = RegisterUseCase(authRepository)
    val verifyEmailUseCase = VerifyEmailUseCase(authRepository)
    val resendVerificationUseCase = ResendVerificationUseCase(authRepository)
}