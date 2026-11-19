package com.example.data.local

import com.example.util.PowerFactorCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DemoDataGenerator {

    fun generate30DaysReadings(customerId: Long): List<MeterReading> {
        val readings = mutableListOf<MeterReading>()
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

        val calendar = Calendar.getInstance()
        // Anchor to today 09:00 AM
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Starting meter base 30 days ago
        var runningKwh = 12500.0
        var runningKvarh = 3400.0

        // Daily variations to create realistic industrial demand pattern
        val dailyUnitsPattern = doubleArrayOf(
            320.0, 345.0, 360.0, 385.0, 390.0, 290.0, 210.0, // Week 1 (Mon-Sun)
            340.0, 365.0, 375.0, 410.0, 395.0, 305.0, 220.0, // Week 2
            350.0, 370.0, 380.0, 420.0, 400.0, 310.0, 215.0, // Week 3
            355.0, 380.0, 395.0, 415.0, 385.0, 315.0, 225.0, // Week 4
            365.0, 390.0                                      // Days 29-30
        )

        val totalDays = dailyUnitsPattern.size
        for (i in 0 until totalDays) {
            val daysAgo = totalDays - 1 - i
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis - (daysAgo * 24 * 60 * 60 * 1000L)
            }

            val units = dailyUnitsPattern[i]
            val prevKwh = runningKwh
            runningKwh += units

            // Reactive power factor simulation (PF ~ 0.95 - 0.97)
            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
            val dailyKvarh = if (isWeekend) units * 0.22 else units * 0.28
            runningKvarh += dailyKvarh

            val dailyKvah = Math.sqrt((units * units) + (dailyKvarh * dailyKvarh))
            val calculatedPf = PowerFactorCalculator.calculatePf(units, dailyKvah)
            val clampedPf = String.format(Locale.US, "%.3f", Math.min(0.985, Math.max(0.92, calculatedPf))).toDouble()

            val runningKvah = Math.sqrt((runningKwh * runningKwh) + (runningKvarh * runningKvarh))

            val remark = when {
                isWeekend -> "Weekend light shift / Maintenance"
                units >= 410.0 -> "Peak production shift"
                i == totalDays - 1 -> "Today's morning reading"
                else -> "Regular daily inspection"
            }

            readings.add(
                MeterReading(
                    customerId = customerId,
                    readingDate = dateFormat.format(dayCal.time),
                    readingTime = timeFormat.format(dayCal.time),
                    timestamp = dayCal.timeInMillis,
                    kwh = String.format(Locale.US, "%.1f", runningKwh).toDouble(),
                    kvarh = String.format(Locale.US, "%.1f", runningKvarh).toDouble(),
                    kvah = String.format(Locale.US, "%.1f", runningKvah).toDouble(),
                    powerFactor = clampedPf,
                    isPfAutoCalculated = true,
                    previousKwh = String.format(Locale.US, "%.1f", prevKwh).toDouble(),
                    unitsConsumed = units,
                    remarks = remark
                )
            )
        }

        return readings
    }
}
