package com.carled.app.presentation.screens.commission

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carled.app.domain.model.commission.CommissionPriceCalculator
import com.carled.app.domain.model.commission.ModifierType
import com.carled.app.domain.model.commission.PricingOption
import com.carled.app.domain.model.commission.PricingRule
import com.carled.app.domain.model.commission.PricingTier
import com.carled.app.domain.model.commission.UiType
import com.carled.app.presentation.components.ChevronGlyph
import com.carled.app.presentation.components.HardCheckbox
import com.carled.app.presentation.components.MirageButton
import com.carled.app.presentation.components.flatSurface
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink
import java.text.NumberFormat
import java.util.Locale

private fun formatVndAmount(amount: Long) =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " VND"

/** One tier card: a fixed-price preset, or the CUSTOM tier priced live from [liveCustomTotal]. */
@Composable
internal fun TierCard(
    tier: PricingTier,
    selected: Boolean,
    liveCustomTotal: Long?,
    eligible: Boolean,
    onSelect: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .flatSurface(color = if (selected) GreenLight else Cream)
            .clickable(enabled = eligible, onClick = onSelect)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tier.tierName, color = Ink, fontWeight = FontWeight.ExtraBold)
            if (selected) ChevronGlyph()
        }
        tier.description?.let { Text(it, color = Ink, style = MaterialTheme.typography.bodySmall) }
        val price = if (tier.isCustom) liveCustomTotal else tier.basePrice
        Text(
            if (price != null) "Price: ${formatVndAmount(price)}" else "Configure options below to see price",
            color = Ink,
            fontWeight = FontWeight.Bold
        )
        if (!tier.isCustom) {
            val revisionsLine = "Includes ${tier.maxRevisions} revision(s)" +
                    if (tier.extraRevisionFeePercent > 0) " · +${tier.extraRevisionFeePercent}% per extra revision" else ""
            Text(revisionsLine, color = Ink, style = MaterialTheme.typography.bodySmall)
            if (tier.discountPercent > 0) {
                Text("Save ${tier.discountPercent}% vs à la carte", color = Ink, style = MaterialTheme.typography.bodySmall)
            }
            if (tier.customCalculatedPrice != null && (tier.savingsAmount ?: 0) > 0) {
                Text(
                    "À la carte would cost ${formatVndAmount(tier.customCalculatedPrice)} · you save ${formatVndAmount(tier.savingsAmount!!)}",
                    color = Ink,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (!eligible) {
            Text("Select the required options below to unlock this package.", color = Ink, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** One dynamic pricing rule, rendered per its uiType (RADIO, DROPDOWN, CHECKBOX, NUMBER, TEXT). */
@Composable
internal fun PricingRuleRow(
    rule: PricingRule,
    selection: CommissionPriceCalculator.RuleSelection?,
    onSelectSingle: (String) -> Unit,
    onToggleMulti: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onTextChange: (String) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().flatSurface(color = Cream).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(rule.configName + if (rule.isRequired) " *" else "", color = Ink, fontWeight = FontWeight.Bold)
        rule.description?.let { Text(it, color = Ink, style = MaterialTheme.typography.bodySmall) }
        when (rule.uiType) {
            UiType.RADIO -> Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rule.options.sortedBy { it.displayOrder }.forEach { option ->
                    val isSelected = option.optionCode in selection?.selectedOptionCodes.orEmpty()
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelectSingle(option.optionCode) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = { onSelectSingle(option.optionCode) })
                        Text(optionLabelWithPrice(option), color = Ink)
                    }
                }
            }
            UiType.DROPDOWN -> {
                var expanded by remember { mutableStateOf(false) }
                val current = rule.options.firstOrNull { it.optionCode in selection?.selectedOptionCodes.orEmpty() }
                Box {
                    MirageButton(current?.let { optionLabelWithPrice(it) } ?: "Choose…", { expanded = true }, secondary = true)
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        rule.options.sortedBy { it.displayOrder }.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(optionLabelWithPrice(option)) },
                                onClick = { onSelectSingle(option.optionCode); expanded = false }
                            )
                        }
                    }
                }
            }
            UiType.CHECKBOX -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rule.options.sortedBy { it.displayOrder }.forEach { option ->
                    val isChecked = option.optionCode in selection?.selectedOptionCodes.orEmpty()
                    HardCheckbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleMulti(option.optionCode) },
                        label = optionLabelWithPrice(option)
                    )
                }
            }
            UiType.NUMBER -> {
                val qty = selection?.quantity ?: rule.minQuantity ?: 0
                val min = rule.minQuantity ?: 0
                val max = rule.maxQuantity ?: Int.MAX_VALUE
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = { if (qty - 1 >= min) onQuantityChange(qty - 1) }, enabled = qty - 1 >= min) {
                        Text("−", color = Ink, fontWeight = FontWeight.Bold)
                    }
                    Text("$qty", color = Ink, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { if (qty + 1 <= max) onQuantityChange(qty + 1) }, enabled = qty + 1 <= max) {
                        Text("+", color = Ink, fontWeight = FontWeight.Bold)
                    }
                }
            }
            UiType.TEXT -> CommissionField(selection?.text.orEmpty(), onTextChange, rule.configName)
        }
    }
}

private fun optionLabelWithPrice(option: PricingOption): String {
    if (option.basePrice == 0L) return option.optionLabel
    val suffix = when (option.modifierType) {
        ModifierType.FLAT_ADD -> " (+${formatVndAmount(option.basePrice)})"
        ModifierType.BASE_PERCENT -> " (+${option.basePrice}% of base)"
        ModifierType.SUBTOTAL_PERCENT -> " (+${option.basePrice}% of subtotal)"
    }
    return option.optionLabel + suffix
}