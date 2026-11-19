package com.carled.app.domain.model.commission

/**
 * Deterministic, reusable price calculator for the CUSTOM configurator.
 *
 * ASSUMPTION (the exact arithmetic isn't spelled out beyond the enum names
 * themselves — AC02 only says the three modifier types exist and that
 * isBasePrice "marks rules contributing to the base bucket for BASE_PERCENT
 * calculation"). This is the one reading consistent with that description:
 *
 *  1. baseBucket = sum of selected-option contributions from rules where
 *     isBasePrice = true (these are themselves FLAT_ADD options, e.g. MATRIX_SIZE).
 *  2. flatTotal  = sum of every selected FLAT_ADD option's contribution, across
 *     ALL rules (already includes the base-bucket rules — not double counted).
 *  3. basePercentTotal = sum over selected BASE_PERCENT options of
 *     (option.basePrice / 100) * baseBucket.
 *  4. runningSubtotal  = flatTotal + basePercentTotal.
 *  5. subtotalPercentTotal = sum over selected SUBTOTAL_PERCENT options of
 *     (option.basePrice / 100) * runningSubtotal.
 *  6. total = runningSubtotal + subtotalPercentTotal.
 *
 * This fixed 3-pass order is what makes the total independent of each rule's
 * own displayOrder — only modifierType decides when an option is applied.
 * QUANTITY rules multiply their single option's contribution by the chosen
 * quantity before it enters whichever pass its modifierType belongs to.
 * TEXT_INPUT rules never affect price.
 *
 * If the backend's real formula differs, this is the one place to change —
 * CommissionViewModel and the form only ever call [calculate] / [visibleRulesInOrder].
 */
object CommissionPriceCalculator {

    data class RuleSelection(
        val selectedOptionCodes: Set<String> = emptySet(), // SINGLE_SELECT: 0 or 1. MULTI_SELECT: 0..n.
        val quantity: Int? = null,                          // QUANTITY rules only.
        val text: String? = null                            // TEXT_INPUT rules only.
    )

    data class LineItem(
        val ruleKey: String,
        val configName: String,
        val optionLabel: String,
        val modifierType: ModifierType,
        val quantity: Int,
        val amount: Long
    )

    data class Result(
        val total: Long,
        val baseBucket: Long,
        val lineItems: List<LineItem>,
        val missingRequiredRuleKeys: List<String>
    )

    /** True if [rule]'s single visibility dependency (if any) is currently satisfied. */
    private fun dependencySatisfied(
        rule: PricingRule,
        rulesByKey: Map<String, PricingRule>,
        selections: Map<String, RuleSelection>
    ): Boolean {
        if (rule.isRootRule) return true
        val parentKey = rule.dependsOnRuleKey ?: return true
        val parentRule = rulesByKey[parentKey] ?: return true
        val requiredOption = parentRule.options.firstOrNull {
            (rule.dependsOnOptionId != null && it.optionId == rule.dependsOnOptionId) ||
                    (rule.dependsOnOptionCode != null && it.optionCode == rule.dependsOnOptionCode)
        } ?: return true
        val parentSelection = selections[parentKey] ?: return false
        return requiredOption.optionCode in parentSelection.selectedOptionCodes
    }

    /**
     * Rules currently visible, in display order, walking the dependency chain
     * (AC02 caps this at depth 5 server-side; mirrored here as a safety bound).
     */
    fun visibleRulesInOrder(
        allRules: List<PricingRule>,
        selections: Map<String, RuleSelection>
    ): List<PricingRule> {
        val rulesByKey = allRules.associateBy { it.ruleKey }
        return allRules.sortedBy { it.displayOrder }.filter { rule ->
            var current: PricingRule? = rule
            var depth = 0
            var visible = true
            while (current != null && !current.isRootRule && depth < 5) {
                if (!dependencySatisfied(current, rulesByKey, selections)) {
                    visible = false
                    break
                }
                current = current.dependsOnRuleKey?.let { rulesByKey[it] }
                depth++
            }
            visible
        }
    }

