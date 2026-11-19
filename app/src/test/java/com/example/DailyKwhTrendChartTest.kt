package com.example

import com.example.data.local.DemoDataGenerator
import com.example.data.local.MeterReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyKwhTrendChartTest {

    @Test
    fun testDemoDataGeneratorProduces30DaysOfReadings() {
        val readings = DemoDataGenerator.generate30DaysReadings(customerId = 101L)

        // Must produce 30 daily readings
        assertEquals(30, readings.size)

        // Ensure chronological timestamp ordering
        for (i in 0 until readings.size - 1) {
            assertTrue("Timestamp should advance day by day", readings[i].timestamp < readings[i + 1].timestamp)
            assertTrue("kWh should be strictly non-decreasing", readings[i].kwh <= readings[i + 1].kwh)
            assertNotNull(readings[i].unitsConsumed)
            assertTrue("Daily units consumed should be positive", readings[i].unitsConsumed!! > 0.0)
        }
    }

    @Test
    fun test30DayConsumptionRangeMetrics() {
        val readings = DemoDataGenerator.generate30DaysReadings(customerId = 1L)

        val totalUnits = readings.mapNotNull { it.unitsConsumed }.sum()
        val avgUnits = readings.mapNotNull { it.unitsConsumed }.average()
        val maxUnits = readings.mapNotNull { it.unitsConsumed }.maxOrNull() ?: 0.0
        val minUnits = readings.mapNotNull { it.unitsConsumed }.minOrNull() ?: 0.0

        // Realistic industrial verification
        assertTrue("Total 30-day units should exceed 5,000 kWh", totalUnits > 5000.0)
        assertTrue("Average daily units should be within 200 - 500 kWh", avgUnits in 200.0..500.0)
        assertTrue("Max units should be greater than min units", maxUnits > minUnits)
        assertTrue("Power factor should be maintained above 0.90", readings.all { it.powerFactor >= 0.90 })
    }

    @Test
    fun testTrendPercentageCalculation() {
        val ascendingValues = listOf(100.0, 110.0, 120.0, 130.0)
        val half = ascendingValues.size / 2
        val firstHalfAvg = ascendingValues.take(half).average()
        val secondHalfAvg = ascendingValues.takeLast(half).average()
        val trend = ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100.0

        // firstHalf = 105, secondHalf = 125, trend = +19.04%
        assertTrue("Trend should be positive for increasing consumption", trend > 0.0)
        assertEquals(19.04, trend, 0.05)
    }
}
