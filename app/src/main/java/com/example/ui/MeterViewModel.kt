package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import com.example.data.remote.CloudCustomer
import com.example.data.remote.CloudReading
import com.example.data.remote.TeamSyncRepository
import com.example.data.remote.TeamWorkspace
import com.example.data.repository.MeterRepository
import com.example.util.AppBrandingConfig
import com.example.util.AppBrandingManager
import com.example.util.AppIconStyle
import com.example.util.AppVisualTheme
import com.example.util.BackupParsedData
import com.example.util.BackupRestoreHelper
import com.example.util.DriveBackupConfig
import com.example.util.DriveBackupFrequency
import com.example.util.DriveBackupSnapshot
import com.example.util.ExcelBackupRestoreHelper
import com.example.util.ExcelExportGenerator
import com.example.util.FileShareHelper
import com.example.util.GoogleDriveBackupManager
import com.example.util.GoogleSheetSyncConfig
import com.example.util.GoogleSheetSyncHelper
import com.example.util.ParsedSheetTab
import com.example.util.PdfReportGenerator
import com.example.util.PowerFactorCalculator
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilter(val labelEn: String, val labelHi: String) {
    ALL("All Time", "सभी"),
    THIS_MONTH("This Month", "इस माह"),
    LAST_7_DAYS("Last 7 Days", "पिछले 7 दिन")
}

data class MeterUiState(
    val currentTab: Int = 0, // 0: Dashboard, 1: Past Readings, 2: Customers, 3: Export & Restore Setting
    val customers: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val readings: List<MeterReading> = emptyList(),
    val latestReading: MeterReading? = null,
    val dateFilter: DateFilter = DateFilter.ALL,
    val isSortDescending: Boolean = true,
    val searchQuery: String = "",
    val showAddReadingDialog: Boolean = false,
    val isInputScreenOpen: Boolean = false,
    val readingToEdit: MeterReading? = null,
    val showCustomerDialog: Boolean = false,
    val customerToEdit: Customer? = null,
    val showCustomerListDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showPfGuideDialog: Boolean = false,
    val showKvarCalculatorDialog: Boolean = false,
    val kvarSelectedReading: MeterReading? = null,
    val showRestoreConfirmDialog: Boolean = false,
    val pendingRestoreData: BackupParsedData? = null,
    val showDriveRestoreDialog: Boolean = false,
    val driveSnapshots: List<DriveBackupSnapshot> = emptyList(),
    val isHindi: Boolean = true,
    val statusMessage: String? = null,
    val dbStats: Triple<Int, Int, Double>? = null,
    val driveConfig: DriveBackupConfig = DriveBackupConfig(),
    val isDriveSyncing: Boolean = false,
    val activeWorkspace: TeamWorkspace? = null,
    val userWorkspaces: List<TeamWorkspace> = emptyList(),
    val cloudCustomers: List<CloudCustomer> = emptyList(),
    val cloudReadings: List<CloudReading> = emptyList(),
    val showTeamSyncDialog: Boolean = false,
    val isTeamSyncLoading: Boolean = false,
    val currentUserName: String = "",
    val currentUserEmail: String = "",
    val brandingConfig: AppBrandingConfig = AppBrandingConfig(),
    val showBrandingDialog: Boolean = false,
    val sheetSyncConfig: GoogleSheetSyncConfig = GoogleSheetSyncConfig(),
    val showGoogleSheetSyncDialog: Boolean = false,
    val isSheetSyncing: Boolean = false
)