    private fun contributionsFor(
        rule: PricingRule,
        selection: RuleSelection?
    ): List<Triple<PricingOption, Int, ModifierType>> = when (rule.selectionType) {
        SelectionType.SINGLE_SELECT -> {
            val code = selection?.selectedOptionCodes?.firstOrNull()
            val option = rule.options.firstOrNull { it.optionCode == code }
            if (option == null) emptyList() else listOf(Triple(option, 1, option.modifierType))
        }
        SelectionType.MULTI_SELECT -> {
            val codes = selection?.selectedOptionCodes.orEmpty()
            rule.options.filter { it.optionCode in codes }.map { Triple(it, 1, it.modifierType) }
        }
        SelectionType.QUANTITY -> {
            val qty = selection?.quantity ?: 0
            val option = rule.options.firstOrNull()
            if (option == null || qty <= 0) emptyList() else listOf(Triple(option, qty, option.modifierType))
        }
        SelectionType.TEXT_INPUT -> emptyList()
    }

    private fun isSatisfied(rule: PricingRule, selection: RuleSelection?): Boolean = when (rule.selectionType) {
        SelectionType.SINGLE_SELECT -> selection?.selectedOptionCodes?.isNotEmpty() == true
        SelectionType.MULTI_SELECT -> selection?.selectedOptionCodes?.isNotEmpty() == true
        SelectionType.QUANTITY -> (selection?.quantity ?: 0) > 0
        SelectionType.TEXT_INPUT -> !selection?.text.isNullOrBlank()
    }

    fun calculate(rules: List<PricingRule>, selections: Map<String, RuleSelection>): Result {
        val visibleRules = visibleRulesInOrder(rules, selections)
        val lineItems = mutableListOf<LineItem>()
        val missing = visibleRules.filter { it.isRequired && !isSatisfied(it, selections[it.ruleKey]) }.map { it.ruleKey }

        val contribsByRule = visibleRules.associateWith { contributionsFor(it, selections[it.ruleKey]) }

        // Pass 1 — base bucket + overall flat total.
        var baseBucket = 0L
        var flatTotal = 0L
        for (rule in visibleRules) {
            contribsByRule.getValue(rule).filter { it.third == ModifierType.FLAT_ADD }.forEach { (option, qty, _) ->
                val amount = option.basePrice * qty
                flatTotal += amount
                if (rule.isBasePrice) baseBucket += amount
                lineItems += LineItem(rule.ruleKey, rule.configName, option.optionLabel, ModifierType.FLAT_ADD, qty, amount)
            }
        }

        // Pass 2 — BASE_PERCENT, off the frozen base bucket.
        var basePercentTotal = 0L
        for (rule in visibleRules) {
            contribsByRule.getValue(rule).filter { it.third == ModifierType.BASE_PERCENT }.forEach { (option, qty, _) ->
                val amount = (baseBucket * option.basePrice * qty) / 100
                basePercentTotal += amount
                lineItems += LineItem(rule.ruleKey, rule.configName, option.optionLabel, ModifierType.BASE_PERCENT, qty, amount)
            }
        }

        val runningSubtotal = flatTotal + basePercentTotal

        // Pass 3 — SUBTOTAL_PERCENT, off the running subtotal from passes 1-2.
        var subtotalPercentTotal = 0L
        for (rule in visibleRules) {
            contribsByRule.getValue(rule).filter { it.third == ModifierType.SUBTOTAL_PERCENT }.forEach { (option, qty, _) ->
                val amount = (runningSubtotal * option.basePrice * qty) / 100
                subtotalPercentTotal += amount
                lineItems += LineItem(rule.ruleKey, rule.configName, option.optionLabel, ModifierType.SUBTOTAL_PERCENT, qty, amount)
            }
        }

        return Result(
            total = runningSubtotal + subtotalPercentTotal,
            baseBucket = baseBucket,
            lineItems = lineItems,
            missingRequiredRuleKeys = missing
        )
    }
}