package com.example

import com.example.data.local.MeterReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PastReadingsSortTest {

    @Test
    fun testPastReadingsSortDescendingByDateTimestamp() {
        val reading1 = MeterReading(
            id = 1,
            customerId = 1,
            readingDate = "2026-10-01",
            readingTime = "10:00 AM",
            timestamp = 1727776800000L, // Oct 1, 2026
            kwh = 1000.0,
            kvarh = 300.0,
            kvah = 1044.0,
            powerFactor = 0.958
        )
        val reading2 = MeterReading(
            id = 2,
            customerId = 1,
            readingDate = "2026-10-05",
            readingTime = "10:00 AM",
            timestamp = 1728122400000L, // Oct 5, 2026
            kwh = 1250.0,
            kvarh = 380.0,
            kvah = 1306.0,
            powerFactor = 0.957
        )
        val reading3 = MeterReading(
            id = 3,
            customerId = 1,
            readingDate = "2026-10-03",
            readingTime = "10:00 AM",
            timestamp = 1727949600000L, // Oct 3, 2026
            kwh = 1120.0,
            kvarh = 340.0,
            kvah = 1170.0,
            powerFactor = 0.957
        )

        val rawList = listOf(reading1, reading2, reading3)

        // Sort descending (latest date first)
        val sortedDescending = rawList.sortedByDescending { it.timestamp }

        assertEquals(3, sortedDescending.size)
        assertEquals("2026-10-05", sortedDescending[0].readingDate)
        assertEquals("2026-10-03", sortedDescending[1].readingDate)
        assertEquals("2026-10-01", sortedDescending[2].readingDate)

        // Verify timestamps are strictly in descending order
        assertTrue(sortedDescending[0].timestamp > sortedDescending[1].timestamp)
        assertTrue(sortedDescending[1].timestamp > sortedDescending[2].timestamp)
    }

    @Test
    fun testTotalUnitsAndAveragePfComputation() {
        val readings = listOf(
            MeterReading(id = 1, customerId = 1, readingDate = "2026-10-03", readingTime = "10:00", timestamp = 3000L, kwh = 1200.0, kvarh = 350.0, kvah = 1250.0, powerFactor = 0.96, unitsConsumed = 100.0),
            MeterReading(id = 2, customerId = 1, readingDate = "2026-10-02", readingTime = "10:00", timestamp = 2000L, kwh = 1100.0, kvarh = 320.0, kvah = 1150.0, powerFactor = 0.94, unitsConsumed = 80.0),
            MeterReading(id = 3, customerId = 1, readingDate = "2026-10-01", readingTime = "10:00", timestamp = 1000L, kwh = 1020.0, kvarh = 300.0, kvah = 1060.0, powerFactor = 0.95, unitsConsumed = null)
        )

        val totalUnits = readings.mapNotNull { it.unitsConsumed }.sum()
        assertEquals(180.0, totalUnits, 0.001)

        val avgPf = readings.map { it.powerFactor }.average()
        assertEquals(0.95, avgPf, 0.001)
    }
}
