package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.dialogs.AppBrandIcon
import com.example.util.DriveBackupFrequency
import com.example.util.DriveBackupSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportRestoreScreen(
    viewModel: MeterViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isHindi = uiState.isHindi

    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var restoreOptionMerge by remember { mutableStateOf(true) }

    // Launcher to select JSON or Excel backup file from Google Drive or device storage
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.handlePickedBackupUri(context, it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: App Name & Icon Customizer Card (अॅपचे नाव आणि आयकॉन बदला)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_app_branding_card"),
            colors = CardDefaults.cardColors(
                containerColor = Color(uiState.brandingConfig.iconStyle.bgHex)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AppBrandIcon(
                        style = uiState.brandingConfig.iconStyle,
                        size = 50.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = uiState.brandingConfig.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = if (isHindi)
                                "अॅपचे नाव, सबटायटल व आयकॉन बदला"
                            else
                                "Change App Name, Subtitle & Icon",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.openBrandingDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(uiState.brandingConfig.iconStyle.accentHex)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("settings_change_app_name_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "नाव/आयकॉन बदला" else "Customize",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Top Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("export_restore_header_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        contentDescription = "Backup and Restore",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (isHindi) "गूगल ड्राइव्ह बॅकअप, रिस्टोर व रिपोर्ट्स" else "Google Drive Backup, Restore & Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isHindi) "Google Drive क्लाउड बॅकअप-रिस्टोर, Google Sheet सिंक, Excel व PDF रिपोर्ट जनरेटर" else "Cloud backup & restore, Google Sheet sync, Excel (.csv) and PDF reports",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 0A: Google Sheet Customer-Wise Sync Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_google_sheet_sync_card"),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E9)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFC8E6C9), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Google Sheet Sync",
                            tint = Color(0xFF1D6F42),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "गूगल शीट कस्टमर-वाईज सिंक (Google Sheet Sync)" else "Google Sheet Customer-Wise Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = if (uiState.sheetSyncConfig.sheetUrl.isNotBlank())
                                (if (isHindi) "लिंक जोडलेली आहे • ${uiState.sheetSyncConfig.lastSyncedTabsCount} कस्टमर टॅब सिंक" else "Sheet linked • ${uiState.sheetSyncConfig.lastSyncedTabsCount} tabs synced")
                            else
                                (if (isHindi) "Date, Time, kWh, kVArh, kVAh व Remark (PF) कस्टमर टॅबनुसार सिंक करा" else "Sync customer-wise sheet tabs via URL or CSV/XLSX"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.openGoogleSheetSyncDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6F42)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_sheet_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "शीट लिंक सिंक करा" else "Link & Sync Sheet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Section 0: Colleague Live Cloud Sync Hub (सहकारी लाईव्ह सिंक)
        val ws = uiState.activeWorkspace
        val inviteCode = ws?.inviteCode ?: "TEAM101"
        val memberCount = ws?.memberNames?.size ?: 1
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_colleague_sync_card"),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1F8E9)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFCEEAD6), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Colleague Sync",
                            tint = Color(0xFF137333),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "सहकारी लाईव्ह सिंक (Colleague Team Sync)" else "Colleague Live Cloud Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = if (isHindi)
                                "टीम कोड: $inviteCode • $memberCount सहकारी • ${uiState.cloudReadings.size} एन्ट्री सिंक"
                            else
                                "Team Code: $inviteCode • $memberCount members • ${uiState.cloudReadings.size} synced entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = if (isHindi)
                        "हा एप तुमच्या सहकाऱ्याला द्या आणि त्यांना तुमचा टीम कोड ($inviteCode) टाकायला सांगा. त्यांनी केलेली प्रत्येक मीटर रीडिंग एन्ट्री तुमच्याकडे रिअल-टाईममध्ये आपोआप सिंक होईल!"
                    else
                        "Share your Team Code ($inviteCode) with your colleague. Every meter reading they enter will automatically sync to your phone in real time!",
                    fontSize = 12.sp,
                    color = Color(0xFF33691E),
                    lineHeight = 17.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.openTeamSyncDialog() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_team_sync_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF137333)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "सहकारी टीम सेटिंग व कोड" else "Manage Team & Code",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.syncAllToColleagueCloudNow() },
                        modifier = Modifier.testTag("settings_sync_team_now_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = Color(0xFF137333),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "आता सिंक करा" else "Sync Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF137333)
                        )
                    }
                }
            }
        }

        // Section: Google Drive Auto Backup & Restore Hub
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("google_drive_backup_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0xFFE8F0FE), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Google Drive",
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isHindi) "गूगल ड्राइव्ह बॅकअप व रिस्टोर" else "Google Drive Backup & Restore",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE6F4EA), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Cloud",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF137333)
                                )
                            }
                        }
                        Text(
                            text = if (isHindi) "Google Drive वर डेटा सुरक्षित करा आणि कधीही १-क्लिकवर रिस्टोर करा" else "Backup & restore meter readings via Google Drive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.driveConfig.isAutoBackupEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.updateDriveConfig(
                                enabled = isChecked,
                                frequency = uiState.driveConfig.frequency,
                                wifiOnly = uiState.driveConfig.wifiOnly,
                                includePdf = uiState.driveConfig.includePdf,
                                email = uiState.driveConfig.userEmail
                            )
                        },
                        modifier = Modifier.testTag("drive_auto_backup_switch")
                    )
                }

                // Account & Last Sync Status Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color(0xFF1A73E8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "Google Drive खाते: " else "Google Account: ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.driveConfig.userEmail,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val latestSnap = uiState.driveSnapshots.firstOrNull()
                        val effectiveSyncTime = if (uiState.driveConfig.lastSyncTimestamp > 0)
                            uiState.driveConfig.lastSyncTimestamp
                        else latestSnap?.timestamp ?: 0L

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (effectiveSyncTime > 0) Icons.Default.CheckCircle else Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (effectiveSyncTime > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val lastSyncText = if (effectiveSyncTime > 0) {
                                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                (if (isHindi) "शेवटचा बॅकअप सिंक: " else "Last Synced: ") + sdf.format(Date(effectiveSyncTime))
                            } else {
                                if (isHindi) "अद्याप बॅकअप नाही (Not synced yet)" else "Never synced yet"
                            }
                            Text(
                                text = lastSyncText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (effectiveSyncTime > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                            )
                        }

                        if (latestSnap != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi)
                                        "क्लाउडमध्ये उपलब्ध: ${latestSnap.customerCount} ग्राहक • ${latestSnap.readingCount} रीडिंग्स (${uiState.driveSnapshots.size} बॅकअप प्रती)"
                                    else
                                        "Cloud Ready: ${latestSnap.customerCount} customers • ${latestSnap.readingCount} readings (${uiState.driveSnapshots.size} snapshots)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (uiState.driveConfig.isAutoBackupEnabled) {
                    // Frequency Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isHindi) "ऑटो बॅकअप वेळ (Auto-Backup Frequency):" else "Auto-Backup Frequency:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(DriveBackupFrequency.AFTER_READING, DriveBackupFrequency.DAILY).forEach { freq ->
                                val selected = uiState.driveConfig.frequency == freq
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        viewModel.updateDriveConfig(
                                            enabled = true,
                                            frequency = freq,
                                            wifiOnly = uiState.driveConfig.wifiOnly,
                                            includePdf = uiState.driveConfig.includePdf,
                                            email = uiState.driveConfig.userEmail
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (isHindi) freq.labelHi else freq.labelEn,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(DriveBackupFrequency.WEEKLY, DriveBackupFrequency.MANUAL).forEach { freq ->
                                val selected = uiState.driveConfig.frequency == freq
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        viewModel.updateDriveConfig(
                                            enabled = true,
                                            frequency = freq,
                                            wifiOnly = uiState.driveConfig.wifiOnly,
                                            includePdf = uiState.driveConfig.includePdf,
                                            email = uiState.driveConfig.userEmail
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (isHindi) freq.labelHi else freq.labelEn,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Primary Action Row: Backup Now + Restore from Google Drive
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Backup Now to Google Drive
                    Button(
                        onClick = { viewModel.syncGoogleDriveNow(context, openExternalDriveApp = false) },
                        enabled = !uiState.isDriveSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A73E8)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sync_google_drive_now_button")
                    ) {
                        if (uiState.isDriveSyncing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "बॅकअप सुरू..." else "Syncing...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "ड्राइव्ह बॅकअप करा" else "Backup to Drive",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Restore from Google Drive
                    Button(
                        onClick = { viewModel.openDriveRestoreDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF137333)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_from_google_drive_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "ड्राइव्ह रिस्टोर करा" else "Restore Drive",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Secondary Action Row: Upload to Google Drive App OR Pick Backup File from Drive Storage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.uploadLatestBackupToDriveApp(context, asExcel = false) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_to_drive_app_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "Drive App वर सेव्ह करा" else "Save to Drive App",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/comma-separated-values",
                                    "text/csv",
                                    "text/*",
                                    "*/*"
                                )
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_from_drive_storage_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFF137333),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHindi) "Drive फाईल निवडा" else "Pick Drive File",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Inline Preview of Recent Google Drive Cloud Backups with 1-Tap Restore
                if (uiState.driveSnapshots.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "उपलब्ध Google Drive बॅकअप (1-Tap Restore):" else "Available Google Drive Backups:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (uiState.driveSnapshots.size > 3) {
                                TextButton(
                                    onClick = { viewModel.openDriveRestoreDialog() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "सर्व पहा (${uiState.driveSnapshots.size})" else "View All (${uiState.driveSnapshots.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        uiState.driveSnapshots.take(3).forEachIndexed { index, snapshot ->
                            DriveSnapshotRowItem(
                                snapshot = snapshot,
                                isLatest = index == 0,
                                isHindi = isHindi,
                                onRestore = { viewModel.restoreFromDriveSnapshot(snapshot) },
                                onShareToDrive = {
                                    viewModel.shareDriveSnapshotToApp(context, snapshot, asExcel = false)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Excel Database Backup & Restore
        SettingActionCard(
            title = if (isHindi) "1. एक्सेल बैकअप व रिस्टोर (Excel Backup & Restore)" else "1. Excel Backup & Restore (.csv)",
            description = if (isHindi)
                "Microsoft Excel व Google Sheets में खुलने वाली संपूर्ण स्प्रेडशीट (.csv) एक्सपोर्ट करें, या कंप्यूटर/फोन पर तैयार Excel फ़ाइल से ग्राहकों और मीटर रीडिंग्स को सीधे रिस्टोर करें।"
            else
                "Export complete database to Excel-compatible CSV spreadsheet, or restore/import customers and readings directly from an Excel file.",
            icon = Icons.Default.TableChart,
            iconTint = Color(0xFF1D6F42),
            iconBg = Color(0xFFE8F5E9)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.exportExcelFullBackup(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6F42)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_excel_full_backup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "Excel बैकअप एक्सपोर्ट करें (.csv)" else "Export Excel Backup (.csv)",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { filePickerLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/vnd.ms-excel", "text/*", "*/*")) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1D6F42)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restore_excel_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "Excel फ़ाइल से रिस्टोर करें (Import Excel)" else "Restore / Import from Excel File",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section 2: Full Database Backup (JSON)
        SettingActionCard(
            title = if (isHindi) "2. संपूर्ण JSON बैकअप (JSON Full Backup)" else "2. Full JSON Backup & Restore",
            description = if (isHindi)
                "सभी ग्राहकों और मीटर रीडिंग्स का बैकअप पोर्टेबल JSON फ़ाइल में बनाएं या पहले से सहेजी JSON फ़ाइल से रिस्टोर करें।"
            else
                "Export complete database into a portable JSON backup file, or restore from previously saved JSON.",
            icon = Icons.Default.CloudDownload,
            iconTint = Color(0xFF006877),
            iconBg = Color(0xFFE0F7FA)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.exportFullBackup(context) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_full_backup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "JSON एक्सपोर्ट" else "Export JSON",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_backup_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "JSON रिस्टोर" else "Restore JSON",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section 3: Date-Wise Reports (PDF & Excel) Hub
        SettingActionCard(
            title = if (isHindi) "3. डेट-वार रिपोर्ट (Date-Wise PDF & Excel)" else "3. Date-Wise Reports (PDF & Excel)",
            description = if (isHindi)
                "तारीख के अनुसार (आज, 7 दिन, इस माह, या कस्टम फ्रॉम-टू डेट) औपचारिक A4 PDF और Excel स्प्रेडशीट बनाएं।"
            else
                "Export formatted A4 PDF statements and Microsoft Excel compatible CSV files filtered by date range.",
            icon = Icons.Default.DateRange,
            iconTint = MaterialTheme.colorScheme.primary,
            iconBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.openExportDialog() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_date_export_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "PDF रिपोर्ट" else "PDF Report",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.openExportDialog() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_excel_export_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "Excel रिपोर्ट" else "Excel Report",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Section 4: Database Statistics Card
        val stats = uiState.dbStats
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("db_stats_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "डेटाबेस आंकड़े (Database Statistics)" else "Database Statistics",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPill(
                        label = if (isHindi) "कुल ग्राहक" else "Customers",
                        value = "${stats?.first ?: uiState.customers.size}",
                        icon = Icons.Default.Business,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        label = if (isHindi) "कुल रीडिंग्स" else "Readings",
                        value = "${stats?.second ?: 0}",
                        icon = Icons.Default.ElectricMeter,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        label = if (isHindi) "कुल खपत" else "Total Units",
                        value = String.format(Locale.US, "%.0f", stats?.third ?: 0.0),
                        icon = Icons.Default.Info,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clear All Data button
                OutlinedButton(
                    onClick = { showResetConfirmDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_demo_data_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "सर्व ग्राहक व डेटा क्लीन करा (Clear All Data)" else "Clear All Customers & Readings",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    // Modal 1: Google Drive Backup History & Restore Picker Dialog
    if (uiState.showDriveRestoreDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeDriveRestoreDialog() },
            modifier = Modifier.testTag("drive_restore_dialog"),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isHindi) "Google Drive मधून रिस्टोर करा" else "Restore from Google Drive",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.driveConfig.userEmail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Quick 1-Tap Latest Restore Button
                    if (uiState.driveSnapshots.isNotEmpty()) {
                        val latest = uiState.driveSnapshots.first()
                        Button(
                            onClick = { viewModel.restoreFromDriveSnapshot(latest) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF137333)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_latest_drive_backup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi)
                                    "सर्वात नवीन बॅकअप रिस्टोर करा (${latest.readingCount} रीडिंग्स)"
                                else
                                    "Restore Latest Backup (${latest.readingCount} readings)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Browse Google Drive App File Picker
                    OutlinedButton(
                        onClick = {
                            viewModel.closeDriveRestoreDialog()
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/comma-separated-values",
                                    "text/csv",
                                    "text/*",
                                    "*/*"
                                )
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("browse_drive_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "Google Drive फोल्डर मधून फाईल निवडा" else "Browse File from Google Drive App",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = if (isHindi)
                            "उपलब्ध Google Drive क्लाउड बॅकअप (${uiState.driveSnapshots.size}):"
                        else
                            "Saved Google Drive Cloud Backups (${uiState.driveSnapshots.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (uiState.driveSnapshots.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isHindi)
                                    "कोणताही क्लाउड बॅकअप आढळला नाही. कृपया आधी 'ड्राइव्ह बॅकअप करा' वर क्लिक करा."
                                else
                                    "No cloud backups found. Tap 'Backup to Drive' first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.driveSnapshots, key = { it.id }) { snap ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.restoreFromDriveSnapshot(snap) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = snap.formattedDate,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${snap.customerCount} ग्राहक • ${snap.readingCount} रीडिंग्स • ${String.format(Locale.US, "%.0f", snap.totalUnitsKwh)} kWh",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = snap.triggerType,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Button(
                                                    onClick = { viewModel.restoreFromDriveSnapshot(snap) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text(
                                                        text = if (isHindi) "रिस्टोर" else "Restore",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteDriveSnapshot(snap) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete backup",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { viewModel.closeDriveRestoreDialog() }) {
                    Text(if (isHindi) "बंद करा (Close)" else "Close")
                }
            }
        )
    }

    // Modal 2: Restore Confirmation Dialog
    val pending = uiState.pendingRestoreData
    if (uiState.showRestoreConfirmDialog && pending != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreDialog() },
            modifier = Modifier.testTag("restore_confirm_dialog"),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "डेटा रिस्टोरची पुष्टी करा (Confirm Restore)" else "Confirm Data Restore",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHindi) "Google Drive / बॅकअप फाईल यशस्वीरित्या तपासली गेली:" else "Backup file successfully validated:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "• स्रोत (Source): ${pending.fileType}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "• बॅकअप तारीख: ${pending.exportDate}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• एकूण ग्राहक: ${pending.customers.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• एकूण मीटर रीडिंग्स: ${pending.readings.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Text(
                        text = if (isHindi) "रिस्टोर पर्याय निवडा (Choose Restore Mode):" else "Choose Restore Method:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Option 1: Merge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoreOptionMerge = true }
                    ) {
                        RadioButton(
                            selected = restoreOptionMerge,
                            onClick = { restoreOptionMerge = true }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isHindi) "सध्याच्या डेटामध्ये जोडा (Merge Data)" else "Merge Records",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (isHindi) "जुना डेटा तसाच राहील आणि नवीन जोडला जाईल" else "Keep existing data and append",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Option 2: Replace
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoreOptionMerge = false }
                    ) {
                        RadioButton(
                            selected = !restoreOptionMerge,
                            onClick = { restoreOptionMerge = false }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isHindi) "जुना डेटा काढून पूर्ण रिस्टोर करा (Replace All)" else "Replace Everything",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (isHindi) "सध्याचा डेटा जाऊन Google Drive बॅकअप पूर्णपणे लोड होईल" else "Wipes current data and restores clean backup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.executeRestore(replaceExisting = !restoreOptionMerge) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.testTag("confirm_restore_action_button")
                ) {
                    Text(if (isHindi) "आता रिस्टोर करा (Restore Now)" else "Restore Data")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissRestoreDialog() }) {
                    Text(if (isHindi) "रद्द करा" else "Cancel")
                }
            }
        )
    }

    // Modal 3: Clear All Data Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "सर्व डेटा क्लीन करायचा?" else "Clear All Data?",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = if (isHindi)
                        "यामुळे अॅपमधील सर्व ग्राहक आणि मीटर रीडिंग नोंदी कायमच्या काढून टाकल्या जातील आणि अॅप पूर्णपणे क्लीन होईल."
                    else
                        "This will permanently remove all customers and meter readings from the app."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllCustomersAndReadings()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isHindi) "हो, क्लीन करा (Clear All)" else "Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text(if (isHindi) "रद्द करा" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun DriveSnapshotRowItem(
    snapshot: DriveBackupSnapshot,
    isLatest: Boolean,
    isHindi: Boolean,
    onRestore: () -> Unit,
    onShareToDrive: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isLatest) Color(0xFF1A73E8).copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .background(
                color = if (isLatest) Color(0xFFE8F0FE).copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = snapshot.formattedDate,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isLatest) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF137333), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isHindi) "नवीनतम" else "Latest",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Text(
                text = "${snapshot.customerCount} ग्राहक • ${snapshot.readingCount} रीडिंग्स • ${String.format(Locale.US, "%.0f", snapshot.totalUnitsKwh)} kWh",
                fontSize = 11.sp,
                color = Color(0xFF1A73E8),
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onShareToDrive,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Upload to Drive App",
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF137333)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("quick_restore_snapshot_${snapshot.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isHindi) "रिस्टोर" else "Restore",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SettingActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
