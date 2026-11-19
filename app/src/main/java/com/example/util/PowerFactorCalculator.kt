package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PowerFactorGood
import com.example.ui.theme.PowerFactorPenalty
import com.example.ui.theme.PowerFactorWarning
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class PfRating(
    val labelEn: String,
    val labelHi: String,
    val labelMr: String,
    val color: Color,
    val advice: String
) {
    EXCELLENT(
        "Excellent (>= 0.95)",
        "उत्कृष्ट (इंसेंटिव योग्य)",
        "उत्कृष्ट (इन्सेंटिव्ह पात्र >= 0.95)",
        PowerFactorGood,
        "DISCOM बिजली बिल में प्रोत्साहन/छूट (Incentive) मिलेगी। कैपेसिटर बैंक सही कार्य कर रहा है।"
    ),
    FAIR(
        "Fair (0.85 - 0.94)",
        "संतोषजनक (सुधार आवश्यक)",
        "समाधानकारक (सुधारणा आवश्यक 0.85 - 0.94)",
        PowerFactorWarning,
        "पावर फैक्टर सामान्य है, परंतु पेनल्टी से बचने और 0.95+ इंसेंटिव के लिए APFC या कैपेसिटर चालू रखें।"
    ),
    POOR(
        "Low / Penalty (< 0.85)",
        "कम (पेनल्टी चेतावनी!)",
        "कमी (दंड / पेनल्टी इशारा < 0.85!)",
        PowerFactorPenalty,
        "चेतावनी: DISCOM द्वारा लो पावर फैक्टर सरचार्ज (Low PF Penalty) लगेगा! तुरंत कैपेसिटर बैंक चेक करें।"
    )
}

data class KvarCalculationResult(
    val initialPf: Double,
    val targetPf: Double,
    val loadKw: Double,
    val multiplierK: Double,
    val requiredKvar: Double,
    val recommendedBankSize: Double,
    val excessDailyKvarh: Double? = null,
    val recommendedSteps: List<Int> = emptyList(),
    val initialKva: Double = 0.0,
    val targetKva: Double = 0.0,
    val kvaReduction: Double = 0.0,
    val currentReductionPercent: Double = 0.0
)

object PowerFactorCalculator {

    /**
     * Calculate Power Factor from active (kWh) and apparent (kVAh) energy.
     * PF = kWh / kVAh (range 0.00 to 1.00)
     */
    fun calculatePf(kwh: Double, kvah: Double): Double {
        if (kvah <= 0.0) return 0.0
        val pf = kwh / kvah
        return ((pf * 1000.0).roundToInt() / 1000.0).coerceIn(0.0, 1.0)
    }

    fun calculateAutoPf(kwh: Double, kvarh: Double, kvah: Double): Double {
        if (kwh <= 0.0) return 0.0
        return when {
            kvah >= kwh && kvah > 0.0 -> calculatePf(kwh, kvah)
            kvarh > 0.0 -> {
                val estKvah = calculateKvahFromKvarh(kwh, kvarh)
                calculatePf(kwh, estKvah)
            }
            kvah > 0.0 -> calculatePf(kwh, kvah)
            else -> 0.95
        }
    }

    /**
     * Theoretical kVAh calculation from kWh and kVArh:
     * kVAh = sqrt(kWh^2 + kVArh^2)
     */
    fun calculateKvahFromKvarh(kwh: Double, kvarh: Double): Double {
        val kvah = sqrt(kwh.pow(2) + kvarh.pow(2))
        return (kvah * 100.0).roundToInt() / 100.0
    }

