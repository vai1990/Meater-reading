package com.example.util

import android.content.Context

enum class AppVisualTheme(
    val id: String,
    val labelMr: String,
    val labelEn: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val headerStartHex: Long,
    val headerEndHex: Long,
    val backgroundHex: Long,
    val surfaceHex: Long,
    val surfaceVariantHex: Long,
    val isDark: Boolean = false
) {
    EMERALD_ENERGY(
        id = "EMERALD_ENERGY",
        labelMr = "एमराल्ड एनर्जी (Emerald Pro)",
        labelEn = "Emerald Energy Pro",
        primaryHex = 0xFF047857,
        secondaryHex = 0xFF0D9488,
        headerStartHex = 0xFF064E3B,
        headerEndHex = 0xFF047857,
        backgroundHex = 0xFFF0FDF4,
        surfaceHex = 0xFFFFFFFF,
        surfaceVariantHex = 0xFFD1FAE5,
        isDark = false
    ),
    CYBER_DARK(
        id = "CYBER_DARK",
        labelMr = "सायबर डार्क निऑन (Dark Mode)",
        labelEn = "Cyber Dark Neon",
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFF10B981,
        headerStartHex = 0xFF060B14,
        headerEndHex = 0xFF111C35,
        backgroundHex = 0xFF090D16,
        surfaceHex = 0xFF131C2E,
        surfaceVariantHex = 0xFF1E293B,
        isDark = true
    ),
    ROYAL_INDIGO(
        id = "ROYAL_INDIGO",
        labelMr = "रॉयल इंडिगो (Royal Indigo)",
        labelEn = "Royal Indigo",
        primaryHex = 0xFF4338CA,
        secondaryHex = 0xFF7C3AED,
        headerStartHex = 0xFF1E1B4B,
        headerEndHex = 0xFF4338CA,
        backgroundHex = 0xFFEEF2FF,
        surfaceHex = 0xFFFFFFFF,
        surfaceVariantHex = 0xFFE0E7FF,
        isDark = false
    ),
    SOLAR_GOLD(
        id = "SOLAR_GOLD",
        labelMr = "सोलर अंबर गोल्ड (Solar Gold)",
        labelEn = "Solar Amber Gold",
        primaryHex = 0xFFB45309,
        secondaryHex = 0xFFD97706,
        headerStartHex = 0xFF78350F,
        headerEndHex = 0xFFB45309,
        backgroundHex = 0xFFFFFBEB,
        surfaceHex = 0xFFFFFFFF,
        surfaceVariantHex = 0xFFFEF3C7,
        isDark = false
    ),
    ELECTRIC_BLUE(
        id = "ELECTRIC_BLUE",
        labelMr = "इलेक्ट्रिक ब्लू (Electric Blue)",
        labelEn = "Electric Sapphire Blue",
        primaryHex = 0xFF1D4ED8,
        secondaryHex = 0xFF0284C7,
        headerStartHex = 0xFF0F172A,
        headerEndHex = 0xFF1D4ED8,
        backgroundHex = 0xFFF1F5F9,
        surfaceHex = 0xFFFFFFFF,
        surfaceVariantHex = 0xFFE2E8F0,
        isDark = false
    );

    companion object {
        fun fromId(id: String?): AppVisualTheme {
            return entries.find { it.id == id } ?: CYBER_DARK
        }
    }
}

enum class AppIconStyle(
    val id: String,
    val labelMr: String,
    val labelEn: String,
    val accentHex: Long,
    val bgHex: Long
) {
    SMART_METER_3D(
        id = "SMART_METER_3D",
        labelMr = "3D स्मार्ट मीटर प्रो (नवीन आयकॉन)",
        labelEn = "3D Smart Meter Pro (New)",
        accentHex = 0xFF047857,
        bgHex = 0xFFECFDF5
    ),
    ELECTRIC_BOLT(
        id = "ELECTRIC_BOLT",
        labelMr = "इलेक्ट्रिक बोल्ट (Electric Blue)",
        labelEn = "Electric Bolt Blue",
        accentHex = 0xFF1A73E8,
        bgHex = 0xFFE8F0FE
    ),
    DIGITAL_METER(
        id = "DIGITAL_METER",
        labelMr = "डिजिटल मीटर (Emerald Green)",
        labelEn = "Digital Meter Green",
        accentHex = 0xFF137333,
        bgHex = 0xFFE6F4EA
    ),
    INDUSTRIAL_POWER(
        id = "INDUSTRIAL_POWER",
        labelMr = "इंडस्ट्रियल पॉवर (Amber Orange)",
        labelEn = "Industrial Power Orange",
        accentHex = 0xFFE65100,
        bgHex = 0xFFFFF3E0
    ),
    SOLAR_GRID(
        id = "SOLAR_GRID",
        labelMr = "सोलर एनर्जी ग्रिड (Gold)",
        labelEn = "Solar Energy Grid",
        accentHex = 0xFFF57F17,
        bgHex = 0xFFFFFDE7
    ),
    HIGH_VOLTAGE(
        id = "HIGH_VOLTAGE",
        labelMr = "हाय व्होल्टेज शिल्ड (Royal Purple)",
        labelEn = "High Voltage Shield",
        accentHex = 0xFF6A1B9A,
        bgHex = 0xFFF3E5F5
    );

    companion object {
        fun fromId(id: String?): AppIconStyle {
            return entries.find { it.id == id } ?: SMART_METER_3D
        }
    }
}

data class AppBrandingConfig(
    val appName: String = "Smart Meter Pro",
    val customSubtitle: String = "",
    val iconStyle: AppIconStyle = AppIconStyle.SMART_METER_3D,
    val visualTheme: AppVisualTheme = AppVisualTheme.CYBER_DARK
)

class AppBrandingManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "smart_meter_branding_prefs",
        Context.MODE_PRIVATE
    )

    fun getBranding(): AppBrandingConfig {
        val name = prefs.getString("app_name", "Smart Meter Pro")
            ?.takeIf { it.isNotBlank() }
            ?: "Smart Meter Pro"
        val subtitle = prefs.getString("custom_subtitle", "") ?: ""
        val iconStyle = AppIconStyle.fromId(prefs.getString("icon_style", AppIconStyle.SMART_METER_3D.id))
        val visualTheme = AppVisualTheme.fromId(prefs.getString("visual_theme_dark_v1", AppVisualTheme.CYBER_DARK.id))
        return AppBrandingConfig(
            appName = name,
            customSubtitle = subtitle,
            iconStyle = iconStyle,
            visualTheme = visualTheme
        )
    }

    fun saveBranding(
        appName: String,
        customSubtitle: String,
        iconStyle: AppIconStyle,
        visualTheme: AppVisualTheme = getBranding().visualTheme
    ): AppBrandingConfig {
        val cleanName = appName.trim().ifBlank { "Smart Meter Pro" }.take(40)
        val cleanSub = customSubtitle.trim().take(60)
        prefs.edit()
            .putString("app_name", cleanName)
            .putString("custom_subtitle", cleanSub)
            .putString("icon_style", iconStyle.id)
            .putString("visual_theme_dark_v1", visualTheme.id)
            .apply()
        return AppBrandingConfig(
            appName = cleanName,
            customSubtitle = cleanSub,
            iconStyle = iconStyle,
            visualTheme = visualTheme
        )
    }
}
