package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MeterReading
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class ChartTimeRange(val days: Int, val labelEn: String, val labelHi: String) {
    LAST_7_DAYS(7, "7 Days", "७ दिवस"),
    LAST_14_DAYS(14, "14 Days", "१४ दिवस"),
    LAST_30_DAYS(30, "30 Days", "३० दिवस (पूर्ण महिना)")
}

enum class ChartMetricType(val labelEn: String, val labelHi: String, val unit: String) {
    DAILY_CONSUMPTION("Daily Consumed Units", "दैनिक वापर (Units)", "kWh"),
    CUMULATIVE_METER("Cumulative Meter Total", "संचयी रिडिंग (Total)", "kWh")
}

data class ChartPoint(
    val reading: MeterReading,
    val displayValue: Double,
    val dateLabel: String,
    val shortDate: String,
    val timestamp: Long
)

/**
 * Recharts & D3-inspired Line Chart for Jetpack Compose:
 * - Smooth Monotone Cubic Bézier curve
 * - Vertical gradient area under the curve
 * - Dotted horizontal gridlines with Y-axis tick values
 * - Reference line indicating the average daily consumption
 * - Interactive touch-scrubbing crosshair with rich floating tooltip
 * - Range selector (7, 14, 30 Days) and Metric toggle (Daily Units vs Cumulative)
 */
