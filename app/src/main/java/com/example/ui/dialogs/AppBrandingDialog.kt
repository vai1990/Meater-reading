package com.example.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.util.AppBrandingConfig
import com.example.util.AppIconStyle
import com.example.util.AppVisualTheme

@Composable
fun AppBrandIcon(
    style: AppIconStyle,
    size: Dp = 34.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape((size.value * 0.28f).dp)
    if (style == AppIconStyle.SMART_METER_3D) {
        Image(
            painter = painterResource(id = R.drawable.ic_smart_meter_logo_1794757725848),
            contentDescription = "Smart Meter Pro Icon",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
        )
    } else {
        val accent = Color(style.accentHex)
        val bg = Color(style.bgHex)
        val vector = when (style) {
            AppIconStyle.ELECTRIC_BOLT -> Icons.Default.Bolt
            AppIconStyle.DIGITAL_METER -> Icons.Default.ElectricMeter
            AppIconStyle.INDUSTRIAL_POWER -> Icons.Default.Factory
            AppIconStyle.SOLAR_GRID -> Icons.Default.WbSunny
            AppIconStyle.HIGH_VOLTAGE -> Icons.Default.Shield
            AppIconStyle.SMART_METER_3D -> Icons.Default.ElectricMeter
        }
        Box(
            modifier = modifier
                .size(size)
                .background(bg, shape)
                .border(1.dp, accent.copy(alpha = 0.35f), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = vector,
                contentDescription = style.labelEn,
                tint = accent,
                modifier = Modifier.size((size.value * 0.62f).dp)
            )
        }
    }
}

@Composable
fun AppBrandingDialog(
    currentBranding: AppBrandingConfig,
    isHindi: Boolean,
    onSave: (appName: String, customSubtitle: String, iconStyle: AppIconStyle, visualTheme: AppVisualTheme) -> Unit,
    onDismiss: () -> Unit
) {
    var appNameInput by remember(currentBranding) { mutableStateOf(currentBranding.appName) }
    var subtitleInput by remember(currentBranding) { mutableStateOf(currentBranding.customSubtitle) }
    var selectedStyle by remember(currentBranding) { mutableStateOf(currentBranding.iconStyle) }
    var selectedTheme by remember(currentBranding) { mutableStateOf(currentBranding.visualTheme) }

    val presetNames = listOf(
        "Smart Meter Pro",
        "वैभव मीटर प्रो",
        "ऊर्जा मीटर ट्रॅकर",
        "MSEDCL मीटर लॉग",
        "kVAr & Meter Pro"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .testTag("app_branding_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(selectedTheme.primaryHex).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color(selectedTheme.primaryHex),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "अॅपचा लूक, थीम व नाव बदला" else "Change App Look, Theme & Name",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isHindi)
                                    "तुमच्या आवडीप्रमाणे अॅपचा कलर लूक, नाव व आयकॉन निवडा"
                                else
                                    "Choose app visual theme, header title, and icon style",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_branding_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Live Gradient Header Preview Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(selectedTheme.headerStartHex),
                                    Color(selectedTheme.headerEndHex)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppBrandIcon(style = selectedStyle, size = 52.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "लाईव्ह प्रिव्ह्यू • ${selectedTheme.labelMr}" else "Live Preview • ${selectedTheme.labelEn}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = appNameInput.ifBlank { "Smart Meter Pro" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = subtitleInput.ifBlank {
                                    if (isHindi) "दैनिक मीटर रीडिंग व kVAr डॅशबोर्ड" else "Daily Meter Reading & kVAr Dashboard"
                                },
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // 1. Choose App Look / Visual Theme
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHindi) "१. अॅपचा लूक / थीम निवडा (Choose App Look):" else "1. Choose App Visual Theme:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    AppVisualTheme.entries.forEach { theme ->
                        val isSelected = selectedTheme == theme
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(theme.primaryHex) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedTheme = theme }
                                .testTag("select_visual_theme_${theme.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    Color(theme.primaryHex).copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(theme.headerStartHex),
                                                        Color(theme.primaryHex)
                                                    )
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isHindi) theme.labelMr else theme.labelEn,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = theme.labelEn,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(theme.primaryHex),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. App Name Input + Preset Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isHindi) "२. अॅपचे नाव (App Name):" else "2. App Name:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = appNameInput,
                        onValueChange = { appNameInput = it },
                        label = { Text(if (isHindi) "अॅपचे नवीन नाव टाका" else "Enter Custom App Name") },
                        placeholder = { Text("Smart Meter Pro") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_app_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetNames.forEach { preset ->
                            FilterChip(
                                selected = appNameInput == preset,
                                onClick = { appNameInput = preset },
                                label = { Text(preset, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }

                // 3. Custom Subtitle / Company Name
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isHindi)
                            "३. कंपनीचे नाव / सबटायटल (ऐच्छिक):"
                        else
                            "3. Custom Subtitle / Company Name (Optional):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = subtitleInput,
                        onValueChange = { subtitleInput = it },
                        label = {
                            Text(
                                if (isHindi) "उदा. वैभव इलेक्ट्रिकल्स अँड इंजिनिअर्स"
                                else "e.g., Vaibhav Electricals & Engineers"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_app_subtitle_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 4. Choose App Icon Style
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHindi) "४. अॅपचा आयकॉन निवडा (Choose App Icon):" else "4. Select App Icon:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    AppIconStyle.entries.forEach { style ->
                        val isSelected = selectedStyle == style
                        val borderColor = if (isSelected) Color(style.accentHex) else Color.Transparent
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                                .clickable { selectedStyle = style }
                                .testTag("select_icon_style_${style.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    Color(style.bgHex)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppBrandIcon(style = style, size = 44.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isHindi) style.labelMr else style.labelEn,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = style.labelEn,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(style.accentHex),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            appNameInput = "Smart Meter Pro"
                            subtitleInput = ""
                            selectedStyle = AppIconStyle.SMART_METER_3D
                            selectedTheme = AppVisualTheme.EMERALD_ENERGY
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            text = if (isHindi) "डिफॉल्ट रिसेट" else "Reset Default",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            onSave(
                                appNameInput.trim().ifBlank { "Smart Meter Pro" },
                                subtitleInput.trim(),
                                selectedStyle,
                                selectedTheme
                            )
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_branding_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(selectedTheme.primaryHex)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            text = if (isHindi) "लूक व नाव सेव्ह करा" else "Save Look & Name",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
