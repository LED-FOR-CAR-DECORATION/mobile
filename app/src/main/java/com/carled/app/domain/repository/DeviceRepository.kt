package com.carled.app.domain.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.remote.dto.DeviceDetailDto
import com.carled.app.domain.model.OwnedDevice

interface DeviceRepository {

    suspend fun getOwnedDevices(): Resource<List<OwnedDevice>>

    suspend fun getOwnedDevicesFromCache(): Resource<List<OwnedDevice>>

    suspend fun getDeviceDetail(deviceId: Long): Resource<DeviceDetailDto>

    suspend fun getDeviceDetailFromCache(deviceId: Long): Resource<DeviceDetailDto>


    suspend fun isMacOwned(macAddress: String): Boolean


    suspend fun cacheOwnedDevice(device: OwnedDevice)
}