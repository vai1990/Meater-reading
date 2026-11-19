package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlin.math.abs
import kotlin.math.roundToLong

data class ParsedSheetTab(
    val tabName: String,
    val customer: Customer,
    val readings: List<MeterReading>
)

data class SheetSyncResult(
    val tabs: List<ParsedSheetTab>,
    val sourceLabel: String
)

data class GoogleSheetSyncConfig(
    val sheetUrl: String = "",
    val tabNamesCsv: String = "",
    val autoSyncOnOpen: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncedTabsCount: Int = 0,
    val lastSyncedReadingsCount: Int = 0
)

object GoogleSheetSyncHelper {

    private const val PREFS_NAME = "google_sheet_sync_prefs"
    private const val KEY_SHEET_URL = "sheet_url"
    private const val KEY_TAB_NAMES = "tab_names_csv"
    private const val KEY_AUTO_SYNC = "auto_sync_on_open"
    private const val KEY_LAST_SYNC_TS = "last_sync_ts"
    private const val KEY_LAST_TABS_COUNT = "last_tabs_count"
    private const val KEY_LAST_READINGS_COUNT = "last_readings_count"
    private const val KEY_BUNDLED_IMPORTED = "bundled_tirupati_imported_v1"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(null)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun getConfig(context: Context): GoogleSheetSyncConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return GoogleSheetSyncConfig(
            sheetUrl = prefs.getString(KEY_SHEET_URL, "") ?: "",
            tabNamesCsv = prefs.getString(KEY_TAB_NAMES, "") ?: "",
            autoSyncOnOpen = prefs.getBoolean(KEY_AUTO_SYNC, true),
            lastSyncTimestamp = prefs.getLong(KEY_LAST_SYNC_TS, 0L),
            lastSyncedTabsCount = prefs.getInt(KEY_LAST_TABS_COUNT, 0),
            lastSyncedReadingsCount = prefs.getInt(KEY_LAST_READINGS_COUNT, 0)
        )
    }

    fun saveConfig(
        context: Context,
        sheetUrl: String,
        tabNamesCsv: String,
        tabsCount: Int,
        readingsCount: Int,
        autoSyncOnOpen: Boolean = true
    ): GoogleSheetSyncConfig {
        val now = System.currentTimeMillis()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SHEET_URL, sheetUrl.trim())
            .putString(KEY_TAB_NAMES, tabNamesCsv.trim())
            .putBoolean(KEY_AUTO_SYNC, autoSyncOnOpen)
            .putLong(KEY_LAST_SYNC_TS, now)
            .putInt(KEY_LAST_TABS_COUNT, tabsCount)
            .putInt(KEY_LAST_READINGS_COUNT, readingsCount)
            .apply()
        return GoogleSheetSyncConfig(
            sheetUrl = sheetUrl.trim(),
            tabNamesCsv = tabNamesCsv.trim(),
            autoSyncOnOpen = autoSyncOnOpen,
            lastSyncTimestamp = now,
            lastSyncedTabsCount = tabsCount,
            lastSyncedReadingsCount = readingsCount
        )
    }

    fun isBundledSheetImported(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BUNDLED_IMPORTED, false)
    }

    fun markBundledSheetImported(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_BUNDLED_IMPORTED, true)
            .apply()
    }

    fun loadBundledSheetTabs(context: Context): SheetSyncResult {
        val csvText = context.assets.open("daily_meter_reading_tirupati.csv")
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }
        val tab = parseCustomerSheetCsv(csvText, fallbackCustomerName = "TIRUPATI CRUSHER")
        return SheetSyncResult(
            tabs = listOf(tab),
            sourceLabel = "Daily_Meter_Reading (TIRUPATI CRUSHER)"
        )
    }

    fun extractSpreadsheetId(url: String): String? {
        if (url.contains("/d/e/", ignoreCase = true)) {
            return null
        }
        val regex = Regex("spreadsheets(?:/u/\\d+)?/d/([a-zA-Z0-9-_]+)")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }

    fun extractPublishedDocId(url: String): String? {
        val regex = Regex("spreadsheets(?:/u/\\d+)?/d/e/([a-zA-Z0-9-_]+)")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }

    fun extractGid(url: String): String? {
        val regex = Regex("[#&?]gid=([0-9]+)")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }

    suspend fun fetchAndParseFromSheetUrl(
        rawUrl: String,
        tabNamesCsv: String = ""
    ): SheetSyncResult = withContext(Dispatchers.IO) {
        val trimmedUrl = rawUrl.trim()
        require(trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
            "कृपया वैध Google Sheet लिंक प्रविष्ट करा (उदा. https://docs.google.com/spreadsheets/d/...)"
        }

        val spreadsheetId = extractSpreadsheetId(trimmedUrl)
        val publishedId = extractPublishedDocId(trimmedUrl)
        val gid = extractGid(trimmedUrl)
        val explicitTabs = tabNamesCsv.split(",", "\n", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val cacheBust = System.currentTimeMillis()

        // 1. If user specified explicit tab names and we have a spreadsheet ID, fetch each customer tab via gviz CSV
        if (spreadsheetId != null && explicitTabs.isNotEmpty()) {
            val fetchedTabs = mutableListOf<ParsedSheetTab>()
            for (tabName in explicitTabs) {
                val encodedTab = URLEncoder.encode(tabName, "UTF-8")
                val csvUrl = "https://docs.google.com/spreadsheets/d/$spreadsheetId/gviz/tq?tqx=out:csv&sheet=$encodedTab&_cb=$cacheBust"
                val csvBytes = downloadBytes(csvUrl)
                val csvText = csvBytes.toString(StandardCharsets.UTF_8)
                if (!looksLikeHtmlLoginOrError(csvText)) {
                    val parsed = runCatching { parseCustomerSheetCsv(csvText, fallbackCustomerName = tabName) }.getOrNull()
                    if (parsed != null && parsed.readings.isNotEmpty()) {
                        fetchedTabs.add(parsed)
                    }
                }
            }
            if (fetchedTabs.isNotEmpty()) {
                return@withContext SheetSyncResult(
                    tabs = fetchedTabs,
                    sourceLabel = "Google Sheet (${fetchedTabs.size} Customer Tabs)"
                )
            }
        }

        // 1B. If "Published to Web" Google Sheet URL (/spreadsheets/d/e/2PACX-...)
        if (publishedId != null) {
            val xlsxPubUrl = "https://docs.google.com/spreadsheets/d/e/$publishedId/pub?output=xlsx&_cb=$cacheBust"
            val xlsxTabs = runCatching {
                val bytes = downloadBytes(xlsxPubUrl)
                if (isZipArchive(bytes)) {
                    parseXlsxWorkbook(ByteArrayInputStream(bytes))
                } else {
                    emptyList()
                }
            }.getOrNull().orEmpty()

            if (xlsxTabs.isNotEmpty()) {
                return@withContext SheetSyncResult(
                    tabs = xlsxTabs,
                    sourceLabel = "Published Sheet (${xlsxTabs.size} Customer Tabs)"
                )
            }

            val csvPubUrl = if (gid != null) {
                "https://docs.google.com/spreadsheets/d/e/$publishedId/pub?gid=$gid&single=true&output=csv&_cb=$cacheBust"
            } else {
                "https://docs.google.com/spreadsheets/d/e/$publishedId/pub?output=csv&_cb=$cacheBust"
            }
            val csvBytes = downloadBytes(csvPubUrl)
            val csvText = csvBytes.toString(StandardCharsets.UTF_8)
            if (looksLikeHtmlLoginOrError(csvText)) {
                throw IllegalStateException("Published Google Sheet मधून डेटा मिळाला नाही. कृपया नॉर्मल Share लिंक ('Anyone with the link') किंवा .xlsx/.csv फाईल वापरा.")
            }
            val parsed = parseCustomerSheetCsv(csvText, fallbackCustomerName = "Sheet Customer")
            return@withContext SheetSyncResult(
                tabs = listOf(parsed),
                sourceLabel = "Published Sheet (${parsed.tabName})"
            )
        }

        // 2. If direct CSV link (pub?output=csv, format=csv, tqx=out:csv, or .csv)
        val isExplicitCsv = trimmedUrl.contains("output=csv", ignoreCase = true) ||
            trimmedUrl.contains("format=csv", ignoreCase = true) ||
            trimmedUrl.contains("tqx=out:csv", ignoreCase = true) ||
            trimmedUrl.endsWith(".csv", ignoreCase = true)

        if (isExplicitCsv) {
            val sep = if (trimmedUrl.contains("?")) "&" else "?"
            val bytes = downloadBytes("$trimmedUrl${sep}_cb=$cacheBust")
            val text = bytes.toString(StandardCharsets.UTF_8)
            if (looksLikeHtmlLoginOrError(text)) {
                throw IllegalStateException("Google Sheet खाजगी (Private) आहे. कृपया Sheet शेअरिंग 'Anyone with the link can view' करा किंवा File Import वापरा.")
            }
            val parsed = parseCustomerSheetCsv(text, fallbackCustomerName = "Sheet Customer")
            return@withContext SheetSyncResult(
                tabs = listOf(parsed),
                sourceLabel = "Google Sheet CSV (${parsed.tabName})"
            )
        }

        // 3. If Google Sheet URL with spreadsheetId: try full multi-tab XLSX workbook first so ALL customer tabs sync automatically!
        if (spreadsheetId != null) {
            val xlsxUrl = "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=xlsx&_cb=$cacheBust"
            val xlsxResult = runCatching {
                val bytes = downloadBytes(xlsxUrl)
                if (isZipArchive(bytes)) {
                    parseXlsxWorkbook(ByteArrayInputStream(bytes))
                } else {
                    emptyList()
                }
            }.getOrNull().orEmpty()

            if (xlsxResult.isNotEmpty()) {
                return@withContext SheetSyncResult(
                    tabs = xlsxResult,
                    sourceLabel = "Google Sheet (${xlsxResult.size} Customer Tabs)"
                )
            }

            // 4. Fallback: fetch the active gid tab (or default sheet) as CSV
            val csvUrl = if (gid != null) {
                "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=$gid&_cb=$cacheBust"
            } else {
                "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&_cb=$cacheBust"
            }
            val csvBytes = downloadBytes(csvUrl)
            val csvText = csvBytes.toString(StandardCharsets.UTF_8)
            if (looksLikeHtmlLoginOrError(csvText)) {
                throw IllegalStateException(
                    "Google Sheet प्रवेश नाकारला गेला (Private Sheet). कृपया Google Sheet मध्ये Share -> 'Anyone with the link (Viewer)' निवडा आणि पुन्हा प्रयत्न करा."
                )
            }
            val parsed = parseCustomerSheetCsv(csvText, fallbackCustomerName = "Sheet Customer")
            return@withContext SheetSyncResult(
                tabs = listOf(parsed),
                sourceLabel = "Google Sheet (${parsed.tabName})"
            )
        }

        // 5. Generic URL fetch (could be raw CSV or XLSX)
        val bytes = downloadBytes(trimmedUrl)
        return@withContext if (isZipArchive(bytes)) {
            val tabs = parseXlsxWorkbook(ByteArrayInputStream(bytes))
            SheetSyncResult(tabs = tabs, sourceLabel = "Spreadsheet (${tabs.size} Customer Tabs)")
        } else {
            val text = bytes.toString(StandardCharsets.UTF_8)
            if (looksLikeHtmlLoginOrError(text)) {
                throw IllegalStateException("लिंकवरून स्प्रेडशीट डेटा मिळाला नाही. कृपया 'Anyone with the link' शेअरिंग तपासा.")
            }
            val parsed = parseCustomerSheetCsv(text, fallbackCustomerName = "Sheet Customer")
            SheetSyncResult(tabs = listOf(parsed), sourceLabel = "Sheet (${parsed.tabName})")
        }
    }

    suspend fun parsePickedSheetUri(context: Context, uri: Uri): SheetSyncResult = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("निवडलेली फाईल वाचता आली नाही.")

        return@withContext if (isZipArchive(bytes)) {
            val tabs = parseXlsxWorkbook(ByteArrayInputStream(bytes))
            require(tabs.isNotEmpty()) { "Excel (.xlsx) फाईलमध्ये वैध रीडिंग डेटा सापडला नाही." }
            SheetSyncResult(tabs = tabs, sourceLabel = "Excel Workbook (${tabs.size} Tabs)")
        } else {
            val text = bytes.toString(StandardCharsets.UTF_8)
            val parsed = parseCustomerSheetCsv(text, fallbackCustomerName = "Imported Sheet")
            SheetSyncResult(tabs = listOf(parsed), sourceLabel = "CSV Sheet (${parsed.tabName})")
        }
    }

    /**
     * Parses a single customer-tab CSV such as:
     * TIRUPATI CRUSHER,,,,,
     * DATE,TIME,Kwh,KvArh,KvAh,PF
     * 07-Sep-2026,8:41:00 AM,54654,24897,70422,513
     */
    fun parseCustomerSheetCsv(
        csvContent: String,
        fallbackCustomerName: String = "Sheet Customer"
    ): ParsedSheetTab {
        val rawLines = csvContent.lines()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }

        val rows = rawLines.map { line ->
            ExcelBackupRestoreHelper.parseCsvLine(line).map { cleanCsvCell(it) }
        }
        return parseRowsForCustomerTab(rows, fallbackCustomerName)
    }

    fun parseRowsForCustomerTab(
        rows: List<List<String>>,
        fallbackCustomerName: String
    ): ParsedSheetTab {
        val nonEmptyRows = rows.filter { row -> row.any { it.isNotBlank() } }
        require(nonEmptyRows.isNotEmpty()) { "शीटमध्ये कोणताही डेटा नाही ($fallbackCustomerName)." }

        // Find the header row (must have DATE and Kwh/KvArh/KvAh)
        var headerIndex = -1
        for (i in nonEmptyRows.indices) {
            val normalized = nonEmptyRows[i].map { it.trim().lowercase(Locale.US) }
            val hasDate = normalized.any { it == "date" || it.contains("reading date") || it.contains("दिनांक") }
            val hasKwh = normalized.any { it == "kwh" || it.contains("kwh") }
            if (hasDate && hasKwh) {
                headerIndex = i
                break
            }
        }

        if (headerIndex == -1) {
            // Secondary heuristic: header with just DATE or Kwh
            headerIndex = nonEmptyRows.indexOfFirst { row ->
                val norm = row.map { it.trim().lowercase(Locale.US) }
                norm.any { it.contains("kwh") } && (norm.any { it.contains("kvarh") } || norm.any { it.contains("kvah") } || norm.any { it.contains("date") })
            }
        }

        require(headerIndex != -1) {
            "शीट '$fallbackCustomerName' मध्ये DATE, TIME, Kwh, KvArh, KvAh, PF कॉलम्स सापडले नाहीत."
        }

        // Determine Customer Name from title row(s) above header if available (e.g., "TIRUPATI CRUSHER,,,,,")
        var detectedCustomerName = fallbackCustomerName.trim()
        if (headerIndex > 0) {
            for (i in 0 until headerIndex) {
                val candidate = nonEmptyRows[i].firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                if (candidate.isNotBlank() &&
                    !candidate.equals("DATE", ignoreCase = true) &&
                    !candidate.contains("METERREAD", ignoreCase = true)
                ) {
                    detectedCustomerName = candidate
                    break
                }
            }
        }
        if (detectedCustomerName.isBlank()) {
            detectedCustomerName = "Sheet Customer"
        }

        val headerTokens = nonEmptyRows[headerIndex].map { it.trim().lowercase(Locale.US) }
        fun findCol(vararg keywords: String): Int {
            // Exact match first
            for (kw in keywords) {
                val exactIdx = headerTokens.indexOfFirst { it == kw }
                if (exactIdx != -1) return exactIdx
            }
            // Substring match fallback
            for (kw in keywords) {
                val subIdx = headerTokens.indexOfFirst { it.contains(kw) }
                if (subIdx != -1) return subIdx
            }
            return -1
        }

        val colDate = findCol("date", "reading date", "दिनांक", "तारीख", "dt")
        val colTime = findCol("time", "reading time", "वेळ", "समय")
        val colKwh = findCol("kwh", "current kwh", "active energy", "kw h", "kw-h")
        val colKvarh = findCol("kvarh", "reactive", "kvar h", "kvar-h")
        val colKvah = findCol("kvah", "apparent", "kva h", "kva-h")
        val colPf = findCol("pf", "power factor", "p.f", "p.f.", "cos")
        val colRemarks = findCol("remark", "remarks", "note", "notes", "टिप्पणी", "शेरा")

        val parsedReadings = mutableListOf<MeterReading>()

        for (i in (headerIndex + 1) until nonEmptyRows.size) {
            val row = nonEmptyRows[i]
            fun cell(idx: Int): String = if (idx in row.indices) row[idx].trim() else ""

            val rawDate = cell(colDate)
            val rawTime = cell(colTime).ifBlank { "08:30:00 AM" }
            val kwhVal = parseNumericValue(cell(colKwh)) ?: continue
            val kvarhVal = parseNumericValue(cell(colKvarh)) ?: 0.0
            val kvahVal = parseNumericValue(cell(colKvah)) ?: PowerFactorCalculator.calculateKvahFromKvarh(kwhVal, kvarhVal)

            val rawPfStr = cell(colPf)
            val rawRemarkStr = cell(colRemarks)

            // User requirement: "मला शीटमधील PF अॅपच्या Remark मध्ये पाहिजे आणि PF ऑटो फीड झाला पाहिजे"
            // 1. Auto-feed / auto-calculate Power Factor from kWh, kVArh, and kVAh
            val autoCalculatedPf = PowerFactorCalculator.calculateAutoPf(kwhVal, kvarhVal, kvahVal)

            // 2. Store the Sheet's PF column value inside the App's Remarks field
            val combinedRemarks = buildSheetRemarks(rawPfStr, rawRemarkStr, autoCalculatedPf)

            val normalizedDate = normalizeDateDisplay(rawDate)
            val normalizedTime = normalizeTimeDisplay(rawTime)
            val timestamp = parseDateTimeToMillis(normalizedDate, normalizedTime)

            parsedReadings.add(
                MeterReading(
                    id = 0L,
                    customerId = 0L,
                    readingDate = normalizedDate,
                    readingTime = normalizedTime,
                    timestamp = timestamp,
                    kwh = kwhVal,
                    kvarh = kvarhVal,
                    kvah = kvahVal,
                    powerFactor = autoCalculatedPf,
                    isPfAutoCalculated = true,
                    previousKwh = null,
                    unitsConsumed = null,
                    remarks = combinedRemarks
                )
            )
        }

        // Sort chronologically and compute previousKwh / unitsConsumed
        val chronological = parsedReadings.sortedBy { it.timestamp }
        val enriched = chronological.mapIndexed { idx, reading ->
            if (idx == 0) {
                reading
            } else {
                val prevKwh = chronological[idx - 1].kwh
                val consumed = if (reading.kwh >= prevKwh) {
                    ((reading.kwh - prevKwh) * 100.0).roundToLong() / 100.0
                } else null
                reading.copy(
                    previousKwh = prevKwh,
                    unitsConsumed = consumed
                )
            }
        }

        val meterSuffix = detectedCustomerName
            .uppercase(Locale.US)
            .replace("[^A-Z0-9]".toRegex(), "")
            .take(6)
            .ifBlank { "SHEET" }

        val customer = Customer(
            id = 0L,
            name = detectedCustomerName,
            address = "Google Sheet Sync ($fallbackCustomerName)",
            meterNumber = "SHT-$meterSuffix",
            consumerNumber = "",
            phoneNumber = "",
            connectionType = "3-Phase Industrial",
            sanctionedLoad = "65 kW"
        )

        return ParsedSheetTab(
            tabName = detectedCustomerName,
            customer = customer,
            readings = enriched
        )
    }

    internal fun parseSheetPowerFactor(rawPfStr: String, kwh: Double, kvah: Double): Pair<Double, Boolean> {
        return Pair(PowerFactorCalculator.calculatePf(kwh, kvah), true)
    }

    internal fun buildSheetRemarks(rawPfStr: String, rawRemarkStr: String, normalizedPf: Double = 0.0): String {
        val parts = mutableListOf<String>()
        if (rawPfStr.isNotBlank()) {
            val cleanPf = rawPfStr.trim()
            if (cleanPf.startsWith("PF", ignoreCase = true)) {
                parts.add(cleanPf)
            } else {
                parts.add("PF: $cleanPf")
            }
        }
        if (rawRemarkStr.isNotBlank() && !rawRemarkStr.equals(rawPfStr, ignoreCase = true)) {
            val cleanRem = rawRemarkStr.trim()
            if (rawPfStr.isBlank() && parseNumericValue(cleanRem) != null && !cleanRem.startsWith("PF", ignoreCase = true)) {
                parts.add("PF: $cleanRem")
            } else {
                parts.add(cleanRem)
            }
        }
        return parts.joinToString(" • ")
    }

    private fun parseNumericValue(text: String): Double? {
        val cleaned = text.trim().replace(",", "")
        cleaned.toDoubleOrNull()?.let { return it }
        // Extract first number if text has prefix/suffix like "PF 0.98" or "985 lag"
        val match = Regex("[-+]?\\d*\\.?\\d+").find(cleaned)
        return match?.value?.toDoubleOrNull()
    }

    /**
     * Parses a multi-tab Excel (.xlsx) workbook using standard ZipInputStream + XmlPullParser.
     * Each worksheet tab is treated as a Customer Tab.
     */
    fun parseXlsxWorkbook(inputStream: InputStream): List<ParsedSheetTab> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val relTargets = entries["xl/_rels/workbook.xml.rels"]?.let { parseWorkbookRels(it) } ?: emptyMap()
        val sheetDefs = entries["xl/workbook.xml"]?.let { parseWorkbookSheets(it) } ?: emptyList()

        val resultTabs = mutableListOf<ParsedSheetTab>()
        for ((sheetName, relId) in sheetDefs) {
            val targetPath = relTargets[relId]?.let { target ->
                val trimmed = target.trimStart('/')
                if (trimmed.startsWith("xl/")) trimmed else "xl/$trimmed"
            } ?: continue

            val sheetBytes = entries[targetPath] ?: continue
            val rows = parseWorksheetXml(sheetBytes, sharedStrings)
            val parsed = runCatching {
                parseRowsForCustomerTab(rows, fallbackCustomerName = sheetName)
            }.getOrNull()

            if (parsed != null && parsed.readings.isNotEmpty()) {
                resultTabs.add(parsed)
            }
        }
        return resultTabs
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var insideSi = false
        var insideT = false
        val currentText = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            insideSi = true
                            currentText.setLength(0)
                        }
                        "t" -> if (insideSi) insideT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideSi && insideT) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> insideT = false
                        "si" -> {
                            insideSi = false
                            list.add(currentText.toString())
                        }
                    }
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun parseWorkbookRels(bytes: ByteArray): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                if (!id.isNullOrBlank() && !target.isNullOrBlank()) {
                    map[id] = target
                }
            }
            event = parser.next()
        }
        return map
    }

    private fun parseWorkbookSheets(bytes: ByteArray): List<Pair<String, String>> {
        val sheets = mutableListOf<Pair<String, String>>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "sheet") {
                val name = parser.getAttributeValue(null, "name") ?: "Sheet"
                var relId: String? = null
                for (i in 0 until parser.attributeCount) {
                    val attrName = parser.getAttributeName(i)
                    if (attrName == "id" || attrName.endsWith(":id")) {
                        relId = parser.getAttributeValue(i)
                        break
                    }
                }
                if (!relId.isNullOrBlank()) {
                    sheets.add(Pair(name, relId))
                }
            }
            event = parser.next()
        }
        return sheets
    }

    private fun parseWorksheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var currentRow = mutableListOf<String>()
        var currentColIndex = -1
        var lastColIndex = -1
        var cellType = ""
        var insideV = false
        var insideInlineT = false
        val valueBuilder = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableListOf()
                            currentColIndex = -1
                            lastColIndex = -1
                        }
                        "c" -> {
                            val cellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentColIndex = if (cellRef.isNotBlank()) {
                                columnIndexFromCellRef(cellRef)
                            } else {
                                lastColIndex + 1
                            }
                            lastColIndex = currentColIndex
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            valueBuilder.setLength(0)
                        }
                        "v" -> insideV = true
                        "t" -> if (cellType == "inlineStr") insideInlineT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV || insideInlineT) {
                        valueBuilder.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> insideV = false
                        "t" -> insideInlineT = false
                        "c" -> {
                            val raw = valueBuilder.toString().trim()
                            val resolved = when (cellType) {
                                "s" -> {
                                    val idx = raw.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                                }
                                else -> raw
                            }
                            while (currentRow.size <= currentColIndex) {
                                currentRow.add("")
                            }
                            if (currentColIndex >= 0) {
                                currentRow[currentColIndex] = resolved
                            }
                        }
                        "row" -> {
                            if (currentRow.any { it.isNotBlank() }) {
                                rows.add(currentRow)
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }

        // Convert any Excel serial dates/times in Date and Time columns
        val headerRowIdx = rows.indexOfFirst { row ->
            val norm = row.map { it.trim().lowercase(Locale.US) }
            norm.any { it == "date" || it.contains("reading date") || it.contains("दिनांक") || it.contains("तारीख") } &&
                norm.any { it.contains("kwh") }
        }
        if (headerRowIdx != -1) {
            val headerNorm = rows[headerRowIdx].map { it.trim().lowercase(Locale.US) }
            val dateCol = headerNorm.indexOfFirst {
                it == "date" || it.contains("reading date") || it.contains("दिनांक") || it.contains("तारीख") || it == "dt"
            }
            val timeCol = headerNorm.indexOfFirst {
                it == "time" || it.contains("reading time") || it.contains("वेळ") || it.contains("समय")
            }
            return rows.mapIndexed { rIdx, row ->
                if (rIdx <= headerRowIdx) {
                    row
                } else {
                    row.mapIndexed { cIdx, cellVal ->
                        when {
                            cIdx == dateCol -> convertExcelSerialDateIfNeeded(cellVal)
                            cIdx == timeCol -> convertExcelSerialTimeIfNeeded(cellVal)
                            else -> cellVal
                        }
                    }
                }
            }
        }
        return rows
    }

    private fun columnIndexFromCellRef(cellRef: String): Int {
        val letters = cellRef.takeWhile { it.isLetter() }.uppercase(Locale.US)
        if (letters.isEmpty()) return 0
        var idx = 0
        for (ch in letters) {
            idx = idx * 26 + (ch - 'A' + 1)
        }
        return (idx - 1).coerceAtLeast(0)
    }

    private fun convertExcelSerialDateIfNeeded(value: String): String {
        val serial = value.toDoubleOrNull() ?: return value
        if (serial < 1000.0 || serial > 100000.0) return value
        val days = serial.toInt()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
            set(1899, Calendar.DECEMBER, 30, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, days)
        }
        val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(cal.time)
    }

    private fun convertExcelSerialTimeIfNeeded(value: String): String {
        val serial = value.toDoubleOrNull() ?: return value
        if (serial < 0.0 || serial > 100000.0) return value
        val fraction = serial - serial.toInt()
        val totalSeconds = (fraction * 86400.0).roundToLong().toInt()
        val hours = (totalSeconds / 3600) % 24
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        val cal = Calendar.getInstance(Locale.US).apply {
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, mins)
            set(Calendar.SECOND, secs)
        }
        return SimpleDateFormat("h:mm:ss a", Locale.US).format(cal.time)
    }

    private fun normalizeDateDisplay(rawDate: String): String {
        val trimmed = rawDate.trim()
        if (trimmed.isBlank()) {
            return SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date())
        }
        // Handle Excel serial numbers if passed directly as string
        val serialConverted = convertExcelSerialDateIfNeeded(trimmed)
        val candidate = if (serialConverted != trimmed) {
            serialConverted
        } else if (Regex("^\\d{4}-\\d{2}-\\d{2}T.*").matches(trimmed)) {
            trimmed.substringBefore("T")
        } else {
            trimmed
        }

        val formats = listOf(
            "dd-MMM-yyyy",
            "d-MMM-yyyy",
            "dd MMM yyyy",
            "d MMM yyyy",
            "dd/MM/yyyy",
            "d/M/yyyy",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "d-M-yyyy",
            "dd.MM.yyyy",
            "d.M.yyyy",
            "MM/dd/yyyy",
            "MMM dd, yyyy",
            "MMM d, yyyy",
            "dd-MMM-yy",
            "d-MMM-yy",
            "dd/MM/yy",
            "d/M/yy",
            "dd-MM-yy",
            "d-M-yy",
            "dd.MM.yy"
        )
        for (fmt in formats) {
            try {
                val parser = SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }
                val date = parser.parse(candidate)
                if (date != null) {
                    return SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(date)
                }
            } catch (_: Exception) {
            }
        }
        return candidate
    }

    private fun normalizeTimeDisplay(rawTime: String): String {
        val trimmed = rawTime.trim()
        if (trimmed.isBlank()) return "08:30:00 AM"
        val converted = convertExcelSerialTimeIfNeeded(trimmed)
        return converted.trim().ifBlank { "08:30:00 AM" }
    }

    fun parseDateTimeToMillis(dateStr: String, timeStr: String): Long {
        val normDate = normalizeDateDisplay(dateStr)
        val normTime = normalizeTimeDisplay(timeStr)
        val combined = "$normDate $normTime".trim()
        val dateTimeFormats = listOf(
            "dd-MMM-yyyy h:mm:ss a",
            "dd-MMM-yyyy hh:mm:ss a",
            "dd-MMM-yyyy h:mm a",
            "dd-MMM-yyyy hh:mm a",
            "dd-MMM-yyyy HH:mm:ss",
            "dd-MMM-yyyy H:mm:ss",
            "dd-MMM-yyyy HH:mm",
            "dd-MMM-yyyy H:mm",
            "dd/MM/yyyy h:mm:ss a",
            "dd/MM/yyyy h:mm a",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm"
        )
        for (fmt in dateTimeFormats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }
                val d = sdf.parse(combined)
                if (d != null) return d.time
            } catch (_: Exception) {
            }
        }
        val dateOnlyFormats = listOf("dd-MMM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
        for (fmt in dateOnlyFormats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }
                val d = sdf.parse(normDate)
                if (d != null) return d.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

    private fun downloadBytes(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SmartMeterPro-Android/1.0")
            .header("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
            .header("Pragma", "no-cache")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Google Sheet डाउनलोड अयशस्वी (HTTP ${response.code}). Sheet पब्लिक/शेअर केलेली असल्याची खात्री करा.")
            }
            return response.body?.bytes() ?: ByteArray(0)
        }
    }

    private fun isZipArchive(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte()
    }

    private fun looksLikeHtmlLoginOrError(text: String): Boolean {
        val sample = text.trimStart().take(300).lowercase(Locale.US)
        return sample.startsWith("<!doctype html") ||
            sample.startsWith("<html") ||
            sample.contains("accounts.google.com/servicelogin")
    }

    private fun cleanCsvCell(token: String): String {
        var res = token.trim()
        if (res.startsWith("\uFEFF")) {
            res = res.substring(1)
        }
        if (res.startsWith("\"") && res.endsWith("\"") && res.length >= 2) {
            res = res.substring(1, res.length - 1).replace("\"\"", "\"")
        }
        return res.trim()
    }
}
