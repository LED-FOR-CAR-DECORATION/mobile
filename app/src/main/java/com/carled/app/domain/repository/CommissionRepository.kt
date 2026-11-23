package com.carled.app.domain.repository

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.commission.ApproveDeliverableCardResponse
import com.carled.app.domain.model.commission.CommissionMessageResponse
import com.carled.app.domain.model.commission.CommissionResponse
import com.carled.app.domain.model.commission.CommissionSummaryResponse
import com.carled.app.domain.model.commission.ConfirmRevisionCardResponse
import com.carled.app.domain.model.commission.CreateCommissionMessageRequest
import com.carled.app.domain.model.commission.CreateCommissionRequest
import com.carled.app.domain.model.PaymentSessionResponse

interface CommissionRepository {
    suspend fun submit(request: CreateCommissionRequest): Resource<CommissionResponse>
    suspend fun getCommissionDetail(id: Long): Resource<CommissionResponse>
    suspend fun getMessages(id: Long): Resource<List<CommissionMessageResponse>>
    suspend fun sendMessage(id: Long, request: CreateCommissionMessageRequest): Resource<CommissionMessageResponse>
    suspend fun getMyCommissions(status: String? = null, sortDirection: String = "DESC", page: Int = 0, size: Int = 20): Resource<List<CommissionSummaryResponse>>
    suspend fun getPaymentSession(code: String): Resource<PaymentSessionResponse>
    suspend fun mockPaymentSuccess(code: String): Resource<PaymentSessionResponse>
    suspend fun approveDeliverable(id: Long): Resource<ApproveDeliverableCardResponse>
    suspend fun confirmRevisionCard(id: Long, messageId: Long): Resource<ConfirmRevisionCardResponse>
    suspend fun cancelRevisionCard(id: Long): Resource<Unit>
}