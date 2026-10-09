package com.carled.app.data.remote.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String
)

data class ProductDto(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val stockQuantity: Int,
    val category: String
)
