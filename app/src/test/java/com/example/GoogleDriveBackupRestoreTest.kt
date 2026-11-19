package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import com.example.util.GoogleDriveBackupManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoogleDriveBackupRestoreTest {

    @Test
    fun testCreateAndRestoreGoogleDriveCloudSnapshot() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val driveManager = GoogleDriveBackupManager(context)

        val customers = listOf(
            Customer(
                id = 1L,
                name = "श्री गणेश इंडस्ट्रीज (Ganesh Industries)",
                address = "एमआयडीसी पुणे",
                meterNumber = "MTR-99881",
                consumerNumber = "CN-100200",
                phoneNumber = "9876543210",
                connectionType = "3-Phase Industrial",
                sanctionedLoad = "50 kW"
            )
        )

        val readings = listOf(
            MeterReading(
                id = 10L,
                customerId = 1L,
                readingDate = "10-Nov-2026",
                readingTime = "09:00 AM",
                timestamp = 1731210000000L,
                kwh = 15400.0,
                kvarh = 4100.0,
                kvah = 15936.0,
                powerFactor = 0.966,
                isPfAutoCalculated = true,
                previousKwh = 15000.0,
                unitsConsumed = 400.0,
                remarks = "Google Drive Backup Test Reading"
            )
        )

        val snapshot = driveManager.createCloudBackupSnapshot(
            context = context,
            customers = customers,
            readings = readings,
            triggerType = "Manual Google Drive Backup"
        )

        assertEquals(1, snapshot.customerCount)
        assertEquals(1, snapshot.readingCount)
        assertEquals(400.0, snapshot.totalUnitsKwh, 0.01)

        // Verify snapshot appears in available Google Drive backups
        val available = driveManager.getAvailableDriveSnapshots()
        assertTrue(available.isNotEmpty())
        assertEquals(snapshot.fileName, available.first().fileName)

        // Verify restoring from the snapshot recovers exact customer & reading records
        val restoredData = driveManager.readSnapshotData(snapshot)
        assertEquals(1, restoredData.customers.size)
        assertEquals("श्री गणेश इंडस्ट्रीज (Ganesh Industries)", restoredData.customers[0].name)
        assertEquals(1, restoredData.readings.size)
        assertEquals(15400.0, restoredData.readings[0].kwh, 0.01)
        assertEquals(0.966, restoredData.readings[0].powerFactor, 0.001)

        // Verify getLatestSnapshotData works for 1-Tap Restore
        val latestData = driveManager.getLatestSnapshotData()
        assertNotNull(latestData)
        assertEquals(1, latestData!!.customers.size)
        assertEquals(1, latestData.readings.size)
    }
}
