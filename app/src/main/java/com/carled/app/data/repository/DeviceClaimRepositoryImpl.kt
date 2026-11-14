package com.carled.app.data.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.dto.ClaimDeviceRequest
import com.carled.app.domain.model.ClaimedDevice
import com.carled.app.domain.model.DeviceClaimPayload
import com.carled.app.domain.repository.DeviceClaimRepository
import java.io.IOException


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
                request = ClaimDeviceRequest(
                    v = payload.version,
                    sn = payload.serialNumber,
                    mac = payload.macAddress,
                    sig = payload.signature
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Resource.Error("Phản hồi trống từ máy chủ.")
                Resource.Success(
                    ClaimedDevice(
                        id = body.id,
                        deviceName = body.deviceName,
                        macAddress = body.mac,
                        serialNumber = body.sn
                    )
                )
            } else {
                val message = when (response.code()) {
                    401, 403 -> "Mã QR không hợp lệ hoặc đã được dùng để xác nhận thiết bị khác."
                    404 -> "Không tìm thấy thiết bị với mã này."
                    else -> "Không thể xác nhận thiết bị (lỗi máy chủ ${response.code()})."
                }
                Resource.Error(message)
            }
        } catch (e: IOException) {
            Resource.Error("Không có kết nối mạng. Kiểm tra Internet và thử lại.", e)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Đã xảy ra lỗi khi xác nhận thiết bị.", e)
        }
    }
}