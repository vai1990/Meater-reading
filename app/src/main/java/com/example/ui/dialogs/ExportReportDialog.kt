package com.example.ui.dialogs

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import com.example.util.PowerFactorCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExportDatePreset(val labelEn: String, val labelHi: String) {
    ALL("All Time", "सभी"),
    TODAY("Today", "आज"),
    YESTERDAY("Yesterday", "कल"),
    LAST_7_DAYS("Last 7 Days", "7 दिन"),
    THIS_MONTH("This Month", "इस माह"),
    LAST_MONTH("Last Month", "पिछला माह"),
    CUSTOM("Custom Dates", "कस्टम तारीख")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportReportDialog(
    customer: Customer?,
    allCustomerReadings: List<MeterReading>,
    isHindi: Boolean,
    onExportPdf: (List<MeterReading>, String) -> Unit,
    onExportExcel: (List<MeterReading>, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd-MMM-yyyy", Locale.US) }
    val displayCal = remember { Calendar.getInstance() }

    // Initial default: This Month
    var selectedPreset by remember { mutableStateOf(ExportDatePreset.THIS_MONTH) }

    // Start date (1st of this month)
    val startOfMonthCal = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    var fromDateMillis by remember { mutableStateOf(startOfMonthCal.timeInMillis) }
    var toDateMillis by remember {
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        mutableStateOf(endCal.timeInMillis)
    }

    // Function to update millisecond bounds according to preset
    fun applyPreset(preset: ExportDatePreset) {
        selectedPreset = preset
        val nowCal = Calendar.getInstance()
        when (preset) {
            ExportDatePreset.ALL -> {
                fromDateMillis = 0L
                nowCal.set(Calendar.HOUR_OF_DAY, 23)
                nowCal.set(Calendar.MINUTE, 59)
                nowCal.set(Calendar.SECOND, 59)
                toDateMillis = nowCal.timeInMillis
            }
            ExportDatePreset.TODAY -> {
                nowCal.set(Calendar.HOUR_OF_DAY, 0)
                nowCal.set(Calendar.MINUTE, 0)
                nowCal.set(Calendar.SECOND, 0)
                fromDateMillis = nowCal.timeInMillis
                nowCal.set(Calendar.HOUR_OF_DAY, 23)
                nowCal.set(Calendar.MINUTE, 59)
                nowCal.set(Calendar.SECOND, 59)
                toDateMillis = nowCal.timeInMillis
            }
            ExportDatePreset.YESTERDAY -> {
                nowCal.add(Calendar.DAY_OF_YEAR, -1)
                nowCal.set(Calendar.HOUR_OF_DAY, 0)
                nowCal.set(Calendar.MINUTE, 0)
                nowCal.set(Calendar.SECOND, 0)
                fromDateMillis = nowCal.timeInMillis
                nowCal.set(Calendar.HOUR_OF_DAY, 23)
                nowCal.set(Calendar.MINUTE, 59)
                nowCal.set(Calendar.SECOND, 59)
                toDateMillis = nowCal.timeInMillis
            }
            ExportDatePreset.LAST_7_DAYS -> {
                nowCal.set(Calendar.HOUR_OF_DAY, 23)
                nowCal.set(Calendar.MINUTE, 59)
                nowCal.set(Calendar.SECOND, 59)
                toDateMillis = nowCal.timeInMillis
                val past7 = Calendar.getInstance()
                past7.add(Calendar.DAY_OF_YEAR, -6)
                past7.set(Calendar.HOUR_OF_DAY, 0)
                past7.set(Calendar.MINUTE, 0)
                past7.set(Calendar.SECOND, 0)
                fromDateMillis = past7.timeInMillis
            }
            ExportDatePreset.THIS_MONTH -> {
                val startCal = Calendar.getInstance()
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                fromDateMillis = startCal.timeInMillis
                val endCal = Calendar.getInstance()
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                toDateMillis = endCal.timeInMillis
            }
            ExportDatePreset.LAST_MONTH -> {
                val lastMonthStart = Calendar.getInstance()
                lastMonthStart.add(Calendar.MONTH, -1)
                lastMonthStart.set(Calendar.DAY_OF_MONTH, 1)
                lastMonthStart.set(Calendar.HOUR_OF_DAY, 0)
                lastMonthStart.set(Calendar.MINUTE, 0)
                lastMonthStart.set(Calendar.SECOND, 0)
                fromDateMillis = lastMonthStart.timeInMillis

                val lastMonthEnd = Calendar.getInstance()
                lastMonthEnd.set(Calendar.DAY_OF_MONTH, 1)
                lastMonthEnd.add(Calendar.DAY_OF_YEAR, -1)
                lastMonthEnd.set(Calendar.HOUR_OF_DAY, 23)
                lastMonthEnd.set(Calendar.MINUTE, 59)
                lastMonthEnd.set(Calendar.SECOND, 59)
                toDateMillis = lastMonthEnd.timeInMillis
            }
            ExportDatePreset.CUSTOM -> {
                // Keep currently selected custom dates
            }
        }
    }

    // Filter readings based on date range
    val filteredReadings = remember(allCustomerReadings, fromDateMillis, toDateMillis, selectedPreset) {
        if (selectedPreset == ExportDatePreset.ALL) {
            allCustomerReadings
        } else {
            allCustomerReadings.filter { reading ->
                reading.timestamp in fromDateMillis..toDateMillis
            }
        }
    }

    val totalUnits = filteredReadings.mapNotNull { it.unitsConsumed }.sum()
    val avgPf = if (filteredReadings.isNotEmpty()) filteredReadings.map { it.powerFactor }.average() else 0.0

    // Construct human-readable Date Range Label
    val dateRangeLabel = remember(selectedPreset, fromDateMillis, toDateMillis) {
        when (selectedPreset) {
            ExportDatePreset.ALL -> if (isHindi) "सभी रिकॉर्ड (All Time)" else "All Recorded Entries"
            ExportDatePreset.TODAY -> "${if (isHindi) "आज" else "Today"} (${dateFormat.format(Date(fromDateMillis))})"
            ExportDatePreset.YESTERDAY -> "${if (isHindi) "कल" else "Yesterday"} (${dateFormat.format(Date(fromDateMillis))})"
            ExportDatePreset.THIS_MONTH -> "${dateFormat.format(Date(fromDateMillis))} to ${dateFormat.format(Date(toDateMillis))}"
            ExportDatePreset.LAST_MONTH -> "${dateFormat.format(Date(fromDateMillis))} to ${dateFormat.format(Date(toDateMillis))}"
            ExportDatePreset.LAST_7_DAYS -> "${dateFormat.format(Date(fromDateMillis))} to ${dateFormat.format(Date(toDateMillis))}"
            ExportDatePreset.CUSTOM -> "${dateFormat.format(Date(fromDateMillis))} to ${dateFormat.format(Date(toDateMillis))}"
        }
    }

    // DatePicker launchers for Custom Date selection
    val openFromDatePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = fromDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                fromDateMillis = newCal.timeInMillis
                selectedPreset = ExportDatePreset.CUSTOM
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val openToDatePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = toDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                toDateMillis = newCal.timeInMillis
                selectedPreset = ExportDatePreset.CUSTOM
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("export_report_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "डेट वाईस रिपोर्ट एक्सपोर्ट (Date-Wise)" else "Date-Wise Report Export",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Customer Info
                if (customer != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = customer.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "मीटर: ${customer.meterNumber} | CA: ${customer.consumerNumber.ifBlank { "N/A" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Section 2: Date Filter Presets
                Text(
                    text = if (isHindi) "तारीख अवधि चुनें (Select Date Period):" else "Select Date Range:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ExportDatePreset.entries.forEach { preset ->
                        val isSelected = selectedPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { applyPreset(preset) },
                            label = {
                                Text(
                                    text = if (isHindi) preset.labelHi else preset.labelEn,
                                    fontSize = 11.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Section 3: Custom Date Pickers (From Date & To Date)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // From Date Field
                    OutlinedTextField(
                        value = if (fromDateMillis > 0) dateFormat.format(Date(fromDateMillis)) else "Start",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isHindi) "आरंभ तारीख (From)" else "From Date", fontSize = 11.sp) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pick From Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { openFromDatePicker() }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { openFromDatePicker() }
                            .testTag("export_from_date_input"),
                        singleLine = true
                    )

                    // To Date Field
                    OutlinedTextField(
                        value = dateFormat.format(Date(toDateMillis)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isHindi) "अंतिम तारीख (To)" else "To Date", fontSize = 11.sp) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pick To Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { openToDatePicker() }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { openToDatePicker() }
                            .testTag("export_to_date_input"),
                        singleLine = true
                    )
                }

                // Section 4: Live Period Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "चयनित दिनांक रिपोर्ट सारांश:" else "Selected Period Summary:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "अवधि: $dateRangeLabel",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "कुल रीडिंग: ${filteredReadings.size} दिन",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "कुल खपत: ${String.format(Locale.US, "%.1f", totalUnits)} Units",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (filteredReadings.isNotEmpty()) {
                            Text(
                                text = "औसत PF: ${String.format(Locale.US, "%.3f", avgPf)} (${PowerFactorCalculator.getPfRating(avgPf).labelEn})",
                                style = MaterialTheme.typography.bodySmall,
                                color = PowerFactorCalculator.getPfRating(avgPf).color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (filteredReadings.isEmpty()) {
                    Text(
                        text = if (isHindi) "⚠ इस दिनांक सीमा में कोई मीटर रीडिंग नहीं मिली। कृपया कोई अन्य तारीख चुनें।"
                               else "⚠ No readings found for this date range. Please select another date.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isHindi) "एक्सपोर्ट फॉर्मेट चुनें:" else "Choose Export Format:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                // PDF Export Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF",
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "PDF रिपोर्ट (A4 डॉक्युमेंट)" else "PDF Report (Printable)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isHindi) "तारीख-अनुसार तालिका व हस्ताक्षर" else "Date-wise table & signatures",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Button(
                            onClick = { onExportPdf(filteredReadings, dateRangeLabel) },
                            enabled = filteredReadings.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("export_pdf_button")
                        ) {
                            Text(if (isHindi) "PDF बनाएं" else "PDF", fontSize = 12.sp)
                        }
                    }
                }

                // Excel Export Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = "Excel",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "एक्सेल रिपोर्ट (CSV / Sheets)" else "Excel Spreadsheet (CSV)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isHindi) "UTF-8 एक्सेल संगत स्प्रेडशीट" else "Compatible with Excel & Sheets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Button(
                            onClick = { onExportExcel(filteredReadings, dateRangeLabel) },
                            enabled = filteredReadings.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("export_excel_button")
                        ) {
                            Text(if (isHindi) "Excel बनाएं" else "Excel", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_export_dialog")
            ) {
                Text(if (isHindi) "बंद करें (Close)" else "Close")
            }
        }
    )
}
