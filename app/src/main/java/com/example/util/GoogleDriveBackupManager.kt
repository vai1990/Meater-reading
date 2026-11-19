package com.example.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DriveBackupFrequency(val labelEn: String, val labelHi: String) {
    AFTER_READING("After Every Reading", "प्रत्येक रिडिंग नंतर (Auto)"),
    DAILY("Daily (दैनिक)", "दररोज (Daily)"),
    WEEKLY("Weekly (साप्ताहिक)", "साप्ताहिक (Weekly)"),
    MANUAL("Manual Only", "फक्त मॅन्युअल (Manual)")
}

data class DriveBackupConfig(
    val isAutoBackupEnabled: Boolean = true,
    val frequency: DriveBackupFrequency = DriveBackupFrequency.AFTER_READING,
    val userEmail: String = "vaibhavghag1990@gmail.com",
    val lastSyncTimestamp: Long = 0L,
    val lastBackupCustomersCount: Int = 0,
    val lastBackupReadingsCount: Int = 0,
    val lastBackupFileName: String = "",
    val wifiOnly: Boolean = false,
    val includePdf: Boolean = true
)

data class DriveBackupSnapshot(
    val id: String,
    val fileName: String,
    val filePath: String,
    val excelFilePath: String?,
    val timestamp: Long,
    val formattedDate: String,
    val customerCount: Int,
    val readingCount: Int,
    val totalUnitsKwh: Double,
    val sizeBytes: Long,
    val triggerType: String
)

