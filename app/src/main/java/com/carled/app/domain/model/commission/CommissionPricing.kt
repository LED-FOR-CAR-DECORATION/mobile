package com.carled.app.domain.model.commission

/**
 * Mirrors the response of GET /api/commissions/pricing (permitAll).
 * Field names match the JSON 1:1 — no @SerialName / @Json mapping needed with
 * kotlinx.serialization, Moshi or Gson, whichever this project already uses.
 */

enum class SelectionType { SINGLE_SELECT, MULTI_SELECT, QUANTITY, TEXT_INPUT }
enum class UiType { RADIO, DROPDOWN, CHECKBOX, NUMBER, TEXT }
enum class ModifierType { FLAT_ADD, BASE_PERCENT, SUBTOTAL_PERCENT }

data class PricingOption(
    val optionId: Long,
    val optionCode: String,
    val optionLabel: String,
    val modifierType: ModifierType,
    // FLAT_ADD: a VND amount. BASE_PERCENT / SUBTOTAL_PERCENT: a percent value (10 = 10%).
    val basePrice: Long,
    val isDefault: Boolean = false,
    val displayOrder: Int = 0
)

data class PricingRule(
    val configId: Long,
    val ruleKey: String,
    val configName: String,
    val description: String? = null,
    val selectionType: SelectionType,
    val uiType: UiType,
    val isBasePrice: Boolean = false,
    val minQuantity: Int? = null,
    val maxQuantity: Int? = null,
    val isRequired: Boolean = false,
    val isSystem: Boolean = false,
    val displayOrder: Int = 0,
    val dependsOnOptionId: Long? = null,
    val dependsOnRuleKey: String? = null,
    val dependsOnOptionCode: String? = null,
    val options: List<PricingOption> = emptyList()
) {
    /** No visibility dependency — always shown. */
    val isRootRule: Boolean
        get() = dependsOnOptionId == null && dependsOnRuleKey == null && dependsOnOptionCode == null
}

data class PricingTier(
    val tierId: Long,
    val tierCode: String,
    val tierName: String,
    val description: String? = null,
    val isCustom: Boolean = false,
    val basePrice: Long = 0,
    val discountPercent: Int = 0,
    val maxRevisions: Int = 0,
    val extraRevisionFeePercent: Int = 0,
    val displayOrder: Int = 0,
    // Empty => this tier is always offered, regardless of what's selected elsewhere.
    val triggerOptionIds: List<Long> = emptyList(),
    // Informational only — what this preset bundles, not independently editable here.
    val bundledOptionIds: List<Long> = emptyList(),
    // Informational only — what the equivalent à-la-carte CUSTOM build would cost.
    val customCalculatedPrice: Long? = null,
    val savingsAmount: Long? = null
)

data class CommissionPricingResponse(
    val tiers: List<PricingTier> = emptyList(),
    val rules: List<PricingRule> = emptyList()
) {
    val customTier: PricingTier? get() = tiers.firstOrNull { it.isCustom }
    val presetTiers: List<PricingTier> get() = tiers.filter { !it.isCustom }.sortedBy { it.displayOrder }
}

/** Shape of the 500/4xx error body shown in the API spec. */
data class CommissionApiError(
    val code: String? = null,
    val message: String? = null,
    val status: Int? = null,
    val timestamp: String? = null,
    val traceId: String? = null,
    val details: List<String> = emptyList()
)