    /**
     * Calculate required capacitor bank size (kVAr) to improve PF from initial to target.
     * Formula: kVAr = kW * (tan(phi1) - tan(phi2))
     * where tan(phi) = sqrt(1 - pf^2) / pf
     */
    fun calculateRequiredKvar(
        loadKw: Double,
        initialPf: Double,
        targetPf: Double = 0.99
    ): KvarCalculationResult {
        val safeLoad = loadKw.coerceAtLeast(0.1)
        val safeInitialPf = initialPf.coerceIn(0.2, 0.999)
        val safeTargetPf = targetPf.coerceIn(0.2, 1.0)

        if (safeInitialPf >= safeTargetPf) {
            val currentKva = (safeLoad / safeInitialPf * 100.0).roundToInt() / 100.0
            return KvarCalculationResult(
                initialPf = safeInitialPf,
                targetPf = safeTargetPf,
                loadKw = safeLoad,
                multiplierK = 0.0,
                requiredKvar = 0.0,
                recommendedBankSize = 0.0,
                recommendedSteps = emptyList(),
                initialKva = currentKva,
                targetKva = currentKva,
                kvaReduction = 0.0,
                currentReductionPercent = 0.0
            )
        }

        val tanPhi1 = sqrt(1.0 - safeInitialPf.pow(2)) / safeInitialPf
        val tanPhi2 = if (safeTargetPf >= 1.0) 0.0 else sqrt(1.0 - safeTargetPf.pow(2)) / safeTargetPf

        val multiplierK = (tanPhi1 - tanPhi2).coerceAtLeast(0.0)
        val rawKvar = (safeLoad * multiplierK).coerceAtLeast(0.0)
        val requiredKvar = (rawKvar * 100.0).roundToInt() / 100.0

        val bankSize = recommendStandardBankSize(rawKvar)
        val steps = generateApfcSteps(bankSize.toInt())

        val initialKva = safeLoad / safeInitialPf
        val targetKva = safeLoad / safeTargetPf
        val kvaSaved = (initialKva - targetKva).coerceAtLeast(0.0)
        val currentRedPct = ((1.0 - (safeInitialPf / safeTargetPf)) * 100.0).coerceAtLeast(0.0)

        return KvarCalculationResult(
            initialPf = safeInitialPf,
            targetPf = safeTargetPf,
            loadKw = (safeLoad * 100.0).roundToInt() / 100.0,
            multiplierK = (multiplierK * 1000.0).roundToInt() / 1000.0,
            requiredKvar = requiredKvar,
            recommendedBankSize = bankSize,
            recommendedSteps = steps,
            initialKva = (initialKva * 100.0).roundToInt() / 100.0,
            targetKva = (targetKva * 100.0).roundToInt() / 100.0,
            kvaReduction = (kvaSaved * 100.0).roundToInt() / 100.0,
            currentReductionPercent = (currentRedPct * 10.0).roundToInt() / 10.0
        )
    }

    /**
     * Calculate required kVAr rating for power factor correction directly from a current meter reading
     * (and optional previous reading / load) and target power factor input.
     */
    fun calculateRequiredKvarFromReadingInput(
        currentKwh: Double,
        previousKwh: Double? = null,
        currentKvarh: Double = 0.0,
        currentKvah: Double = 0.0,
        currentPfInput: Double? = null,
        targetPf: Double = 0.99,
        sanctionedLoadKw: Double = 50.0,
        operatingHours: Double = 24.0
    ): KvarCalculationResult {
        val effectivePf = when {
            currentPfInput != null && currentPfInput in 0.2..1.0 -> currentPfInput
            currentKwh > 0.0 && currentKvah > 0.0 -> calculatePf(currentKwh, currentKvah).coerceIn(0.2, 1.0)
            currentKwh > 0.0 && currentKvarh > 0.0 -> {
                val estKvah = calculateKvahFromKvarh(currentKwh, currentKvarh)
                calculatePf(currentKwh, estKvah).coerceIn(0.2, 1.0)
            }
            else -> 0.85
        }

        val deltaKwh = if (previousKwh != null && currentKwh > previousKwh) {
            currentKwh - previousKwh
        } else null

        val safeHours = operatingHours.coerceAtLeast(1.0)
        val effectiveLoadKw = if (deltaKwh != null && deltaKwh > 0.0) {
            (deltaKwh / safeHours).coerceAtLeast(1.0)
        } else {
            sanctionedLoadKw.coerceAtLeast(1.0)
        }

        val baseResult = calculateRequiredKvar(
            loadKw = effectiveLoadKw,
            initialPf = effectivePf,
            targetPf = targetPf
        )

        val energyForExcess = deltaKwh ?: currentKwh
        val excessKvarh = if (energyForExcess > 0.0 && effectivePf < targetPf) {
            val tan1 = sqrt(1.0 - effectivePf.pow(2)) / effectivePf
            val tan2 = if (targetPf >= 1.0) 0.0 else sqrt(1.0 - targetPf.pow(2)) / targetPf
            val rawExcess = (energyForExcess * (tan1 - tan2)).coerceAtLeast(0.0)
            (rawExcess * 100.0).roundToInt() / 100.0
        } else null

        return baseResult.copy(excessDailyKvarh = excessKvarh)
    }