class MeterViewModel(
    private val repository: MeterRepository,
    private val driveManager: GoogleDriveBackupManager,
    private val teamSyncRepository: TeamSyncRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MeterUiState(
            driveConfig = driveManager.getConfig(),
            driveSnapshots = driveManager.getAvailableDriveSnapshots()
        )
    )
    val uiState: StateFlow<MeterUiState> = _uiState.asStateFlow()

    private var readingsJob: Job? = null

    init {
        // Observe customers
        viewModelScope.launch {
            repository.allCustomers.collectLatest { customerList ->
                val currentSelected = _uiState.value.selectedCustomer
                val newSelected = when {
                    customerList.isEmpty() -> null
                    currentSelected != null && customerList.any { it.id == currentSelected.id } -> {
                        customerList.first { it.id == currentSelected.id }
                    }
                    else -> customerList.first()
                }

                _uiState.update {
                    it.copy(
                        customers = customerList,
                        selectedCustomer = newSelected
                    )
                }

                if (newSelected != null) {
                    observeReadingsForCustomer(newSelected.id)
                } else {
                    readingsJob?.cancel()
                    _uiState.update {
                        it.copy(
                            readings = emptyList(),
                            latestReading = null
                        )
                    }
                }
                refreshDbStats()

                // Ensure an initial Google Drive Cloud Backup snapshot exists when data is present
                if (customerList.isNotEmpty() && driveManager.getAvailableDriveSnapshots().isEmpty()) {
                    val allReadings = repository.getAllReadingsList()
                    if (allReadings.isNotEmpty()) {
                        driveManager.createAutoCloudSnapshot(
                            customers = customerList,
                            readings = allReadings,
                            triggerType = "Initial Auto Cloud Backup"
                        )
                        refreshDriveConfig()
                    }
                }
            }
        }

        initializeColleagueCloudSync()
    }

    private fun initializeColleagueCloudSync() {
        val syncRepo = teamSyncRepository ?: return
        val currentUser = try {
            Firebase.auth.currentUser
        } catch (_: Exception) {
            null
        } ?: return

        val displayName = currentUser.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "वैभव (Team Member)"
        val email = currentUser.email ?: ""

        _uiState.update {
            it.copy(
                currentUserName = displayName,
                currentUserEmail = email
            )
        }

        // 1. Observe user's shared workspaces (or bootstrap default workspace)
        viewModelScope.launch {
            try {
                syncRepo.observeUserWorkspaces().collectLatest { workspaces ->
                    if (workspaces.isEmpty()) {
                        val defaultCode = syncRepo.generateDefaultInviteCode(currentUser.uid)
                        val created = syncRepo.createOrGetWorkspace(
                            inviteCode = defaultCode,
                            teamName = "$displayName टीम",
                            userDisplayName = displayName
                        ).getOrNull()
                        if (created != null) {
                            _uiState.update {
                                it.copy(
                                    activeWorkspace = created,
                                    userWorkspaces = listOf(created)
                                )
                            }
                            pushAllLocalRecordsToWorkspace(created, displayName)
                        }
                    } else {
                        val currentActive = _uiState.value.activeWorkspace
                        val selectedWs = if (currentActive != null) {
                            workspaces.find { it.inviteCode == currentActive.inviteCode } ?: workspaces.first()
                        } else {
                            workspaces.first()
                        }
                        _uiState.update {
                            it.copy(
                                activeWorkspace = selectedWs,
                                userWorkspaces = workspaces
                            )
                        }
                        syncRepo.propagateWorkspaceMembersToExistingRecords(selectedWs)
                    }
                }
            } catch (_: Exception) {
                // Handled by FirestoreErrorHandler
            }
        }

        // 2. Observe shared customers from colleagues in real time and merge into local Room DB
        viewModelScope.launch {
            try {
                syncRepo.observeSharedCustomers().collectLatest { cloudCustomers ->
                    _uiState.update { it.copy(cloudCustomers = cloudCustomers) }
                    mergeCloudCustomersIntoLocalDb(cloudCustomers)
                }
            } catch (_: Exception) {
                // Handled by FirestoreErrorHandler
            }
        }

        // 3. Observe shared meter readings from colleagues in real time and merge into local Room DB
        viewModelScope.launch {
            try {
                syncRepo.observeSharedReadings().collectLatest { cloudReadings ->
                    _uiState.update { it.copy(cloudReadings = cloudReadings) }
                    mergeCloudReadingsIntoLocalDb(cloudReadings, currentUser.uid)
                }
            } catch (_: Exception) {
                // Handled by FirestoreErrorHandler
            }
        }
    }

    private fun isGenericMeterString(meter: String): Boolean {
        val m = meter.trim().uppercase()
        return m.isEmpty() ||
            m == "UNKNOWN" ||
            m == "N/A" ||
            m == "MTR-N/A" ||
            m.startsWith("SHEET-MTR-") ||
            Regex("^MTR-\\d+$").matches(m)
    }

    private fun isMatchingCustomer(
        local: Customer,
        remoteName: String,
        remoteMeter: String
    ): Boolean {
        val lName = local.name.trim()
        val rName = remoteName.trim()
        if (!lName.equals(rName, ignoreCase = true)) return false

        val lMeter = local.meterNumber.trim()
        val rMeter = remoteMeter.trim()
        return lMeter.equals(rMeter, ignoreCase = true) ||
            isGenericMeterString(lMeter) ||
            isGenericMeterString(rMeter)
    }

    private suspend fun mergeCloudCustomersIntoLocalDb(cloudCustomers: List<CloudCustomer>) {
        if (cloudCustomers.isEmpty()) return
        val localCustomers = repository.getAllCustomersList().toMutableList()
        for (cc in cloudCustomers) {
            val match = localCustomers.find { isMatchingCustomer(it, cc.name, cc.meterNumber) }
            if (match == null) {
                val newCust = Customer(
                    name = cc.name.trim(),
                    address = cc.address.trim(),
                    meterNumber = cc.meterNumber.trim(),
                    consumerNumber = cc.consumerNumber.trim(),
                    phoneNumber = cc.phoneNumber.trim(),
                    connectionType = cc.connectionType,
                    sanctionedLoad = cc.sanctionedLoad.trim()
                )
                val newId = repository.insertCustomer(newCust)
                localCustomers.add(newCust.copy(id = newId))
            }
        }
    }

    private suspend fun mergeCloudReadingsIntoLocalDb(
        cloudReadings: List<CloudReading>,
        currentUid: String
    ) {
        if (cloudReadings.isEmpty()) return
        var localCustomers = repository.getAllCustomersList()
        val allLocalReadings = repository.getAllReadingsList()
        var newlySyncedFromColleague = 0
        var lastColleagueName = ""

        for (cr in cloudReadings) {
            var customer = localCustomers.find {
                isMatchingCustomer(it, cr.customerName, cr.customerMeterNumber)
            }
            if (customer == null) {
                val newCustId = repository.insertCustomer(
                    Customer(
                        name = cr.customerName.trim(),
                        address = "",
                        meterNumber = cr.customerMeterNumber.trim()
                    )
                )
                localCustomers = repository.getAllCustomersList()
                customer = localCustomers.find { it.id == newCustId }
            }
            val targetCustomer = customer ?: continue

            val alreadyExists = allLocalReadings.any { lr ->
                lr.customerId == targetCustomer.id &&
                    (lr.timestamp == cr.readingTimestamp ||
                        (lr.readingDate == cr.readingDate && kotlin.math.abs(lr.kwh - cr.kwh) < 0.01))
            }

            if (!alreadyExists) {
                val colleagueBadge = "[सहकारी सिंक: ${cr.createdByName}]"
                val mergedRemarks = if (cr.remarks.contains(cr.createdByName)) {
                    cr.remarks
                } else if (cr.remarks.isBlank()) {
                    colleagueBadge
                } else {
                    "${cr.remarks} $colleagueBadge"
                }

                repository.insertReading(
                    MeterReading(
                        customerId = targetCustomer.id,
                        readingDate = cr.readingDate,
                        readingTime = cr.readingTime,
                        timestamp = cr.readingTimestamp,
                        kwh = cr.kwh,
                        kvarh = cr.kvarh,
                        kvah = cr.kvah,
                        powerFactor = cr.powerFactor,
                        isPfAutoCalculated = cr.isPfAutoCalculated,
                        previousKwh = cr.previousKwh.takeIf { it > 0.0 },
                        unitsConsumed = cr.unitsConsumed.takeIf { it > 0.0 },
                        remarks = mergedRemarks
                    )
                )
                if (cr.ownerId != currentUid) {
                    newlySyncedFromColleague++
                    lastColleagueName = cr.createdByName
                }
            }
        }

        if (newlySyncedFromColleague > 0) {
            refreshDbStats()
            _uiState.update {
                it.copy(
                    statusMessage = "सहकारी '$lastColleagueName' ने केलेली $newlySyncedFromColleague नवीन रीडिंग एन्ट्री आपोआप सिंक झाली!"
                )
            }
        }
    }

    private suspend fun pushAllLocalRecordsToWorkspace(
        workspace: TeamWorkspace,
        authorName: String
    ) {
        val syncRepo = teamSyncRepository ?: return
        val customers = repository.getAllCustomersList()
        val readings = repository.getAllReadingsList()
        val customerMap = customers.associateBy { it.id }

        for (cust in customers) {
            syncRepo.pushCustomerToCloud(workspace, cust, authorName)
        }
        for (read in readings) {
            val cust = customerMap[read.customerId] ?: continue
            syncRepo.pushReadingToCloud(workspace, cust, read, authorName)
        }
    }

    fun openTeamSyncDialog() {
        _uiState.update { it.copy(showTeamSyncDialog = true) }
    }

    fun closeTeamSyncDialog() {
        _uiState.update { it.copy(showTeamSyncDialog = false) }
    }

    fun loadBranding(context: Context) {
        val cfg = AppBrandingManager(context).getBranding()
        _uiState.update { it.copy(brandingConfig = cfg) }
    }

    fun openBrandingDialog() {
        _uiState.update { it.copy(showBrandingDialog = true) }
    }

    fun closeBrandingDialog() {
        _uiState.update { it.copy(showBrandingDialog = false) }
    }

    fun updateAppBranding(
        context: Context,
        appName: String,
        customSubtitle: String,
        iconStyle: AppIconStyle,
        visualTheme: AppVisualTheme = _uiState.value.brandingConfig.visualTheme
    ) {
        val saved = AppBrandingManager(context).saveBranding(appName, customSubtitle, iconStyle, visualTheme)
        _uiState.update {
            it.copy(
                brandingConfig = saved,
                showBrandingDialog = false,
                statusMessage = "अॅपचा लूक '${saved.visualTheme.labelMr}', नाव '${saved.appName}' आणि आयकॉन अपडेट झाला!"
            )
        }
    }

    fun selectVisualTheme(context: Context, visualTheme: AppVisualTheme) {
        val current = _uiState.value.brandingConfig
        val saved = AppBrandingManager(context).saveBranding(
            appName = current.appName,
            customSubtitle = current.customSubtitle,
            iconStyle = current.iconStyle,
            visualTheme = visualTheme
        )
        _uiState.update {
            it.copy(
                brandingConfig = saved,
                statusMessage = "अॅपचा लूक '${visualTheme.labelMr}' लागू केला!"
            )
        }
    }

    fun toggleDarkMode(context: Context) {
        val current = _uiState.value.brandingConfig
        val nextTheme = if (current.visualTheme.isDark) {
            AppVisualTheme.EMERALD_ENERGY
        } else {
            AppVisualTheme.CYBER_DARK
        }
        selectVisualTheme(context, nextTheme)
    }

    // --- GOOGLE SHEET CUSTOMER-WISE TAB SYNC ---

    fun initializeSheetSyncAndImportBundled(context: Context) {
        val cfg = GoogleSheetSyncHelper.getConfig(context)
        _uiState.update { it.copy(sheetSyncConfig = cfg) }

        viewModelScope.launch {
            normalizeExistingReadingsAutoPfAndRemarks()
        }

        // Automatically sync user's linked Google Sheet when opening the app so new entries appear immediately
        if (cfg.sheetUrl.isNotBlank() && cfg.autoSyncOnOpen) {
            syncGoogleSheetFromUrl(
                context = context,
                sheetUrl = cfg.sheetUrl,
                tabNamesCsv = cfg.tabNamesCsv,
                isSilentAutoSync = true
            )
        }
    }

    private suspend fun normalizeExistingReadingsAutoPfAndRemarks() {
        val allReadings = repository.getAllReadingsList()
        for (r in allReadings) {
            val autoPf = PowerFactorCalculator.calculateAutoPf(r.kwh, r.kvarh, r.kvah)
            // Clean up old parenthetical normalized PF from remarks e.g. "PF: 513 (0.513)" -> "PF: 513"
            val cleanedRemarks = r.remarks
                .replace(Regex("\\s*\\(Lead\\s+\\d+\\.\\d+\\)"), "")
                .replace(Regex("\\s*\\(\\d+\\.\\d+\\)"), "")
                .trim()
            if (!r.isPfAutoCalculated ||
                kotlin.math.abs(r.powerFactor - autoPf) >= 0.001 ||
                cleanedRemarks != r.remarks
            ) {
                repository.updateReading(
                    r.copy(
                        powerFactor = autoPf,
                        isPfAutoCalculated = true,
                        remarks = cleanedRemarks
                    )
                )
            }
        }
    }

    fun openGoogleSheetSyncDialog() {
        _uiState.update { it.copy(showGoogleSheetSyncDialog = true) }
    }

    fun closeGoogleSheetSyncDialog() {
        _uiState.update { it.copy(showGoogleSheetSyncDialog = false) }
    }

    fun syncSavedGoogleSheetNow(context: Context) {
        val cfg = _uiState.value.sheetSyncConfig
        if (cfg.sheetUrl.isBlank()) {
            openGoogleSheetSyncDialog()
        } else {
            syncGoogleSheetFromUrl(
                context = context,
                sheetUrl = cfg.sheetUrl,
                tabNamesCsv = cfg.tabNamesCsv,
                isSilentAutoSync = false
            )
        }
    }

    fun syncGoogleSheetFromUrl(
        context: Context,
        sheetUrl: String,
        tabNamesCsv: String,
        isSilentAutoSync: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSheetSyncing = true) }
            try {
                val result = GoogleSheetSyncHelper.fetchAndParseFromSheetUrl(sheetUrl, tabNamesCsv)
                val (custCount, readCount) = mergeSheetTabsIntoLocalDb(
                    tabs = result.tabs,
                    selectImportedCustomer = _uiState.value.selectedCustomer == null || !isSilentAutoSync
                )
                val totalSheetRows = result.tabs.sumOf { it.readings.size }
                val latestSheetEntry = result.tabs.flatMap { it.readings }.maxByOrNull { it.timestamp }
                val latestDateLabel = latestSheetEntry?.readingDate ?: "-"
                val updatedCfg = GoogleSheetSyncHelper.saveConfig(
                    context = context,
                    sheetUrl = sheetUrl,
                    tabNamesCsv = tabNamesCsv,
                    tabsCount = result.tabs.size,
                    readingsCount = totalSheetRows
                )
                val msg = when {
                    isSilentAutoSync && readCount == 0 && custCount == 0 -> null
                    readCount == 0 && custCount == 0 -> {
                        "शीट सिंक पूर्ण (${result.tabs.size} टॅब, $totalSheetRows नोंदी • शेवटची तारीख: $latestDateLabel). कोणतीही नवीन एन्ट्री आढळली नाही — Google Sheet मध्ये नवीन एन्ट्री सेव्ह (✓) असल्याची व 'Anyone with the link' शेअरिंगची खात्री करा."
                    }
                    else -> {
                        "Google Sheet सिंक पूर्ण! ${result.tabs.size} कस्टमर टॅब, $readCount नवीन/अपडेटेड एन्ट्री सिंक झाल्या (एकूण $totalSheetRows नोंदी • शेवटची: $latestDateLabel)."
                    }
                }
                _uiState.update {
                    it.copy(
                        isSheetSyncing = false,
                        showGoogleSheetSyncDialog = false,
                        sheetSyncConfig = updatedCfg,
                        statusMessage = msg ?: it.statusMessage
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isSheetSyncing = false,
                        statusMessage = if (isSilentAutoSync) it.statusMessage else "Google Sheet सिंक त्रुटी: ${e.message}"
                    )
                }
            }
        }
    }

    fun syncGoogleSheetFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSheetSyncing = true) }
            try {
                val result = GoogleSheetSyncHelper.parsePickedSheetUri(context, uri)
                val (_, readCount) = mergeSheetTabsIntoLocalDb(
                    tabs = result.tabs,
                    selectImportedCustomer = true
                )
                val totalSheetRows = result.tabs.sumOf { it.readings.size }
                val existingCfg = _uiState.value.sheetSyncConfig
                val updatedCfg = GoogleSheetSyncHelper.saveConfig(
                    context = context,
                    sheetUrl = existingCfg.sheetUrl,
                    tabNamesCsv = existingCfg.tabNamesCsv,
                    tabsCount = result.tabs.size,
                    readingsCount = totalSheetRows
                )
                _uiState.update {
                    it.copy(
                        isSheetSyncing = false,
                        showGoogleSheetSyncDialog = false,
                        sheetSyncConfig = updatedCfg,
                        statusMessage = "शीट फाईल सिंक पूर्ण! ${result.tabs.size} कस्टमर टॅब, $readCount नवीन एन्ट्री जोडल्या."
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isSheetSyncing = false,
                        statusMessage = "शीट फाईल त्रुटी: ${e.message}"
                    )
                }
            }
        }
    }

    fun syncBundledTirupatiSheet(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSheetSyncing = true) }
            try {
                val result = GoogleSheetSyncHelper.loadBundledSheetTabs(context)
                val (_, readCount) = mergeSheetTabsIntoLocalDb(
                    tabs = result.tabs,
                    selectImportedCustomer = true
                )
                GoogleSheetSyncHelper.markBundledSheetImported(context)
                val totalSheetRows = result.tabs.sumOf { it.readings.size }
                _uiState.update {
                    it.copy(
                        isSheetSyncing = false,
                        showGoogleSheetSyncDialog = false,
                        statusMessage = "TIRUPATI CRUSHER शीट सिंक पूर्ण! $readCount नवीन एन्ट्री जोडल्या (एकूण $totalSheetRows नोंदी)."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSheetSyncing = false,
                        statusMessage = "शीट सिंक त्रुटी: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun mergeSheetTabsIntoLocalDb(
        tabs: List<ParsedSheetTab>,
        selectImportedCustomer: Boolean
    ): Pair<Int, Int> {
        var customersCreated = 0
        var readingsInsertedOrUpdated = 0
        var firstImportedCustomer: Customer? = null

        val activeWs = _uiState.value.activeWorkspace
        val authorName = _uiState.value.currentUserName.ifBlank { "Sheet Sync" }

        for (tab in tabs) {
            val allLocalCustomers = repository.getAllCustomersList()
            val currentSelBefore = _uiState.value.selectedCustomer
            val existingCustomer = allLocalCustomers.find { local ->
                local.name.trim().equals(tab.customer.name.trim(), ignoreCase = true)
            } ?: if (tabs.size == 1 && tab.customer.name.equals("Sheet Customer", ignoreCase = true)) {
                currentSelBefore ?: allLocalCustomers.singleOrNull()
            } else {
                null
            }

            val targetCustomer = if (existingCustomer != null) {
                existingCustomer
            } else {
                val newId = repository.insertCustomer(tab.customer.copy(id = 0L))
                customersCreated++
                val inserted = tab.customer.copy(id = newId)
                if (activeWs != null && teamSyncRepository != null) {
                    runCatching { teamSyncRepository?.pushCustomerToCloud(activeWs, inserted, authorName) }
                }
                inserted
            }

            if (firstImportedCustomer == null) {
                firstImportedCustomer = targetCustomer
            }

            val existingReadings = repository.getReadingsForCustomerList(targetCustomer.id).toMutableList()
            val matchedLocalIds = mutableSetOf<Long>()
            var lastKnownKwh: Double? = existingReadings.maxByOrNull { it.timestamp }?.kwh

            for (sheetReading in tab.readings.sortedBy { it.timestamp }) {
                val duplicate = existingReadings.find { lr ->
                    lr.id !in matchedLocalIds &&
                        lr.readingDate.equals(sheetReading.readingDate, ignoreCase = true) &&
                        kotlin.math.abs(lr.kwh - sheetReading.kwh) < 0.01
                } ?: existingReadings.find { lr ->
                    lr.id !in matchedLocalIds &&
                        lr.readingDate.equals(sheetReading.readingDate, ignoreCase = true) &&
                        sheetReading.readingTime != "08:30:00 AM" &&
                        lr.readingTime.equals(sheetReading.readingTime, ignoreCase = true) &&
                        tab.readings.none { other ->
                            other !== sheetReading &&
                                other.readingDate.equals(lr.readingDate, ignoreCase = true) &&
                                kotlin.math.abs(other.kwh - lr.kwh) < 0.01
                        }
                }

                if (duplicate != null) {
                    matchedLocalIds.add(duplicate.id)
                    // Check if any value (kWh, kVArh, kVAh, PF, isPfAutoCalculated, remarks) was updated on the Google Sheet
                    val hasChanged = kotlin.math.abs(duplicate.kwh - sheetReading.kwh) >= 0.01 ||
                        kotlin.math.abs(duplicate.kvarh - sheetReading.kvarh) >= 0.01 ||
                        kotlin.math.abs(duplicate.kvah - sheetReading.kvah) >= 0.01 ||
                        kotlin.math.abs(duplicate.powerFactor - sheetReading.powerFactor) >= 0.001 ||
                        duplicate.isPfAutoCalculated != sheetReading.isPfAutoCalculated ||
                        (sheetReading.remarks.isNotBlank() && duplicate.remarks != sheetReading.remarks)

                    if (hasChanged) {
                        val updated = duplicate.copy(
                            readingTime = sheetReading.readingTime,
                            kwh = sheetReading.kwh,
                            kvarh = sheetReading.kvarh,
                            kvah = sheetReading.kvah,
                            powerFactor = sheetReading.powerFactor,
                            isPfAutoCalculated = sheetReading.isPfAutoCalculated,
                            previousKwh = sheetReading.previousKwh ?: duplicate.previousKwh,
                            unitsConsumed = sheetReading.unitsConsumed ?: duplicate.unitsConsumed,
                            remarks = sheetReading.remarks.ifBlank { duplicate.remarks }
                        )
                        repository.updateReading(updated)
                        readingsInsertedOrUpdated++
                    }
                    lastKnownKwh = maxOf(lastKnownKwh ?: sheetReading.kwh, sheetReading.kwh)
                    continue
                }

                val prevKwh = sheetReading.previousKwh ?: lastKnownKwh
                val units = sheetReading.unitsConsumed ?: if (prevKwh != null && sheetReading.kwh >= prevKwh) {
                    ((sheetReading.kwh - prevKwh) * 100.0).toInt() / 100.0
                } else null

                val toInsert = sheetReading.copy(
                    id = 0L,
                    customerId = targetCustomer.id,
                    previousKwh = prevKwh,
                    unitsConsumed = units
                )
                val newReadId = repository.insertReading(toInsert)
                matchedLocalIds.add(newReadId)
                existingReadings.add(toInsert.copy(id = newReadId))
                readingsInsertedOrUpdated++
                lastKnownKwh = toInsert.kwh

                if (activeWs != null && teamSyncRepository != null) {
                    runCatching {
                        teamSyncRepository?.pushReadingToCloud(
                            activeWs,
                            targetCustomer,
                            toInsert.copy(id = newReadId),
                            authorName
                        )
                    }
                }
            }
        }

        val currentSel = _uiState.value.selectedCustomer
        val updatedCustomers = repository.getAllCustomersList()
        val currentStillExists = currentSel != null && updatedCustomers.any { it.id == currentSel.id }
        if (currentStillExists && currentSel != null) {
            observeReadingsForCustomer(currentSel.id)
        } else if (selectImportedCustomer && firstImportedCustomer != null) {
            selectCustomer(firstImportedCustomer)
        } else if (firstImportedCustomer != null && currentSel == null) {
            selectCustomer(firstImportedCustomer)
        }
        refreshDbStats()
        return Pair(customersCreated, readingsInsertedOrUpdated)
    }

    fun joinColleagueWorkspace(inviteCode: String, colleagueName: String) {
        val syncRepo = teamSyncRepository
        if (syncRepo == null) {
            _uiState.update {
                it.copy(statusMessage = "सहकारी लाईव्ह सिंकसाठी कृपया आधी Google खात्याने लॉगिन करा (Sign in with Google)")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTeamSyncLoading = true,
                    currentUserName = colleagueName.ifBlank { it.currentUserName }
                )
            }
            val result = syncRepo.joinWorkspaceByCode(inviteCode, colleagueName)
            result.onSuccess { joinedWs ->
                pushAllLocalRecordsToWorkspace(joinedWs, colleagueName)
                syncRepo.propagateWorkspaceMembersToExistingRecords(joinedWs)
                _uiState.update {
                    it.copy(
                        isTeamSyncLoading = false,
                        activeWorkspace = joinedWs,
                        statusMessage = "टीम '${joinedWs.inviteCode}' सोबत जोडले गेले! सहकाऱ्यांच्या एन्ट्री आता लाईव्ह सिंक होतील."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isTeamSyncLoading = false,
                        statusMessage = "टीम जॉईन त्रुटी: ${err.cause?.message ?: err.message}"
                    )
                }
            }
        }
    }

    fun createCustomTeamWorkspace(customCode: String, teamName: String, userName: String) {
        val syncRepo = teamSyncRepository
        if (syncRepo == null) {
            _uiState.update {
                it.copy(statusMessage = "नवीन टीम कोड तयार करण्यासाठी आधी Google खात्याने लॉगिन करा (Sign in with Google)")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTeamSyncLoading = true,
                    currentUserName = userName.ifBlank { it.currentUserName }
                )
            }
            val result = syncRepo.createOrGetWorkspace(
                inviteCode = customCode,
                teamName = teamName,
                userDisplayName = userName
            )
            result.onSuccess { ws ->
                pushAllLocalRecordsToWorkspace(ws, userName)
                _uiState.update {
                    it.copy(
                        isTeamSyncLoading = false,
                        activeWorkspace = ws,
                        statusMessage = "नवीन टीम कोड '${ws.inviteCode}' तयार झाला! सहकाऱ्याला हा कोड शेअर करा."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isTeamSyncLoading = false,
                        statusMessage = "टीम तयार करताना त्रुटी: ${err.cause?.message ?: err.message}"
                    )
                }
            }
        }
    }

    fun syncAllToColleagueCloudNow() {
        val syncRepo = teamSyncRepository
        if (syncRepo == null) {
            _uiState.update {
                it.copy(statusMessage = "क्लाउड सिंकसाठी कृपया आधी Google खात्याने लॉगिन करा (Sign in with Google)")
            }
            return
        }
        val ws = _uiState.value.activeWorkspace ?: return
        val author = _uiState.value.currentUserName.ifBlank { "Team Member" }
        viewModelScope.launch {
            _uiState.update { it.copy(isTeamSyncLoading = true) }
            try {
                syncRepo.propagateWorkspaceMembersToExistingRecords(ws)
                pushAllLocalRecordsToWorkspace(ws, author)
                _uiState.update {
                    it.copy(
                        isTeamSyncLoading = false,
                        statusMessage = "सर्व ग्राहक व मीटर रीडिंग्स टीम '${ws.inviteCode}' वर लाईव्ह सिंक झाले!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTeamSyncLoading = false,
                        statusMessage = "क्लाउड सिंक त्रुटी: ${e.message}"
                    )
                }
            }
        }
    }

    fun setCurrentTab(tab: Int) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == 2 || tab == 3) {
            refreshDbStats()
            refreshDriveConfig()
        }
    }

    fun refreshDriveConfig() {
        val config = driveManager.getConfig()
        val snapshots = driveManager.getAvailableDriveSnapshots()
        _uiState.update {
            it.copy(
                driveConfig = config,
                driveSnapshots = snapshots
            )
        }
    }

    fun updateDriveConfig(
        enabled: Boolean,
        frequency: DriveBackupFrequency,
        wifiOnly: Boolean,
        includePdf: Boolean,
        email: String = "vaibhavghag1990@gmail.com"
    ) {
        driveManager.updateConfig(enabled, frequency, wifiOnly, includePdf, email)
        refreshDriveConfig()
        _uiState.update {
            it.copy(
                statusMessage = if (enabled) "Google Drive ऑटो बॅकअप सुरू आहे (Drive Auto Backup Enabled)"
                else "Google Drive ऑटो बॅकअप बंद करण्यात आला"
            )
        }
    }

    fun syncGoogleDriveNow(context: Context, openExternalDriveApp: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDriveSyncing = true) }
            try {
                val customers = repository.getAllCustomersList()
                val readings = repository.getAllReadingsList()
                if (customers.isEmpty() && readings.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isDriveSyncing = false,
                            statusMessage = "Google Drive वर बॅकअपसाठी कोणताही डेटा उपलब्ध नाही"
                        )
                    }
                    return@launch
                }

                val snapshot = driveManager.createCloudBackupSnapshot(
                    context = context,
                    customers = customers,
                    readings = readings,
                    triggerType = "Manual Google Drive Backup"
                )
                if (openExternalDriveApp) {
                    driveManager.uploadToGoogleDrive(
                        context = context,
                        file = File(snapshot.filePath),
                        title = "MeterRead Pro - Google Drive Backup"
                    )
                }
                refreshDriveConfig()
                _uiState.update {
                    it.copy(
                        isDriveSyncing = false,
                        statusMessage = "Google Drive बॅकअप यशस्वी! (${snapshot.customerCount} ग्राहक, ${snapshot.readingCount} रीडिंग्स क्लाउडवर सुरक्षित)"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isDriveSyncing = false,
                        statusMessage = "Drive Sync Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun uploadLatestBackupToDriveApp(context: Context, asExcel: Boolean = false) {
        viewModelScope.launch {
            try {
                val customers = repository.getAllCustomersList()
                val readings = repository.getAllReadingsList()
                if (customers.isEmpty() && readings.isEmpty()) {
                    _uiState.update {
                        it.copy(statusMessage = "Google Drive वर अपलोड करण्यासाठी डेटा नाही")
                    }
                    return@launch
                }
                val snapshot = driveManager.createCloudBackupSnapshot(
                    context = context,
                    customers = customers,
                    readings = readings,
                    triggerType = if (asExcel) "Drive App Excel Upload" else "Drive App JSON Upload"
                )
                refreshDriveConfig()
                if (asExcel && snapshot.excelFilePath != null) {
                    driveManager.uploadToGoogleDrive(
                        context = context,
                        file = File(snapshot.excelFilePath),
                        title = "MeterRead Pro - Excel Backup",
                        mimeType = "text/comma-separated-values"
                    )
                } else {
                    driveManager.uploadToGoogleDrive(
                        context = context,
                        file = File(snapshot.filePath),
                        title = "MeterRead Pro - JSON Backup",
                        mimeType = "application/json"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(statusMessage = "Upload Error: ${e.message}") }
            }
        }
    }

    fun openDriveRestoreDialog() {
        refreshDriveConfig()
        _uiState.update { it.copy(showDriveRestoreDialog = true) }
    }

    fun closeDriveRestoreDialog() {
        _uiState.update { it.copy(showDriveRestoreDialog = false) }
    }

    fun restoreFromDriveSnapshot(snapshot: DriveBackupSnapshot) {
        viewModelScope.launch {
            try {
                val parsed = driveManager.readSnapshotData(snapshot)
                _uiState.update {
                    it.copy(
                        showDriveRestoreDialog = false,
                        pendingRestoreData = parsed,
                        showRestoreConfirmDialog = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(statusMessage = "Google Drive रिस्टोर त्रुटी (Restore Error): ${e.message}")
                }
            }
        }
    }

    fun restoreLatestFromGoogleDrive() {
        viewModelScope.launch {
            try {
                val parsed = driveManager.getLatestSnapshotData()
                if (parsed == null) {
                    _uiState.update {
                        it.copy(
                            statusMessage = "Google Drive वर कोणताही बॅकअप सापडला नाही. आधी 'बॅकअप करा' वर क्लिक करा किंवा फाईल निवडा."
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        showDriveRestoreDialog = false,
                        pendingRestoreData = parsed,
                        showRestoreConfirmDialog = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(statusMessage = "Google Drive Restore Error: ${e.message}")
                }
            }
        }
    }

    fun deleteDriveSnapshot(snapshot: DriveBackupSnapshot) {
        viewModelScope.launch {
            driveManager.deleteSnapshot(snapshot)
            refreshDriveConfig()
            _uiState.update {
                it.copy(statusMessage = "Google Drive बॅकअप नोंद हटवली (${snapshot.formattedDate})")
            }
        }
    }

    fun shareDriveSnapshotToApp(context: Context, snapshot: DriveBackupSnapshot, asExcel: Boolean = false) {
        val targetFile = if (asExcel && snapshot.excelFilePath != null) {
            File(snapshot.excelFilePath)
        } else {
            File(snapshot.filePath)
        }
        val mime = if (asExcel && snapshot.excelFilePath != null) "text/comma-separated-values" else "application/json"
        driveManager.uploadToGoogleDrive(
            context = context,
            file = targetFile,
            title = "MeterRead Pro Backup - ${snapshot.formattedDate}",
            mimeType = mime
        )
    }

    fun refreshDbStats() {
        viewModelScope.launch {
            val stats = repository.getTotalStats()
            _uiState.update { it.copy(dbStats = stats) }
        }
    }

    fun selectCustomer(customer: Customer) {
        _uiState.update { it.copy(selectedCustomer = customer, showCustomerListDialog = false) }
        observeReadingsForCustomer(customer.id)
    }

    private fun observeReadingsForCustomer(customerId: Long) {
        readingsJob?.cancel()
        readingsJob = viewModelScope.launch {
            repository.getReadingsForCustomer(customerId).collectLatest { readingList ->
                _uiState.update {
                    it.copy(
                        readings = readingList,
                        latestReading = readingList.firstOrNull()
                    )
                }
                refreshDbStats()
            }
        }
    }

    fun setDateFilter(filter: DateFilter) {
        _uiState.update { it.copy(dateFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleLanguage() {
        _uiState.update { it.copy(isHindi = !it.isHindi) }
    }

    // Input Screen & Dialog controls
    fun openInputScreen(reading: MeterReading? = null) {
        _uiState.update {
            it.copy(
                isInputScreenOpen = true,
                showAddReadingDialog = false,
                readingToEdit = reading
            )
        }
    }

    fun closeInputScreen() {
        _uiState.update {
            it.copy(
                isInputScreenOpen = false,
                readingToEdit = null
            )
        }
    }

    fun openAddReadingDialog(reading: MeterReading? = null) {
        openInputScreen(reading)
    }

    fun closeAddReadingDialog() {
        closeInputScreen()
    }

    fun openCustomerDialog(customer: Customer? = null) {
        _uiState.update {
            it.copy(
                showCustomerDialog = true,
                customerToEdit = customer
            )
        }
    }

    fun closeCustomerDialog() {
        _uiState.update {
            it.copy(
                showCustomerDialog = false,
                customerToEdit = null
            )
        }
    }

    fun openCustomerListDialog() {
        _uiState.update { it.copy(showCustomerListDialog = true) }
    }

    fun closeCustomerListDialog() {
        _uiState.update { it.copy(showCustomerListDialog = false) }
    }

    fun openExportDialog() {
        _uiState.update { it.copy(showExportDialog = true) }
    }

    fun closeExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }

    fun openPfGuideDialog() {
        _uiState.update { it.copy(showPfGuideDialog = true) }
    }

    fun closePfGuideDialog() {
        _uiState.update { it.copy(showPfGuideDialog = false) }
    }

    fun openKvarCalculator(reading: MeterReading? = null) {
        _uiState.update {
            it.copy(
                showKvarCalculatorDialog = true,
                kvarSelectedReading = reading ?: it.latestReading
            )
        }
    }

    fun closeKvarCalculator() {
        _uiState.update {
            it.copy(
                showKvarCalculatorDialog = false,
                kvarSelectedReading = null
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    // Customer operations
    fun saveCustomer(
        name: String,
        address: String,
        meterNumber: String,
        consumerNumber: String,
        phoneNumber: String,
        connectionType: String,
        sanctionedLoad: String,
        existingId: Long = 0
    ) {
        viewModelScope.launch {
            val customer = Customer(
                id = existingId,
                name = name.trim(),
                address = address.trim(),
                meterNumber = meterNumber.trim(),
                consumerNumber = consumerNumber.trim(),
                phoneNumber = phoneNumber.trim(),
                connectionType = connectionType,
                sanctionedLoad = sanctionedLoad.trim()
            )

            val savedCustomer = if (existingId == 0L) {
                val newId = repository.insertCustomer(customer)
                val inserted = customer.copy(id = newId)
                selectCustomer(inserted)
                _uiState.update { it.copy(statusMessage = "ग्राहक सफलतापूर्वक सेव किया गया (Customer Saved)") }
                inserted
            } else {
                repository.updateCustomer(customer)
                _uiState.update { it.copy(statusMessage = "ग्राहक विवरण अपडेट किया गया (Customer Updated)") }
                customer
            }
            closeCustomerDialog()
            refreshDbStats()

            // Push to Colleague Team Workspace in Cloud Firestore
            val activeWs = _uiState.value.activeWorkspace
            val authorName = _uiState.value.currentUserName.ifBlank { "Team Member" }
            if (activeWs != null && teamSyncRepository != null) {
                teamSyncRepository.pushCustomerToCloud(activeWs, savedCustomer, authorName)
            }

            // Auto-backup to Google Drive after customer update if enabled
            val cfg = _uiState.value.driveConfig
            if (cfg.isAutoBackupEnabled) {
                val allCusts = repository.getAllCustomersList()
                val allReads = repository.getAllReadingsList()
                driveManager.createAutoCloudSnapshot(
                    customers = allCusts,
                    readings = allReads,
                    triggerType = "Auto Sync (Customer Saved)"
                )
                refreshDriveConfig()
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _uiState.update { it.copy(statusMessage = "ग्राहक काढून टाकला (Customer Deleted)") }
            refreshDbStats()
        }
    }

    fun clearAllCustomersAndReadings() {
        viewModelScope.launch {
            readingsJob?.cancel()
            repository.clearAllData()
            _uiState.update {
                it.copy(
                    customers = emptyList(),
                    selectedCustomer = null,
                    readings = emptyList(),
                    latestReading = null,
                    statusMessage = "सर्व ग्राहक व मीटर रीडिंग डेटा क्लीन केला! (All Data Cleared)"
                )
            }
            refreshDbStats()
        }
    }

    // Reading operations
    fun saveReading(
        readingDate: String,
        readingTime: String,
        timestamp: Long,
        kwh: Double,
        kvarh: Double,
        kvah: Double,
        powerFactor: Double,
        isPfAutoCalculated: Boolean,
        remarks: String,
        existingId: Long = 0
    ) {
        val currentCustomer = _uiState.value.selectedCustomer ?: return

        viewModelScope.launch {
            val latest = repository.getLatestReading(currentCustomer.id)
            val editingReading = _uiState.value.readingToEdit
            val prevKwh = if (existingId != 0L && editingReading?.customerId == currentCustomer.id) {
                editingReading.previousKwh
            } else {
                latest?.kwh
            }

            val consumed = if (prevKwh != null && kwh >= prevKwh) {
                kwh - prevKwh
            } else null

            val reading = MeterReading(
                id = existingId,
                customerId = currentCustomer.id,
                readingDate = readingDate,
                readingTime = readingTime,
                timestamp = timestamp,
                kwh = kwh,
                kvarh = kvarh,
                kvah = kvah,
                powerFactor = powerFactor,
                isPfAutoCalculated = isPfAutoCalculated,
                previousKwh = prevKwh,
                unitsConsumed = consumed,
                remarks = remarks.trim()
            )

            val savedReading = if (existingId == 0L) {
                val newId = repository.insertReading(reading)
                reading.copy(id = newId)
            } else {
                repository.updateReading(reading)
                reading
            }
            closeAddReadingDialog()
            refreshDbStats()

            // Push to Colleague Team Workspace in Cloud Firestore
            val activeWs = _uiState.value.activeWorkspace
            val authorName = _uiState.value.currentUserName.ifBlank { "Team Member" }
            if (activeWs != null && teamSyncRepository != null) {
                teamSyncRepository.pushCustomerToCloud(activeWs, currentCustomer, authorName)
                teamSyncRepository.pushReadingToCloud(activeWs, currentCustomer, savedReading, authorName)
            }

            // Auto-sync to Google Drive if configured
            val cfg = _uiState.value.driveConfig
            if (cfg.isAutoBackupEnabled && cfg.frequency != DriveBackupFrequency.MANUAL) {
                val allCusts = repository.getAllCustomersList()
                val allReads = repository.getAllReadingsList()
                driveManager.createAutoCloudSnapshot(
                    customers = allCusts,
                    readings = allReads,
                    triggerType = "Auto Sync (Reading Logged)"
                )
                refreshDriveConfig()
                _uiState.update {
                    it.copy(
                        statusMessage = if (existingId == 0L)
                            "रीडिंग जतन केली आणि सहकाऱ्यांसोबत लाईव्ह सिंक झाली!"
                        else
                            "रीडिंग अपडेट केली आणि सहकाऱ्यांसोबत सिंक झाली!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        statusMessage = if (existingId == 0L)
                            "मीटर रीडिंग जतन व सहकाऱ्यांसोबत सिंक झाली (Reading Synced)"
                        else
                            "रीडिंग अपडेट की गई (Reading Updated)"
                    )
                }
            }
        }
    }

    fun deleteReading(reading: MeterReading) {
        viewModelScope.launch {
            repository.deleteReading(reading)
            _uiState.update { it.copy(statusMessage = "रीडिंग हटाई गई (Reading Deleted)") }
            refreshDbStats()
        }
    }

    // Export operations
    fun exportPdf(
        context: Context,
        readingsToExport: List<MeterReading>? = null,
        customRangeLabel: String? = null
    ) {
        val customer = _uiState.value.selectedCustomer ?: return
        val list = readingsToExport ?: getFilteredReadings()
        if (list.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "एक्सपोर्ट के लिए कोई रीडिंग उपलब्ध नहीं है (No readings to export)") }
            return
        }

        try {
            val rangeLabel = customRangeLabel ?: when (_uiState.value.dateFilter) {
                DateFilter.ALL -> "All Recorded Entries"
                DateFilter.THIS_MONTH -> "This Month"
                DateFilter.LAST_7_DAYS -> "Last 7 Days"
            }
            val file = PdfReportGenerator.generatePdfReport(context, customer, list, rangeLabel)
            FileShareHelper.shareFile(
                context = context,
                file = file,
                mimeType = "application/pdf",
                title = "Share Meter Reading PDF Report"
            )
            _uiState.update { it.copy(showExportDialog = false, statusMessage = "PDF रिपोर्ट जनरेट हो गई (PDF Report Generated)") }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(statusMessage = "PDF Export Error: ${e.message}") }
        }
    }

    fun exportExcel(
        context: Context,
        readingsToExport: List<MeterReading>? = null,
        customRangeLabel: String? = null
    ) {
        val customer = _uiState.value.selectedCustomer ?: return
        val list = readingsToExport ?: getFilteredReadings()
        if (list.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "एक्सपोर्ट के लिए कोई रीडिंग उपलब्ध नहीं है (No readings to export)") }
            return
        }

        try {
            val rangeLabel = customRangeLabel ?: when (_uiState.value.dateFilter) {
                DateFilter.ALL -> "All Recorded Entries"
                DateFilter.THIS_MONTH -> "This Month"
                DateFilter.LAST_7_DAYS -> "Last 7 Days"
            }
            val file = ExcelExportGenerator.generateExcelCsv(context, customer, list, rangeLabel)
            FileShareHelper.shareFile(
                context = context,
                file = file,
                mimeType = "text/csv",
                title = "Share Meter Reading Excel (CSV)"
            )
            _uiState.update { it.copy(showExportDialog = false, statusMessage = "Excel/CSV रिपोर्ट जनरेट हो गई (Excel Report Generated)") }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(statusMessage = "Excel Export Error: ${e.message}") }
        }
    }

    // --- FULL DATABASE BACKUP & RESTORE ---

    fun exportFullBackup(context: Context) {
        viewModelScope.launch {
            try {
                val customers = repository.getAllCustomersList()
                val readings = repository.getAllReadingsList()
                if (customers.isEmpty() && readings.isEmpty()) {
                    _uiState.update { it.copy(statusMessage = "बैकअप के लिए कोई डेटा नहीं है (No data to backup)") }
                    return@launch
                }

                val json = BackupRestoreHelper.createBackupJson(customers, readings)
                val file = BackupRestoreHelper.createBackupFile(context, json)

                FileShareHelper.shareFile(
                    context = context,
                    file = file,
                    mimeType = "application/json",
                    title = "Share MeterRead Full Backup (JSON)"
                )
                _uiState.update { it.copy(statusMessage = "संपूर्ण बैकअप फ़ाइल तैयार है (Full JSON Backup Created)") }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(statusMessage = "Backup Failed: ${e.message}") }
            }
        }
    }

    fun exportExcelFullBackup(context: Context) {
        viewModelScope.launch {
            try {
                val customers = repository.getAllCustomersList()
                val readings = repository.getAllReadingsList()
                if (customers.isEmpty() && readings.isEmpty()) {
                    _uiState.update { it.copy(statusMessage = "बैकअप के लिए कोई डेटा नहीं है (No data to export)") }
                    return@launch
                }

                val excelFile = ExcelBackupRestoreHelper.exportFullDatabaseToExcel(context, customers, readings)
                FileShareHelper.shareFile(
                    context = context,
                    file = excelFile,
                    mimeType = "text/comma-separated-values",
                    title = "Share MeterRead Full Database Excel Sheet"
                )
                _uiState.update { it.copy(statusMessage = "Excel बैकअप स्प्रेडशीट सफलतापूर्वक तैयार (Excel Backup Created)") }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(statusMessage = "Excel Export Error: ${e.message}") }
            }
        }
    }

    fun handlePickedBackupUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val fileContent = BackupRestoreHelper.readTextFromUri(context, uri)
                val trimmed = fileContent.trim()
                val parsed: BackupParsedData = if (trimmed.startsWith("{") || trimmed.contains("\"app\":") || trimmed.contains("\"customers\":")) {
                    BackupRestoreHelper.parseBackupJson(fileContent)
                } else {
                    // Try parsing as Excel CSV
                    ExcelBackupRestoreHelper.parseExcelBackup(fileContent)
                }
                _uiState.update {
                    it.copy(
                        pendingRestoreData = parsed,
                        showRestoreConfirmDialog = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback attempt with Excel parser
                try {
                    val fileContent = BackupRestoreHelper.readTextFromUri(context, uri)
                    val parsed = ExcelBackupRestoreHelper.parseExcelBackup(fileContent)
                    _uiState.update {
                        it.copy(
                            pendingRestoreData = parsed,
                            showRestoreConfirmDialog = true
                        )
                    }
                } catch (e2: Exception) {
                    _uiState.update { it.copy(statusMessage = "अमान्य बैकअप फ़ाइल (Invalid File): ${e.message ?: e2.message}") }
                }
            }
        }
    }

    fun executeRestore(replaceExisting: Boolean) {
        val parsed = _uiState.value.pendingRestoreData ?: return
        viewModelScope.launch {
            try {
                readingsJob?.cancel()
                val (custCount, readCount) = repository.restoreData(
                    customers = parsed.customers,
                    readings = parsed.readings,
                    replaceExisting = replaceExisting
                )
                val updatedCustomers = repository.getAllCustomersList()
                val prevSelectedName = _uiState.value.selectedCustomer?.name
                val nextSelected = if (replaceExisting) {
                    updatedCustomers.firstOrNull()
                } else {
                    updatedCustomers.firstOrNull {
                        prevSelectedName != null && it.name.trim().equals(prevSelectedName.trim(), ignoreCase = true)
                    } ?: updatedCustomers.firstOrNull()
                }

                _uiState.update {
                    it.copy(
                        customers = updatedCustomers,
                        selectedCustomer = nextSelected,
                        readings = emptyList(),
                        latestReading = null,
                        showRestoreConfirmDialog = false,
                        pendingRestoreData = null,
                        statusMessage = "डेटा सफलतापूर्वक रिस्टोर हो गया ($custCount ग्राहक, $readCount रीडिंग)"
                    )
                }
                if (nextSelected != null) {
                    observeReadingsForCustomer(nextSelected.id)
                }
                refreshDbStats()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(statusMessage = "Restore Error: ${e.message}") }
            }
        }
    }

    fun dismissRestoreDialog() {
        _uiState.update {
            it.copy(
                showRestoreConfirmDialog = false,
                pendingRestoreData = null
            )
        }
    }

    fun resetDemoData() {
        clearAllCustomersAndReadings()
    }

    fun toggleSortOrder() {
        _uiState.update { it.copy(isSortDescending = !it.isSortDescending) }
    }

    fun setSortDescending(descending: Boolean) {
        _uiState.update { it.copy(isSortDescending = descending) }
    }

    fun getFilteredReadings(): List<MeterReading> {
        val list = _uiState.value.readings
        val query = _uiState.value.searchQuery.trim().lowercase()

        val dateFiltered = when (_uiState.value.dateFilter) {
            DateFilter.ALL -> list
            DateFilter.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val startOfMonth = cal.timeInMillis
                list.filter { it.timestamp >= startOfMonth }
            }
            DateFilter.LAST_7_DAYS -> {
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                list.filter { it.timestamp >= sevenDaysAgo }
            }
        }

        val filtered = if (query.isEmpty()) {
            dateFiltered
        } else {
            dateFiltered.filter {
                it.readingDate.lowercase().contains(query) ||
                it.remarks.lowercase().contains(query) ||
                it.kwh.toString().contains(query)
            }
        }

        return if (_uiState.value.isSortDescending) {
            filtered.sortedByDescending { it.timestamp }
        } else {
            filtered.sortedBy { it.timestamp }
        }
    }
}

class MeterViewModelFactory(
    private val repository: MeterRepository,
    private val driveManager: GoogleDriveBackupManager,
    private val teamSyncRepository: TeamSyncRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeterViewModel(repository, driveManager, teamSyncRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
