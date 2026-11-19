package com.example.ui.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import com.example.util.PowerFactorCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReadingDialog(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    previousReading: MeterReading?,
    readingToEdit: MeterReading?,
    isHindi: Boolean,
    onSelectCustomer: (Customer) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        date: String,
        time: String,
        timestamp: Long,
        kwh: Double,
        kvarh: Double,
        kvah: Double,
        pf: Double,
        isAutoPf: Boolean,
        remarks: String
    ) -> Unit
) {
    val now = remember { Date() }
    val defaultDate = remember { SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(now) }
    val defaultTime = remember { SimpleDateFormat("hh:mm a", Locale.US).format(now) }

    var isCustomerExpanded by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf(readingToEdit?.readingDate ?: defaultDate) }
    var timeText by remember { mutableStateOf(readingToEdit?.readingTime ?: defaultTime) }
    var kwhText by remember { mutableStateOf(readingToEdit?.kwh?.toString() ?: "") }
    var kvarhText by remember { mutableStateOf(readingToEdit?.kvarh?.toString() ?: "") }
    var kvahText by remember { mutableStateOf(readingToEdit?.kvah?.toString() ?: "") }
    var manualPfText by remember { mutableStateOf(readingToEdit?.powerFactor?.toString() ?: "0.95") }
    var isAutoPf by remember { mutableStateOf(readingToEdit?.isPfAutoCalculated ?: true) }
    var remarksText by remember { mutableStateOf(readingToEdit?.remarks ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Derived values
    val currentKwh = kwhText.toDoubleOrNull() ?: 0.0
    val currentKvarh = kvarhText.toDoubleOrNull() ?: 0.0
    val currentKvah = kvahText.toDoubleOrNull() ?: 0.0

    // Auto-compute PF if enabled
    val calculatedPf = if (isAutoPf) {
        if (currentKvah > 0.0 && currentKwh > 0.0) {
            PowerFactorCalculator.calculatePf(currentKwh, currentKvah)
        } else if (currentKwh > 0.0 && currentKvarh > 0.0) {
            val estimatedKvah = PowerFactorCalculator.calculateKvahFromKvarh(currentKwh, currentKvarh)
            PowerFactorCalculator.calculatePf(currentKwh, estimatedKvah)
        } else {
            0.95
        }
    } else {
        manualPfText.toDoubleOrNull() ?: 0.95
    }

    val prevKwh = if (readingToEdit != null && selectedCustomer?.id == readingToEdit.customerId) {
        readingToEdit.previousKwh
    } else {
        previousReading?.kwh
    }
    val consumedUnits = if (currentKwh > 0 && prevKwh != null && currentKwh >= prevKwh) {
        currentKwh - prevKwh
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_reading_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ElectricMeter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (readingToEdit != null) {
                        if (isHindi) "मीटर रीडिंग अपडेट करें" else "Edit Meter Reading"
                    } else {
                        if (isHindi) "दैनिक मीटर रीडिंग दर्ज करें" else "Add Daily Meter Reading"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Customer selector dropdown
                ExposedDropdownMenuBox(
                    expanded = isCustomerExpanded,
                    onExpandedChange = { isCustomerExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: if (isHindi) "ग्राहक निवडा" else "Select Customer",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text(if (isHindi) "ग्राहक निवडा (Customer) *" else "Customer *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dialog_customer_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = isCustomerExpanded,
                        onDismissRequest = { isCustomerExpanded = false }
                    ) {
                        customers.forEach { cust ->
                            val isSelected = selectedCustomer?.id == cust.id
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${cust.name} (${cust.meterNumber})",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Business, contentDescription = null)
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null,
                                onClick = {
                                    onSelectCustomer(cust)
                                    isCustomerExpanded = false
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }

                // Previous reading reference banner
                if (prevKwh != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "पिछली रीडिंग (Previous):" else "Previous kWh:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f kWh", prevKwh),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Date & Time row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text(if (isHindi) "दिनांक (Date)" else "Date") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reading_date_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text(if (isHindi) "समय (Time)" else "Time") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reading_time_input"),
                        singleLine = true
                    )
                }

                // kWh (Active Energy) input
                OutlinedTextField(
                    value = kwhText,
                    onValueChange = {
                        kwhText = it
                        errorMessage = null
                    },
                    label = { Text(if (isHindi) "सक्रिय ऊर्जा (kWh Active Energy) *" else "Active Energy (kWh) *") },
                    placeholder = { Text("उदा. 12850.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reading_kwh_input"),
                    singleLine = true
                )

                // Consumed Units Preview
                if (consumedUnits != null) {
                    Text(
                        text = if (isHindi) "खपत यूनिट (Consumption): +${String.format(Locale.US, "%.1f", consumedUnits)} kWh"
                               else "Net Consumed: +${String.format(Locale.US, "%.1f", consumedUnits)} kWh",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // kVArh (Reactive Energy) input
                OutlinedTextField(
                    value = kvarhText,
                    onValueChange = { kvarhText = it },
                    label = { Text(if (isHindi) "रिएक्टिव ऊर्जा (kVArh Reactive) *" else "Reactive Energy (kVArh) *") },
                    placeholder = { Text("उदा. 3580.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reading_kvarh_input"),
                    singleLine = true
                )

                // kVAh (Apparent Energy) input
                OutlinedTextField(
                    value = kvahText,
                    onValueChange = { kvahText = it },
                    label = { Text(if (isHindi) "अपैरेंट ऊर्जा (kVAh Apparent) *" else "Apparent Energy (kVAh) *") },
                    placeholder = { Text("उदा. 13350.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reading_kvah_input"),
                    singleLine = true
                )

                // Power Factor Section (Auto vs Manual)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "पावर फैक्टर (PF) ऑटो-कैलकुलेट" else "Auto-Calculate PF",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isAutoPf) "Formula: PF = kWh / kVAh" else "Manual Meter Entry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAutoPf,
                        onCheckedChange = { isAutoPf = it },
                        modifier = Modifier.testTag("auto_pf_switch")
                    )
                }

                if (isAutoPf) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "कैलकुलेटेड पावर फैक्टर:" else "Calculated Power Factor:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val rating = PowerFactorCalculator.getPfRating(calculatedPf)
                            Text(
                                text = String.format(Locale.US, "%.3f (%s)", calculatedPf, if (isHindi) rating.labelHi else rating.labelEn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = rating.color
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = manualPfText,
                        onValueChange = { manualPfText = it },
                        label = { Text(if (isHindi) "पावर फैक्टर (0.00 - 1.00)" else "Power Factor (0.00 - 1.00)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_pf_input"),
                        singleLine = true
                    )
                }

                // Remarks input
                OutlinedTextField(
                    value = remarksText,
                    onValueChange = { remarksText = it },
                    label = { Text(if (isHindi) "टिप्पणी (Remarks, e.g. Normal, DG Set)" else "Remarks (Optional)") },
                    placeholder = { Text("Normal / Peak Load / Substation OK") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reading_remarks_input"),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCustomer == null) {
                        errorMessage = if (isHindi) "कृपया प्रथम ग्राहक निवडा" else "Please select a customer"
                        return@Button
                    }

                    val kwhVal = kwhText.toDoubleOrNull()
                    val kvarhVal = kvarhText.toDoubleOrNull() ?: 0.0
                    val kvahVal = kvahText.toDoubleOrNull() ?: (if (kwhVal != null && kvarhVal > 0) PowerFactorCalculator.calculateKvahFromKvarh(kwhVal, kvarhVal) else 0.0)

                    if (kwhVal == null || kwhVal <= 0.0) {
                        errorMessage = if (isHindi) "कृपया सही kWh रिडिंग दर्ज करें" else "Please enter valid kWh reading"
                        return@Button
                    }

                    val finalPf = if (isAutoPf) {
                        if (kvahVal > 0.0) PowerFactorCalculator.calculatePf(kwhVal, kvahVal) else 0.95
                    } else {
                        (manualPfText.toDoubleOrNull() ?: 0.95).coerceIn(0.0, 1.0)
                    }

                    onSave(
                        dateText.trim(),
                        timeText.trim(),
                        System.currentTimeMillis(),
                        kwhVal,
                        kvarhVal,
                        kvahVal,
                        finalPf,
                        isAutoPf,
                        remarksText.trim()
                    )
                },
                modifier = Modifier.testTag("save_reading_button")
            ) {
                Text(if (isHindi) "सेव करें (Save)" else "Save Reading")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_reading_button")
            ) {
                Text(if (isHindi) "रद्द करें" else "Cancel")
            }
        }
    )
}
