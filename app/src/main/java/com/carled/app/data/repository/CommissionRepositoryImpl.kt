package com.carled.app.data.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.dto.ApiErrorResponse
import com.carled.app.data.remote.dto.ApiResponse
import com.carled.app.domain.model.*
import com.carled.app.domain.model.commission.ApproveDeliverableCardResponse
import com.carled.app.domain.model.commission.CommissionMessageResponse
import com.carled.app.domain.model.commission.CommissionResponse
import com.carled.app.domain.model.commission.CommissionSummaryResponse
import com.carled.app.domain.model.commission.ConfirmRevisionCardRequest
import com.carled.app.domain.model.commission.ConfirmRevisionCardResponse
import com.carled.app.domain.model.commission.CreateCommissionMessageRequest
import com.carled.app.domain.model.commission.CreateCommissionRequest
import com.carled.app.domain.repository.CommissionRepository
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class CommissionRepositoryImpl(
    private val api: ApiService,
    private val authHeaderProvider: suspend () -> String?,
    private val gson: Gson
) : CommissionRepository {

    override suspend fun submit(request: CreateCommissionRequest): Resource<CommissionResponse> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập trước khi gửi yêu cầu commission.")
        return try {
            val response = api.createCommission(authHeader, request)
            if (response.isSuccessful) {
                // NOT wrapped in ApiResponse — the body is CustomerCommissionDetailResponse
                // directly (confirmed via /v3/api-docs), unlike every GET endpoint here.
                val body = response.body() ?: return Resource.Error("Phản hồi từ máy chủ trống")
                Resource.Success(body)
            } else {
                Resource.Error(parseErrorBody(response, "Không thể gửi yêu cầu commission (lỗi ${response.code()})."))
            }
        } catch (e: IOException) {
            Resource.Error("Không kết nối được tới máy chủ. Kiểm tra mạng và thử lại.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể gửi yêu cầu commission.")
        }
    }

    override suspend fun getMyCommissions(status: String?, sortDirection: String, page: Int, size: Int): Resource<List<CommissionSummaryResponse>> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập trước khi xem danh sách commission.")
        return safeCall("Không tải được danh sách commission.") {
            api.getMyCommissions(authHeader, status, sortDirection, page, size)
        }
    }

    override suspend fun getCommissionDetail(id: Long): Resource<CommissionResponse> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập trước khi xem commission.")
        return safeCall("Không tải được chi tiết commission.") { api.getCommissionDetail(authHeader, id) }
    }

    override suspend fun getMessages(id: Long): Resource<List<CommissionMessageResponse>> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập trước khi xem tin nhắn.")
        return safeCall("Không tải được tin nhắn.") { api.getCommissionMessages(authHeader, id) }
    }

    override suspend fun sendMessage(id: Long, request: CreateCommissionMessageRequest): Resource<CommissionMessageResponse> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập trước khi gửi tin nhắn.")
        return safeCall("Không gửi được tin nhắn.") { api.sendCommissionMessage(authHeader, id, request) }
    }

    override suspend fun getPaymentSession(code: String): Resource<PaymentSessionResponse> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập.")
        return safeCall("Không tải được phiên thanh toán.") { api.getPaymentSession(authHeader, code) }
    }

    override suspend fun mockPaymentSuccess(code: String): Resource<PaymentSessionResponse> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập.")
        return safeCall("Không thể mô phỏng thanh toán.") { api.mockPaymentSuccess(authHeader, code) }
    }

    /** Shared isSuccessful/body().data/error-parsing shape used by every method above. */
    private suspend fun <T> safeCall(defaultError: String, block: suspend () -> Response<ApiResponse<T>>): Resource<T> = try {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()?.data ?: return Resource.Error("Phản hồi từ máy chủ trống")
            Resource.Success(body)
        } else {
            Resource.Error(parseErrorBody(response, "$defaultError (lỗi ${response.code()})."))
        }
    } catch (e: IOException) {
        Resource.Error("Không kết nối được tới máy chủ. Kiểm tra mạng và thử lại.")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resource.Error(e.message ?: defaultError)
    }

    private fun parseErrorBody(response: Response<*>, defaultMessage: String): String = try {
        response.errorBody()?.string()?.takeIf { it.isNotBlank() }
            ?.let { gson.fromJson(it, ApiErrorResponse::class.java).resolvedMessage() }
            ?: defaultMessage
    } catch (e: CancellationException) { throw e } catch (e: Exception) { defaultMessage }

    override suspend fun approveDeliverable(id: Long): Resource<ApproveDeliverableCardResponse> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập.")
        return try {
            val response = api.approveDeliverableCard(authHeader, id)
            if (response.isSuccessful) {
                response.body()?.let { Resource.Success(it) } ?: Resource.Error("Phản hồi từ máy chủ trống")
            } else {
                Resource.Error(parseErrorBody(response, "Không thể duyệt bản xem trước (lỗi ${response.code()})."))
            }
        } catch (e: IOException) {
            Resource.Error("Không kết nối được tới máy chủ. Kiểm tra mạng và thử lại.")
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { Resource.Error(e.message ?: "Không thể duyệt bản xem trước.") }
    }

    override suspend fun confirmRevisionCard(id: Long, messageId: Long): Resource<ConfirmRevisionCardResponse> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập.")
        return safeCall("Không thể xác nhận yêu cầu sửa.") { api.confirmRevisionCard(authHeader, id,
            ConfirmRevisionCardRequest(messageId)
        ) }
    }

    override suspend fun cancelRevisionCard(id: Long): Resource<Unit> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập.")
        return try {
            val response = api.cancelRevisionCard(authHeader, id)
            if (response.isSuccessful) Resource.Success(Unit)
            else Resource.Error(parseErrorBody(response, "Không thể huỷ yêu cầu sửa (lỗi ${response.code()})."))
        } catch (e: IOException) {
            Resource.Error("Không kết nối được tới máy chủ. Kiểm tra mạng và thử lại.")
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { Resource.Error(e.message ?: "Không thể huỷ yêu cầu sửa.") }
    }
}