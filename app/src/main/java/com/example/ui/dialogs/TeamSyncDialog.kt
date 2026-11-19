package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.CloudReading
import com.example.data.remote.TeamWorkspace
import com.example.ui.auth.GoogleSignInButton

@Composable
fun TeamSyncDialog(
    activeWorkspace: TeamWorkspace?,
    cloudReadings: List<CloudReading>,
    currentUserName: String,
    currentUserEmail: String,
    isLoading: Boolean,
    isHindi: Boolean,
    onJoinTeamByCode: (inviteCode: String, colleagueName: String) -> Unit,
    onCreateCustomTeam: (customCode: String, teamName: String, userName: String) -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var joinCodeInput by remember { mutableStateOf("") }
    var colleagueNameInput by remember(currentUserName) {
        mutableStateOf(currentUserName.ifBlank { "सहकारी (Colleague)" })
    }
    var customTeamCodeInput by remember { mutableStateOf("") }
    var customTeamNameInput by remember { mutableStateOf("") }
    var showCreateCustomSection by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }

    val inviteCode = activeWorkspace?.inviteCode ?: "TEAM101"
    val memberNames = activeWorkspace?.memberNames ?: listOf(currentUserName.ifBlank { "Admin" })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .testTag("team_sync_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
                                .size(42.dp)
                                .background(Color(0xFFE8F5E9), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "सहकारी लाईव्ह सिंक (Colleague Sync)" else "Colleague Live Cloud Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isHindi)
                                    "सहकाऱ्याने केलेली एन्ट्री लगेच तुमच्या मोबाईलवर सिंक होईल"
                                else
                                    "Real-time meter reading sync with your colleagues",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_team_sync_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (activeWorkspace == null && currentUserEmail.isBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF8E1)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isHindi)
                                    "क्लाउडवर सहकाऱ्यांसोबत लाईव्ह सिंक सुरू करण्यासाठी खालील बटणाने Google लॉगिन करा:"
                                else
                                    "Sign in with Google below to activate live cloud sync with your colleagues:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE65100)
                            )
                            if (signInError != null) {
                                Text(
                                    text = signInError!!,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            GoogleSignInButton(
                                onSignInSuccess = { signInError = null },
                                onSignInError = { err -> signInError = err }
                            )
                        }
                    }
                }

                // 1. Active Team Invite Code Card (Share with Colleague)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isHindi) "तुमचा टीम कोड (Share Team Code)" else "Your Team Invite Code",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A73E8)
                                )
                                Text(
                                    text = activeWorkspace?.name ?: "MeterRead Team Workspace",
                                    fontSize = 11.sp,
                                    color = Color(0xFF174EA6)
                                )
                            }
                            Surface(
                                color = Color(0xFFCEEAD6),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF137333),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${memberNames.size} सदस्य जोडलेले",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF137333)
                                    )
                                }
                            }
                        }

                        // Large Invite Code Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.5.dp, Color(0xFF1A73E8), RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = inviteCode,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = Color(0xFF0D47A1),
                                letterSpacing = 2.sp,
                                modifier = Modifier.testTag("active_team_invite_code_text")
                            )
                        }

                        Text(
                            text = if (isHindi)
                                "हा टीम कोड तुमच्या सहकाऱ्याला WhatsApp वर पाठवा. त्यांनी हा कोड टाकताच त्यांच्या सर्व एन्ट्री तुमच्याकडे आपोआप दिसतील!"
                            else
                                "Share this code with your colleague. Once they join, all entries they make sync to your phone automatically!",
                            fontSize = 11.5.sp,
                            color = Color(0xFF174EA6),
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    copyInviteCode(context, inviteCode)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_team_code_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "कोड कॉपी करा" else "Copy Code",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = {
                                    shareInviteCode(context, inviteCode, activeWorkspace?.name ?: "MeterRead Team")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_team_code_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "सहकाऱ्याला पाठवा" else "Share Code",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // 2. Join Colleague's Team Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi)
                                    "सहकाऱ्याच्या टीममध्ये सामील व्हा (Join Colleague's Team)"
                                else
                                    "Join Colleague's Team via Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }

                        OutlinedTextField(
                            value = colleagueNameInput,
                            onValueChange = { colleagueNameInput = it },
                            label = {
                                Text(if (isHindi) "तुमचे नाव (Your Name on Entries)" else "Your Name")
                            },
                            placeholder = { Text("उदा. राहुल / वैभव") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("colleague_name_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = joinCodeInput,
                            onValueChange = { joinCodeInput = it.uppercase() },
                            label = {
                                Text(if (isHindi) "सहकाऱ्याचा टीम कोड टाका (Enter Team Code)" else "Enter Colleague's Team Code")
                            },
                            placeholder = { Text("उदा. TEAM101") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_team_code_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (joinCodeInput.trim().length >= 3) {
                                        onJoinTeamByCode(
                                            joinCodeInput.trim(),
                                            colleagueNameInput.trim().ifBlank { "Colleague" }
                                        )
                                        joinCodeInput = ""
                                    }
                                },
                                enabled = joinCodeInput.trim().length >= 3 && !isLoading,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("join_team_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isHindi) "टीम जॉईन करा (Join & Sync)" else "Join & Sync Team",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }

                            OutlinedButton(
                                onClick = onSyncNow,
                                enabled = !isLoading,
                                modifier = Modifier.testTag("manual_team_sync_now_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHindi) "आता सिंक करा" else "Sync Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Optional: Create custom easy-to-remember team code (e.g. TEAM101)
                        OutlinedButton(
                            onClick = { showCreateCustomSection = !showCreateCustomSection },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isHindi)
                                    "नवीन सोपा टीम कोड तयार करा (उदा. TEAM101)"
                                else
                                    "Create Custom Team Code (e.g., TEAM101)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (showCreateCustomSection) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customTeamCodeInput,
                                    onValueChange = { customTeamCodeInput = it.uppercase() },
                                    label = { Text(if (isHindi) "नवीन टीम कोड (उदा. TEAM101)" else "New Team Code") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = customTeamNameInput,
                                    onValueChange = { customTeamNameInput = it },
                                    label = { Text(if (isHindi) "टीमचे नाव (उदा. पुणे मीटर टीम)" else "Team Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Button(
                                    onClick = {
                                        if (customTeamCodeInput.trim().length >= 3) {
                                            onCreateCustomTeam(
                                                customTeamCodeInput.trim(),
                                                customTeamNameInput.trim().ifBlank { "MeterRead Team" },
                                                colleagueNameInput.trim().ifBlank { "Admin" }
                                            )
                                            showCreateCustomSection = false
                                            customTeamCodeInput = ""
                                        }
                                    },
                                    enabled = customTeamCodeInput.trim().length >= 3 && !isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (isHindi) "नवीन टीम कोड सेट करा" else "Create & Activate Code")
                                }
                            }
                        }
                    }
                }

                // 3. Connected Team Members & Recent Synced Entries
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isHindi)
                                "जोडलेले सहकारी सदस्य (${memberNames.size}):"
                            else
                                "Connected Team Members (${memberNames.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            memberNames.forEach { member ->
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = member,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = if (isHindi)
                                "क्लाउडवर सिंक झालेल्या ताज्या एन्ट्री (${cloudReadings.size}):"
                            else
                                "Live Cloud Synced Readings (${cloudReadings.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )

                        if (cloudReadings.isEmpty()) {
                            Text(
                                text = if (isHindi)
                                    "अद्याप क्लाउडवर कोणतीही एन्ट्री नाही. 'आता सिंक करा' दाबा किंवा नवीन रीडिंग सेव्ह करा."
                                else
                                    "No cloud readings yet. Tap 'Sync Now' or log a new reading.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            cloudReadings.sortedByDescending { it.readingTimestamp }.take(5).forEach { cr ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${cr.customerName} (${cr.customerMeterNumber})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${cr.readingDate} • ${cr.kwh} kWh • PF: ${String.format("%.2f", cr.powerFactor)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFFE8F0FE),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color(0xFF1A73E8),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = cr.createdByName,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1A73E8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Signed-in Account Footer & Sign Out
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUserName.ifBlank { "Google User" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        if (currentUserEmail.isNotBlank()) {
                            Text(
                                text = currentUserEmail,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "लॉगआउट" else "Sign Out",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun copyInviteCode(context: Context, inviteCode: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("MeterRead Team Code", inviteCode))
    Toast.makeText(
        context,
        "टीम कोड '$inviteCode' कॉपी केला! (Team Code Copied)",
        Toast.LENGTH_SHORT
    ).show()
}

fun shareInviteCode(context: Context, inviteCode: String, teamName: String) {
    val shareText = buildString {
        appendLine("⚡ *MeterRead Pro - सहकारी लाईव्ह सिंक (Colleague Live Sync)*")
        appendLine("टीमचे नाव: $teamName")
        appendLine("🔑 *माझा टीम कोड (Team Code): $inviteCode*")
        appendLine()
        appendLine("एप उघडून 'सहकारी सिंक (Team Sync)' वर क्लिक करा आणि हा टीम कोड टाका. तुम्ही केलेली प्रत्येक मीटर रीडिंग एन्ट्री माझ्या मोबाईलवर आपोआप सिंक होईल!")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "MeterRead Pro - Team Sync Code: $inviteCode")
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(sendIntent, "सहकाऱ्याला टीम कोड पाठवा (Share Team Code)").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
