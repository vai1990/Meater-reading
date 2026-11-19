package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PowerFactorGood
import com.example.ui.theme.PowerFactorPenalty
import com.example.ui.theme.PowerFactorWarning

@Composable
fun PfGuideDialog(
    isHindi: Boolean,
    onOpenKvarCalculator: () -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("pf_guide_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "पावर फैक्टर गाइड (Power Factor Guide)" else "Power Factor & Tariff Guide",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Formula Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "कैलकुलेशन सूत्र (Formulas):" else "Calculations:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Power Factor (PF) = kWh / kVAh",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "• kVAh \u2248 \u221A(kWh\u00B2 + kVArh\u00B2)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• Active Energy = kWh (सक्रिय ऊर्जा - वास्तविक कार्य)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Reactive Energy = kVArh (चुंबकीय ऊर्जा - मोटर्स/ट्रांसफार्मर)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tier 1: Excellent (Incentive)
                GuideTierCard(
                    title = "PF \u2265 0.95 - उत्कृष्ट (Incentive Range)",
                    desc = if (isHindi)
                        "बिजली वितरण कंपनी (DISCOM) द्वारा मासिक बिल पर 0.5% से 2% तक इंसेंटिव / रिबेट छूट दी जाती है।"
                    else
                        "Eligible for DISCOM power factor rebate/incentive on monthly energy charges.",
                    color = PowerFactorGood,
                    icon = Icons.Default.CheckCircle
                )

                // Tier 2: Fair
                GuideTierCard(
                    title = "PF 0.85 - 0.94 - सामान्य (Fair)",
                    desc = if (isHindi)
                        "सामान्य परिचालन स्थिति। कोई पेनल्टी नहीं लगती, परंतु 0.95 तक सुधार करने पर बिल में बचत होगी।"
                    else
                        "Standard operating range. No penalties, but optimizing towards 0.95 unlocks bill rebates.",
                    color = PowerFactorWarning,
                    icon = Icons.Default.Info
                )

                // Tier 3: Penalty
                GuideTierCard(
                    title = "PF < 0.85 - पेनल्टी अलर्ट (Penalty Warning!)",
                    desc = if (isHindi)
                        "चेतावनी! DISCOM द्वारा लो पावर फैक्टर सरचार्ज (Low PF Penalty) लगाया जाता है। तुरंत APFC पैनल या कैपेसिटर बैंक चालू करें।"
                    else
                        "Danger! Heavy DISCOM low power factor surcharge applied. Service APFC panel or turn on capacitor banks.",
                    color = PowerFactorPenalty,
                    icon = Icons.Default.Warning
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_pf_guide_button")
            ) {
                Text(if (isHindi) "समजले (Close)" else "Got it")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenKvarCalculator()
                },
                modifier = Modifier.testTag("guide_open_kvar_calc_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("kVAr कॅल्क्युलेटर", fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun GuideTierCard(
    title: String,
    desc: String,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = color
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
