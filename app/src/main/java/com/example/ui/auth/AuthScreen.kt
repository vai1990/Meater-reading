package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

suspend fun attemptAutoSignIn(
    activity: Activity,
    webClientId: String,
    onSuccess: () -> Unit
) {
    val credentialManager = CredentialManager.create(activity)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(activity, request)
        handleSignInResult(result, onSuccess) { /* Silent failure for auto sign-in */ }
    } catch (_: NoCredentialException) {
        // Normal on first launch — wait for user to click the Google Sign-In button
    } catch (e: GetCredentialException) {
        Log.d("Auth", "Auto sign-in unavailable: ${e.message}")
    }
}

suspend fun onGoogleSignInClicked(
    activity: Activity,
    webClientId: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val credentialManager = CredentialManager.create(activity)
    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()

    try {
        val result = credentialManager.getCredential(activity, request)
        handleSignInResult(result, onSuccess, onError)
    } catch (_: GetCredentialCancellationException) {
        // User dismissed the dialog
    } catch (e: NoCredentialException) {
        Log.w("Auth", "No Google accounts on device", e)
        onError("डिव्हाइसवर कोणतेही Google खाते आढळले नाही. कृपया सेटिंग्जमध्ये Google खाते जोडा.")
    } catch (e: GetCredentialException) {
        Log.e("Auth", "Credential Manager failure", e)
        onError(e.localizedMessage ?: "Google Sign-In अयशस्वी झाले.")
    }
}

private suspend fun handleSignInResult(
    result: GetCredentialResponse,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val credential = result.credential
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            Firebase.auth.signInWithCredential(firebaseCredential).await()
            onSuccess()
        } catch (e: Exception) {
            Log.e("Auth", "Firebase signInWithCredential failed", e)
            onError(e.localizedMessage ?: "Authentication failed.")
        }
    } else {
        onError("Unrecognized credential type.")
    }
}

suspend fun signOut(context: Context) {
    Firebase.auth.signOut()
    try {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state", e)
    }
}

@Composable
fun GoogleSignInButton(
    onSignInSuccess: () -> Unit,
    onSignInError: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webClientId = remember { context.getString(R.string.default_web_client_id) }
    var isSigningIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val activity = context.findActivity() ?: return@LaunchedEffect
        attemptAutoSignIn(activity, webClientId, onSignInSuccess)
    }

    Button(
        onClick = {
            val activity = context.findActivity()
            if (activity == null) {
                onSignInError("Unable to start sign-in: no host Activity.")
                return@Button
            }
            coroutineScope.launch {
                isSigningIn = true
                onGoogleSignInClicked(
                    activity = activity,
                    webClientId = webClientId,
                    onSuccess = {
                        isSigningIn = false
                        onSignInSuccess()
                    },
                    onError = { msg ->
                        isSigningIn = false
                        onSignInError(msg)
                    }
                )
            }
        },
        enabled = !isSigningIn,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1A73E8),
            contentColor = Color.White
        )
    ) {
        if (isSigningIn) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Google खात्याशी जोडत आहे...",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        } else {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Sign in with Google (गूगलने लॉगिन करा)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun AuthScreen(
    onSignInSuccess: () -> Unit = {},
    onContinueOffline: () -> Unit = {}
) {
    val context = LocalContext.current
    val branding = remember { com.example.util.AppBrandingManager(context).getBranding() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D47A1),
                            Color(0xFF1565C0),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .testTag("auth_screen_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    com.example.ui.dialogs.AppBrandIcon(
                        style = branding.iconStyle,
                        size = 76.dp
                    )

                    Text(
                        text = branding.appName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "दैनिक मीटर रीडिंग आणि क्लाउड डेटाबेस\n(Daily Meter Reading & Cloud Tracker)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF1F8E9)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "तुमचा सर्व मीटर रीडिंग डेटा क्लाउडवर सुरक्षित राहतो!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Google खात्याने लॉगिन करा आणि मोबाईल व वेबवर डेटा थेट सिंक करा.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF33691E)
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    GoogleSignInButton(
                        onSignInSuccess = {
                            errorMessage = null
                            onSignInSuccess()
                        },
                        onSignInError = { err ->
                            errorMessage = err
                        }
                    )

                    androidx.compose.material3.OutlinedButton(
                        onClick = onContinueOffline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("continue_offline_dashboard_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "ऑफलाईन डॅशबोर्ड उघडा (Open Local Dashboard)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}
