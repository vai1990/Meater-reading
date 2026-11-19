package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MeterReading
import com.example.ui.components.CustomerWiseTabStrip
import com.example.ui.components.ReadingCard
import com.example.ui.theme.PowerFactorGood
import com.example.ui.theme.PowerFactorPenalty
import com.example.ui.theme.PowerFactorWarning
import com.example.util.PowerFactorCalculator
import java.util.Locale

@Composable
fun PastReadingsScreen(
    viewModel: MeterViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isHindi = uiState.isHindi

    var readingToDelete by remember { mutableStateOf<MeterReading?>(null) }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Readings fetched from Room database, sorted descending by date/timestamp
    val rawReadings = viewModel.getFilteredReadings()
    val readingsList by remember(rawReadings, uiState.isSortDescending) {
        derivedStateOf {
            if (uiState.isSortDescending) {
                rawReadings.sortedByDescending { it.timestamp }
            } else {
                rawReadings.sortedBy { it.timestamp }
            }
        }
    }

    // Aggregate statistics for the filtered list
    val totalUnits = remember(readingsList) {
        readingsList.mapNotNull { it.unitsConsumed }.sum()
    }
    val avgPf = remember(readingsList) {
        if (readingsList.isNotEmpty()) readingsList.map { it.powerFactor }.average() else 0.0
    }
    val avgRating = PowerFactorCalculator.getPfRating(avgPf)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (uiState.selectedCustomer != null) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openInputScreen() },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text(if (isHindi) "नई रिडिंग दर्ज करें" else "नवीन रिडिंग नोंदवा", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("past_readings_add_fab")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer-Wise Tabs Strip
            item {
                CustomerWiseTabStrip(
                    customers = uiState.customers,
                    selectedCustomer = uiState.selectedCustomer,
                    isHindi = isHindi,
                    hasSavedSheetLink = uiState.sheetSyncConfig.sheetUrl.isNotBlank(),
                    onSelectCustomer = { viewModel.selectCustomer(it) },
                    onOpenSheetSync = { viewModel.openGoogleSheetSyncDialog() },
                    onAddCustomer = { viewModel.openCustomerDialog(null) }
                )
            }

            // Header Card: Title, Customer Details, and Total Count Badge
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("past_readings_header_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isHindi) "मागील मीटर रिडिंग्स इतिहास" else "मागील मीटर रिडिंग्स (Past Readings)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Room डेटाबेसमधून तारीख उतरत्या क्रमाने (Date Descending)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Total count pill
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${readingsList.size} नोंदी",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Customer details chip
                        uiState.selectedCustomer?.let { cust ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${cust.name} • मीटर: ${cust.meterNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = cust.connectionType,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Summary Metrics Card
            if (readingsList.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("past_readings_summary_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "एकूण युनिट्स खपत",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", totalUnits),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("kWh", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    text = "सरासरी पॉवर फॅक्टर",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = String.format(Locale.US, "%.3f", avgPf),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = avgRating.color
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (avgPf >= 0.95) "चांगला" else if (avgPf >= 0.85) "सामान्य" else "कमी!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = avgRating.color
                                    )
                                }
                            }

                            // Export shortcut button
                            IconButton(
                                onClick = { viewModel.openExportDialog() },
                                modifier = Modifier.testTag("past_readings_export_shortcut")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export Reports",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isSortDescending) "नवीन नोंदी आधी \u2193 (Newest First)" else "जुनी नोंदी आधी \u2191 (Oldest First)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Sort Order Toggle Button
                            IconButton(
                                onClick = { viewModel.toggleSortOrder() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("sort_order_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isSortDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = "Toggle Sort Order",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Search Toggle Button
                            IconButton(
                                onClick = { isSearchVisible = !isSearchVisible },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("toggle_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (uiState.searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Search input field
                    if (isSearchVisible || uiState.searchQuery.isNotEmpty()) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("तारीख, kWh रिडिंग किंवा शेरा शोधा...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("past_readings_search_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Date Period Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateFilter.entries.forEach { filter ->
                            val isSelected = uiState.dateFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setDateFilter(filter) },
                                label = { Text(if (isHindi) filter.labelHi else filter.labelEn, fontSize = 11.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Readings List (Chronologically Sorted Descending)
            if (readingsList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .testTag("past_readings_empty_state"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricMeter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "कोणतीही मागील मीटर रिडिंग सापडली नाही",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "डेटाबेसमधून शोधताना निवडलेल्या फिल्टरनुसार कोणतीही नोंद उपलब्ध नाही.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.openInputScreen() },
                                modifier = Modifier.testTag("empty_add_reading_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ नवीन रिडिंग नोंदवा")
                            }
                        }
                    }
                }
            } else {
                items(readingsList, key = { it.id }) { reading ->
                    ReadingCard(
                        reading = reading,
                        isHindi = isHindi,
                        onEdit = { viewModel.openInputScreen(reading) },
                        onDelete = { readingToDelete = reading },
                        onCalculateKvar = { viewModel.openKvarCalculator(reading) },
                        modifier = Modifier.testTag("past_reading_card_${reading.id}")
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    val toDelete = readingToDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { readingToDelete = null },
            title = {
                Text(
                    text = if (isHindi) "रिडिंग हटवायची आहे का?" else "Delete Meter Reading?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${toDelete.readingDate} ची रिडिंग (${toDelete.kwh} kWh) डेटाबेसमधून कायमची काढून टाकली जाईल."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReading(toDelete)
                        readingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हटवा (Delete)")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { readingToDelete = null }) {
                    Text("रद्द करा")
                }
            }
        )
    }
}
