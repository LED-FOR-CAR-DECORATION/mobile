package com.carled.app.data.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.dto.LoginRequest
import com.carled.app.domain.model.User
import com.carled.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(
        userPreferences.getAuthToken()?.let { token ->
            User(
                id = userPreferences.getUserId() ?: "usr_001",
                email = userPreferences.getUserEmail() ?: "user@carled.vn",
                fullName = "Car LED User",
                role = "Customer",
                token = token
            )
        }
    )

    override fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            userPreferences.saveAuthToken(response.token)
            userPreferences.saveUser(response.user.id, response.user.email)

            val domainUser = User(
                id = response.user.id,
                email = response.user.email,
                fullName = response.user.fullName,
                role = response.user.role,
                token = response.token
            )
            _currentUser.value = domainUser
            Resource.Success(domainUser)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Đăng nhập không thành công", e)
        }
    }

    override suspend fun register(email: String, password: String): Resource<User> {
        return try {
            val response = apiService.register(LoginRequest(email, password))
            userPreferences.saveAuthToken(response.token)
            userPreferences.saveUser(response.user.id, response.user.email)

            val domainUser = User(
                id = response.user.id,
                email = response.user.email,
                fullName = response.user.fullName,
                role = response.user.role,
                token = response.token
            )
            _currentUser.value = domainUser
            Resource.Success(domainUser)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Đăng ký không thành công", e)
        }
    }

    override suspend fun logout(): Resource<Unit> {
        userPreferences.clear()
        _currentUser.value = null
        return Resource.Success(Unit)
    }

    override suspend fun isLoggedIn(): Boolean {
        return !userPreferences.getAuthToken().isNullOrBlank()
    }
}
