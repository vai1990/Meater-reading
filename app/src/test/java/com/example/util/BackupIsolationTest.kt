package com.example.util

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import com.example.data.repository.MeterRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupIsolationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: MeterRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MeterRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun excelBackupAndRestoreKeepsCustomersWithSameMeterIsolated() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val custA = Customer(
            id = 10L,
            name = "TIRUPATI CRUSHER",
            address = "MIDC Plot 1",
            meterNumber = "MTR-001",
            sanctionedLoad = "100 kW"
        )
        val custB = Customer(
            id = 20L,
            name = "SHIVAM AGRO",
            address = "MIDC Plot 2",
            meterNumber = "MTR-001", // Same placeholder meter number!
            sanctionedLoad = "65 kW"
        )

        val readings = listOf(
            MeterReading(
                id = 1L,
                customerId = 10L,
                readingDate = "10-Nov-2026",
                readingTime = "09:00 AM",
                timestamp = 1731210000000L,
                kwh = 10000.0,
                kvarh = 2000.0,
                kvah = 10198.0,
                powerFactor = 0.980,
                isPfAutoCalculated = true,
                remarks = "Tirupati Only"
            ),
            MeterReading(
                id = 2L,
                customerId = 20L,
                readingDate = "10-Nov-2026",
                readingTime = "09:30 AM",
                timestamp = 1731211800000L,
                kwh = 50000.0,
                kvarh = 5000.0,
                kvah = 50249.0,
                powerFactor = 0.995,
                isPfAutoCalculated = true,
                remarks = "Shivam Only"
            )
        )

        val excelFile = ExcelBackupRestoreHelper.exportFullDatabaseToExcel(
            context = context,
            customers = listOf(custA, custB),
            readings = readings
        )
        val parsed = ExcelBackupRestoreHelper.parseExcelBackup(excelFile.readText(Charsets.UTF_8))

        assertEquals(2, parsed.customers.size)
        assertEquals(2, parsed.readings.size)

        val parsedTirupati = parsed.customers.first { it.name == "TIRUPATI CRUSHER" }
        val parsedShivam = parsed.customers.first { it.name == "SHIVAM AGRO" }

        val tirupatiReadings = parsed.readings.filter { it.customerId == parsedTirupati.id }
        val shivamReadings = parsed.readings.filter { it.customerId == parsedShivam.id }

        assertEquals(1, tirupatiReadings.size)
        assertEquals(10000.0, tirupatiReadings.first().kwh, 0.01)
        assertEquals("Tirupati Only", tirupatiReadings.first().remarks)

        assertEquals(1, shivamReadings.size)
        assertEquals(50000.0, shivamReadings.first().kwh, 0.01)
        assertEquals("Shivam Only", shivamReadings.first().remarks)
    }

    @Test
    fun repositoryMergeRestoreDoesNotDuplicateOrMixCustomers() = runTest {
        // Pre-existing DB has TIRUPATI CRUSHER with 1 reading
        val existingId = repository.insertCustomer(
            Customer(
                name = "TIRUPATI CRUSHER",
                address = "Site A",
                meterNumber = "MTR-001",
                sanctionedLoad = "100 kW"
            )
        )
        repository.insertReading(
            MeterReading(
                customerId = existingId,
                readingDate = "09-Nov-2026",
                readingTime = "09:00 AM",
                timestamp = 1731123600000L,
                kwh = 10000.0,
                kvarh = 2000.0,
                kvah = 10198.0,
                powerFactor = 0.980,
                isPfAutoCalculated = true,
                remarks = "Existing Tirupati"
            )
        )

        // Restoring backup containing both TIRUPATI CRUSHER (1 duplicate + 1 new) and SHIVAM AGRO (same MTR-001!)
        val backupJson = BackupRestoreHelper.createBackupJson(
            customers = listOf(
                Customer(id = 101L, name = "TIRUPATI CRUSHER", address = "Site A", meterNumber = "MTR-001"),
                Customer(id = 102L, name = "SHIVAM AGRO", address = "Site B", meterNumber = "MTR-001")
            ),
            readings = listOf(
                MeterReading(
                    id = 1L,
                    customerId = 101L,
                    readingDate = "09-Nov-2026",
                    readingTime = "09:00 AM",
                    timestamp = 1731123600000L,
                    kwh = 10000.0,
                    kvarh = 2000.0,
                    kvah = 10198.0,
                    powerFactor = 0.980,
                    isPfAutoCalculated = true,
                    remarks = "Existing Tirupati"
                ),
                MeterReading(
                    id = 2L,
                    customerId = 101L,
                    readingDate = "10-Nov-2026",
                    readingTime = "09:00 AM",
                    timestamp = 1731210000000L,
                    kwh = 10500.0,
                    kvarh = 2100.0,
                    kvah = 10700.0,
                    powerFactor = 0.981,
                    isPfAutoCalculated = true,
                    remarks = "New Tirupati"
                ),
                MeterReading(
                    id = 3L,
                    customerId = 102L,
                    readingDate = "10-Nov-2026",
                    readingTime = "10:00 AM",
                    timestamp = 1731213600000L,
                    kwh = 88000.0,
                    kvarh = 4000.0,
                    kvah = 88090.0,
                    powerFactor = 0.998,
                    isPfAutoCalculated = true,
                    remarks = "Shivam Reading"
                )
            )
        )

        val parsed = BackupRestoreHelper.parseBackupJson(backupJson)
        repository.restoreData(
            customers = parsed.customers,
            readings = parsed.readings,
            replaceExisting = false
        )

        val allCusts = repository.getAllCustomersList()
        assertEquals(2, allCusts.size)

        val tirupati = allCusts.first { it.name == "TIRUPATI CRUSHER" }
        val shivam = allCusts.first { it.name == "SHIVAM AGRO" }

        val tirupatiReads = repository.getReadingsForCustomerList(tirupati.id)
        val shivamReads = repository.getReadingsForCustomerList(shivam.id)

        assertEquals(2, tirupatiReads.size)
        assertEquals(1, shivamReads.size)
        assertEquals(88000.0, shivamReads.first().kwh, 0.01)
    }
}
