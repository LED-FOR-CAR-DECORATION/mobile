package com.carled.app.domain.repository

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.ClaimedDevice
import com.carled.app.domain.model.DeviceClaimPayload

interface DeviceClaimRepository {
    suspend fun claimDevice(payload: DeviceClaimPayload): Resource<ClaimedDevice>
}