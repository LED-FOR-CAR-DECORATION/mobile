package com.carled.app.data.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.dto.CustomerActiveSubscriptionResponse
import com.carled.app.domain.repository.SubscriptionRepository
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class SubscriptionRepositoryImpl(
    private val api: ApiService,
    private val authHeaderProvider: suspend () -> String?
) : SubscriptionRepository {

    override suspend fun getActiveSubscriptions(): Resource<List<CustomerActiveSubscriptionResponse>> {
        val authHeader = authHeaderProvider()
            ?: return Resource.Error("Bạn cần đăng nhập trước khi xem gói đăng ký.")
        return try {
            val response = api.getActiveSubscriptions(authHeader)
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data.orEmpty())
            } else {
                Resource.Error("Không tải được gói đăng ký (lỗi ${response.code()}).")
            }
        } catch (e: IOException) {
            Resource.Error("Không kết nối được tới máy chủ. Kiểm tra mạng và thử lại.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không tải được gói đăng ký.")
        }
    }

    override suspend fun getActiveSubscription(): Resource<CustomerActiveSubscriptionResponse?> {
        val authHeader = authHeaderProvider()
            ?: return Resource.Error("Bạn cần đăng nhập trước khi xem gói đăng ký.")
        return try {
            val response = api.getActiveSubscription(authHeader)
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data)
            } else {
                Resource.Error("Không tải được gói đăng ký (lỗi ${response.code()}).")
            }
        } catch (e: IOException) {
            Resource.Error("Không kết nối được tới máy chủ. Kiểm tra mạng và thử lại.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không tải được gói đăng ký.")
        }
    }
}