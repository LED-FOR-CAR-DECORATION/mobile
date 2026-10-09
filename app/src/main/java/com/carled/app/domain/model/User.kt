package com.carled.app.domain.model

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String, // Customer, Staff, etc.
    val token: String? = null
)
