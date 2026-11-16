package com.carled.app.domain.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.remote.dto.DeviceDetailDto
import com.carled.app.domain.model.OwnedDevice

interface DeviceRepository {

    /**
     * Network first, falls back to cache on any failure.
     * Call only when a fresh server round-trip is explicitly needed:
     * after a successful device claim, or when [getOwnedDevicesFromCache] returns
     * nothing (first time this account is used on this device).
     */
    suspend fun getOwnedDevices(): Resource<List<OwnedDevice>>

    /**
     * Cache only — never touches the network.
     * Returns [Resource.Error] when no cache exists yet (null / blank JSON).
     * Returns [Resource.Success] with an empty list when the server previously
     * confirmed the account has no devices.
     */
    suspend fun getOwnedDevicesFromCache(): Resource<List<OwnedDevice>>

    /**
     * Network first, falls back to cache on any failure.
     */
    suspend fun getDeviceDetail(deviceId: Long): Resource<DeviceDetailDto>

    /**
     * Cache only — never touches the network.
     * Returns [Resource.Error] when no detail has been cached for this id yet.
     */
    suspend fun getDeviceDetailFromCache(deviceId: Long): Resource<DeviceDetailDto>
}