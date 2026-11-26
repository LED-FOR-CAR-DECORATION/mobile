package com.carled.app.domain.model.commission


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

        var basePercentTotal = 0L
        for (rule in visibleRules) {
            contribsByRule.getValue(rule).filter { it.third == ModifierType.BASE_PERCENT }.forEach { (option, qty, _) ->
                val amount = (baseBucket * option.basePrice * qty) / 100
                basePercentTotal += amount
                lineItems += LineItem(rule.ruleKey, rule.configName, option.optionLabel, ModifierType.BASE_PERCENT, qty, amount)
            }
        }

        val runningSubtotal = flatTotal + basePercentTotal

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