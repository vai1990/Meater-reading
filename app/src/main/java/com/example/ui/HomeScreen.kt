package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.ElectricMeter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MeterReading
import com.example.ui.auth.signOut
import com.example.ui.components.CustomerCard
import com.example.ui.components.CustomerWiseTabStrip
import com.example.ui.components.PowerFactorGauge
import com.example.ui.components.ReadingCard
import com.example.ui.components.StatSummaryCard
import com.example.ui.dialogs.AddCustomerDialog
import com.example.ui.dialogs.AddReadingDialog
import com.example.ui.dialogs.AppBrandIcon
import com.example.ui.dialogs.AppBrandingDialog
import com.example.ui.dialogs.CustomerListDialog
import com.example.ui.dialogs.ExportReportDialog
import com.example.ui.dialogs.GoogleSheetSyncDialog
import com.example.ui.dialogs.KvarCalculatorDialog
import com.example.ui.dialogs.PfGuideDialog
import com.example.ui.dialogs.TeamSyncDialog
import com.example.ui.dialogs.shareInviteCode
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppVisualTheme
import com.example.util.PowerFactorCalculator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MeterViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var readingToDelete by remember { mutableStateOf<MeterReading?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadBranding(context)
        viewModel.initializeSheetSyncAndImportBundled(context)
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val filteredReadings = viewModel.getFilteredReadings()
    val isHindi = uiState.isHindi

    val activeTheme = uiState.brandingConfig.visualTheme

    MyApplicationTheme(visualTheme = activeTheme) {
    // Dedicated Meter Reading Input Form Screen
    if (uiState.isInputScreenOpen) {
        MeterReadingInputScreen(
            customers = uiState.customers,
            customer = uiState.selectedCustomer,
            previousReading = uiState.latestReading,
            readingToEdit = uiState.readingToEdit,
            isHindi = isHindi,
            onSelectCustomer = { viewModel.selectCustomer(it) },
            onAddNewCustomer = { viewModel.openCustomerDialog(null) },
            onBack = { viewModel.closeInputScreen() },
            onSave = { date, time, timestamp, kwh, kvarh, kvah, pf, isAutoPf, remarks ->
                viewModel.saveReading(
                    readingDate = date,
                    readingTime = time,
                    timestamp = timestamp,
                    kwh = kwh,
                    kvarh = kvarh,
                    kvah = kvah,
                    powerFactor = pf,
                    isPfAutoCalculated = isAutoPf,
                    remarks = remarks,
                    existingId = uiState.readingToEdit?.id ?: 0L
                )
            },
            modifier = modifier
        )

        if (uiState.showCustomerDialog) {
            AddCustomerDialog(
                customerToEdit = uiState.customerToEdit,
                isHindi = isHindi,
                onDismiss = { viewModel.closeCustomerDialog() },
                onSave = { name, address, meterNumber, consumerNumber, phoneNumber, connectionType, sanctionedLoad, existingId ->
                    viewModel.saveCustomer(
                        name = name,
                        address = address,
                        meterNumber = meterNumber,
                        consumerNumber = consumerNumber,
                        phoneNumber = phoneNumber,
                        connectionType = connectionType,
                        sanctionedLoad = sanctionedLoad,
                        existingId = existingId
                    )
                }
            )
        }
        return@MyApplicationTheme
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            var isMoreActionsExpanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(activeTheme.headerStartHex),
                                Color(activeTheme.headerEndHex)
                            )
                        )
                    )
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { viewModel.openBrandingDialog() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("top_bar_app_branding_button")
                        ) {
                            AppBrandIcon(
                                style = uiState.brandingConfig.iconStyle,
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = uiState.brandingConfig.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                val defaultSubtitle = when (uiState.currentTab) {
                                    0 -> if (isHindi) "दैनिक मीटर रीडिंग डॅशबोर्ड" else "Daily Meter Reading Dashboard"
                                    1 -> if (isHindi) "मागील मीटर रिडिंग्स (उतरता क्रम)" else "Past Meter Readings"
                                    2 -> if (isHindi) "ग्राहक निर्देशिका व मीटर" else "Customer Profiles & Meters"
                                    else -> if (isHindi) "डेटा बॅकअप, रिस्टोर व सेटिंग" else "Backup, Restore & Settings"
                                }
                                Text(
                                    text = uiState.brandingConfig.customSubtitle.ifBlank { defaultSubtitle },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.5.sp,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                        }
                    },
                    actions = {
                        // 1-Tap Dark / Light Mode Toggle Button
                        IconButton(
                            onClick = { viewModel.toggleDarkMode(context) },
                            modifier = Modifier.testTag("top_bar_dark_mode_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.18f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (activeTheme.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (activeTheme.isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                                    tint = if (activeTheme.isDark) Color(0xFFFDE68A) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // App Look / Theme Switcher Button
                        IconButton(
                            onClick = { viewModel.openBrandingDialog() },
                            modifier = Modifier.testTag("top_bar_look_theme_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.18f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Change App Look",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Language Switcher
                        IconButton(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier.testTag("toggle_language_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.18f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "EN" else "म/हिं",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        // Google Sheet Customer-Wise Sync shortcut
                        IconButton(
                            onClick = { viewModel.openGoogleSheetSyncDialog() },
                            modifier = Modifier.testTag("top_bar_sheet_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = "Google Sheet Sync",
                                tint = Color(0xFFA7F3D0)
                            )
                        }

                        // Direct Date-wise Export dialog shortcut
                        IconButton(
                            onClick = { viewModel.openExportDialog() },
                            modifier = Modifier.testTag("open_export_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export Report",
                                tint = Color.White
                            )
                        }

                        // More Tools Menu (kVAr Calculator, Team Sync, Drive Backup)
                        Box {
                            IconButton(
                                onClick = { isMoreActionsExpanded = true },
                                modifier = Modifier.testTag("top_bar_more_tools_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "More Tools",
                                    tint = Color(0xFFFDE68A)
                                )
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = isMoreActionsExpanded,
                                onDismissRequest = { isMoreActionsExpanded = false }
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(if (isHindi) "अॅपचा लूक व नाव बदला" else "Change App Look & Theme", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        isMoreActionsExpanded = false
                                        viewModel.openBrandingDialog()
                                    },
                                    modifier = Modifier.testTag("top_bar_menu_look_button")
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(if (isHindi) "kVAr कॅल्क्युलेटर" else "kVAr Calculator", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        isMoreActionsExpanded = false
                                        viewModel.openKvarCalculator(uiState.latestReading)
                                    },
                                    modifier = Modifier.testTag("top_bar_kvar_calc_button")
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(if (isHindi) "सहकारी लाईव्ह सिंक" else "Colleague Live Sync", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF047857))
                                    },
                                    onClick = {
                                        isMoreActionsExpanded = false
                                        viewModel.openTeamSyncDialog()
                                    },
                                    modifier = Modifier.testTag("top_bar_team_sync_button")
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(if (isHindi) "Google Drive बॅकअप" else "Google Drive Backup", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF1A73E8))
                                    },
                                    onClick = {
                                        isMoreActionsExpanded = false
                                        viewModel.setCurrentTab(3)
                                    },
                                    modifier = Modifier.testTag("top_bar_drive_backup_button")
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                // Tab 0: Readings Dashboard
                NavigationBarItem(
                    selected = uiState.currentTab == 0,
                    onClick = { viewModel.setCurrentTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == 0) Icons.Default.ElectricMeter else Icons.Outlined.ElectricMeter,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = {
                        Text(
                            text = if (isHindi) "डॅशबोर्ड" else "Dashboard",
                            fontSize = 11.sp,
                            fontWeight = if (uiState.currentTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                // Tab 1: Past Readings (Fetched from Room DB - Date Descending)
                NavigationBarItem(
                    selected = uiState.currentTab == 1,
                    onClick = { viewModel.setCurrentTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == 1) Icons.Default.History else Icons.Outlined.History,
                            contentDescription = "Past Readings"
                        )
                    },
                    label = {
                        Text(
                            text = if (isHindi) "मागील नोंदी" else "History",
                            fontSize = 11.sp,
                            fontWeight = if (uiState.currentTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_past_readings")
                )

                // Tab 2: Customers
                NavigationBarItem(
                    selected = uiState.currentTab == 2,
                    onClick = { viewModel.setCurrentTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == 2) Icons.Default.People else Icons.Outlined.People,
                            contentDescription = "Customers"
                        )
                    },
                    label = {
                        Text(
                            text = if (isHindi) "ग्राहक" else "Customers",
                            fontSize = 11.sp,
                            fontWeight = if (uiState.currentTab == 2) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_customers")
                )

                // Tab 3: Export & Restore Setting Tab
                NavigationBarItem(
                    selected = uiState.currentTab == 3,
                    onClick = { viewModel.setCurrentTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == 3) Icons.Default.SettingsBackupRestore else Icons.Outlined.SettingsBackupRestore,
                            contentDescription = "Export & Restore"
                        )
                    },
                    label = {
                        Text(
                            text = if (isHindi) "बॅकअप/रिस्टोर" else "Backup",
                            fontSize = 11.sp,
                            fontWeight = if (uiState.currentTab == 3) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_export_restore")
                )
            }
        },
        floatingActionButton = {
            if (uiState.currentTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddReadingDialog() },
                    modifier = Modifier.testTag("add_reading_fab"),
                    shape = RoundedCornerShape(16.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add"
                        )
                    },
                    text = {
                        Text(
                            text = if (isHindi) "नवीन रिडिंग नोंदवा" else "Log Reading",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val horizontalPad = if (maxWidth >= 600.dp) 28.dp else 16.dp
            AnimatedContent(
                targetState = uiState.currentTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -width / 4 } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> width / 4 } + fadeOut())
                    }
                },
                label = "main_tab_flow_transition"
            ) { currentTab ->
                when (currentTab) {
                    1 -> {
                        // Tab 1: Dedicated Past Readings Screen (Room DB - Date Descending)
                        PastReadingsScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    2 -> {
                        // Tab 2: Customer Directory Screen
                        CustomerTabScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    3 -> {
                        // Tab 3: Export & Restore Setting Tab
                        ExportRestoreScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    else -> {
                        // Tab 0: Readings Dashboard
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background),
                            contentPadding = PaddingValues(start = horizontalPad, end = horizontalPad, top = 12.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                    // 00. 1-Tap App Look / Theme Switcher Strip (अॅपचा लूक १-क्लिकमध्ये बदला)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_look_switcher_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "अॅपचा लूक / थीम बदला (1-Tap Look)" else "App Look / Theme Switcher",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = if (isHindi) "कस्टमाईझ \u2192" else "Customize \u2192",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { viewModel.openBrandingDialog() }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AppVisualTheme.entries.forEach { themeItem ->
                                        val isSelected = activeTheme == themeItem
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    if (isSelected)
                                                        Brush.horizontalGradient(
                                                            listOf(
                                                                Color(themeItem.headerStartHex),
                                                                Color(themeItem.primaryHex)
                                                            )
                                                        )
                                                    else
                                                        Brush.horizontalGradient(
                                                            listOf(
                                                                MaterialTheme.colorScheme.surfaceVariant,
                                                                MaterialTheme.colorScheme.surfaceVariant
                                                            )
                                                        )
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) Color(themeItem.primaryHex) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .clickable { viewModel.selectVisualTheme(context, themeItem) }
                                                .padding(horizontal = 11.dp, vertical = 6.dp)
                                                .testTag("quick_theme_chip_${themeItem.id}"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isSelected) Color.White else Color(themeItem.primaryHex)
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isHindi) themeItem.labelMr.substringBefore(" (") else themeItem.labelEn,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 0. Customer-Wise Tabs Strip (mirrors customer-wise tabs in Google Sheet)
                    item {
                        CustomerWiseTabStrip(
                            customers = uiState.customers,
                            selectedCustomer = uiState.selectedCustomer,
                            isHindi = isHindi,
                            hasSavedSheetLink = uiState.sheetSyncConfig.sheetUrl.isNotBlank(),
                            isSyncing = uiState.isSheetSyncing,
                            onSelectCustomer = { viewModel.selectCustomer(it) },
                            onOpenSheetSync = { viewModel.openGoogleSheetSyncDialog() },
                            onQuickRefreshSheet = { viewModel.syncSavedGoogleSheetNow(context) },
                            onAddCustomer = { viewModel.openCustomerDialog(null) }
                        )
                    }

                    // 1. Customer Details Card
                    item {
                        CustomerCard(
                            customer = uiState.selectedCustomer,
                            isHindi = isHindi,
                            onSwitchCustomer = { viewModel.setCurrentTab(2) },
                            onEditCustomer = { viewModel.openCustomerDialog(uiState.selectedCustomer) },
                            onAddCustomer = { viewModel.openCustomerDialog(null) }
                        )
                    }

                    // 1A-0. Google Sheet Live Sync Quick Card (गूगल शीटवरील नवीन एन्ट्री १-क्लिक सिंक)
                    item {
                        val hasSheetLink = uiState.sheetSyncConfig.sheetUrl.isNotBlank()
                        val isDark = activeTheme.isDark
                        val sheetCardBg = if (isDark) Color(0xFF0A281E) else Color(0xFFECFDF5)
                        val sheetIconBoxBg = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                        val sheetTitleColor = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                        val sheetSubColor = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)
                        val sheetAccentColor = if (isDark) Color(0xFF10B981) else Color(0xFF047857)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_google_sheet_sync_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = sheetCardBg
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(sheetIconBoxBg, RoundedCornerShape(11.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TableChart,
                                            contentDescription = null,
                                            tint = sheetTitleColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isHindi) "Google Sheet एन्ट्री लाईव्ह सिंक" else "Google Sheet Live Sync",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = sheetTitleColor
                                        )
                                        Text(
                                            text = if (uiState.isSheetSyncing) {
                                                if (isHindi) "Google Sheet मधून नवीन एन्ट्री सिंक होत आहेत..." else "Syncing latest entries from Sheet..."
                                            } else if (hasSheetLink) {
                                                if (isHindi)
                                                    "${uiState.sheetSyncConfig.lastSyncedTabsCount} कस्टमर टॅब • ${uiState.sheetSyncConfig.lastSyncedReadingsCount} एन्ट्री जोडलेल्या"
                                                else
                                                    "${uiState.sheetSyncConfig.lastSyncedTabsCount} tabs • ${uiState.sheetSyncConfig.lastSyncedReadingsCount} entries"
                                            } else {
                                                if (isHindi) "तुमच्या Google Sheet ची लिंक जोडा व सर्व एन्ट्री सिंक करा" else "Link your Google Sheet to sync entries"
                                            },
                                            fontSize = 11.sp,
                                            color = sheetSubColor
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (hasSheetLink) {
                                        OutlinedButton(
                                            onClick = { viewModel.openGoogleSheetSyncDialog() },
                                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("dashboard_sheet_link_settings_button")
                                        ) {
                                            Text(
                                                text = if (isHindi) "लिंक" else "Link",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = sheetTitleColor
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (hasSheetLink) {
                                                viewModel.syncSavedGoogleSheetNow(context)
                                            } else {
                                                viewModel.openGoogleSheetSyncDialog()
                                            }
                                        },
                                        enabled = !uiState.isSheetSyncing,
                                        colors = ButtonDefaults.buttonColors(containerColor = sheetAccentColor),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("dashboard_sheet_sync_now_button")
                                    ) {
                                        Text(
                                            text = if (uiState.isSheetSyncing) {
                                                if (isHindi) "सिंक..." else "Syncing..."
                                            } else if (hasSheetLink) {
                                                if (isHindi) "आता सिंक करा" else "Sync Now"
                                            } else {
                                                if (isHindi) "शीट जोडा" else "Link Sheet"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 1A. Colleague Live Cloud Sync & Google Drive Quick Actions Strip
                    item {
                        val ws = uiState.activeWorkspace
                        val inviteCode = ws?.inviteCode ?: "TEAM101"
                        val memberCount = ws?.memberNames?.size ?: 1
                        val isDark = activeTheme.isDark
                        val teamAccent = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_team_sync_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(teamAccent.copy(alpha = 0.16f), RoundedCornerShape(11.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Groups,
                                                contentDescription = null,
                                                tint = teamAccent,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isHindi) "सहकारी सिंक (कोड: $inviteCode)" else "Colleague Sync ($inviteCode)",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isHindi)
                                                    "$memberCount सहकारी • ${uiState.cloudReadings.size} क्लाउड नोंदी"
                                                else
                                                    "$memberCount members • ${uiState.cloudReadings.size} synced",
                                                fontSize = 11.sp,
                                                color = teamAccent
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                shareInviteCode(
                                                    context = context,
                                                    inviteCode = inviteCode,
                                                    teamName = ws?.name ?: "MeterRead Team"
                                                )
                                            },
                                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("dashboard_share_team_code_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                tint = Color(0xFF047857),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isHindi) "शेअर" else "Share",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857)
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.openTeamSyncDialog() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("dashboard_team_sync_button")
                                        ) {
                                            Text(
                                                text = if (isHindi) "टीम सिंक" else "Team Sync",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // 1B. Google Drive Quick Backup & Restore Strip
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_drive_quick_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(Color(0xFFEFF6FF), RoundedCornerShape(11.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudSync,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isHindi) "Google Drive बॅकअप व रिस्टोर" else "Google Drive Cloud Sync",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (uiState.driveSnapshots.isNotEmpty())
                                                    (if (isHindi) "${uiState.driveSnapshots.size} क्लाउड बॅकअप सुरक्षित" else "${uiState.driveSnapshots.size} backups ready")
                                                else
                                                    uiState.driveConfig.userEmail,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { viewModel.syncGoogleDriveNow(context, openExternalDriveApp = false) },
                                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("dashboard_drive_backup_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isHindi) "बॅकअप" else "Backup",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.setCurrentTab(3)
                                                viewModel.openDriveRestoreDialog()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("dashboard_drive_restore_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isHindi) "रिस्टोर" else "Restore",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Power Factor Gauge & Status Card (if readings exist)
                    val latest = uiState.latestReading
                    if (latest != null) {
                        item {
                            val customerLoad = uiState.selectedCustomer?.sanctionedLoad?.let {
                                PowerFactorCalculator.parseLoadKw(it)
                            } ?: 50.0

                            PowerFactorGauge(
                                pfValue = latest.powerFactor,
                                isHindi = isHindi,
                                onInfoClick = { viewModel.openPfGuideDialog() },
                                onCalculateKvarClick = { viewModel.openKvarCalculator(latest) },
                                loadKw = customerLoad
                            )
                        }
                    }

                    // 3. Overall Energy Consumption Summary
                    if (filteredReadings.isNotEmpty()) {
                        item {
                            StatSummaryCard(
                                readings = filteredReadings,
                                isHindi = isHindi
                            )
                        }
                    }

                    // 4. Filter Chips & Search Bar
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHindi) "रीडिंग इतिहास (Reading Logs)" else "Daily Reading Logs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { viewModel.setCurrentTab(1) },
                                        modifier = Modifier.testTag("view_all_past_readings_button")
                                    ) {
                                        Text(
                                            text = if (isHindi) "सर्व पहा →" else "View All →",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = { isSearchExpanded = !isSearchExpanded },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            if (isSearchExpanded) {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text(if (isHindi) "तारीख या टिप्पणी खोजें..." else "Search date or remarks...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search_readings_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            // Date Filters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DateFilter.entries.forEach { filter ->
                                    val isSelected = uiState.dateFilter == filter
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setDateFilter(filter) },
                                        label = {
                                            Text(
                                                text = if (isHindi) filter.labelHi else filter.labelEn,
                                                fontSize = 12.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 5. Readings List Items or Interactive Guided Onboarding Flow
                    if (filteredReadings.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ElectricMeter,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isHindi) "दैनंदिन मीटर रीडिंग फ्लो सुरू करा" else "Start Daily Meter Reading Flow",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isHindi)
                                            "१. ग्राहक निवडा किंवा Google Sheet सिंक करा → २. kWh, kVArh, kVAh नोंदवा → ३. ऑटो PF व आवश्यक kVAr पहा"
                                        else
                                            "1. Select Customer or Sync Sheet → 2. Enter kWh, kVArh, kVAh → 3. View Auto PF & Required kVAr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.openGoogleSheetSyncDialog() },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isHindi) "Sheet सिंक करा" else "Sync Sheet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { viewModel.openAddReadingDialog() },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("empty_state_add_reading_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isHindi) "+ रीडिंग नोंदवा" else "+ Add Reading", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredReadings, key = { it.id }) { reading ->
                            ReadingCard(
                                reading = reading,
                                isHindi = isHindi,
                                onEdit = { viewModel.openAddReadingDialog(reading) },
                                onDelete = { readingToDelete = reading },
                                onCalculateKvar = { viewModel.openKvarCalculator(reading) }
                            )
                        }
                    }
                }
            }
        }
            }
        }
    }

    // Dialog 1: Add / Edit Reading
    if (uiState.showAddReadingDialog) {
        AddReadingDialog(
            customers = uiState.customers,
            selectedCustomer = uiState.selectedCustomer,
            previousReading = uiState.latestReading,
            readingToEdit = uiState.readingToEdit,
            isHindi = isHindi,
            onSelectCustomer = { viewModel.selectCustomer(it) },
            onDismiss = { viewModel.closeAddReadingDialog() },
            onSave = { date, time, timestamp, kwh, kvarh, kvah, pf, isAutoPf, remarks ->
                viewModel.saveReading(
                    readingDate = date,
                    readingTime = time,
                    timestamp = timestamp,
                    kwh = kwh,
                    kvarh = kvarh,
                    kvah = kvah,
                    powerFactor = pf,
                    isPfAutoCalculated = isAutoPf,
                    remarks = remarks,
                    existingId = uiState.readingToEdit?.id ?: 0L
                )
            }
        )
    }

    // Dialog 2: Add / Edit Customer Profile
    if (uiState.showCustomerDialog) {
        AddCustomerDialog(
            customerToEdit = uiState.customerToEdit,
            isHindi = isHindi,
            onDismiss = { viewModel.closeCustomerDialog() },
            onSave = { name, address, meterNumber, consumerNumber, phoneNumber, connectionType, sanctionedLoad, existingId ->
                viewModel.saveCustomer(
                    name = name,
                    address = address,
                    meterNumber = meterNumber,
                    consumerNumber = consumerNumber,
                    phoneNumber = phoneNumber,
                    connectionType = connectionType,
                    sanctionedLoad = sanctionedLoad,
                    existingId = existingId
                )
            }
        )
    }

    // Dialog 3: Customer Directory / List
    if (uiState.showCustomerListDialog) {
        CustomerListDialog(
            customers = uiState.customers,
            selectedCustomer = uiState.selectedCustomer,
            isHindi = isHindi,
            onSelectCustomer = { viewModel.selectCustomer(it) },
            onAddNewCustomer = { viewModel.openCustomerDialog(null) },
            onDeleteCustomer = { viewModel.deleteCustomer(it) },
            onDismiss = { viewModel.closeCustomerListDialog() }
        )
    }

    // Dialog 4: Export Reports (PDF & Excel - Date-Wise)
    if (uiState.showExportDialog) {
        ExportReportDialog(
            customer = uiState.selectedCustomer,
            allCustomerReadings = uiState.readings,
            isHindi = isHindi,
            onExportPdf = { selectedList, label ->
                viewModel.exportPdf(context, selectedList, label)
            },
            onExportExcel = { selectedList, label ->
                viewModel.exportExcel(context, selectedList, label)
            },
            onDismiss = { viewModel.closeExportDialog() }
        )
    }

    // Dialog 5: Power Factor & Tariff Guide
    if (uiState.showPfGuideDialog) {
        PfGuideDialog(
            isHindi = isHindi,
            onOpenKvarCalculator = { viewModel.openKvarCalculator(uiState.latestReading) },
            onDismiss = { viewModel.closePfGuideDialog() }
        )
    }

    // Dialog 6: kVAr Capacitor Bank Requirement Calculator
    if (uiState.showKvarCalculatorDialog) {
        KvarCalculatorDialog(
            customer = uiState.selectedCustomer,
            initialReading = uiState.kvarSelectedReading,
            isHindi = isHindi,
            onDismiss = { viewModel.closeKvarCalculator() }
        )
    }

    // Dialog 7: Colleague Live Cloud Sync (सहकारी लाईव्ह सिंक)
    val coroutineScope = rememberCoroutineScope()
    if (uiState.showTeamSyncDialog) {
        TeamSyncDialog(
            activeWorkspace = uiState.activeWorkspace,
            cloudReadings = uiState.cloudReadings,
            currentUserName = uiState.currentUserName,
            currentUserEmail = uiState.currentUserEmail,
            isLoading = uiState.isTeamSyncLoading,
            isHindi = isHindi,
            onJoinTeamByCode = { code, name ->
                viewModel.joinColleagueWorkspace(code, name)
            },
            onCreateCustomTeam = { code, teamName, userName ->
                viewModel.createCustomTeamWorkspace(code, teamName, userName)
            },
            onSyncNow = {
                viewModel.syncAllToColleagueCloudNow()
            },
            onSignOut = {
                viewModel.closeTeamSyncDialog()
                coroutineScope.launch {
                    signOut(context)
                }
            },
            onDismiss = { viewModel.closeTeamSyncDialog() }
        )
    }

    // Dialog 8: App Look, Theme, Name & Icon Customizer (अॅपचा लूक, नाव आणि आयकॉन बदला)
    if (uiState.showBrandingDialog) {
        AppBrandingDialog(
            currentBranding = uiState.brandingConfig,
            isHindi = isHindi,
            onSave = { newName, newSubtitle, newIconStyle, newVisualTheme ->
                viewModel.updateAppBranding(context, newName, newSubtitle, newIconStyle, newVisualTheme)
            },
            onDismiss = { viewModel.closeBrandingDialog() }
        )
    }

    // Dialog 9: Google Sheet Customer-Wise Sync
    if (uiState.showGoogleSheetSyncDialog) {
        GoogleSheetSyncDialog(
            sheetConfig = uiState.sheetSyncConfig,
            isSyncing = uiState.isSheetSyncing,
            isHindi = isHindi,
            onSyncFromLink = { url, tabs ->
                viewModel.syncGoogleSheetFromUrl(context, url, tabs)
            },
            onSyncFromFileUri = { uri ->
                viewModel.syncGoogleSheetFromUri(context, uri)
            },
            onSyncBundledSheet = {
                viewModel.syncBundledTirupatiSheet(context)
            },
            onDismiss = { viewModel.closeGoogleSheetSyncDialog() }
        )
    }

    // Delete Confirmation Dialog
    val toDelete = readingToDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { readingToDelete = null },
            title = {
                Text(
                    text = if (isHindi) "रीडिंग हटाएं?" else "Delete Reading?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isHindi) "क्या आप निश्चित रूप से ${toDelete.readingDate} की रीडिंग (${toDelete.kwh} kWh) हटाना चाहते हैं?"
                           else "Are you sure you want to delete reading from ${toDelete.readingDate} (${toDelete.kwh} kWh)?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReading(toDelete)
                        readingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isHindi) "हटाएं (Delete)" else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { readingToDelete = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
    }
}
