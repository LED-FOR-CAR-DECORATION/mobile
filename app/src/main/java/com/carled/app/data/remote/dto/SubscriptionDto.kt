package com.carled.app.data.remote.dto

/**
 * Mirrors the OpenAPI `CustomerActiveSubscriptionResponse` schema —
 * "Customer active subscription plan entitlement details" — returned by both
 * GET /api/customer/subscriptions/active and GET /api/customer/subscriptions/active/all.
 *
 * [maxMobileProfiles] is the field the marquee text-profile limit should be
 * driven from (see TextProfile.kt's ServerTextProfilePlanProvider).
 */
data class CustomerActiveSubscriptionResponse(
    val subscriptionId: Long,
    val productId: Long? = null,
    val orderId: Long? = null,
    val planCode: String,
    val planName: String,
    val isLifetime: Boolean,
    val durationDays: Int? = null,
    val allAccessCharacters: Boolean = false,
    val maxCommissionRevisions: Int = 0,
    val remainingCommissionRevisions: Int = 0,
    /** Mobile app benefit: max saved text-profile slots this entitlement grants. */
    val maxMobileProfiles: Int = 0,
    val activatedAt: String? = null,
    val expiresAt: String? = null
)