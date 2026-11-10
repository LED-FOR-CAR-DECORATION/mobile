package com.carled.app.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor tự động gắn Header Authorization nếu có Access Token hợp lệ.
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Nếu request đã có Authorization Header (ví dụ truyền thủ công), giữ nguyên
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        val token = tokenProvider()
        return if (!token.isNullOrBlank()) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
