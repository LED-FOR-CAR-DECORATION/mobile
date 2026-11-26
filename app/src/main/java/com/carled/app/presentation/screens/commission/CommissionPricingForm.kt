package com.carled.app.presentation.screens.commission

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carled.app.core.common.Resource
import com.carled.app.domain.model.User
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

@Composable
internal fun CommissionForm(
    user: User,
    model: CommissionViewModel,
    onSubmit: (Long, String?) -> Unit
) {
    val draft = model.draft
    val pricingState by model.pricing.collectAsState()
    val submissionState by model.submission.collectAsState()
    var editingBrief by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = editingBrief) { editingBrief = false }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) model.addReference(uri, MAX_REFERENCES)
    }

    when (val state = pricingState) {
        is Resource.Loading -> CommissionPanel("Form đăng ký request commission.") {
            CircularProgressIndicator(color = Ink)
            Text("Đang tải bảng giá…", color = Ink)
        }
        is Resource.Error -> CommissionPanel("Form đăng ký request commission.") {
            Text(state.message, color = Ink)
            MirageButton("Thử lại", model::loadPricing)
        }
        is Resource.Success -> {
            val catalog = state.data
            val selectedTier = catalog.tiers.firstOrNull { it.tierCode == draft.selectedTierCode }
            val visibleRules = remember(catalog.rules, draft.selections) {
                CommissionPriceCalculator.visibleRulesInOrder(catalog.rules, draft.selections)
            }
            val priceResult = remember(catalog.rules, draft.selections) {
                CommissionPriceCalculator.calculate(catalog.rules, draft.selections)
            }
            val selectedOptionIds = remember(catalog.rules, draft.selections) {
                catalog.rules.flatMap { rule ->
                    rule.options.filter { it.optionCode in draft.selections[rule.ruleKey]?.selectedOptionCodes.orEmpty() }
                }.map { it.optionId }.toSet()
            }

            if (!editingBrief) {
                CommissionPanel("Dịch vụ thiết kế chuyên nghiệp") {
                    Text("Form tạo request thiết kế commission", color = Ink, style = MaterialTheme.typography.labelLarge)
                    visibleRules.forEach { rule ->
                        PricingRuleRow(
                            rule = rule,
                            selection = draft.selections[rule.ruleKey],
                            onSelectSingle = { code -> model.selectSingle(rule.ruleKey, code) },
                            onToggleMulti = { code -> model.toggleMulti(rule.ruleKey, code) },
                            onQuantityChange = { qty -> model.setQuantity(rule.ruleKey, qty) },
                            onTextChange = { text -> model.setText(rule.ruleKey, text) }
                        )
                    }
                    Text("Chọn gói dịch vụ", color = Ink, style = MaterialTheme.typography.labelLarge)
                    catalog.tiers.sortedBy { it.displayOrder }.forEach { tier ->
                        val eligible = tier.triggerOptionIds.isEmpty() || tier.triggerOptionIds.any { it in selectedOptionIds }
                        TierCard(
                            tier = tier,
                            selected = draft.selectedTierCode == tier.tierCode,
                            liveCustomTotal = if (tier.isCustom) priceResult.total else null,
                            eligible = eligible,
                            onSelect = { model.selectTier(tier.tierCode) }
                        )
                    }
                    val canContinue = selectedTier != null &&
                            (!selectedTier.isCustom || priceResult.missingRequiredRuleKeys.isEmpty())
                    MirageButton(
                        "Tiếp tục đến mô tả chi tiết",
                        { editingBrief = true },
                        Modifier.fillMaxWidth(),
                        enabled = canContinue
                    )
                }
            } else {
                val priceVnd = if (selectedTier?.isCustom == true) priceResult.total else selectedTier?.basePrice
                CommissionPanel("Design brief") {
                    Text(
                        "${selectedTier?.tierName.orEmpty()} · ${priceVnd?.let { formatVnd(it.toDouble()) }.orEmpty()}",
                        color = Ink
                    )
                    TextButton(onClick = { editingBrief = false }) { Text("Change design", color = Ink) }
                    CommissionField(user.fullName?.takeIf { it.isNotBlank() } ?: user.id, {}, "Username (user ID)", readOnly = true)
                    CommissionField(draft.title, model::updateTitle, "Title")
                    CommissionField(draft.description, model::updateDescription, "Description", multiline = true)

                    Text("Reference images (optional, up to $MAX_REFERENCES)", style = MaterialTheme.typography.labelLarge, color = Ink)
                    ReferenceImageGrid(
                        references = draft.references,
                        onAddClick = { picker.launch(ImagePickerRequest) },
                        onRemoveClick = model::removeReference
                    )

                    MirageButton(
                        when (submissionState) {
                            is Resource.Loading -> "Đang gửi…"
                            else -> "Submit request"
                        },
                        model::submit,
                        Modifier.fillMaxWidth(),
                        enabled = draft.isBriefValid && submissionState !is Resource.Loading,
                        secondary = true
                    )
                    (submissionState as? Resource.Error)?.let { err ->
                        Text(err.message, color = MaterialTheme.colorScheme.error)
                    }
                    LaunchedEffect(submissionState) {
                        val success = submissionState as? Resource.Success ?: return@LaunchedEffect
                        editingBrief = false
                        onSubmit(success.data.commissionId, success.data.activePaymentCode)
                        model.consumeSubmission()
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceImageGrid(
    references: List<Uri>,
    onAddClick: () -> Unit,
    onRemoveClick: (Uri) -> Unit
) {
    val slots = references.map<Uri, Uri?> { it } + List(minOf(3, MAX_REFERENCES - references.size)) { null }
    slots.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { uri ->
                val slotModifier = Modifier
                    .weight(1f)
                    .aspectRatio(ImagePreviewAspectRatio)
                    .background(Cream)
                    .border(1.dp, Ink)
                if (uri == null) {
                    Box(
                        slotModifier.clickable(role = Role.Button, onClick = onAddClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NoteAdd, "Add reference image", tint = Ink, modifier = Modifier.size(34.dp))
                    }
                } else {
                    Box(slotModifier) {
                        LocalImagePreview(uri, "Reference image", Modifier.fillMaxSize())
                        TextButton(
                            onClick = { onRemoveClick(uri) },
                            modifier = Modifier.align(Alignment.TopEnd).size(44.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("×", color = Ink, modifier = Modifier.background(Cream))
                        }
                    }
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

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
        Modifier
            .fillMaxWidth()
            .flatSurface(color = Cream)
            .padding(12.dp),
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