    /**
     * Calculate required capacitor bank size from daily readings (kWh and kVArh consumed).
     */
    fun calculateRequiredKvarFromDailyReading(
        kwhConsumed: Double,
        kvarhConsumed: Double,
        operationalHours: Double = 12.0,
        targetPf: Double = 0.99
    ): KvarCalculationResult {
        val safeHours = operationalHours.coerceAtLeast(1.0)
        val avgLoadKw = (kwhConsumed / safeHours).coerceAtLeast(1.0)

        val kvah = sqrt(kwhConsumed.pow(2) + kvarhConsumed.pow(2))
        val currentPf = if (kvah > 0.0) (kwhConsumed / kvah).coerceIn(0.2, 1.0) else 0.85

        val baseResult = calculateRequiredKvar(avgLoadKw, currentPf, targetPf)

        // Excess kVArh consumed during this period beyond target PF allowance
        val targetTanPhi = if (targetPf >= 1.0) 0.0 else sqrt(1.0 - targetPf.pow(2)) / targetPf
        val targetKvarh = kwhConsumed * targetTanPhi
        val excessKvarh = (kvarhConsumed - targetKvarh).coerceAtLeast(0.0)

        return baseResult.copy(
            excessDailyKvarh = (excessKvarh * 100.0).roundToInt() / 100.0
        )
    }

    /**
     * Standard commercial capacitor steps available in Indian market:
     * 1, 2, 3, 5, 7.5, 10, 12.5, 15, 20, 25, 30, 40, 50, 75, 100, 125, 150, 200, 250, 300 kVAr
     */
    fun recommendStandardBankSize(rawKvar: Double): Double {
        if (rawKvar <= 0.0) return 0.0
        val standardRatings = listOf(
            1.0, 2.0, 3.0, 5.0, 7.5, 10.0, 12.5, 15.0, 20.0, 25.0, 30.0, 40.0, 50.0,
            60.0, 75.0, 100.0, 125.0, 150.0, 175.0, 200.0, 250.0, 300.0, 400.0, 500.0
        )
        return standardRatings.firstOrNull { it >= rawKvar } ?: (ceil(rawKvar / 50.0) * 50.0)
    }

    /**
     * Suggests practical APFC panel steps for capacitor banks (e.g., 25 kVAr -> [10, 10, 5])
     */
    fun generateApfcSteps(totalKvar: Int): List<Int> {
        if (totalKvar <= 0) return emptyList()
        val steps = mutableListOf<Int>()
        var remaining = totalKvar
        while (remaining > 0) {
            when {
                remaining >= 50 -> { steps.add(50); remaining -= 50 }
                remaining >= 25 -> { steps.add(25); remaining -= 25 }
                remaining >= 20 -> { steps.add(20); remaining -= 20 }
                remaining >= 15 -> { steps.add(15); remaining -= 15 }
                remaining >= 10 -> { steps.add(10); remaining -= 10 }
                remaining >= 5 -> { steps.add(5); remaining -= 5 }
                else -> { steps.add(remaining); remaining = 0 }
            }
        }
        return steps
    }

    /**
     * Parse numeric kW value from string like "65 kW" or "50 HP" or "100"
     */
    fun parseLoadKw(sanctionedLoad: String): Double {
        val clean = sanctionedLoad.trim().lowercase(Locale.US)
        val digits = clean.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 50.0
        return if (clean.contains("hp")) {
            ((digits * 0.746 * 10.0).roundToInt() / 10.0).coerceAtLeast(1.0)
        } else {
            digits.coerceAtLeast(1.0)
        }
    }

    fun getPfRating(pf: Double): PfRating {
        return when {
            pf >= 0.95 -> PfRating.EXCELLENT
            pf >= 0.85 -> PfRating.FAIR
            else -> PfRating.POOR
        }
    }

    fun formatNumber(value: Double, decimals: Int = 2): String {
        return String.format(Locale.US, "%.${decimals}f", value)
    }
}

