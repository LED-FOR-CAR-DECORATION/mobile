package com.carled.app.domain.model

import com.carled.app.core.common.Resource
import com.carled.app.domain.repository.SubscriptionRepository
import java.util.concurrent.atomic.AtomicReference

/**
 * BASIC/EXPANDED are kept as named constants so existing
 * `== TextProfilePlan.BASIC` / `== TextProfilePlan.EXPANDED` checks (e.g. in
 * TextProfilesPanel.kt) keep compiling and behaving the same. Unlike the old
 * enum, this can also represent any other limit the backend reports (e.g. a
 * 5-profile mid tier) via [fromServerLimit] — `maxMobileProfiles` on
 * CustomerActiveSubscriptionResponse is a free integer, not one of two fixed
 * values, so the client type shouldn't be either.
 */
data class TextProfilePlan(val limit: Int, val title: String, val code: String? = null) {
    companion object {
        val BASIC = TextProfilePlan(3, "Mặc định", "BASIC")
        val EXPANDED = TextProfilePlan(10, "Mở rộng", "EXPANDED")

        /** Builds a plan from a server-reported `maxMobileProfiles` value + plan name. */
        fun fromServerLimit(limit: Int, planName: String? = null): TextProfilePlan = when (limit) {
            BASIC.limit -> BASIC
            EXPANDED.limit -> EXPANDED
            else -> TextProfilePlan(limit, planName ?: "Gói $limit profile")
        }
    }
}

data class TextProfile(
    val id: String, val name: String, val text: String, val size: Int,
    val speed: Int, val mode: String, val r: Int, val g: Int, val b: Int,
    val rainbow: Boolean = false,
    /** Nhấp nháy interval, 1..100 (higher = faster). Independent of [speed], which only drives scroll_left. */
    val blinkSpeed: Int = 50
) {
    fun validate() {
        require(id.isNotBlank() && name.isNotBlank() && name.length <= 40) { "Tên profile cần từ 1 đến 40 ký tự." }
        require(text.isNotBlank() && text.all { it.code in 32..126 }) { "Nội dung cần là ký tự tiếng Anh không dấu." }
        require(size in MarqueeLayout.SCALE_RANGE && speed in 1..100 && blinkSpeed in 1..100 && listOf(r,g,b).all { it in 0..255 }) { "Thông số chữ không hợp lệ." }
        require(mode in listOf("scroll_left", "static", "blink")) { "Hiệu ứng không hợp lệ." }
        require(mode == "scroll_left" || MarqueeLayout.fit(text,size).fits) { "Nội dung quá dài. Rút ngắn hoặc chọn Cuộn trái." }
    }
}

interface TextProfileRepository {
    fun plan(): TextProfilePlan
    fun list(): List<TextProfile>
    fun save(profile: TextProfile)
    fun delete(id: String)
}

fun interface TextProfilePlanProvider { fun currentPlan(): TextProfilePlan }

/** Default until a real entitlement has been fetched at least once (e.g. logged-out state, or before the first refresh()). */
object PendingTextProfilePlans : TextProfilePlanProvider {
    override fun currentPlan() = TextProfilePlan.BASIC
}

/**
 * Resolves the text-profile slot limit from the customer's active subscription
 * entitlements (`maxMobileProfiles` from GET /api/customer/subscriptions/active/all),
 * falling back to BASIC until the first successful [refresh] and keeping the
 * last known-good value on any later failure — a transient network error
 * should never silently shrink a paying user's limit.
 *
 * [currentPlan] must stay synchronous because TextProfileStore calls it from
 * inside a `synchronized` block, so the network call lives in [refresh]
 * instead. Call refresh() once after login / app start, and again whenever
 * entitlements might have changed (e.g. right after a subscription purchase
 * completes).
 */
class ServerTextProfilePlanProvider(
    private val subscriptionRepository: SubscriptionRepository
) : TextProfilePlanProvider {

    private val cached = AtomicReference(TextProfilePlan.BASIC)

    override fun currentPlan(): TextProfilePlan = cached.get()

    /** Drops back to BASIC — call on logout so the next signed-in user doesn't inherit a stale cached entitlement. */
    fun reset() { cached.set(TextProfilePlan.BASIC) }

    suspend fun refresh() {
        when (val result = subscriptionRepository.getActiveSubscriptions()) {
            is Resource.Success -> {
                val best = result.data.maxByOrNull { it.maxMobileProfiles }
                cached.set(
                    best?.let { TextProfilePlan.fromServerLimit(it.maxMobileProfiles, it.planName) }
                        ?: TextProfilePlan.BASIC
                )
            }
            is Resource.Error -> Unit // keep last known-good plan
            else -> Unit // forward-compatible with any other Resource case (e.g. Loading)
        }
    }
}