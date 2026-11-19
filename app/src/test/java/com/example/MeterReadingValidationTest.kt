package com.example

import com.example.util.PowerFactorCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeterReadingValidationTest {

    // Helper validation functions matching MeterReadingInputScreen logic
    private fun validateKwh(input: String): String? {
        if (input.isBlank()) return "kWh is required"
        val v = input.toDoubleOrNull() ?: return "Must be a numeric value"
        if (v <= 0.0) return "Must be greater than 0"
        return null
    }

    private fun validateKvarh(input: String): String? {
        if (input.isBlank()) return "kVArh is required"
        val v = input.toDoubleOrNull() ?: return "Must be numeric"
        if (v < 0.0) return "Cannot be negative"
        return null
    }

    private fun validateKvah(input: String, kwh: Double?): String? {
        if (input.isBlank()) return "kVAh is required"
        val v = input.toDoubleOrNull() ?: return "Must be numeric"
        if (v <= 0.0) return "Must be greater than 0"
        if (kwh != null && v < kwh) return "Warning: kVAh is typically >= kWh"
        return null
    }

    private fun validatePowerFactor(input: String): String? {
        if (input.isBlank()) return "PF is required"
        val v = input.toDoubleOrNull() ?: return "Must be numeric"
        if (v < 0.0 || v > 1.0) return "PF must be 0.00 to 1.00"
        return null
    }

    @Test
    fun testValidNumericInputs() {
        assertNull(validateKwh("12450.5"))
        assertNull(validateKvarh("3450.0"))
        assertNull(validateKvah("12968.0", 12450.5))
        assertNull(validatePowerFactor("0.960"))
    }

    @Test
    fun testInvalidNonNumericInputs() {
        assertEquals("Must be a numeric value", validateKwh("12450.5abc"))
        assertEquals("Must be a numeric value", validateKwh("invalid"))
        assertEquals("Must be numeric", validateKvarh("34xx"))
        assertEquals("Must be numeric", validateKvah("kVAh123", 100.0))
        assertEquals("Must be numeric", validatePowerFactor("bad_pf"))
    }

    @Test
    fun testZeroAndNegativeInputs() {
        assertEquals("Must be greater than 0", validateKwh("0"))
        assertEquals("Must be greater than 0", validateKwh("-150.0"))
        assertEquals("Cannot be negative", validateKvarh("-10.0"))
        assertNull(validateKvarh("0.0")) // 0 kVArh is valid (unity PF)
        assertEquals("Must be greater than 0", validateKvah("-5.0", 100.0))
    }

    @Test
    fun testPowerFactorRangeValidation() {
        assertNull(validatePowerFactor("0.00"))
        assertNull(validatePowerFactor("0.85"))
        assertNull(validatePowerFactor("0.99"))
        assertNull(validatePowerFactor("1.00"))
        assertEquals("PF must be 0.00 to 1.00", validatePowerFactor("1.05"))
        assertEquals("PF must be 0.00 to 1.00", validatePowerFactor("-0.1"))
    }

    @Test
    fun testKvahConsistencyWarning() {
        val warning = validateKvah("100.0", 120.0)
        assertNotNull(warning)
        assertTrue(warning!!.contains("Warning"))
    }

    @Test
    fun testAutoCalculatedPowerFactor() {
        val kwh = 12450.0
        val kvah = 12968.0
        val pf = PowerFactorCalculator.calculatePf(kwh, kvah)
        assertEquals(0.96, pf, 0.005)
    }
}
