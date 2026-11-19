package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.GoogleSheetSyncConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoogleSheetSyncDialog(
    sheetConfig: GoogleSheetSyncConfig,
    isSyncing: Boolean,
    isHindi: Boolean,
    onSyncFromLink: (sheetUrl: String, tabNamesCsv: String) -> Unit,
    onSyncFromFileUri: (Uri) -> Unit,
    onSyncBundledSheet: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var sheetUrlInput by remember(sheetConfig.sheetUrl) { mutableStateOf(sheetConfig.sheetUrl) }
    var tabNamesInput by remember(sheetConfig.tabNamesCsv) { mutableStateOf(sheetConfig.tabNamesCsv) }
    var urlError by remember { mutableStateOf<String?>(null) }

    val sheetFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onSyncFromFileUri(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("google_sheet_sync_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Google Sheet Sync",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isHindi) "गूगल शीट सिंक (Customer Tabs)" else "Google Sheet Customer Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isHindi) "Date, Time, kWh, kVArh, kVAh व Remark (PF)" else "Sync customer-wise tabs from Google Sheet",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Saved Sync Status Card
                if (sheetConfig.lastSyncTimestamp > 0L) {
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(Date(sheetConfig.lastSyncTimestamp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isHindi)
                                        "शेवटचे सिंक: $dateStr"
                                    else
                                        "Last Sync: $dateStr",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = if (isHindi)
                                        "${sheetConfig.lastSyncedTabsCount} ग्राहक टॅब • ${sheetConfig.lastSyncedReadingsCount} एन्ट्री सिंक"
                                    else
                                        "${sheetConfig.lastSyncedTabsCount} customer tabs • ${sheetConfig.lastSyncedReadingsCount} entries synced",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 1. Google Sheet Link Input
                OutlinedTextField(
                    value = sheetUrlInput,
                    onValueChange = {
                        sheetUrlInput = it
                        urlError = null
                    },
                    label = { Text(if (isHindi) "Google Sheet लिंक येथे पेस्ट करा *" else "Paste Google Sheet Link *") },
                    placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFF1D6F42)
                        )
                    },
                    isError = urlError != null,
                    supportingText = {
                        if (urlError != null) {
                            Text(urlError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(
                                text = if (isHindi)
                                    "Google Sheet मध्ये Share → 'Anyone with the link (Viewer)' ठेवा म्हणजे सर्व कस्टमर-वाईज टॅब्स आपोआप सिंक होतील."
                                else
                                    "Set sheet sharing to 'Anyone with the link can view' to sync all customer-wise tabs automatically.",
                                fontSize = 10.5.sp
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_sheet_url_input")
                )

                // 2. Optional Customer Tab Names input
                OutlinedTextField(
                    value = tabNamesInput,
                    onValueChange = { tabNamesInput = it },
                    label = { Text(if (isHindi) "विशिष्ट टॅब नावे (ऐच्छिक - स्वल्पविरामाने)" else "Specific Tab Names (Optional, comma-separated)") },
                    placeholder = { Text("TIRUPATI CRUSHER, SHREE GANESH") },
                    supportingText = {
                        Text(
                            text = if (isHindi)
                                "रिकामे सोडल्यास शीटमधील सर्व कस्टमर टॅब्स आपोआप घेतले जातील."
                            else
                                "Leave blank to auto-detect and sync all customer tabs from the workbook.",
                            fontSize = 10.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_sheet_tabs_input")
                )

                // Primary Sync Button
                Button(
                    onClick = {
                        val trimmed = sheetUrlInput.trim()
                        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                            urlError = if (isHindi) "वैध https:// Google Sheet लिंक टाका" else "Enter a valid https:// Google Sheet URL"
                        } else {
                            onSyncFromLink(trimmed, tabNamesInput.trim())
                        }
                    },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6F42)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sync_google_sheet_link_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isHindi) "शीट सिंक होत आहे..." else "Syncing Sheet...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "शीट लिंकवरून सिंक करा (Sync Sheet)" else "Sync Customer Tabs from Link",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. Pick Local Excel (.xlsx) / CSV File from Device or Drive
                OutlinedButton(
                    onClick = {
                        sheetFilePicker.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "text/csv",
                                "text/comma-separated-values",
                                "text/*",
                                "*/*"
                            )
                        )
                    },
                    enabled = !isSyncing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_sheet_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "Excel (.xlsx) / CSV शीट निवडा" else "Import .xlsx / .csv Workbook",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A73E8)
                    )
                }

                // 4. Quick Re-Import Attached Offline Sample Sheet
                OutlinedButton(
                    onClick = onSyncBundledSheet,
                    enabled = !isSyncing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_bundled_tirupati_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "ऑफलाईन नमुना शीट लोड करा (TIRUPATI - 54 नोंदी)" else "Load Offline Sample Sheet (54 rows)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }

                // 5. Troubleshooting Tips if New Entries Do Not Appear
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isHindi) "नवीन एन्ट्री येत नसल्यास काय करावे?" else "New entries not showing? Check:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isHindi)
                                "१. Google Sheet मध्ये Share → 'Anyone with the link' (Viewer) करून ती /edit लिंक येथे पेस्ट करा (Publish to web लिंकवर Google ५ मिनिटे उशिरा अपडेट देते).\n" +
                                    "२. शीटमध्ये नवीन एन्ट्री टाकल्यावर टिक (✓) / Enter दाबून सेव्ह करा आणि नंतर डॅशबोर्डवर 'आता सिंक करा' दाबा.\n" +
                                    "३. प्रत्येक टॅबमध्ये DATE, TIME, Kwh, KvArh, KvAh, PF कॉलम्स असणे आवश्यक आहे."
                            else
                                "1. Use Share → 'Anyone with the link (Viewer)' URL (/edit) rather than 'Publish to web' (which caches 5 mins).\n" +
                                    "2. Press ✓ / Enter in Google Sheets after adding a row, then tap 'Sync Now'.\n" +
                                    "3. Ensure DATE, TIME, Kwh, KvArh, KvAh, PF headers exist.",
                            fontSize = 10.5.sp,
                            lineHeight = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_sheet_sync_dialog_button")
            ) {
                Text(if (isHindi) "बंद करा (Close)" else "Close")
            }
        }
    )
}
