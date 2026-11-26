package com.carled.app.domain.usecase.auth

import com.carled.app.core.common.Resource
import com.carled.app.domain.repository.AuthRepository

/**
 * [onLoggedOut] runs once the server logout succeeds, before this returns.
 * Use it to reset any session-scoped in-memory state that lives outside
 * AuthRepository itself — e.g. ServiceLocator.textProfilePlanProvider.reset() —
 * so a different user logging in next doesn't inherit the previous user's
 * cached subscription plan. Defaults to a no-op so any other existing call
 * site that builds `LogoutUseCase(authRepository)` without the new argument
 * keeps compiling unchanged.
 */
class LogoutUseCase(
    private val repository: AuthRepository,
    private val onLoggedOut: () -> Unit = {}
) {
    suspend operator fun invoke(): Resource<Unit> {
        val result = repository.logout()
        if (result is Resource.Success) onLoggedOut()
        return result
    }
}