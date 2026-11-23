package com.carled.app.data.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.remote.ApiService
import com.carled.app.domain.model.commission.CommissionPricingResponse
import com.carled.app.domain.repository.CommissionPricingRepository
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class CommissionPricingRepositoryImpl(
    private val api: ApiService
) : CommissionPricingRepository {

    override suspend fun getPricing(): Resource<CommissionPricingResponse> = try {
        val response = api.getCommissionPricing()
        if (response.isSuccessful) {
            val body = response.body()?.data
                ?: return Resource.Error("Phản hồi từ máy chủ trống")
            Resource.Success(body)
        } else {
            Resource.Error("Không thể tải bảng giá commission (lỗi ${response.code()}).")
        }
    } catch (e: IOException) {
        Resource.Error("Không kết nối được tới máy chủ. Kiểm tra mạng và thử lại.")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Không tải được bảng giá commission.")
    }
}