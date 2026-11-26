package com.carled.app.domain.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.remote.dto.CustomerActiveSubscriptionResponse

interface SubscriptionRepository {
    /**
     * GET /api/customer/subscriptions/active/all — every active, non-expired
     * entitlement the customer currently holds (e.g. an All-Access VIP Pass
     * stacked with a separate mobile-profile expansion pass). Use this, not
     * [getActiveSubscription], to compute the text-profile slot limit: take
     * the highest `maxMobileProfiles` across the list.
     */
    suspend fun getActiveSubscriptions(): Resource<List<CustomerActiveSubscriptionResponse>>

    /** GET /api/customer/subscriptions/active — a single "primary" active subscription, or null if none is active. */
    suspend fun getActiveSubscription(): Resource<CustomerActiveSubscriptionResponse?>
}