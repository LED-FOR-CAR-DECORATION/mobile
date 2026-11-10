package com.carled.app.domain.model.iam

/**
 * Domain entity chứa kết quả tạo tài khoản mới.
 */
data class RegisterResult(
    val id: Long?,
    val email: String,
    val accountType: String,
    val status: String
)
