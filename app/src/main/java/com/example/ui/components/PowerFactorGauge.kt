package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PowerFactorGood
import com.example.ui.theme.PowerFactorPenalty
import com.example.ui.theme.PowerFactorWarning
import com.example.util.PfRating
import com.example.util.PowerFactorCalculator
import java.util.Locale

@Composable
fun PowerFactorGauge(
    pfValue: Double,
    isHindi: Boolean,
    onInfoClick: () -> Unit,
    onCalculateKvarClick: () -> Unit = {},
    loadKw: Double = 50.0,
    modifier: Modifier = Modifier
) {
    val animatedPf by animateFloatAsState(
        targetValue = pfValue.toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "pfAnimation"
    )

    val rating = PowerFactorCalculator.getPfRating(pfValue)
    var selectedTargetPf by remember { mutableDoubleStateOf(0.99) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pf_gauge_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(rating.color.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "PF Icon",
                            tint = rating.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "पॉवर फॅक्टर स्थिती (Power Factor)" else "Power Factor Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isHindi) "आदर्श रेंज: 0.950 - 1.000 (Incentive Zone)" else "Ideal Range: 0.950 - 1.000",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .testTag("pf_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "PF Info Guide",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Semicircular Radial Gauge + Digital Readout
            val trackBgColor = MaterialTheme.colorScheme.surfaceVariant
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 210.dp, height = 116.dp)
                    .padding(top = 4.dp)
            ) {
                Canvas(modifier = Modifier.size(196.dp, 106.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, (size.height * 2) - strokeWidth)
                    val arcOffset = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Track background (180 degree semicircle)
                    drawArc(
                        color = trackBgColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Penalty zone marker (0.0 to 0.85)
                    drawArc(
                        color = PowerFactorPenalty.copy(alpha = 0.18f),
                        startAngle = 180f,
                        sweepAngle = 180f * 0.85f,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )

                    // Warning zone marker (0.85 to 0.95)
                    drawArc(
                        color = PowerFactorWarning.copy(alpha = 0.22f),
                        startAngle = 180f + (180f * 0.85f),
                        sweepAngle = 180f * 0.10f,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )

                    // Good zone marker (0.95 to 1.0)
                    drawArc(
                        color = PowerFactorGood.copy(alpha = 0.28f),
                        startAngle = 180f + (180f * 0.95f),
                        sweepAngle = 180f * 0.05f,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active value arc
                    val activeSweep = (animatedPf * 180f).coerceIn(0f, 180f)
                    drawArc(
                        color = rating.color,
                        startAngle = 180f,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 34.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.3f", pfValue),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = rating.color
                    )
                    Text(
                        text = "Cos \u03A6 (PF)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rating Pill Badge
            Box(
                modifier = Modifier
                    .background(
                        color = rating.color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = rating.color.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (rating == PfRating.POOR) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = rating.color,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) rating.labelHi else rating.labelEn,
                        color = rating.color,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // kVAr Capacitor Recommendation Banner with Target PF Selector
            Spacer(modifier = Modifier.height(14.dp))

            val kvarResult = PowerFactorCalculator.calculateRequiredKvar(
                loadKw = loadKw,
                initialPf = pfValue,
                targetPf = selectedTargetPf
            )

            Card(
                onClick = onCalculateKvarClick,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kvar_recommendation_banner")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "आवश्यक kVAr (Target ${String.format(Locale.US, "%.2f", selectedTargetPf)} PF साठी):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "~${String.format(Locale.US, "%.1f", kvarResult.requiredKvar)} kVAr",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${kvarResult.recommendedBankSize.toInt()}k बँक)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onCalculateKvarClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "कॅल्क्युलेटर",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Target PF Quick Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target PF:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(0.95, 0.98, 0.99, 1.00).forEach { targetOption ->
                            val isTargetSelected = kotlin.math.abs(selectedTargetPf - targetOption) < 0.002
                            FilterChip(
                                selected = isTargetSelected,
                                onClick = { selectedTargetPf = targetOption },
                                label = {
                                    Text(
                                        text = String.format(Locale.US, "%.2f", targetOption),
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
