package com.carled.app.core.network

import com.carled.app.data.remote.iam.api.IamApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory tạo Retrofit instance và ApiService với OkHttpClient được cấu hình đầy đủ.
 */
object RetrofitClient {

    val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    fun createOkHttpClient(
        tokenProvider: () -> String?,
        enableLogging: Boolean = true
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(NetworkConfig.TIMEOUT_CONNECT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.TIMEOUT_READ_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.TIMEOUT_WRITE_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenProvider))

        if (enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    fun createRetrofit(
        baseUrl: String = NetworkConfig.activeBaseUrl,
        okHttpClient: OkHttpClient
    ): Retrofit {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun createIamApiService(
        baseUrl: String = NetworkConfig.activeBaseUrl,
        tokenProvider: () -> String?
    ): IamApiService {
        val client = createOkHttpClient(tokenProvider)
        val retrofit = createRetrofit(baseUrl, client)
        return retrofit.create(IamApiService::class.java)
    }
}
