package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import com.example.ui.theme.PowerFactorGood
import com.example.ui.theme.PowerFactorPenalty
import com.example.ui.theme.PowerFactorWarning
import com.example.util.PowerFactorCalculator
import java.util.Locale

@Composable
fun KvarCalculatorDialog(
    customer: Customer?,
    initialReading: MeterReading?,
    isHindi: Boolean,
    onDismiss: () -> Unit
) {
    // Mode 0: By Load kW & PF, Mode 1: By Current Meter Reading (kWh/kVArh/kVAh) & Target PF
    var calculationMode by remember { mutableStateOf(if (initialReading != null) 1 else 0) }

    // Initial values
    val defaultLoadKw = customer?.sanctionedLoad?.let { PowerFactorCalculator.parseLoadKw(it) } ?: 50.0
    var loadKwInput by remember { mutableStateOf(String.format(Locale.US, "%.1f", defaultLoadKw)) }

    val initialPfValue = initialReading?.powerFactor ?: 0.82
    var currentPf by remember { mutableDoubleStateOf(initialPfValue.coerceIn(0.40, 0.99)) }
    var currentPfText by remember { mutableStateOf(String.format(Locale.US, "%.3f", currentPf)) }

    var targetPf by remember { mutableDoubleStateOf(0.99) } // 0.99 recommended for max incentive
    var targetPfText by remember { mutableStateOf("0.99") }

    // Current Meter Reading mode inputs
    val initialUnits = initialReading?.unitsConsumed?.takeIf { it > 0.0 }
        ?: initialReading?.kwh?.takeIf { it > 0.0 }
        ?: 350.0
    var kwhConsumedInput by remember { mutableStateOf(String.format(Locale.US, "%.1f", initialUnits)) }
    val initialKvarh = initialReading?.kvarh?.takeIf { it > 0.0 } ?: (initialUnits * 0.6)
    var kvarhConsumedInput by remember { mutableStateOf(String.format(Locale.US, "%.1f", initialKvarh)) }
    val initialKvah = initialReading?.kvah?.takeIf { it > 0.0 }
        ?: PowerFactorCalculator.calculateKvahFromKvarh(initialUnits, initialKvarh)
    var kvahConsumedInput by remember { mutableStateOf(String.format(Locale.US, "%.1f", initialKvah)) }
    var operatingHoursInput by remember { mutableStateOf("12") }

    // Live calculation result
    val result by remember {
        derivedStateOf {
            val parsedTarget = targetPfText.toDoubleOrNull()?.coerceIn(0.50, 1.0) ?: targetPf
            if (calculationMode == 0) {
                val kw = loadKwInput.toDoubleOrNull() ?: defaultLoadKw
                val pf1 = currentPfText.toDoubleOrNull()?.coerceIn(0.20, 0.999) ?: currentPf
                PowerFactorCalculator.calculateRequiredKvar(
                    loadKw = kw,
                    initialPf = pf1,
                    targetPf = parsedTarget
                )
            } else {
                val kwh = kwhConsumedInput.toDoubleOrNull() ?: initialUnits
                val kvarh = kvarhConsumedInput.toDoubleOrNull() ?: initialKvarh
                val kvah = kvahConsumedInput.toDoubleOrNull() ?: 0.0
                val hours = operatingHoursInput.toDoubleOrNull() ?: 12.0
                val manualPfOverride = currentPfText.toDoubleOrNull()?.takeIf { it in 0.20..0.999 }
                if (kwh > 0.0 && kvarh > 0.0 && manualPfOverride == null) {
                    PowerFactorCalculator.calculateRequiredKvarFromDailyReading(
                        kwhConsumed = kwh,
                        kvarhConsumed = kvarh,
                        operationalHours = hours,
                        targetPf = parsedTarget
                    )
                } else {
                    PowerFactorCalculator.calculateRequiredKvarFromReadingInput(
                        currentKwh = kwh,
                        previousKwh = 0.0,
                        currentKvarh = kvarh,
                        currentKvah = kvah,
                        currentPfInput = manualPfOverride,
                        targetPf = parsedTarget,
                        sanctionedLoadKw = defaultLoadKw,
                        operatingHours = hours
                    )
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("kvar_calculator_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "kVAr Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "आवश्यक kVAr कॅपेसिटर बँक",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Required kVAr Rating for PF Correction",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = calculationMode == 0,
                        onClick = { calculationMode = 0 },
                        label = { Text("लोड आधारित (kW)", fontSize = 11.5.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kvar_mode_load_chip")
                    )
                    FilterChip(
                        selected = calculationMode == 1,
                        onClick = { calculationMode = 1 },
                        label = { Text("चालू रीडिंग (kWh/kVArh)", fontSize = 11.5.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kvar_mode_reading_chip")
                    )
                }

                // Mode 0: Load in kW
                if (calculationMode == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "कनेक्टेड लोड / वीज भार (kW):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            customer?.sanctionedLoad?.let {
                                if (it.isNotBlank()) {
                                    Text(
                                        text = "मंजूर: $it",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = loadKwInput,
                            onValueChange = { loadKwInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            suffix = { Text("kW") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kvar_load_kw_input")
                        )

                        // Quick Load preset chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(25.0, 50.0, 65.0, 100.0).forEach { kwVal ->
                                OutlinedButton(
                                    onClick = { loadKwInput = kwVal.toString() },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${kwVal.toInt()}k", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Mode 1: Current Meter Reading (kWh, kVArh, kVAh)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = kwhConsumedInput,
                                onValueChange = { newKwh ->
                                    kwhConsumedInput = newKwh
                                    val kwhVal = newKwh.toDoubleOrNull() ?: 0.0
                                    val kvahVal = kvahConsumedInput.toDoubleOrNull() ?: 0.0
                                    val kvarhVal = kvarhConsumedInput.toDoubleOrNull() ?: 0.0
                                    if (kwhVal > 0.0 && kvarhVal > 0.0) {
                                        val estKvah = PowerFactorCalculator.calculateKvahFromKvarh(kwhVal, kvarhVal)
                                        kvahConsumedInput = String.format(Locale.US, "%.1f", estKvah)
                                        val newPf = PowerFactorCalculator.calculatePf(kwhVal, estKvah).coerceIn(0.40, 0.99)
                                        currentPf = newPf
                                        currentPfText = String.format(Locale.US, "%.3f", newPf)
                                    } else if (kwhVal > 0.0 && kvahVal > 0.0) {
                                        val newPf = PowerFactorCalculator.calculatePf(kwhVal, kvahVal).coerceIn(0.40, 0.99)
                                        currentPf = newPf
                                        currentPfText = String.format(Locale.US, "%.3f", newPf)
                                    }
                                },
                                label = { Text("चालू रीडिंग / खपत (kWh)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kvar_reading_kwh_input")
                            )
                            OutlinedTextField(
                                value = kvarhConsumedInput,
                                onValueChange = { newKvarh ->
                                    kvarhConsumedInput = newKvarh
                                    val kwhVal = kwhConsumedInput.toDoubleOrNull() ?: 0.0
                                    val kvarhVal = newKvarh.toDoubleOrNull() ?: 0.0
                                    if (kwhVal > 0.0 && kvarhVal >= 0.0) {
                                        val estKvah = PowerFactorCalculator.calculateKvahFromKvarh(kwhVal, kvarhVal)
                                        kvahConsumedInput = String.format(Locale.US, "%.1f", estKvah)
                                        val newPf = PowerFactorCalculator.calculatePf(kwhVal, estKvah).coerceIn(0.40, 0.99)
                                        currentPf = newPf
                                        currentPfText = String.format(Locale.US, "%.3f", newPf)
                                    }
                                },
                                label = { Text("रीडिंग kVArh") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kvar_reading_kvarh_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = kvahConsumedInput,
                                onValueChange = { newKvah ->
                                    kvahConsumedInput = newKvah
                                    val kwhVal = kwhConsumedInput.toDoubleOrNull() ?: 0.0
                                    val kvahVal = newKvah.toDoubleOrNull() ?: 0.0
                                    if (kwhVal > 0.0 && kvahVal >= kwhVal) {
                                        val newPf = PowerFactorCalculator.calculatePf(kwhVal, kvahVal).coerceIn(0.40, 0.99)
                                        currentPf = newPf
                                        currentPfText = String.format(Locale.US, "%.3f", newPf)
                                    }
                                },
                                label = { Text("रीडिंग kVAh") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kvar_reading_kvah_input")
                            )
                            OutlinedTextField(
                                value = operatingHoursInput,
                                onValueChange = { operatingHoursInput = it },
                                label = { Text("कालावधी (Hours)") },
                                suffix = { Text("तास") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kvar_operating_hours_input")
                            )
                        }
                    }
                }

                // Current Power Factor Slider & Direct Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "सध्याचा पॉवर फॅक्टर (Current PF):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val pfColor = when {
                            result.initialPf >= 0.95 -> PowerFactorGood
                            result.initialPf >= 0.85 -> PowerFactorWarning
                            else -> PowerFactorPenalty
                        }
                        Text(
                            text = String.format(Locale.US, "%.3f", result.initialPf),
                            fontWeight = FontWeight.ExtraBold,
                            color = pfColor,
                            fontSize = 16.sp
                        )
                    }

                    OutlinedTextField(
                        value = currentPfText,
                        onValueChange = { text ->
                            currentPfText = text
                            text.toDoubleOrNull()?.let { parsed ->
                                if (parsed in 0.40..0.99) {
                                    currentPf = parsed
                                }
                            }
                        },
                        label = { Text("Current PF (0.40 - 0.99)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kvar_current_pf_input")
                    )

                    Slider(
                        value = currentPf.toFloat().coerceIn(0.50f, 0.98f),
                        onValueChange = {
                            val rounded = (it * 1000).toInt() / 1000.0
                            currentPf = rounded
                            currentPfText = String.format(Locale.US, "%.3f", rounded)
                        },
                        valueRange = 0.50f..0.98f,
                        modifier = Modifier.testTag("kvar_current_pf_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.50 (खूप कमी)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text("0.85 (सामान्य)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text("0.95 (चांगला)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Target Power Factor Selector & Direct Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "लक्ष्यित पॉवर फॅक्टर (Target PF):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = targetPfText,
                        onValueChange = { text ->
                            targetPfText = text
                            text.toDoubleOrNull()?.let { parsed ->
                                if (parsed in 0.80..1.00) {
                                    targetPf = parsed
                                }
                            }
                        },
                        label = { Text("Target PF (0.85 - 1.00)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kvar_target_pf_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.95, 0.98, 0.99, 1.00).forEach { pfVal ->
                            val isSelected = kotlin.math.abs(targetPf - pfVal) < 0.002
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    targetPf = pfVal
                                    targetPfText = String.format(Locale.US, "%.2f", pfVal)
                                },
                                label = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (pfVal == 1.00) "1.00" else String.format(Locale.US, "%.2f", pfVal),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
                                        if (pfVal == 0.99) {
                                            Text("इन्सेंटिव्ह ★", fontSize = 8.5.sp, color = Color(0xFF137333))
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // HERO RESULT CARD: Required kVAr & Recommended Bank Size
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("kvar_result_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "आवश्यक कॅपेसिटर क्षमता (Required kVAr):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", result.requiredKvar),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("kvar_required_value_text")
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "kVAr",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                            }

                            // Standard commercial size badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = PowerFactorGood.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, PowerFactorGood, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "शिफारस बँक",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PowerFactorGood
                                    )
                                    Text(
                                        text = "${result.recommendedBankSize.toInt()} kVAr",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PowerFactorGood
                                    )
                                }
                            }
                        }

                        // APFC Steps recommendation
                        if (result.recommendedSteps.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "APFC पॅनेल स्टेप्स: ${result.recommendedSteps.joinToString(" + ") { "${it}k" }} kVAr",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Demand & Current Reduction Metrics
                        if (result.kvaReduction > 0.0 || result.currentReductionPercent > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "kVA डिमांड बचत: -${String.format(Locale.US, "%.1f", result.kvaReduction)} kVA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PowerFactorGood
                                )
                                Text(
                                    text = "प्रवाह घट (Current): -${String.format(Locale.US, "%.1f", result.currentReductionPercent)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Multiplier and Excess detail
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "मल्टिप्लायर फॅक्टर K: ${result.multiplierK} (लोड: ${String.format(Locale.US, "%.1f", result.loadKw)} kW)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            result.excessDailyKvarh?.let { excess ->
                                Text(
                                    text = "अतिरिक्त kVArh: ~${excess}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Tariff & Incentive Benefit Note
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "महावितरण (MSEDCL) बिल फायदा / इन्सेंटिव्ह:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = "• PF \u2265 0.95 वर मासिक बिलात 3.5% ते 7% थेट सूट (Incentive) मिळते.\n• ${result.recommendedBankSize.toInt()} kVAr बँक जोडल्यास लो पॉवर फॅक्टर पेनल्टी १००% बंद होईल.",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Formula helper section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "सूत्र (Formula):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "kVAr = kW \u00D7 (tan \u03A6\u2081 - tan \u03A6\u2082)\nजिथे tan \u03A6 = \u221A(1 - PF\u00B2) / PF",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_kvar_dialog_button")
            ) {
                Text("समजले (Close)")
            }
        }
    )
}