@Composable
fun DailyKwhTrendChart(
    readings: List<MeterReading>,
    isHindi: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(ChartTimeRange.LAST_30_DAYS) }
    var selectedMetric by remember { mutableStateOf(ChartMetricType.DAILY_CONSUMPTION) }
    var hoveredPointIndex by remember { mutableStateOf<Int?>(null) }

    // Parse & filter data chronologically (Oldest to Newest for the timeline)
    val chartPoints by remember(readings, selectedRange, selectedMetric) {
        derivedStateOf {
            if (readings.isEmpty()) return@derivedStateOf emptyList<ChartPoint>()

            // Sort ascending by timestamp to plot left-to-right
            val sortedAsc = readings.sortedBy { it.timestamp }

            // Filter by selected range (up to N days from latest reading)
            val latestTime = sortedAsc.lastOrNull()?.timestamp ?: System.currentTimeMillis()
            val cutoff = latestTime - (selectedRange.days.toLong() * 24 * 60 * 60 * 1000L)
            val filtered = sortedAsc.filter { it.timestamp >= cutoff }

            val listToUse = if (filtered.size >= 2) filtered else sortedAsc.takeLast(selectedRange.days)

            val shortFormat = SimpleDateFormat("dd MMM", Locale.US)

            listToUse.mapIndexed { index, reading ->
                val prevReading = if (index > 0) listToUse[index - 1] else null
                val dailyValue = reading.unitsConsumed
                    ?: if (prevReading != null && reading.kwh >= prevReading.kwh) reading.kwh - prevReading.kwh
                    else 0.0

                val value = if (selectedMetric == ChartMetricType.DAILY_CONSUMPTION) dailyValue else reading.kwh

                ChartPoint(
                    reading = reading,
                    displayValue = max(0.0, value),
                    dateLabel = reading.readingDate,
                    shortDate = try {
                        shortFormat.format(reading.timestamp)
                    } catch (e: Exception) {
                        reading.readingDate.take(6)
                    },
                    timestamp = reading.timestamp
                )
            }
        }
    }

    // Key metrics for the selected range
    val values = chartPoints.map { it.displayValue }
    val totalConsumption = remember(chartPoints) {
        chartPoints.sumOf { pt -> pt.reading.unitsConsumed ?: 0.0 }
    }
    val avgValue = remember(values) { if (values.isNotEmpty()) values.average() else 0.0 }
    val maxValue = remember(values) { if (values.isNotEmpty()) values.maxOrNull() ?: 1.0 else 1.0 }
    val minValue = remember(values) { if (values.isNotEmpty()) values.minOrNull() ?: 0.0 else 0.0 }

    // 30-day Trend direction (comparing first half vs second half)
    val trendPercentage = remember(values) {
        if (values.size >= 4) {
            val half = values.size / 2
            val firstHalfAvg = values.take(half).average()
            val secondHalfAvg = values.takeLast(half).average()
            if (firstHalfAvg > 0) ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100.0 else 0.0
        } else 0.0
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_kwh_trend_chart_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title, Metric, and Trend badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "दैनिक kWh वापर ट्रेंड (३० दिवस)" else "Daily kWh Consumption Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isHindi) "डेटाबेस इतिहासावर आधारित लाइन चार्ट (D3/Recharts शैली)"
                                   else "Interactive Line Chart with 30-Day Database History",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Trend badge
                if (values.size >= 4) {
                    val isUp = trendPercentage >= 0
                    Surface(
                        color = if (isUp) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isUp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format(Locale.US, "%+.1f%%", trendPercentage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // KPI Summary Row (Total Units, Average/Day, Peak Day)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metric 1: Total Range Units
                Column {
                    Text(
                        text = if (isHindi) "एकूण वापर (${selectedRange.labelHi})" else "Total Consumed",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.0f", totalConsumption),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("kWh", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Metric 2: Daily Average
                Column {
                    Text(
                        text = if (isHindi) "दैनिक सरासरी" else "Daily Average",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.1f", avgValue),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(selectedMetric.unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Metric 3: Peak Usage
                Column {
                    Text(
                        text = if (isHindi) "उच्चतम वापर" else "Peak Day",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.0f", maxValue),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(selectedMetric.unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Row: Range & Metric Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Range Chips (7d, 14d, 30d)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChartTimeRange.entries.forEach { range ->
                        val isSelected = selectedRange == range
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRange = range },
                            label = {
                                Text(
                                    text = if (isHindi) range.labelHi else range.labelEn,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Metric Toggle (Daily units vs Cumulative total)
                FilterChip(
                    selected = selectedMetric == ChartMetricType.CUMULATIVE_METER,
                    onClick = {
                        selectedMetric = if (selectedMetric == ChartMetricType.DAILY_CONSUMPTION)
                            ChartMetricType.CUMULATIVE_METER else ChartMetricType.DAILY_CONSUMPTION
                    },
                    label = {
                        Text(
                            text = if (selectedMetric == ChartMetricType.DAILY_CONSUMPTION)
                                (if (isHindi) "दैनिक वापर \u21c4" else "Daily Units \u21c4")
                            else (if (isHindi) "संचयी kWh \u21c4" else "Meter Total \u21c4"),
                            fontSize = 11.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hovered / Selected Tooltip Banner
            val hoveredPoint = hoveredPointIndex?.let { chartPoints.getOrNull(it) }
            AnimatedVisibility(
                visible = hoveredPoint != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (hoveredPoint != null) {
                    val r = hoveredPoint.reading
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${hoveredPoint.dateLabel} (${r.readingTime})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.inverseOnSurface
                                )
                                if (r.remarks.isNotEmpty()) {
                                    Text(
                                        text = r.remarks,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", hoveredPoint.displayValue)} ${selectedMetric.unit}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    )
                                    Text(
                                        text = "PF: ${String.format(Locale.US, "%.3f", r.powerFactor)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // The Chart Canvas
            if (chartPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isHindi) "३० दिवसांच्या ट्रेंडसाठी पुरेसा डेटा उपलब्ध नाही"
                               else "Not enough reading data available for trend",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                RechartsLineChartCanvas(
                    points = chartPoints,
                    avgValue = avgValue,
                    minValue = minValue,
                    maxValue = maxValue,
                    selectedIndex = hoveredPointIndex,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    onHoverIndex = { hoveredPointIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Chart Legend & Guidance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedMetric == ChartMetricType.DAILY_CONSUMPTION) "Daily Units" else "Cumulative Meter",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(Color(0xFFFB8C00))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Avg: ${String.format(Locale.US, "%.0f", avgValue)}",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (isHindi) "टच करून स्क्रोल करा (Drag/Tap)" else "Drag finger to inspect",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Native Canvas Line Chart rendering Recharts/D3-like smooth Bézier curves,
 * gradient fill, gridlines, reference average, and touch scrubbing crosshair.
 */
@Composable
private fun RechartsLineChartCanvas(
    points: List<ChartPoint>,
    avgValue: Double,
    minValue: Double,
    maxValue: Double,
    selectedIndex: Int?,
    primaryColor: Color,
    onHoverIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val labelColor = android.graphics.Color.GRAY
    val avgLineColor = Color(0xFFFB8C00)

    Box(
        modifier = modifier
            .pointerInput(points) {
                detectTapGestures(
                    onPress = { offset ->
                        val index = computeNearestPointIndex(offset.x, size.width.toFloat(), points.size)
                        onHoverIndex(index)
                    },
                    onTap = { offset ->
                        val index = computeNearestPointIndex(offset.x, size.width.toFloat(), points.size)
                        onHoverIndex(index)
                    }
                )
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = computeNearestPointIndex(offset.x, size.width.toFloat(), points.size)
                        onHoverIndex(index)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val index = computeNearestPointIndex(change.position.x, size.width.toFloat(), points.size)
                        onHoverIndex(index)
                    },
                    onDragEnd = { /* Keep last touched visible or auto-clear */ },
                    onDragCancel = { }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingLeft = 42.dp.toPx()
            val paddingRight = 16.dp.toPx()
            val paddingTop = 20.dp.toPx()
            val paddingBottom = 32.dp.toPx()

            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            if (chartWidth <= 0 || chartHeight <= 0 || points.isEmpty()) return@Canvas

            // Dynamic Y-axis scaling (with 15% headroom above max and cushion below min)
            val rangeDiff = if (maxValue > minValue) maxValue - minValue else max(maxValue * 0.2, 10.0)
            val yMax = maxValue + (rangeDiff * 0.15)
            val yMin = max(0.0, minValue - (rangeDiff * 0.10))
            val yRange = if (yMax > yMin) yMax - yMin else 1.0

            fun getX(index: Int): Float {
                return if (points.size <= 1) paddingLeft + (chartWidth / 2f)
                else paddingLeft + (index.toFloat() / (points.size - 1)) * chartWidth
            }

            fun getY(value: Double): Float {
                val normalized = ((value - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                return paddingTop + (1f - normalized) * chartHeight
            }

            // 1. Draw Horizontal Grid Lines & Y-Axis Labels (4 ticks)
            val numTicks = 4
            val textPaint = android.graphics.Paint().apply {
                color = labelColor
                textSize = 9.sp.toPx()
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.RIGHT
            }

            for (i in 0..numTicks) {
                val tickRatio = i.toFloat() / numTicks
                val tickValue = yMin + (tickRatio * yRange)
                val yPos = paddingTop + (1f - tickRatio) * chartHeight

                // Grid line
                drawLine(
                    color = axisColor,
                    start = Offset(paddingLeft, yPos),
                    end = Offset(size.width - paddingRight, yPos),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Y-Axis label
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%.0f", tickValue),
                    paddingLeft - 6.dp.toPx(),
                    yPos + 3.dp.toPx(),
                    textPaint
                )
            }

            // 2. Draw Average Reference Line (Dashed Orange Line)
            val avgY = getY(avgValue)
            drawLine(
                color = avgLineColor.copy(alpha = 0.8f),
                start = Offset(paddingLeft, avgY),
                end = Offset(size.width - paddingRight, avgY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )

            // 3. Generate Smooth Bézier Curve (Monotone Cubic Interpolation)
            val coordinates = points.mapIndexed { i, pt -> Offset(getX(i), getY(pt.displayValue)) }

            val linePath = Path()
            val fillPath = Path()

            if (coordinates.isNotEmpty()) {
                linePath.moveTo(coordinates[0].x, coordinates[0].y)
                fillPath.moveTo(coordinates[0].x, coordinates[0].y)

                if (coordinates.size == 1) {
                    fillPath.lineTo(coordinates[0].x, paddingTop + chartHeight)
                } else {
                    for (i in 0 until coordinates.size - 1) {
                        val p0 = if (i > 0) coordinates[i - 1] else coordinates[i]
                        val p1 = coordinates[i]
                        val p2 = coordinates[i + 1]
                        val p3 = if (i + 2 < coordinates.size) coordinates[i + 2] else p2

                        // Smooth control points
                        val cp1X = p1.x + (p2.x - p0.x) * 0.2f
                        val cp1Y = p1.y + (p2.y - p0.y) * 0.2f
                        val cp2X = p2.x - (p3.x - p1.x) * 0.2f
                        val cp2Y = p2.y - (p3.y - p1.y) * 0.2f

                        linePath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p2.x, p2.y)
                        fillPath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p2.x, p2.y)
                    }

                    // Close area fill path down to baseline
                    val baselineY = paddingTop + chartHeight
                    fillPath.lineTo(coordinates.last().x, baselineY)
                    fillPath.lineTo(coordinates.first().x, baselineY)
                    fillPath.close()
                }

                // Draw Gradient Area Fill under the curve
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.12f),
                        primaryColor.copy(alpha = 0.00f)
                    ),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                )
                drawPath(path = fillPath, brush = gradientBrush)

                // Draw the Main Smooth Line
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 4. Draw Discrete Data Points
                val isDense = points.size > 20
                coordinates.forEachIndexed { i, offset ->
                    val isSelected = selectedIndex == i
                    // Only draw circles for sampled points if very dense, or when selected
                    if (!isDense || isSelected || i == 0 || i == coordinates.size - 1 || i % 4 == 0) {
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx(),
                            center = offset
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx(),
                            center = offset,
                            style = Stroke(width = if (isSelected) 3.dp.toPx() else 2.dp.toPx())
                        )
                    }
                }

                // 5. Active Hover Scrubber / Crosshair
                if (selectedIndex != null && selectedIndex in coordinates.indices) {
                    val selCoord = coordinates[selectedIndex]

                    // Vertical crosshair line
                    drawLine(
                        color = primaryColor.copy(alpha = 0.7f),
                        start = Offset(selCoord.x, paddingTop),
                        end = Offset(selCoord.x, paddingTop + chartHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    // Glowing outer pulse ring
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.25f),
                        radius = 12.dp.toPx(),
                        center = selCoord
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = selCoord
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = selCoord
                    )
                }

                // 6. X-Axis Date Labels (draw 4-5 spaced dates)
                val xTextPaint = android.graphics.Paint().apply {
                    color = labelColor
                    textSize = 9.5.sp.toPx()
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                val step = max(1, points.size / 5)
                val indicesToLabel = (0 until points.size step step).toMutableList()
                if (points.size - 1 !in indicesToLabel) {
                    indicesToLabel.add(points.size - 1)
                }

                indicesToLabel.forEach { idx ->
                    val pt = points[idx]
                    val x = getX(idx)
                    val y = paddingTop + chartHeight + 18.dp.toPx()
                    drawContext.canvas.nativeCanvas.drawText(
                        pt.shortDate,
                        x,
                        y,
                        xTextPaint
                    )
                }
            }
        }
    }
}

private fun computeNearestPointIndex(touchX: Float, fullWidth: Float, count: Int): Int {
    if (count <= 1) return 0
    val ratio = (touchX / fullWidth).coerceIn(0f, 1f)
    return (ratio * (count - 1)).toInt().coerceIn(0, count - 1)
}
