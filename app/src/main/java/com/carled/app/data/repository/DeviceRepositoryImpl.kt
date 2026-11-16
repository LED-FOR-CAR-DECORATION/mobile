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

class DeviceRepositoryImpl(
    private val api: ApiService,
    private val userPreferences: UserPreferences,
    private val gson: Gson,
    private val authHeaderProvider: suspend () -> String?
) : DeviceRepository {

    override suspend fun getOwnedDevices(): Resource<List<OwnedDevice>> {
        val authHeader = authHeaderProvider()

        return try {
            if (authHeader != null) {
                android.util.Log.d("API_DEBUG", "Outgoing Authorization Header: $authHeader")
                // Pass page = 0 to satisfy validation: page >= 0
                val response = api.getDevices(authorization = authHeader, page = 0, size = 50)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val deviceDtos = apiResponse?.data ?: emptyList()

                    val domainModels = deviceDtos.map { dto ->
                        OwnedDevice(
                            id = dto.deviceId.toString(),
                            deviceName = dto.deviceName,
                            macAddress = dto.macAddress,
                            serialNumber = dto.serialNumber,
                            firmwareVersion = dto.currentFirmwareId?.toString(),
                            lastUpdated = dto.boundAt
                        )
                    }

                    // Save successfully fetched devices to local cache
                    userPreferences.saveCachedDevices(gson.toJson(domainModels))

                    return Resource.Success(domainModels)
                } else {
                    // Include response.code() to easily diagnose 401, 403, 500, etc.
                    return loadFromCacheOrError(
                        "Không thể tải danh sách thiết bị từ máy chủ (lỗi ${response.code()})."
                    )
                }
            }

            // If token is missing, try loading from local cache
            loadFromCacheOrError("Bạn cần đăng nhập trước khi xem danh sách thiết bị.")
        } catch (e: IOException) {
            // Offline fallback: load from local storage
            loadFromCacheOrError("Không có kết nối mạng. Đang hiển thị dữ liệu lưu trữ cục bộ.")
        } catch (e: Exception) {
            loadFromCacheOrError(e.message ?: "Đã xảy ra lỗi khi tải danh sách thiết bị.")
        }
    }

    override suspend fun getDeviceDetail(deviceId: Long): Resource<DeviceDetailDto> {
        val authHeader = authHeaderProvider()

        if (deviceId <= 0) {
            return Resource.Error("ID thiết bị phải lớn hơn 0.")
        }

        return try {
            if (authHeader != null) {
                val response = api.getDeviceById(authorization = authHeader, id = deviceId)
                if (response.isSuccessful) {
                    val body = response.body()?.data
                    if (body != null) {
                        // Save detail to local cache
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

            // Fallback to local cache if offline or request fails
            loadDetailFromCacheOrError(deviceId, "Không thể tải thông tin chi tiết thiết bị từ máy chủ.")
        } catch (e: IOException) {
            loadDetailFromCacheOrError(deviceId, "Không có kết nối mạng. Đang hiển thị dữ liệu lưu trữ cục bộ.")
        } catch (e: Exception) {
            loadDetailFromCacheOrError(deviceId, e.message ?: "Đã xảy ra lỗi.")
        }
    }

    private fun loadFromCacheOrError(fallbackMessage: String): Resource<List<OwnedDevice>> {
        val cachedJson = userPreferences.getCachedDevices()
        if (!cachedJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<OwnedDevice>>() {}.type
                val cachedList = gson.fromJson<List<OwnedDevice>>(cachedJson, type)
                if (!cachedList.isNullOrEmpty()) {
                    return Resource.Success(cachedList)
                }
            } catch (_: Exception) {}
        }
        return Resource.Error(fallbackMessage)
    }

    private fun loadDetailFromCacheOrError(deviceId: Long, fallbackMessage: String): Resource<DeviceDetailDto> {
        val cachedJson = userPreferences.getCachedDeviceDetail(deviceId)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val cachedDetail = gson.fromJson(cachedJson, DeviceDetailDto::class.java)
                if (cachedDetail != null) {
                    return Resource.Success(cachedDetail)
                }
            } catch (_: Exception) {}
        }
        return Resource.Error(fallbackMessage)
    }
}