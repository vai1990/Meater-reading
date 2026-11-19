package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import com.example.ui.theme.PowerFactorGood
import com.example.ui.theme.PowerFactorPenalty
import com.example.ui.theme.PowerFactorWarning
import com.example.util.PowerFactorCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingInputScreen(
    customers: List<Customer>,
    customer: Customer?,
    previousReading: MeterReading?,
    readingToEdit: MeterReading?,
    isHindi: Boolean,
    onSelectCustomer: (Customer) -> Unit,
    onAddNewCustomer: () -> Unit,
    onBack: () -> Unit,
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
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept hardware/gesture back press to return to readings list
    BackHandler { onBack() }

    val context = LocalContext.current
    val now = remember { Date() }
    val defaultDate = remember { SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(now) }
    val defaultTime = remember { SimpleDateFormat("hh:mm a", Locale.US).format(now) }

    // Form Field States
    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf(readingToEdit?.readingDate ?: defaultDate) }
    var timeText by remember { mutableStateOf(readingToEdit?.readingTime ?: defaultTime) }
    var kwhInput by remember { mutableStateOf(readingToEdit?.kwh?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var kvarhInput by remember { mutableStateOf(readingToEdit?.kvarh?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var kvahInput by remember { mutableStateOf(readingToEdit?.kvah?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var manualPfInput by remember { mutableStateOf(readingToEdit?.powerFactor?.let { String.format(Locale.US, "%.3f", it) } ?: "0.950") }
    var isAutoPf by remember { mutableStateOf(readingToEdit?.isPfAutoCalculated ?: true) }
    var remarksInput by remember { mutableStateOf(readingToEdit?.remarks ?: "") }
    var hasSubmittedOnce by remember { mutableStateOf(false) }

    // Reference Previous Reading
    val prevKwh = if (readingToEdit != null && customer?.id == readingToEdit.customerId) {
        readingToEdit.previousKwh
    } else {
        previousReading?.kwh
    }

    // Numeric Parsed Values
    val parsedKwh = kwhInput.toDoubleOrNull()
    val parsedKvarh = kvarhInput.toDoubleOrNull()
    val parsedKvah = kvahInput.toDoubleOrNull()
    val parsedManualPf = manualPfInput.toDoubleOrNull()

    // Calculated Power Factor (Auto-Fed from kWh, kVArh, kVAh)
    val calculatedPf = remember(parsedKwh, parsedKvah, parsedKvarh, isAutoPf, parsedManualPf) {
        if (isAutoPf) {
            val kwh = parsedKwh ?: 0.0
            val kvah = parsedKvah ?: 0.0
            val kvarh = parsedKvarh ?: 0.0
            if (kwh > 0.0 && (kvah > 0.0 || kvarh > 0.0)) {
                PowerFactorCalculator.calculateAutoPf(kwh, kvarh, kvah)
            } else {
                0.950
            }
        } else {
            (parsedManualPf ?: 0.950).coerceIn(0.0, 1.0)
        }
    }

    // Units Consumed Calculation
    val consumedUnits = remember(parsedKwh, prevKwh) {
        if (parsedKwh != null && prevKwh != null && parsedKwh >= prevKwh) {
            parsedKwh - prevKwh
        } else null
    }

    // Validation Logic
    val kwhError by remember(kwhInput, hasSubmittedOnce) {
        derivedStateOf {
            if (kwhInput.isBlank()) {
                if (hasSubmittedOnce) "kWh रिडिंग आवश्यक आहे (kWh is required)" else null
            } else {
                val v = kwhInput.toDoubleOrNull()
                when {
                    v == null -> "अवैध संख्या! फक्त अंकीय मूल्य टाका (Must be a numeric value e.g. 12850.5)"
                    v <= 0.0 -> "kWh रिडिंग शून्यापेक्षा जास्त असावी (Must be greater than 0)"
                    else -> null
                }
            }
        }
    }

    val kvarhError by remember(kvarhInput, hasSubmittedOnce) {
        derivedStateOf {
            if (kvarhInput.isBlank()) {
                if (hasSubmittedOnce) "kVArh रिडिंग आवश्यक आहे (kVArh is required)" else null
            } else {
                val v = kvarhInput.toDoubleOrNull()
                when {
                    v == null -> "अवैध संख्या! फक्त अंकीय मूल्य टाका (Must be numeric e.g. 3450.0)"
                    v < 0.0 -> "kVArh रिडिंग ऋण असू शकत नाही (Cannot be negative)"
                    else -> null
                }
            }
        }
    }

    val kvahError by remember(kvahInput, hasSubmittedOnce, parsedKwh) {
        derivedStateOf {
            if (kvahInput.isBlank()) {
                if (hasSubmittedOnce) "kVAh रिडिंग आवश्यक आहे (kVAh is required)" else null
            } else {
                val v = kvahInput.toDoubleOrNull()
                when {
                    v == null -> "अवैध संख्या! फक्त अंकीय मूल्य टाका (Must be numeric e.g. 13350.0)"
                    v <= 0.0 -> "kVAh रिडिंग शून्यापेक्षा जास्त असावी (Must be greater than 0)"
                    parsedKwh != null && v < parsedKwh -> "सावधान: सामान्यतः kVAh \u2265 kWh असते (Warning: kVAh is typically \u2265 kWh)"
                    else -> null
                }
            }
        }
    }

    val pfError by remember(manualPfInput, isAutoPf, hasSubmittedOnce) {
        derivedStateOf {
            if (!isAutoPf) {
                if (manualPfInput.isBlank()) {
                    if (hasSubmittedOnce) "पॉवर फॅक्टर आवश्यक आहे (PF is required)" else null
                } else {
                    val v = manualPfInput.toDoubleOrNull()
                    when {
                        v == null -> "अवैध संख्या! ०.०० ते १.०० मधील संख्या टाका (Must be numeric e.g. 0.96)"
                        v < 0.0 || v > 1.0 -> "पॉवर फॅक्टर ०.०० ते १.०० दरम्यान असावा (PF must be 0.00 to 1.00)"
                        else -> null
                    }
                }
            } else null
        }
    }

    val isFormValid = customer != null &&
            parsedKwh != null && parsedKwh > 0 &&
            parsedKvarh != null && parsedKvarh >= 0 &&
            parsedKvah != null && parsedKvah > 0 &&
            (isAutoPf || (parsedManualPf != null && parsedManualPf in 0.0..1.0))

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (readingToEdit != null) {
                                if (isHindi) "मीटर रीडिंग संपादन" else "Edit Meter Reading"
                            } else {
                                if (isHindi) "मीटर रीडिंग फॉर्म" else "Meter Reading Input"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        customer?.let {
                            Text(
                                text = "${it.name} • ${it.meterNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reading_form_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            kwhInput = ""
                            kvarhInput = ""
                            kvahInput = ""
                            manualPfInput = "0.950"
                            remarksInput = ""
                            hasSubmittedOnce = false
                        },
                        modifier = Modifier.testTag("reset_reading_form_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Form",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_customer_header_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "ग्राहक निवडा (Select Customer) *" else "Select Customer *",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "${customers.size} ${if (isHindi) "ग्राहक" else "Customers"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = isCustomerDropdownExpanded,
                        onExpandedChange = { isCustomerDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customer?.name ?: if (customers.isEmpty()) {
                                if (isHindi) "कोणताही ग्राहक नाही - नवीन जोडा" else "No customers - Add one"
                            } else {
                                if (isHindi) "ग्राहक निवडा (Tap to select)" else "Tap to select customer"
                            },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ElectricMeter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerDropdownExpanded)
                            },
                            isError = hasSubmittedOnce && customer == null,
                            supportingText = {
                                if (hasSubmittedOnce && customer == null) {
                                    Text(
                                        text = if (isHindi) "कृपया प्रथम ग्राहक निवडा (Please select a customer)" else "Please select a customer",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else if (customer != null) {
                                    Text(
                                        text = "मीटर नं: ${customer.meterNumber} • लोड: ${customer.sanctionedLoad.ifBlank { "N/A" }} • ${customer.connectionType}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("form_customer_selector_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = isCustomerDropdownExpanded,
                            onDismissRequest = { isCustomerDropdownExpanded = false }
                        ) {
                            customers.forEach { item ->
                                val isSelected = customer?.id == item.id
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = item.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "मीटर: ${item.meterNumber} • लोड: ${item.sanctionedLoad.ifBlank { "N/A" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                                        onSelectCustomer(item)
                                        isCustomerDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("form_customer_option_${item.id}")
                                )
                            }

                            if (customers.isNotEmpty()) {
                                HorizontalDivider()
                            }

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isHindi) "+ नवीन ग्राहक जोडा (Add New Customer)" else "+ Add New Customer",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    isCustomerDropdownExpanded = false
                                    onAddNewCustomer()
                                },
                                modifier = Modifier.testTag("form_add_new_customer_option")
                            )
                        }
                    }

                    // Quick 1-tap customer switching chips when multiple customers exist
                    if (customers.size > 1) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(customers, key = { it.id }) { item ->
                                val isSelected = customer?.id == item.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectCustomer(item) },
                                    label = {
                                        Text(
                                            text = item.name,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.testTag("form_customer_chip_${item.id}")
                                )
                            }
                        }
                    }
                }
            }

            // Previous Reading Context Card
            if (prevKwh != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "मागील रिडिंग (Previous kWh):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.2f kWh", prevKwh),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Date & Time Picker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("दिनांक (Date)") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth)
                                    }
                                    dateText = SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(newCal.time)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_form_date"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("वेळ (Time)") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val cal = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        set(Calendar.MINUTE, minute)
                                    }
                                    timeText = SimpleDateFormat("hh:mm a", Locale.US).format(newCal.time)
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false
                            ).show()
                        }) {
                            Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Pick Time")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_form_time"),
                    singleLine = true
                )
            }

            // --- 1. ACTIVE ENERGY (kWh) INPUT ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = kwhInput,
                        onValueChange = { kwhInput = it },
                        label = { Text("सक्रिय ऊर्जा (Active Energy kWh) *") },
                        placeholder = { Text("उदा. 12850.50") },
                        suffix = { Text("kWh", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = kwhError != null,
                        supportingText = {
                            if (kwhError != null) {
                                Text(kwhError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("मीटरवरील चालू kWh रिडिंग टाका (Numeric only)")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_form_kwh"),
                        singleLine = true
                    )

                    // Consumed Units Badge
                    if (consumedUnits != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "खपत युनिट्स (Net Units Consumed): +${String.format(Locale.US, "%.2f", consumedUnits)} kWh",
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (parsedKwh != null && prevKwh != null && parsedKwh < prevKwh) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "सूचना: चालू रिडिंग मागील रिडिंगपेक्षा (${String.format(Locale.US, "%.1f", prevKwh)}) कमी आहे.",
                                    color = Color(0xFFE65100),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. REACTIVE ENERGY (kVArh) INPUT ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = kvarhInput,
                        onValueChange = { kvarhInput = it },
                        label = { Text("रिएक्टिव ऊर्जा (Reactive Energy kVArh) *") },
                        placeholder = { Text("उदा. 3450.00") },
                        suffix = { Text("kVArh", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = kvarhError != null,
                        supportingText = {
                            if (kvarhError != null) {
                                Text(kvarhError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("मोटर व ट्रान्सफॉर्मरची रिअॅक्टिव्ह ऊर्जा (Numeric kVArh)")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_form_kvarh"),
                        singleLine = true
                    )
                }
            }

            // --- 3. APPARENT ENERGY (kVAh) INPUT ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = kvahInput,
                        onValueChange = { kvahInput = it },
                        label = { Text("अपैरंट ऊर्जा (Apparent Energy kVAh) *") },
                        placeholder = { Text("उदा. 13350.00") },
                        suffix = { Text("kVAh", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = kvahError != null,
                        supportingText = {
                            if (kvahError != null) {
                                Text(kvahError!!, color = if (kvahError!!.startsWith("सावधान")) Color(0xFFE65100) else MaterialTheme.colorScheme.error)
                            } else {
                                Text("एकूण अपैरंट ऊर्जा (kVAh \u2265 kWh)")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_form_kvah"),
                        singleLine = true
                    )

                    // Helper: Auto-calculate kVAh from sqrt(kWh^2 + kVArh^2)
                    if (parsedKwh != null && parsedKvarh != null && parsedKwh > 0 && parsedKvarh > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val estimated = PowerFactorCalculator.calculateKvahFromKvarh(parsedKwh, parsedKvarh)
                                    kvahInput = String.format(Locale.US, "%.2f", estimated)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("kVAh सूत्रानुसार भरा \u221A(kWh\u00B2+kVArh\u00B2)", fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            }

            // --- 4. POWER FACTOR (PF) AUTO/MANUAL SECTION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "पॉवर फॅक्टर (Power Factor)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAutoPf) "ऑटो-कॅल्क्युलेशन (PF = kWh / kVAh)" else "मीटरवरील मॅन्युअल एन्ट्री",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAutoPf) "Auto" else "Manual",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = isAutoPf,
                                onCheckedChange = { isAutoPf = it },
                                modifier = Modifier.testTag("form_auto_pf_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val rating = PowerFactorCalculator.getPfRating(calculatedPf)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rating.color.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .border(1.dp, rating.color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isAutoPf) "ऑटो-फीड PF (Auto-Fed Cos \u03A6):" else "निवडलेला PF (Cos \u03A6):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.US, "%.3f", calculatedPf),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = rating.color
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(rating.color, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = rating.labelMr,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = if (isAutoPf) String.format(Locale.US, "%.3f", calculatedPf) else manualPfInput,
                        onValueChange = {
                            isAutoPf = false
                            manualPfInput = it
                        },
                        readOnly = isAutoPf,
                        label = {
                            Text(
                                if (isAutoPf) "ऑटो-फीड पॉवर फॅक्टर (Auto-Fed PF = kWh / kVAh)"
                                else "मॅन्युअल पॉवर फॅक्टर (0.00 - 1.00) *"
                            )
                        },
                        placeholder = { Text("उदा. 0.965") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = pfError != null,
                        supportingText = {
                            if (pfError != null) {
                                Text(pfError!!, color = MaterialTheme.colorScheme.error)
                            } else if (isAutoPf) {
                                Text("kWh आणि kVAh टाकताच PF येथे आपोआप (Auto-Feed) भरला जातो")
                            } else {
                                Text("मीटरवर दर्शवलेला प्रत्यक्ष PF (०.०० ते १.००)")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_form_manual_pf"),
                        singleLine = true
                    )
                }
            }

            // --- 5. POWER FACTOR CORRECTION (REQUIRED kVAr RATING) SECTION ---
            var targetPfInput by remember { mutableStateOf("0.99") }
            val defaultSanctionedKw = remember(customer) {
                customer?.sanctionedLoad?.let { PowerFactorCalculator.parseLoadKw(it) } ?: 50.0
            }
            var customLoadKwInput by remember(defaultSanctionedKw) {
                mutableStateOf(String.format(Locale.US, "%.1f", defaultSanctionedKw))
            }

            val parsedTargetPf = (targetPfInput.toDoubleOrNull() ?: 0.99).coerceIn(0.80, 1.00)
            val parsedLoadKw = (customLoadKwInput.toDoubleOrNull() ?: defaultSanctionedKw).coerceAtLeast(1.0)

            val kvarCorrectionResult = remember(
                parsedKwh,
                prevKwh,
                parsedKvarh,
                parsedKvah,
                calculatedPf,
                parsedTargetPf,
                parsedLoadKw
            ) {
                PowerFactorCalculator.calculateRequiredKvarFromReadingInput(
                    currentKwh = parsedKwh ?: 0.0,
                    previousKwh = prevKwh,
                    currentKvarh = parsedKvarh ?: 0.0,
                    currentKvah = parsedKvah ?: 0.0,
                    currentPfInput = calculatedPf,
                    targetPf = parsedTargetPf,
                    sanctionedLoadKw = parsedLoadKw,
                    operatingHours = 24.0
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_kvar_correction_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "आवश्यक kVAr कॅल्क्युलेटर (PF Correction)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "चालू रीडिंग व Target PF वरून आवश्यक kVAr क्षमता",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetPfInput,
                            onValueChange = { targetPfInput = it },
                            label = { Text("Target PF (लक्ष्यित PF)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_form_target_pf")
                        )
                        OutlinedTextField(
                            value = customLoadKwInput,
                            onValueChange = { customLoadKwInput = it },
                            label = { Text("लोड (Load kW)") },
                            suffix = { Text("kW") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_form_load_kw")
                        )
                    }

                    // Quick Target PF preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("0.95", "0.98", "0.99", "1.00").forEach { pfPreset ->
                            FilterChip(
                                selected = targetPfInput == pfPreset,
                                onClick = { targetPfInput = pfPreset },
                                label = {
                                    Text(
                                        text = "PF $pfPreset",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Live Required kVAr Result Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        if (calculatedPf < parsedTargetPf) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "आवश्यक kVAr (Required Rating):",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", kvarCorrectionResult.requiredKvar)} kVAr",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.testTag("form_required_kvar_value")
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                PowerFactorGood.copy(alpha = 0.15f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "शिफारस: ${kvarCorrectionResult.recommendedBankSize.toInt()} kVAr बँक",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = PowerFactorGood
                                        )
                                    }
                                }
                                if (kvarCorrectionResult.recommendedSteps.isNotEmpty()) {
                                    Text(
                                        text = "APFC स्टेप्स: ${kvarCorrectionResult.recommendedSteps.joinToString(" + ") { "${it}k" }} kVAr | kVA बचत: -${String.format(Locale.US, "%.1f", kvarCorrectionResult.kvaReduction)} kVA",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PowerFactorGood,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "सध्याचा PF (${String.format(Locale.US, "%.3f", calculatedPf)}) लक्ष्यित PF (${String.format(Locale.US, "%.2f", parsedTargetPf)}) इतका किंवा अधिक आहे. अतिरिक्त kVAr ची गरज नाही (0.0 kVAr).",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PowerFactorGood,
                                    modifier = Modifier.testTag("form_required_kvar_value")
                                )
                            }
                        }
                    }
                }
            }

            // Remarks / Sheet PF Field
            OutlinedTextField(
                value = remarksInput,
                onValueChange = { remarksInput = it },
                label = { Text("टिप्पणी / शेरा (Sheet PF / Remark)") },
                placeholder = { Text("उदा. PF: 985 / Regular / APFC चालू") },
                supportingText = {
                    Text("Google Sheet मधील PF येथे Remark मध्ये सेव्ह होतो")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_form_remarks"),
                singleLine = true
            )

            // Quick Remarks Suggestion Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("नियमित रिडिंग", "APFC चालू", "हेवी लोड", "DG सेट").forEach { chipText ->
                    OutlinedButton(
                        onClick = { remarksInput = chipText },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(chipText, fontSize = 10.5.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Button(
                onClick = {
                    hasSubmittedOnce = true
                    if (isFormValid && parsedKwh != null && parsedKvarh != null && parsedKvah != null) {
                        onSave(
                            dateText.trim(),
                            timeText.trim(),
                            System.currentTimeMillis(),
                            parsedKwh,
                            parsedKvarh,
                            parsedKvah,
                            calculatedPf,
                            isAutoPf,
                            remarksInput.trim()
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_reading_form_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (readingToEdit != null) "रिडिंग अपडेट करा (Update Reading)" else "मीटर रिडिंग सेव्ह करा (Save Reading)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cancel_reading_form_button")
            ) {
                Text("रद्द करा (Cancel)")
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