class GoogleDriveBackupManager(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("google_drive_backup_prefs", Context.MODE_PRIVATE)

    private val driveBackupDir: File
        get() {
            val dir = File(appContext.filesDir, "drive_backups")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun getConfig(): DriveBackupConfig {
        val enabled = prefs.getBoolean("drive_auto_backup_enabled", true)
        val freqStr = prefs.getString("drive_backup_frequency", DriveBackupFrequency.AFTER_READING.name)
            ?: DriveBackupFrequency.AFTER_READING.name
        val freq = try {
            DriveBackupFrequency.valueOf(freqStr)
        } catch (e: Exception) {
            DriveBackupFrequency.AFTER_READING
        }
        val email = prefs.getString("drive_user_email", "vaibhavghag1990@gmail.com")
            ?: "vaibhavghag1990@gmail.com"
        val lastSync = prefs.getLong("drive_last_sync_time", 0L)
        val lastCustCount = prefs.getInt("drive_last_cust_count", 0)
        val lastReadCount = prefs.getInt("drive_last_read_count", 0)
        val lastFileName = prefs.getString("drive_last_file_name", "") ?: ""
        val wifiOnly = prefs.getBoolean("drive_wifi_only", false)
        val includePdf = prefs.getBoolean("drive_include_pdf", true)

        return DriveBackupConfig(
            isAutoBackupEnabled = enabled,
            frequency = freq,
            userEmail = email,
            lastSyncTimestamp = lastSync,
            lastBackupCustomersCount = lastCustCount,
            lastBackupReadingsCount = lastReadCount,
            lastBackupFileName = lastFileName,
            wifiOnly = wifiOnly,
            includePdf = includePdf
        )
    }

    fun updateConfig(
        enabled: Boolean,
        frequency: DriveBackupFrequency,
        wifiOnly: Boolean,
        includePdf: Boolean,
        email: String = "vaibhavghag1990@gmail.com"
    ) {
        prefs.edit()
            .putBoolean("drive_auto_backup_enabled", enabled)
            .putString("drive_backup_frequency", frequency.name)
            .putBoolean("drive_wifi_only", wifiOnly)
            .putBoolean("drive_include_pdf", includePdf)
            .putString("drive_user_email", email)
            .apply()
    }

    fun recordSyncSuccess(
        timestamp: Long = System.currentTimeMillis(),
        customerCount: Int = 0,
        readingCount: Int = 0,
        fileName: String = ""
    ) {
        val editor = prefs.edit().putLong("drive_last_sync_time", timestamp)
        if (customerCount > 0 || readingCount > 0) {
            editor.putInt("drive_last_cust_count", customerCount)
            editor.putInt("drive_last_read_count", readingCount)
        }
        if (fileName.isNotEmpty()) {
            editor.putString("drive_last_file_name", fileName)
        }
        editor.apply()
    }

    fun formatLastSync(timestamp: Long): String {
        if (timestamp == 0L) return "अद्याप बॅकअप नाही (Not synced yet)"
        return SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Creates a persistent Google Drive Cloud Backup Snapshot (both JSON and Excel CSV)
     * so the user can restore anytime with 1 tap or upload to the Google Drive app.
     */
    fun createCloudBackupSnapshot(
        context: Context,
        customers: List<Customer>,
        readings: List<MeterReading>,
        triggerType: String = "Manual Sync"
    ): DriveBackupSnapshot {
        val now = System.currentTimeMillis()
        val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
        val humanDate = SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", Locale.US).format(Date(now))
        val totalKwh = readings.mapNotNull { it.unitsConsumed }.sum()

        // Build enriched JSON with Drive metadata
        val baseJsonString = BackupRestoreHelper.createBackupJson(customers, readings)
        val rootObj = JSONObject(baseJsonString).apply {
            put("driveSyncTimestamp", now)
            put("driveFormattedDate", humanDate)
            put("driveAccount", getConfig().userEmail)
            put("driveTriggerType", triggerType)
            put("totalUnitsKwh", totalKwh)
        }
        val finalJson = rootObj.toString(2)

        val jsonFileName = "MeterRead_GoogleDrive_Backup_$fileStamp.json"
        val jsonFile = File(driveBackupDir, jsonFileName)
        jsonFile.writeText(finalJson, Charsets.UTF_8)

        // Also generate matching Excel CSV snapshot in drive_backups
        val excelFileName = "MeterRead_GoogleDrive_Excel_$fileStamp.csv"
        val excelFile = File(driveBackupDir, excelFileName)
        try {
            val tempExcel = ExcelBackupRestoreHelper.exportFullDatabaseToExcel(context, customers, readings)
            tempExcel.copyTo(excelFile, overwrite = true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Also update "Latest" quick-access files
        val latestJsonFile = File(driveBackupDir, "MeterRead_Drive_Latest_Backup.json")
        latestJsonFile.writeText(finalJson, Charsets.UTF_8)

        recordSyncSuccess(
            timestamp = now,
            customerCount = customers.size,
            readingCount = readings.size,
            fileName = jsonFileName
        )

        pruneOldSnapshots(maxSnapshots = 12)

        return DriveBackupSnapshot(
            id = fileStamp,
            fileName = jsonFileName,
            filePath = jsonFile.absolutePath,
            excelFilePath = if (excelFile.exists()) excelFile.absolutePath else null,
            timestamp = now,
            formattedDate = humanDate,
            customerCount = customers.size,
            readingCount = readings.size,
            totalUnitsKwh = totalKwh,
            sizeBytes = jsonFile.length(),
            triggerType = triggerType
        )
    }

    /**
     * Creates an automatic Google Drive Cloud Backup Snapshot using applicationContext.
     */
    fun createAutoCloudSnapshot(
        customers: List<Customer>,
        readings: List<MeterReading>,
        triggerType: String = "Auto Cloud Sync"
    ): DriveBackupSnapshot {
        return createCloudBackupSnapshot(
            context = appContext,
            customers = customers,
            readings = readings,
            triggerType = triggerType
        )
    }

    /**
     * Backward-compatible sync method returning the JSON File for external sharing/uploading.
     */
    fun performDriveSync(
        context: Context,
        customers: List<Customer>,
        readings: List<MeterReading>
    ): File {
        val snapshot = createCloudBackupSnapshot(
            context = context,
            customers = customers,
            readings = readings,
            triggerType = "Google Drive Manual Sync"
        )
        return File(snapshot.filePath)
    }

    /**
     * Lists all available Google Drive Backup Snapshots sorted by newest first.
     */
    fun getAvailableDriveSnapshots(): List<DriveBackupSnapshot> {
        val files = driveBackupDir.listFiles { file ->
            file.isFile &&
                file.name.startsWith("MeterRead_GoogleDrive_Backup_") &&
                file.name.endsWith(".json")
        } ?: return emptyList()

        val snapshots = mutableListOf<DriveBackupSnapshot>()
        for (file in files) {
            try {
                val content = file.readText(Charsets.UTF_8)
                val obj = JSONObject(content)
                val ts = obj.optLong("driveSyncTimestamp", obj.optLong("timestamp", file.lastModified()))
                val formattedDate = obj.optString(
                    "driveFormattedDate",
                    obj.optString(
                        "exportDate",
                        SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.US).format(Date(ts))
                    )
                )
                val custCount = obj.optInt("totalCustomers", obj.optJSONArray("customers")?.length() ?: 0)
                val readCount = obj.optInt("totalReadings", obj.optJSONArray("readings")?.length() ?: 0)
                val totalKwh = obj.optDouble("totalUnitsKwh", 0.0)
                val trigger = obj.optString("driveTriggerType", "Google Drive Sync")

                val stampId = file.name
                    .removePrefix("MeterRead_GoogleDrive_Backup_")
                    .removeSuffix(".json")
                val matchingExcel = File(driveBackupDir, "MeterRead_GoogleDrive_Excel_$stampId.csv")

                snapshots.add(
                    DriveBackupSnapshot(
                        id = stampId,
                        fileName = file.name,
                        filePath = file.absolutePath,
                        excelFilePath = if (matchingExcel.exists()) matchingExcel.absolutePath else null,
                        timestamp = ts,
                        formattedDate = formattedDate,
                        customerCount = custCount,
                        readingCount = readCount,
                        totalUnitsKwh = totalKwh,
                        sizeBytes = file.length(),
                        triggerType = trigger
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return snapshots.sortedByDescending { it.timestamp }
    }

    /**
     * Reads and parses a specific DriveBackupSnapshot into BackupParsedData for restoration.
     */
    fun readSnapshotData(snapshot: DriveBackupSnapshot): BackupParsedData {
        val file = File(snapshot.filePath)
        if (!file.exists()) {
            throw IllegalStateException("Google Drive backup file not found: ${snapshot.fileName}")
        }
        val content = file.readText(Charsets.UTF_8)
        val parsed = BackupRestoreHelper.parseBackupJson(content)
        return parsed.copy(
            fileType = "Google Drive Backup (${snapshot.fileName})"
        )
    }

    /**
     * Reads the most recent Google Drive backup snapshot for 1-Tap Restore.
     */
    fun getLatestSnapshotData(): BackupParsedData? {
        val latestSnapshot = getAvailableDriveSnapshots().firstOrNull()
        if (latestSnapshot != null) {
            return readSnapshotData(latestSnapshot)
        }
        val fallbackFile = File(driveBackupDir, "MeterRead_Drive_Latest_Backup.json")
        if (fallbackFile.exists()) {
            val parsed = BackupRestoreHelper.parseBackupJson(fallbackFile.readText(Charsets.UTF_8))
            return parsed.copy(fileType = "Google Drive Latest Cloud Backup")
        }
        return null
    }

    fun deleteSnapshot(snapshot: DriveBackupSnapshot): Boolean {
        val jsonFile = File(snapshot.filePath)
        val deleted = if (jsonFile.exists()) jsonFile.delete() else false
        snapshot.excelFilePath?.let { path ->
            val excelFile = File(path)
            if (excelFile.exists()) excelFile.delete()
        }
        return deleted
    }

    private fun pruneOldSnapshots(maxSnapshots: Int) {
        val all = getAvailableDriveSnapshots()
        if (all.size > maxSnapshots) {
            all.drop(maxSnapshots).forEach { old ->
                deleteSnapshot(old)
            }
        }
    }

    /**
     * Launches direct upload intent for Google Drive with FileProvider.
     * Pre-configures Google Drive package if available or falls back to system intent chooser.
     */
    fun uploadToGoogleDrive(
        context: Context,
        file: File,
        title: String,
        mimeType: String = "application/json"
    ) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val driveIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "MeterRead Pro Google Drive Backup file: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.google.android.apps.docs")
            }

            val pm = context.packageManager
            if (driveIntent.resolveActivity(pm) != null) {
                driveIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(driveIntent)
            } else {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, "MeterRead Pro Google Drive Backup (${file.name})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(fallbackIntent, "Save to Google Drive / क्लाउडवर सेव्ह करा")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Google Drive Sync Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
