package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

class MeterRepository(private val database: AppDatabase) {

    private val customerDao = database.customerDao()
    private val readingDao = database.meterReadingDao()

    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdDirect(id: Long): Customer? = customerDao.getCustomerByIdDirect(id)

    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)

    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    fun getReadingsForCustomer(customerId: Long): Flow<List<MeterReading>> =
        readingDao.getReadingsForCustomer(customerId)

    val allReadings: Flow<List<MeterReading>> =
        readingDao.getAllReadings()

    suspend fun getReadingsForCustomerList(customerId: Long): List<MeterReading> =
        readingDao.getReadingsForCustomerList(customerId)

    suspend fun getLatestReading(customerId: Long): MeterReading? =
        readingDao.getLatestReading(customerId)

    suspend fun insertReading(reading: MeterReading): Long =
        readingDao.insertReading(reading)

    suspend fun updateReading(reading: MeterReading) =
        readingDao.updateReading(reading)

    suspend fun deleteReading(reading: MeterReading) =
        readingDao.deleteReading(reading)


    suspend fun getAllCustomersList(): List<Customer> = customerDao.getAllCustomersList()

    suspend fun getAllReadingsList(): List<MeterReading> = readingDao.getAllReadingsList()

    suspend fun getTotalStats(): Triple<Int, Int, Double> {
        val customerCount = customerDao.getCustomerCount()
        val readings = readingDao.getAllReadingsList()
        val totalUnits = readings.mapNotNull { it.unitsConsumed }.sum()
        return Triple(customerCount, readings.size, totalUnits)
    }

    suspend fun restoreData(
        customers: List<Customer>,
        readings: List<MeterReading>,
        replaceExisting: Boolean
    ): Pair<Int, Int> {
        return database.withTransaction {
            if (replaceExisting) {
                readingDao.deleteAllReadings()
                customerDao.deleteAllCustomers()
            }

            val existingCustomers = if (replaceExisting) {
                emptyList()
            } else {
                customerDao.getAllCustomersList()
            }

            // Map each distinct backup customer index & source ID to the local Room customer ID
            val idMap = mutableMapOf<Long, Long>()
            var insertedCustomers = 0
            var insertedReadings = 0

            val existingCache = existingCustomers.toMutableList()

            for (cust in customers) {
                val oldId = cust.id
                val cleanCust = cust.copy(
                    id = 0L,
                    name = cust.name.trim(),
                    address = cust.address.trim(),
                    meterNumber = cust.meterNumber.trim(),
                    consumerNumber = cust.consumerNumber.trim(),
                    phoneNumber = cust.phoneNumber.trim(),
                    sanctionedLoad = cust.sanctionedLoad.trim()
                )

                val matchedExisting = if (replaceExisting) {
                    null
                } else {
                    existingCache.firstOrNull { local ->
                        isSameCustomerRecord(local, cleanCust)
                    }
                }

                val targetId = if (matchedExisting != null) {
                    matchedExisting.id
                } else {
                    val newId = customerDao.insertCustomer(cleanCust)
                    existingCache.add(cleanCust.copy(id = newId))
                    insertedCustomers++
                    newId
                }

                if (oldId != 0L) {
                    idMap[oldId] = targetId
                }
            }

            // Cache existing readings per target customer ID to prevent duplicate mixing on merge
            val existingReadingsByCust = mutableMapOf<Long, MutableList<MeterReading>>()

            for (reading in readings) {
                val mappedCustomerId = idMap[reading.customerId] ?: continue

                val custExistingReadings = existingReadingsByCust.getOrPut(mappedCustomerId) {
                    if (replaceExisting) {
                        mutableListOf()
                    } else {
                        readingDao.getReadingsForCustomerList(mappedCustomerId).toMutableList()
                    }
                }

                val isDuplicate = custExistingReadings.any { existing ->
                    existing.readingDate.trim().equals(reading.readingDate.trim(), ignoreCase = true) &&
                        abs(existing.kwh - reading.kwh) < 0.01 &&
                        abs(existing.kvarh - reading.kvarh) < 0.01 &&
                        abs(existing.kvah - reading.kvah) < 0.01
                }

                if (!isDuplicate) {
                    val inserted = reading.copy(
                        id = 0L,
                        customerId = mappedCustomerId
                    )
                    val newReadId = readingDao.insertReading(inserted)
                    custExistingReadings.add(inserted.copy(id = newReadId))
                    insertedReadings++
                }
            }

            Pair(insertedCustomers, insertedReadings)
        }
    }

    private fun isSameCustomerRecord(a: Customer, b: Customer): Boolean {
        val aName = a.name.trim()
        val bName = b.name.trim()
        if (!aName.equals(bName, ignoreCase = true)) {
            return false
        }

        val aMeter = a.meterNumber.trim()
        val bMeter = b.meterNumber.trim()
        if (aMeter.equals(bMeter, ignoreCase = true)) {
            return true
        }

        return isGenericMeterPlaceholder(aMeter) || isGenericMeterPlaceholder(bMeter)
    }

    private fun isGenericMeterPlaceholder(meter: String): Boolean {
        val m = meter.trim().uppercase()
        return m.isEmpty() ||
            m == "UNKNOWN" ||
            m == "N/A" ||
            m == "MTR-N/A" ||
            m.startsWith("SHEET-MTR-") ||
            Regex("^MTR-\\d+$").matches(m)
    }

    suspend fun clearAllData() {
        database.withTransaction {
            readingDao.deleteAllReadings()
            customerDao.deleteAllCustomers()
        }
    }

    suspend fun resetToDemoData() {
        clearAllData()
    }
}
