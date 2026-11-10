package com.carled.app.domain.model.iam

/**
 * Domain entity đại diện cho thông tin tài khoản người dùng xác thực.
 */
data class IamUser(
    val id: String,
    val email: String,
    val accountType: String = "CUSTOMER",
    val status: String = "ACTIVE",
    val fullName: String = ""
)
