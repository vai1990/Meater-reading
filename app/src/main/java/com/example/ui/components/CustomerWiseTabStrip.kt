package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Customer

/**
 * Customer-Wise Tab Bar mirroring the customer-wise tabs from the user's Google Sheet.
 * Tapping any customer tab switches all dashboard KPIs, PF gauge, and reading logs to that customer.
 */
@Composable
fun CustomerWiseTabStrip(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    isHindi: Boolean,
    hasSavedSheetLink: Boolean,
    onSelectCustomer: (Customer) -> Unit,
    onOpenSheetSync: () -> Unit,
    onAddCustomer: () -> Unit,
    modifier: Modifier = Modifier,
    isSyncing: Boolean = false,
    onQuickRefreshSheet: () -> Unit = onOpenSheetSync
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_wise_tab_strip"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "ग्राहक टॅब व Google Sheet सिंक" else "Customer Tabs & Sheet Sync",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick 1-Tap Sheet Refresh / Link Pill in Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hasSavedSheetLink) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.16f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .clickable(enabled = !isSyncing) { onQuickRefreshSheet() }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                                .testTag("quick_refresh_sheet_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    color = Color(0xFF10B981),
                                    strokeWidth = 1.8.dp,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Refresh Sheet Now",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSyncing) {
                                    if (isHindi) "सिंक सुरू..." else "Syncing..."
                                } else {
                                    if (isHindi) "आता रिफ्रेश करा" else "Sync Now"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .clickable { onOpenSheetSync() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Sheet Link Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (hasSavedSheetLink) {
                                if (isHindi) "शीट लिंक" else "Linked"
                            } else {
                                if (isHindi) "शीट जोडा" else "Link Sheet"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(customers, key = { it.id }) { customer ->
                    val isSelected = selectedCustomer?.id == customer.id
                    val shape = RoundedCornerShape(14.dp)

                    Row(
                        modifier = Modifier
                            .clip(shape)
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                Color(0xFF1E64C8)
                                            )
                                        ),
                                        shape = shape
                                    )
                                } else {
                                    Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                            shape
                                        )
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape
                                        )
                                }
                            )
                            .clickable { onSelectCustomer(customer) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("customer_tab_chip_${customer.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Business,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = customer.name,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick Google Sheet Sync Tab Chip
                item(key = "sheet_sync_tab_chip") {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .clickable {
                                if (hasSavedSheetLink) onQuickRefreshSheet() else onOpenSheetSync()
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("customer_tab_sheet_sync_chip"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasSavedSheetLink) Icons.Default.Sync else Icons.Default.TableChart,
                            contentDescription = "Google Sheet Sync",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isHindi) "शीट सिंक" else "Sheet Sync",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quick Add Customer Tab Chip
                item(key = "add_customer_tab_chip") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onAddCustomer() }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("customer_tab_add_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Customer Tab",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "नवीन ग्राहक" else "Add",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
