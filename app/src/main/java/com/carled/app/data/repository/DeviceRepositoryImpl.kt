package com.carled.app.data.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.dto.DeviceDetailDto
import com.carled.app.domain.model.OwnedDevice
import com.carled.app.domain.repository.DeviceRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class DeviceRepositoryImpl(
    private val api: ApiService,
    private val userPreferences: UserPreferences,
    private val gson: Gson,
    private val authHeaderProvider: suspend () -> String?
) : DeviceRepository {

    // -------------------------------------------------------------------------
    // Device list
    // -------------------------------------------------------------------------

    override suspend fun getOwnedDevices(): Resource<List<OwnedDevice>> {
        val authHeader = authHeaderProvider()

        return try {
            if (authHeader != null) {
                val response = api.getDevices(authorization = authHeader, page = 0, size = 50)
                if (response.isSuccessful) {
                    val domainModels = (response.body()?.data ?: emptyList()).map { dto ->
                        OwnedDevice(
                            id              = dto.deviceId.toString(),
                            deviceName      = dto.deviceName,
                            macAddress      = dto.macAddress,
                            serialNumber    = dto.serialNumber,
                            firmwareVersion = dto.currentFirmwareId?.toString(),
                            lastUpdated     = dto.boundAt
                        )
                    }
                    // Persist immediately so the next cold-start reads fresh data.
                    userPreferences.saveCachedDevices(gson.toJson(domainModels))
                    return Resource.Success(domainModels)
                } else {
                    return loadFromCacheOrError(
                        "Không thể tải danh sách thiết bị từ máy chủ (lỗi ${response.code()})."
                    )
                }
            }
            loadFromCacheOrError("Bạn cần đăng nhập trước khi xem danh sách thiết bị.")
        } catch (e: IOException) {
            loadFromCacheOrError("Không có kết nối mạng. Đang hiển thị dữ liệu lưu trữ cục bộ.")
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            loadFromCacheOrError(e.message ?: "Đã xảy ra lỗi khi tải danh sách thiết bị.")
        }
    }

    override suspend fun getOwnedDevicesFromCache(): Resource<List<OwnedDevice>> =
        loadFromCacheOrError("Chưa có dữ liệu thiết bị được lưu trữ cục bộ.")

    // -------------------------------------------------------------------------
    // Device detail
    // -------------------------------------------------------------------------

    override suspend fun getDeviceDetail(deviceId: Long): Resource<DeviceDetailDto> {
        if (deviceId <= 0) return Resource.Error("ID thiết bị phải lớn hơn 0.")

        val authHeader = authHeaderProvider()

        return try {
            if (authHeader != null) {
                val response = api.getDeviceById(authorization = authHeader, id = deviceId)
                if (response.isSuccessful) {
                    val body = response.body()?.data
                    if (body != null) {
                        userPreferences.saveCachedDeviceDetail(deviceId, gson.toJson(body))
                        return Resource.Success(body)
                    }
                } else {
                    return loadDetailFromCacheOrError(
                        deviceId,
                        "Không thể tải thông tin chi tiết thiết bị (lỗi ${response.code()})."
                    )
                }
            }
            loadDetailFromCacheOrError(deviceId, "Không thể tải thông tin chi tiết thiết bị từ máy chủ.")
        } catch (e: IOException) {
            loadDetailFromCacheOrError(deviceId, "Không có kết nối mạng. Đang hiển thị dữ liệu lưu trữ cục bộ.")
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            loadDetailFromCacheOrError(deviceId, e.message ?: "Đã xảy ra lỗi.")
        }
        catch (e: Exception) {
            loadDetailFromCacheOrError(deviceId, "Không thể tải thông tin chi tiết thiết bị.")
        }
    }

    override suspend fun getDeviceDetailFromCache(deviceId: Long): Resource<DeviceDetailDto> =
        loadDetailFromCacheOrError(deviceId, "Chưa có thông tin chi tiết thiết bị được lưu trữ cục bộ.")

    // -------------------------------------------------------------------------
    // Private cache helpers
    // -------------------------------------------------------------------------

    private fun loadFromCacheOrError(fallbackMessage: String): Resource<List<OwnedDevice>> {
        val json = userPreferences.getCachedDevices()
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<OwnedDevice>>() {}.type
                val list = gson.fromJson<List<OwnedDevice>>(json, type)
                if (list != null) return Resource.Success(list)
            } catch (_: Exception) {}
        }
        return Resource.Error(fallbackMessage)
    }

    private fun loadDetailFromCacheOrError(
        deviceId: Long,
        fallbackMessage: String
    ): Resource<DeviceDetailDto> {
        val json = userPreferences.getCachedDeviceDetail(deviceId)
        if (!json.isNullOrBlank()) {
            try {
                val detail = gson.fromJson(json, DeviceDetailDto::class.java)
                if (detail != null) return Resource.Success(detail)
            } catch (_: Exception) {}
        }
        return Resource.Error(fallbackMessage)
    }
}