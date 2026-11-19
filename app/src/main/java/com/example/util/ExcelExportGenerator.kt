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

object ExcelExportGenerator {

    /**
     * Generates a CSV file structured for Microsoft Excel with UTF-8 BOM.
     * Excel automatically opens this with clean columns and native UTF-8 support for Hindi & English.
     */
    fun generateExcelCsv(
        context: Context,
        customer: Customer,
        readings: List<MeterReading>,
        dateRangeLabel: String = "All Records"
    ): File {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val safeMeterNo = customer.meterNumber.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val safePeriod = dateRangeLabel.replace("[^a-zA-Z0-9]".toRegex(), "_").take(24)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val csvFile = File(reportsDir, "MeterReading_Excel_${safeMeterNo}_${safePeriod}_$timestamp.csv")

        FileOutputStream(csvFile).use { fos ->
            // Write UTF-8 Byte Order Mark (BOM: \uFEFF) so Excel recognizes Hindi/UTF-8 immediately
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                val nowStr = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date())

                // Title Section
                writer.append("\"DAILY ELECTRICITY METER READING REPORT (दैनिक मीटर रीडिंग रिपोर्ट)\"\n")
                writer.append("\"Report Period (अवधि)\",\"${escapeCsv(dateRangeLabel)}\"\n")
                writer.append("\"Generated On:\",\"$nowStr\"\n\n")

                // Customer Profile Details
                writer.append("\"--- CUSTOMER DETAILS (ग्राहक विवरण) ---\"\n")
                writer.append("\"Customer Name (ग्राहक का नाम)\",\"${escapeCsv(customer.name)}\"\n")
                writer.append("\"Address (पता)\",\"${escapeCsv(customer.address)}\"\n")
                writer.append("\"Meter Number (मीटर नं.)\",\"${escapeCsv(customer.meterNumber)}\"\n")
                val caNumber = if (customer.consumerNumber.isNotBlank()) customer.consumerNumber else "N/A"
                writer.append("\"Consumer / CA Number\",\"${escapeCsv(caNumber)}\"\n")
                val phone = if (customer.phoneNumber.isNotBlank()) customer.phoneNumber else "N/A"
                writer.append("\"Mobile Number\",\"${escapeCsv(phone)}\"\n")
                val load = if (customer.sanctionedLoad.isNotBlank()) customer.sanctionedLoad else "N/A"
                writer.append("\"Connection Type / Load\",\"${escapeCsv(customer.connectionType)} (${escapeCsv(load)})\"\n\n")

                // Summary Stats
                val totalUnits = readings.mapNotNull { it.unitsConsumed }.sum()
                val avgPf = if (readings.isNotEmpty()) readings.map { it.powerFactor }.average() else 0.0

                writer.append("\"--- SUMMARY TOTALS (सारांश) ---\"\n")
                writer.append("\"Selected Date Period\",${escapeCsv(dateRangeLabel)}\n")
                writer.append("\"Total Readings Logged\",${readings.size}\n")
                writer.append("\"Net Units Consumed (kWh)\",\"${String.format(Locale.US, "%.2f", totalUnits)}\"\n")
                writer.append("\"Average Power Factor\",\"${String.format(Locale.US, "%.3f", avgPf)}\"\n")
                writer.append("\"Average PF Status\",\"${PowerFactorCalculator.getPfRating(avgPf).labelEn}\"\n\n")

                // Data Table Header
                writer.append("\"S.No\",\"Reading Date\",\"Reading Time\",\"Meter No\",\"Current kWh\",\"Previous kWh\",\"Units Consumed (Diff)\",\"kVArh\",\"kVAh\",\"Power Factor (PF)\",\"PF Status\",\"Remarks (टिप्पणी)\"\n")

                // Data Rows
                for ((index, item) in readings.withIndex()) {
                    val prevKwhStr = if (item.previousKwh != null) String.format(Locale.US, "%.2f", item.previousKwh) else "-"
                    val diffStr = if (item.unitsConsumed != null) String.format(Locale.US, "%.2f", item.unitsConsumed) else "-"
                    val pfStatus = when {
                        item.powerFactor >= 0.95 -> "Good (Incentive)"
                        item.powerFactor >= 0.85 -> "Fair (Normal)"
                        else -> "Penalty (<0.85)"
                    }

                    writer.append("${index + 1},")
                    writer.append("\"${escapeCsv(item.readingDate)}\",")
                    writer.append("\"${escapeCsv(item.readingTime)}\",")
                    writer.append("\"${escapeCsv(customer.meterNumber)}\",")
                    writer.append("\"${String.format(Locale.US, "%.2f", item.kwh)}\",")
                    writer.append("\"$prevKwhStr\",")
                    writer.append("\"$diffStr\",")
                    writer.append("\"${String.format(Locale.US, "%.2f", item.kvarh)}\",")
                    writer.append("\"${String.format(Locale.US, "%.2f", item.kvah)}\",")
                    writer.append("\"${String.format(Locale.US, "%.3f", item.powerFactor)}\",")
                    writer.append("\"$pfStatus\",")
                    writer.append("\"${escapeCsv(item.remarks)}\"\n")
                }

                writer.flush()
            }
        }

        return csvFile
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
