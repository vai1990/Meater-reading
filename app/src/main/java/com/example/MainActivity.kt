package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.remote.TeamSyncRepository
import com.example.data.repository.MeterRepository
import com.example.ui.HomeScreen
import com.example.ui.MeterViewModel
import com.example.ui.MeterViewModelFactory
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.theme.MyApplicationTheme
import com.example.util.GoogleDriveBackupManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current.applicationContext
    val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    var offlineDashboardMode by remember { mutableStateOf(false) }

    if (currentUser == null && !offlineDashboardMode) {
        AuthScreen(
            onSignInSuccess = { /* authStateFlow updates automatically */ },
            onContinueOffline = { offlineDashboardMode = true }
        )
    } else {
        val activeKey = currentUser?.uid ?: "local_offline_user"
        key(activeKey) {
            val meterViewModel: MeterViewModel = viewModel(
                key = "meter_vm_$activeKey",
                factory = remember(activeKey) {
                    val database = AppDatabase.getInstance(context)
                    val repository = MeterRepository(database)
                    val driveManager = GoogleDriveBackupManager(context)
                    val teamSyncRepository = if (currentUser != null) {
                        TeamSyncRepository(context)
                    } else {
                        null
                    }
                    MeterViewModelFactory(repository, driveManager, teamSyncRepository)
                }
            )
            HomeScreen(viewModel = meterViewModel)
        }
    }
}
