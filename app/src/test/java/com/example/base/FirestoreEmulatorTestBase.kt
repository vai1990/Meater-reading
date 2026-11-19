package com.example.base

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
abstract class FirestoreEmulatorTestBase {

    protected lateinit var db: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth
    protected lateinit var projectId: String
    protected lateinit var databaseId: String

    @Before
    open fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        projectId = FirebaseApp.getInstance().options.projectId
            ?: throw IllegalStateException(
                "FirebaseOptions.projectId is null — verify app/google-services.json is processed by the google-services Gradle plugin."
            )

        databaseId = context.getString(R.string.firestore_database_id)

        auth = FirebaseAuth.getInstance()
        try {
            auth.useEmulator("127.0.0.1", 9099)
        } catch (_: IllegalStateException) {
            // Emulator already configured on this singleton instance
        }

        db = FirebaseFirestore.getInstance(databaseId)
        try {
            db.useEmulator("127.0.0.1", 8085)
            db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        } catch (_: IllegalStateException) {
            // Settings already applied on this singleton instance
        }

        clearFirestoreEmulator()
    }

    @After
    open fun tearDown() {
        auth.signOut()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun clearFirestoreEmulator() {
        val url = URL("http://127.0.0.1:8085/emulator/v1/projects/$projectId/databases/$databaseId/documents")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 2000
            readTimeout = 2000
        }
        val code = conn.responseCode
        conn.disconnect()
        check(code in 200..299) {
            "Failed to clear Firestore emulator for project=$projectId db=$databaseId (HTTP $code). Is the emulator running on 127.0.0.1:8085?"
        }
    }

    protected fun runEmulatorTest(block: suspend CoroutineScope.() -> Unit) {
        val errorRef = AtomicReference<Throwable?>(null)
        val done = AtomicBoolean(false)
        val worker = Thread {
            try {
                runBlocking(Dispatchers.IO) {
                    withTimeout(30_000L) {
                        block()
                    }
                }
            } catch (t: Throwable) {
                errorRef.set(t)
            } finally {
                done.set(true)
            }
        }
        worker.start()
        val shadowMainLooper = Shadows.shadowOf(Looper.getMainLooper())
        while (!done.get()) {
            shadowMainLooper.idle()
            Thread.sleep(10)
        }
        shadowMainLooper.idle()
        worker.join()
        errorRef.get()?.let { throw it }
    }

    protected suspend fun signInTestUser(emailPrefix: String, password: String = "TestPass123!"): String {
        val cleanPrefix = emailPrefix.substringBefore("@")
        val uniqueEmail = "${cleanPrefix}_${UUID.randomUUID().toString().take(8)}@example.com"
        val result = auth.createUserWithEmailAndPassword(uniqueEmail, password).await()
        return result.user!!.uid
    }
}
