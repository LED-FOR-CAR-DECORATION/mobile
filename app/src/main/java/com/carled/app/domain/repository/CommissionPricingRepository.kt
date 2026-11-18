package com.carled.app.domain.repository

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.CommissionPricingResponse

interface CommissionPricingRepository {
    suspend fun getPricing(): Resource<CommissionPricingResponse>
}