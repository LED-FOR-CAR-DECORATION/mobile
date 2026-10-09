package com.carled.app.domain.repository

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun login(email: String, password: String): Resource<User>
    suspend fun logout(): Resource<Unit>
    suspend fun isLoggedIn(): Boolean
}
