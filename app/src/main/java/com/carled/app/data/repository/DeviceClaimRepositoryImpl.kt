package com.carled.app.data.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.dto.ClaimDeviceRequest
import com.carled.app.domain.model.ClaimedDevice
import com.carled.app.domain.model.DeviceClaimPayload
import com.carled.app.domain.repository.DeviceClaimRepository
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class DeviceClaimRepositoryImpl(
    private val api: ApiService,
    private val authHeaderProvider: suspend () -> String?
) : DeviceClaimRepository {

    override suspend fun claimDevice(payload: DeviceClaimPayload): Resource<ClaimedDevice> {
        val authHeader = authHeaderProvider()
            ?: return Resource.Error("Bạn cần đăng nhập trước khi thêm thiết bị.")

        return try {
            val response = api.claimDevice(
                authorization = authHeader,
                request = ClaimDeviceRequest(qrPayload = payload.qrPayload)
            )

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Resource.Error("Phản hồi trống từ máy chủ.")
                Resource.Success(
                    ClaimedDevice(
                        id = body.deviceId.toString(),
                        deviceName = body.deviceName,
                        macAddress = body.macAddress,
                        serialNumber = body.serialNumber
                    )
                )
            } else {
                val message = when (response.code()) {
                    400 -> "Mã QR thiết bị không hợp lệ hoặc bị sai chữ ký."
                    401 -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
                    403 -> "Thiết bị này không thuộc đơn hàng của tài khoản hiện tại."
                    404 -> "Không tìm thấy thiết bị với mã này."
                    409 -> "Thiết bị đã được kích hoạt hoặc đơn hàng chưa hoàn tất giao hàng."
                    else -> "Không thể xác nhận thiết bị (lỗi máy chủ ${response.code()})."
                }
                Resource.Error(message)
            }
        } catch (e: IOException) {
            Resource.Error("Không có kết nối mạng. Kiểm tra Internet và thử lại.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Đã xảy ra lỗi khi xác nhận thiết bị.", e)
        }
    }
}