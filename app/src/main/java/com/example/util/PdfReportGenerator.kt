package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    /**
     * Generates a professional A4-sized PDF report for meter readings of a customer.
     * Supports date-wise reporting with multi-page pagination for any number of days.
     */
    fun generatePdfReport(
        context: Context,
        customer: Customer,
        readings: List<MeterReading>,
        dateRangeLabel: String = "All Records"
    ): File {
        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points

        // Setup Paints
        val primaryPaint = Paint().apply {
            color = Color.rgb(0, 97, 164)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(20, 24, 33)
            textSize = 9.5f
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.rgb(20, 24, 33)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subHeaderPaint = Paint().apply {
            color = Color.rgb(209, 228, 255)
            textSize = 9f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(220, 226, 235)
            strokeWidth = 1f
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(245, 248, 253)
            isAntiAlias = true
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(230, 240, 255)
            isAntiAlias = true
        }

        val greenPaint = Paint().apply {
            color = Color.rgb(16, 185, 129)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val redPaint = Paint().apply {
            color = Color.rgb(239, 68, 68)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val amberPaint = Paint().apply {
            color = Color.rgb(217, 119, 6)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val rowsPerPage = 22
        val totalPages = if (readings.isEmpty()) 1 else ((readings.size - 1) / rowsPerPage) + 1
        val nowStr = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date())

        val totalUnits = readings.mapNotNull { it.unitsConsumed }.sum()
        val avgPf = if (readings.isNotEmpty()) readings.map { it.powerFactor }.average() else 0.0
        val latestReading = readings.firstOrNull()

        val colX = floatArrayOf(30f, 52f, 130f, 195f, 255f, 315f, 375f, 425f, 485f)

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Draw Top Banner
            val bannerHeight = 62f
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), bannerHeight, primaryPaint)
            canvas.drawText("METERREAD PRO - DAILY METER READING REPORT", 30f, 30f, headerPaint)
            canvas.drawText("दैनिक मीटर रीडिंग व पावर फैक्टर रिपोर्ट  |  अवधि (Period): $dateRangeLabel  |  पेज ${pageIndex + 1}/$totalPages", 30f, 46f, subHeaderPaint)

            var currentY = 72f

            // Only draw Customer details & Summary box on First Page
            if (pageIndex == 0) {
                // Customer Details Box
                val customerCardRect = RectF(30f, currentY, (pageWidth - 30).toFloat(), currentY + 68f)
                canvas.drawRoundRect(customerCardRect, 8f, 8f, cardBgPaint)

                boldPaint.textSize = 9.5f
                canvas.drawText("CUSTOMER DETAILS / ग्राहक विवरण", 42f, currentY + 16f, primaryPaint)

                textPaint.textSize = 8.5f
                boldPaint.textSize = 8.5f
                canvas.drawText("Name / नाम:", 42f, currentY + 32f, boldPaint)
                canvas.drawText(customer.name.take(36), 125f, currentY + 32f, textPaint)

                canvas.drawText("Meter No. / मीटर:", 310f, currentY + 32f, boldPaint)
                canvas.drawText(customer.meterNumber, 405f, currentY + 32f, textPaint)

                canvas.drawText("Address / पता:", 42f, currentY + 46f, boldPaint)
                canvas.drawText(customer.address.take(45), 125f, currentY + 46f, textPaint)

                canvas.drawText("Account / CA No:", 310f, currentY + 46f, boldPaint)
                canvas.drawText(if (customer.consumerNumber.isNotBlank()) customer.consumerNumber else "N/A", 405f, currentY + 46f, textPaint)

                canvas.drawText("Connection / Load:", 42f, currentY + 60f, boldPaint)
                canvas.drawText("${customer.connectionType} (${customer.sanctionedLoad.ifBlank { "Unspecified" }})", 145f, currentY + 60f, textPaint)

                canvas.drawText("Date Range:", 310f, currentY + 60f, boldPaint)
                canvas.drawText(dateRangeLabel, 405f, currentY + 60f, primaryPaint)

                // Summary Calculations Box
                currentY = 148f
                val summaryRect = RectF(30f, currentY, (pageWidth - 30).toFloat(), currentY + 44f)
                canvas.drawRoundRect(summaryRect, 6f, 6f, cardBgPaint)

                boldPaint.textSize = 8.5f
                canvas.drawText("Total Period Records:", 42f, currentY + 18f, boldPaint)
                canvas.drawText("${readings.size} days logged", 160f, currentY + 18f, textPaint)

                canvas.drawText("Total Units Consumed:", 42f, currentY + 34f, boldPaint)
                canvas.drawText(String.format(Locale.US, "%.1f kWh (Units)", totalUnits), 160f, currentY + 34f, primaryPaint)

                canvas.drawText("Average Power Factor:", 310f, currentY + 18f, boldPaint)
                val avgPfPaint = when {
                    avgPf >= 0.95 -> greenPaint
                    avgPf >= 0.85 -> amberPaint
                    else -> redPaint
                }
                canvas.drawText(String.format(Locale.US, "%.3f (%s)", avgPf, PowerFactorCalculator.getPfRating(avgPf).labelEn), 425f, currentY + 18f, avgPfPaint)

                canvas.drawText("Report Generated On:", 310f, currentY + 34f, boldPaint)
                canvas.drawText(nowStr, 425f, currentY + 34f, textPaint)

                currentY = 202f
            } else {
                currentY = 72f
            }

            // Table Header Bar
            canvas.drawRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 20f, tableHeaderPaint)
            boldPaint.textSize = 8f
            canvas.drawText("#", colX[0] + 4f, currentY + 14f, boldPaint)
            canvas.drawText("Date & Time", colX[1], currentY + 14f, boldPaint)
            canvas.drawText("kWh Reading", colX[2], currentY + 14f, boldPaint)
            canvas.drawText("Diff Units", colX[3], currentY + 14f, boldPaint)
            canvas.drawText("kVArh", colX[4], currentY + 14f, boldPaint)
            canvas.drawText("kVAh", colX[5], currentY + 14f, boldPaint)
            canvas.drawText("PF", colX[6], currentY + 14f, boldPaint)
            canvas.drawText("PF Status", colX[7], currentY + 14f, boldPaint)
            canvas.drawText("Remarks", colX[8], currentY + 14f, boldPaint)

            currentY += 20f
            canvas.drawLine(30f, currentY, (pageWidth - 30).toFloat(), currentY, linePaint)

            // Draw Table Rows for this page
            textPaint.textSize = 7.5f
            val rowHeight = 19f
            val startIndex = pageIndex * rowsPerPage
            val endIndex = minOf(startIndex + rowsPerPage, readings.size)
            val pageReadings = if (readings.isNotEmpty()) readings.subList(startIndex, endIndex) else emptyList()

            for ((i, item) in pageReadings.withIndex()) {
                val absoluteIndex = startIndex + i
                if (i % 2 == 1) {
                    canvas.drawRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + rowHeight, cardBgPaint)
                }

                canvas.drawText("${absoluteIndex + 1}", colX[0] + 4f, currentY + 13f, textPaint)
                canvas.drawText("${item.readingDate} ${item.readingTime.take(5)}", colX[1], currentY + 13f, textPaint)
                canvas.drawText(String.format(Locale.US, "%.1f", item.kwh), colX[2], currentY + 13f, boldPaint)

                val diffStr = if (item.unitsConsumed != null) String.format(Locale.US, "+%.1f", item.unitsConsumed) else "-"
                canvas.drawText(diffStr, colX[3], currentY + 13f, textPaint)

                canvas.drawText(String.format(Locale.US, "%.1f", item.kvarh), colX[4], currentY + 13f, textPaint)
                canvas.drawText(String.format(Locale.US, "%.1f", item.kvah), colX[5], currentY + 13f, textPaint)

                val pfText = String.format(Locale.US, "%.3f", item.powerFactor)
                val pfStatusPaint = when {
                    item.powerFactor >= 0.95 -> greenPaint
                    item.powerFactor >= 0.85 -> amberPaint
                    else -> redPaint
                }
                canvas.drawText(pfText, colX[6], currentY + 13f, pfStatusPaint)

                val statusLabel = when {
                    item.powerFactor >= 0.95 -> "Good"
                    item.powerFactor >= 0.85 -> "Fair"
                    else -> "Penalty"
                }
                canvas.drawText(statusLabel, colX[7], currentY + 13f, pfStatusPaint)
                canvas.drawText(item.remarks.take(16), colX[8], currentY + 13f, textPaint)

                currentY += rowHeight
                canvas.drawLine(30f, currentY, (pageWidth - 30).toFloat(), currentY, linePaint)
            }

            // Bottom Signature only on Last Page
            if (pageIndex == totalPages - 1) {
                val signY = (pageHeight - 80).toFloat()
                canvas.drawLine(40f, signY + 30f, 200f, signY + 30f, linePaint)
                canvas.drawText("उपभोक्ता के हस्ताक्षर (Consumer Signature)", 45f, signY + 44f, textPaint)

                canvas.drawLine((pageWidth - 210).toFloat(), signY + 30f, (pageWidth - 40).toFloat(), signY + 30f, linePaint)
                canvas.drawText("मीटर रीडर / प्राधिकृत हस्ताक्षर (Meter Reader)", (pageWidth - 215).toFloat(), signY + 44f, textPaint)
            }

            // Footer note & pagination
            textPaint.textSize = 7.5f
            textPaint.color = Color.GRAY
            canvas.drawText(
                "MeterRead Pro Date-Wise Report  |  $dateRangeLabel  |  Page ${pageIndex + 1} of $totalPages",
                30f,
                (pageHeight - 16).toFloat(),
                textPaint
            )

            document.finishPage(page)
        }

        // Save file to app cache/reports directory
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        val safeMeterNo = customer.meterNumber.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val safePeriod = dateRangeLabel.replace("[^a-zA-Z0-9]".toRegex(), "_").take(24)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val pdfFile = File(reportsDir, "MeterReport_${safeMeterNo}_${safePeriod}_$timestamp.pdf")

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }
}
