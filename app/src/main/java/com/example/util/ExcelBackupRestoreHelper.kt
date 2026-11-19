package com.example.util

import android.content.Context
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelBackupRestoreHelper {

    /**
     * Exports all customers and readings into an Excel-compatible CSV file with UTF-8 BOM.
     * Microsoft Excel and Google Sheets open this cleanly.
     */
    fun exportFullDatabaseToExcel(
        context: Context,
        customers: List<Customer>,
        readings: List<MeterReading>
    ): File {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val excelFile = File(reportsDir, "MeterRead_ExcelBackup_$timestamp.csv")

        FileOutputStream(excelFile).use { fos ->
            // UTF-8 BOM (\uFEFF) ensures Excel reads Hindi & special characters natively
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                val nowStr = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date())

                // Metadata Header
                writer.append("\"METERREAD PRO - COMPLETE DATABASE EXCEL BACKUP (मीटररीड प्रो सम्पूर्ण एक्सेल बैकअप)\"\n")
                writer.append("\"Export Date (बैकअप तारीख)\",\"$nowStr\"\n")
                writer.append("\"Total Customers (कुल ग्राहक)\",${customers.size}\n")
                writer.append("\"Total Meter Readings (कुल रीडिंग्स)\",${readings.size}\n\n")

                // Column Headers
                writer.append(
                    "\"Customer ID\"," +
                    "\"Customer Name\"," +
                    "\"Address\"," +
                    "\"Meter Number\"," +
                    "\"Consumer Number\"," +
                    "\"Mobile Number\"," +
                    "\"Connection Type\"," +
                    "\"Sanctioned Load\"," +
                    "\"Reading Date\"," +
                    "\"Reading Time\"," +
                    "\"Current kWh\"," +
                    "\"Previous kWh\"," +
                    "\"Units Consumed\"," +
                    "\"kVArh\"," +
                    "\"kVAh\"," +
                    "\"Power Factor\"," +
                    "\"Remarks\"\n"
                )

                // Group readings strictly by customerId
                val readingsByCust = readings.groupBy { it.customerId }

                // Write rows grouped cleanly per customer
                for (cust in customers) {
                    val custList = (readingsByCust[cust.id] ?: emptyList()).sortedBy { it.timestamp }
                    if (custList.isEmpty()) {
                        // Customer with no readings yet
                        writer.append("\"${cust.id}\",")
                        writer.append("\"${escapeCsv(cust.name)}\",")
                        writer.append("\"${escapeCsv(cust.address)}\",")
                        writer.append("\"${escapeCsv(cust.meterNumber)}\",")
                        writer.append("\"${escapeCsv(cust.consumerNumber)}\",")
                        writer.append("\"${escapeCsv(cust.phoneNumber)}\",")
                        writer.append("\"${escapeCsv(cust.connectionType)}\",")
                        writer.append("\"${escapeCsv(cust.sanctionedLoad)}\",")
                        writer.append("\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n")
                    } else {
                        for (r in custList) {
                            val prevStr = if (r.previousKwh != null) String.format(Locale.US, "%.2f", r.previousKwh) else ""
                            val diffStr = if (r.unitsConsumed != null) String.format(Locale.US, "%.2f", r.unitsConsumed) else ""

                            writer.append("\"${cust.id}\",")
                            writer.append("\"${escapeCsv(cust.name)}\",")
                            writer.append("\"${escapeCsv(cust.address)}\",")
                            writer.append("\"${escapeCsv(cust.meterNumber)}\",")
                            writer.append("\"${escapeCsv(cust.consumerNumber)}\",")
                            writer.append("\"${escapeCsv(cust.phoneNumber)}\",")
                            writer.append("\"${escapeCsv(cust.connectionType)}\",")
                            writer.append("\"${escapeCsv(cust.sanctionedLoad)}\",")
                            writer.append("\"${escapeCsv(r.readingDate)}\",")
                            writer.append("\"${escapeCsv(r.readingTime)}\",")
                            writer.append("\"${String.format(Locale.US, "%.2f", r.kwh)}\",")
                            writer.append("\"$prevStr\",")
                            writer.append("\"$diffStr\",")
                            writer.append("\"${String.format(Locale.US, "%.2f", r.kvarh)}\",")
                            writer.append("\"${String.format(Locale.US, "%.2f", r.kvah)}\",")
                            writer.append("\"${String.format(Locale.US, "%.3f", r.powerFactor)}\",")
                            writer.append("\"${escapeCsv(r.remarks)}\"\n")
                        }
                    }
                }

                writer.flush()
            }
        }

        return excelFile
    }

    /**
     * Parses an Excel CSV file into Customers and MeterReadings.
     * Supports both Full Database Excel Backups and single-customer reports generated by ExcelExportGenerator.
     */
    fun parseExcelBackup(csvContent: String): BackupParsedData {
        val lines = csvContent.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            throw IllegalArgumentException("The Excel file is empty.")
        }

        // Check if this is a Report Format (from ExcelExportGenerator)
        val isReportFormat = lines.any { it.contains("--- CUSTOMER DETAILS", ignoreCase = true) }

        if (isReportFormat) {
            return parseReportFormat(lines)
        } else {
            return parseMasterTableFormat(lines)
        }
    }

    private fun parseMasterTableFormat(lines: List<String>): BackupParsedData {
        // Find header row containing "Customer Name" or "Meter Number"
        var headerIndex = -1
        var colMap = mapOf<String, Int>()

        for (i in lines.indices) {
            val tokens = parseCsvLine(lines[i]).map { cleanToken(it).lowercase(Locale.US) }
            if (tokens.any { it.contains("customer name") || it.contains("meter number") || it.contains("current kwh") }) {
                headerIndex = i
                colMap = tokens.mapIndexed { idx, name -> name to idx }.toMap()
                break
            }
        }

        if (headerIndex == -1) {
            throw IllegalArgumentException("Could not find table headers in Excel file. Make sure columns like 'Customer Name', 'Meter Number', 'Current kWh' exist.")
        }

        fun getCol(vararg keywords: String): Int {
            for (kw in keywords) {
                val found = colMap.entries.firstOrNull { it.key.contains(kw) }
                if (found != null) return found.value
            }
            return -1
        }

        val colCustId = getCol("customer id", "cust id")
        val colCustName = getCol("customer name", "name", "ग्राहक")
        val colAddress = getCol("address", "पता")
        val colMeterNo = getCol("meter number", "meter no", "मीटर")
        val colConsumerNo = getCol("consumer number", "ca number", "consumer no")
        val colPhone = getCol("mobile", "phone", "फोन")
        val colConnType = getCol("connection type", "connection")
        val colLoad = getCol("sanctioned load", "load")
        val colDate = getCol("reading date", "date", "तारीख")
        val colTime = getCol("reading time", "time", "समय")
        val colKwh = getCol("current kwh", "kwh")
        val colPrevKwh = getCol("previous kwh", "prev kwh")
        val colUnits = getCol("units consumed", "diff", "units")
        val colKvarh = getCol("kvarh")
        val colKvah = getCol("kvah")
        val colPf = getCol("power factor", "pf")
        val colRemarks = getCol("remarks", "टिप्पणी")

        // Group strictly by customer identity (ID + Name, or Name + MeterNo) so different customers never mix
        val customerMap = LinkedHashMap<String, Customer>()
        val readingsList = mutableListOf<MeterReading>()
        var nextTempCustId = 1L

        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i]
            if (line.startsWith("#")) continue
            val tokens = parseCsvLine(line)
            if (tokens.isEmpty()) continue

            fun cell(idx: Int): String = if (idx in tokens.indices) cleanToken(tokens[idx]) else ""

            val rawCustId = cell(colCustId)
            val custName = cell(colCustName)
            val address = cell(colAddress)
            val meterNo = cell(colMeterNo)
            val consumerNo = cell(colConsumerNo)
            val phone = cell(colPhone)
            val connType = cell(colConnType).ifEmpty { "3-Phase Commercial" }
            val load = cell(colLoad)

            // If line is empty or purely whitespace, skip
            if (custName.isEmpty() && meterNo.isEmpty() && cell(colKwh).isEmpty()) continue

            val normName = custName.trim().lowercase(Locale.US)
            val normMeter = meterNo.trim().lowercase(Locale.US)
            val customerKey = if (rawCustId.isNotEmpty()) {
                "id:$rawCustId|$normName"
            } else {
                "$normName|$normMeter"
            }

            val customer = customerMap.getOrPut(customerKey) {
                val assignedId = nextTempCustId++
                val safeMeterNo = if (meterNo.isNotEmpty()) meterNo else "MTR-$assignedId"
                val safeCustName = if (custName.isNotEmpty()) custName else "Customer $safeMeterNo"
                Customer(
                    id = assignedId,
                    name = safeCustName,
                    address = address.ifEmpty { "Registered Address" },
                    meterNumber = safeMeterNo,
                    consumerNumber = consumerNo,
                    phoneNumber = phone,
                    connectionType = connType,
                    sanctionedLoad = load,
                    createdAt = System.currentTimeMillis()
                )
            }

            // Check reading columns
            val dateStr = cell(colDate)
            val kwhStr = cell(colKwh)

            if (kwhStr.isNotEmpty()) {
                val kwh = kwhStr.toDoubleOrNull() ?: 0.0
                val prevKwh = cell(colPrevKwh).toDoubleOrNull()
                val unitsConsumed = cell(colUnits).toDoubleOrNull()
                val kvarh = cell(colKvarh).toDoubleOrNull() ?: 0.0
                val kvah = cell(colKvah).toDoubleOrNull() ?: kwh
                val rawPf = cell(colPf).toDoubleOrNull()
                val pf = if (rawPf != null && rawPf > 0) rawPf else PowerFactorCalculator.calculatePf(kwh, kvah)
                val remarks = cell(colRemarks)
                val timeStr = cell(colTime).ifEmpty { "09:00 AM" }
                val validDateStr = if (dateStr.isNotEmpty()) dateStr else SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date())

                readingsList.add(
                    MeterReading(
                        id = 0L,
                        customerId = customer.id,
                        readingDate = validDateStr,
                        readingTime = timeStr,
                        timestamp = parseDateToMillis(validDateStr),
                        kwh = kwh,
                        kvarh = kvarh,
                        kvah = kvah,
                        powerFactor = pf,
                        isPfAutoCalculated = (rawPf == null || rawPf <= 0),
                        previousKwh = prevKwh,
                        unitsConsumed = unitsConsumed ?: (if (prevKwh != null) kwh - prevKwh else null),
                        remarks = remarks
                    )
                )
            }
        }

        val exportDateStr = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date())
        return BackupParsedData(
            exportDate = exportDateStr,
            customers = customerMap.values.toList(),
            readings = readingsList,
            fileType = "Excel (.csv)"
        )
    }

    private fun parseReportFormat(lines: List<String>): BackupParsedData {
        var custName = "Restored Customer"
        var address = "Restored Address"
        var meterNo = "MTR-001"
        var consumerNo = ""
        var phone = ""
        var connType = "3-Phase Commercial"
        var load = ""

        var dataTableStartIndex = -1

        for (i in lines.indices) {
            val line = lines[i]
            val tokens = parseCsvLine(line).map { cleanToken(it) }
            if (tokens.size >= 2) {
                val key = tokens[0].lowercase(Locale.US)
                val value = tokens[1]
                if (key.contains("customer name")) custName = value
                if (key.contains("address")) address = value
                if (key.contains("meter number")) meterNo = value
                if (key.contains("consumer / ca")) consumerNo = value
                if (key.contains("mobile number")) phone = value
                if (key.contains("connection type")) {
                    connType = value.substringBefore("(").trim()
                    load = value.substringAfter("(").substringBefore(")").trim()
                }
            }

            if (tokens.any { it.contains("Reading Date", ignoreCase = true) && tokens.any { t -> t.contains("Current kWh", ignoreCase = true) } }) {
                dataTableStartIndex = i
            }
        }

        val customer = Customer(
            id = 1L,
            name = custName,
            address = address,
            meterNumber = meterNo,
            consumerNumber = consumerNo,
            phoneNumber = phone,
            connectionType = connType,
            sanctionedLoad = load,
            createdAt = System.currentTimeMillis()
        )

        val readingsList = mutableListOf<MeterReading>()

        if (dataTableStartIndex != -1) {
            val headerTokens = parseCsvLine(lines[dataTableStartIndex]).map { cleanToken(it).lowercase(Locale.US) }
            val colMap = headerTokens.mapIndexed { idx, name -> name to idx }.toMap()

            fun getCol(vararg kw: String): Int = kw.firstNotNullOfOrNull { k -> colMap.entries.firstOrNull { it.key.contains(k) }?.value } ?: -1

            val colDate = getCol("reading date", "date")
            val colTime = getCol("reading time", "time")
            val colKwh = getCol("current kwh", "kwh")
            val colPrevKwh = getCol("previous kwh", "prev")
            val colUnits = getCol("units consumed", "diff")
            val colKvarh = getCol("kvarh")
            val colKvah = getCol("kvah")
            val colPf = getCol("power factor", "pf")
            val colRemarks = getCol("remarks")

            for (i in (dataTableStartIndex + 1) until lines.size) {
                val tokens = parseCsvLine(lines[i])
                if (tokens.size < 4) continue

                fun cell(idx: Int): String = if (idx in tokens.indices) cleanToken(tokens[idx]) else ""

                val dateStr = cell(colDate)
                val kwhStr = cell(colKwh)
                if (kwhStr.isNotEmpty()) {
                    val kwh = kwhStr.toDoubleOrNull() ?: continue
                    val timeStr = cell(colTime).ifEmpty { "09:00 AM" }
                    val prevKwh = cell(colPrevKwh).toDoubleOrNull()
                    val units = cell(colUnits).toDoubleOrNull()
                    val kvarh = cell(colKvarh).toDoubleOrNull() ?: 0.0
                    val kvah = cell(colKvah).toDoubleOrNull() ?: kwh
                    val pf = cell(colPf).toDoubleOrNull() ?: PowerFactorCalculator.calculatePf(kwh, kvah)
                    val remarks = cell(colRemarks)

                    readingsList.add(
                        MeterReading(
                            id = 0L,
                            customerId = customer.id,
                            readingDate = dateStr,
                            readingTime = timeStr,
                            timestamp = parseDateToMillis(dateStr),
                            kwh = kwh,
                            kvarh = kvarh,
                            kvah = kvah,
                            powerFactor = pf,
                            isPfAutoCalculated = true,
                            previousKwh = prevKwh,
                            unitsConsumed = units ?: (if (prevKwh != null) kwh - prevKwh else null),
                            remarks = remarks
                        )
                    )
                }
            }
        }

        val exportDateStr = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date())
        return BackupParsedData(
            exportDate = exportDateStr,
            customers = listOf(customer),
            readings = readingsList,
            fileType = "Excel (.csv) Report"
        )
    }

    private fun parseDateToMillis(dateStr: String): Long {
        val formats = listOf(
            SimpleDateFormat("dd-MMM-yyyy", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", Locale.US)
        )
        for (sdf in formats) {
            try {
                val d = sdf.parse(dateStr)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++ // Skip escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    private fun cleanToken(token: String): String {
        var res = token.trim()
        if (res.startsWith("\uFEFF")) {
            res = res.substring(1)
        }
        if (res.startsWith("\"") && res.endsWith("\"") && res.length >= 2) {
            res = res.substring(1, res.length - 1).replace("\"\"", "\"")
        }
        return res.trim()
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
