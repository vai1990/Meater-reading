package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerFactorCalculatorTest {

    @Test
    fun testKvarCalculation() {
        // Test: 50 kW load, initial PF = 0.80, target PF = 0.99
        // tan(phi1) = sqrt(1 - 0.8^2) / 0.8 = 0.6 / 0.8 = 0.75
        // tan(phi2) = sqrt(1 - 0.99^2) / 0.99 ≈ 0.1425
        // K = 0.75 - 0.1425 = 0.6075
        // Required kVAr = 50 * 0.6075 ≈ 30.37 kVAr
        val result = PowerFactorCalculator.calculateRequiredKvar(
            loadKw = 50.0,
            initialPf = 0.80,
            targetPf = 0.99
        )

        assertTrue("Required kVAr should be around 30.37", result.requiredKvar in 30.0..31.0)
        assertEquals(40.0, result.recommendedBankSize, 0.1)
        assertTrue(result.recommendedSteps.isNotEmpty())
        assertTrue(result.kvaReduction > 10.0)
        assertTrue(result.currentReductionPercent > 15.0)
    }

    @Test
    fun testCalculateRequiredKvarFromReadingInput() {
        // Current reading: 12480 kWh, Previous reading: 12000 kWh -> 480 kWh over 24h = 20 kW load
        // Current kVAh: 15600 (or PF = 0.80), Target PF = 0.99
        // Required kVAr = 20 * (0.75 - 0.1425) = 12.15 kVAr
        val result = PowerFactorCalculator.calculateRequiredKvarFromReadingInput(
            currentKwh = 12480.0,
            previousKwh = 12000.0,
            currentKvarh = 360.0,
            currentKvah = 15600.0,
            currentPfInput = 0.80,
            targetPf = 0.99,
            sanctionedLoadKw = 50.0,
            operatingHours = 24.0
        )

        assertEquals(20.0, result.loadKw, 0.1)
        assertTrue("Required kVAr should be around 12.15", result.requiredKvar in 12.0..12.3)
        assertEquals(12.5, result.recommendedBankSize, 0.1)
    }

    @Test
    fun testNoKvarNeededWhenCurrentPfExceedsTargetPf() {
        val result = PowerFactorCalculator.calculateRequiredKvar(
            loadKw = 50.0,
            initialPf = 0.99,
            targetPf = 0.95
        )
        assertEquals(0.0, result.requiredKvar, 0.01)
        assertEquals(0.0, result.recommendedBankSize, 0.01)
    }

    @Test
    fun testParseLoadKw() {
        assertEquals(65.0, PowerFactorCalculator.parseLoadKw("65 kW"), 0.1)
        assertEquals(37.3, PowerFactorCalculator.parseLoadKw("50 HP"), 0.1)
        assertEquals(100.0, PowerFactorCalculator.parseLoadKw("100"), 0.1)
    }
}
