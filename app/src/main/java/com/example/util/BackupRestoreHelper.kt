package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupParsedData(
    val exportDate: String,
    val customers: List<Customer>,
    val readings: List<MeterReading>,
    val fileType: String = "JSON"
)

object BackupRestoreHelper {

    fun createBackupJson(customers: List<Customer>, readings: List<MeterReading>): String {
        val root = JSONObject()
        root.put("app", "MeterRead Pro")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        val dateStr = SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", Locale.US).format(Date())
        root.put("exportDate", dateStr)
        root.put("totalCustomers", customers.size)
        root.put("totalReadings", readings.size)

        val customerLookup = customers.associateBy { it.id }

        // Serialize Customers
        val customersArray = JSONArray()
        for (cust in customers) {
            val cObj = JSONObject().apply {
                put("id", cust.id)
                put("name", cust.name)
                put("address", cust.address)
                put("meterNumber", cust.meterNumber)
                put("consumerNumber", cust.consumerNumber)
                put("phoneNumber", cust.phoneNumber)
                put("connectionType", cust.connectionType)
                put("sanctionedLoad", cust.sanctionedLoad)
                put("createdAt", cust.createdAt)
            }
            customersArray.put(cObj)
        }
        root.put("customers", customersArray)

        // Serialize MeterReadings with explicit customer ownership metadata
        val readingsArray = JSONArray()
        for (r in readings) {
            val owner = customerLookup[r.customerId] ?: continue
            val rObj = JSONObject().apply {
                put("id", r.id)
                put("customerId", r.customerId)
                put("customerName", owner.name)
                put("meterNumber", owner.meterNumber)
                put("readingDate", r.readingDate)
                put("readingTime", r.readingTime)
                put("timestamp", r.timestamp)
                put("kwh", r.kwh)
                put("kvarh", r.kvarh)
                put("kvah", r.kvah)
                put("powerFactor", r.powerFactor)
                put("isPfAutoCalculated", r.isPfAutoCalculated)
                if (r.previousKwh != null) put("previousKwh", r.previousKwh)
                if (r.unitsConsumed != null) put("unitsConsumed", r.unitsConsumed)
                put("remarks", r.remarks)
            }
            readingsArray.put(rObj)
        }
        root.put("readings", readingsArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): BackupParsedData {
        val root = JSONObject(jsonString)
        val exportDate = root.optString("exportDate", "Unknown date")

        val customersList = mutableListOf<Customer>()
        val usedCustomerIds = mutableSetOf<Long>()
        var nextFallbackId = 1L

        val cArray = root.optJSONArray("customers") ?: JSONArray()
        for (i in 0 until cArray.length()) {
            val obj = cArray.getJSONObject(i)
            var custId = obj.optLong("id", 0L)
            if (custId <= 0L || usedCustomerIds.contains(custId)) {
                while (usedCustomerIds.contains(nextFallbackId)) nextFallbackId++
                custId = nextFallbackId++
            }
            usedCustomerIds.add(custId)

            customersList.add(
                Customer(
                    id = custId,
                    name = obj.optString("name", "").trim(),
                    address = obj.optString("address", "").trim(),
                    meterNumber = obj.optString("meterNumber", "").trim(),
                    consumerNumber = obj.optString("consumerNumber", "").trim(),
                    phoneNumber = obj.optString("phoneNumber", "").trim(),
                    connectionType = obj.optString("connectionType", "3-Phase Commercial"),
                    sanctionedLoad = obj.optString("sanctionedLoad", "").trim(),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val customerById = customersList.associateBy { it.id }
        val readingsList = mutableListOf<MeterReading>()
        val rArray = root.optJSONArray("readings") ?: JSONArray()
        for (i in 0 until rArray.length()) {
            val obj = rArray.getJSONObject(i)
            val rawCustId = obj.optLong("customerId", -1L)
            val hintCustName = obj.optString("customerName", "").trim()
            val hintMeterNo = obj.optString("meterNumber", "").trim()

            // Verify reading belongs strictly to its owning customer
            val verifiedCustomer = customerById[rawCustId]?.let { candidate ->
                if (hintCustName.isNotEmpty() && !candidate.name.equals(hintCustName, ignoreCase = true)) {
                    // Fallback match by exact name + meter when ID was remapped externally
                    customersList.firstOrNull {
                        it.name.equals(hintCustName, ignoreCase = true) &&
                            (hintMeterNo.isEmpty() || it.meterNumber.equals(hintMeterNo, ignoreCase = true))
                    }
                } else {
                    candidate
                }
            } ?: if (hintCustName.isNotEmpty()) {
                customersList.firstOrNull {
                    it.name.equals(hintCustName, ignoreCase = true) &&
                        (hintMeterNo.isEmpty() || it.meterNumber.equals(hintMeterNo, ignoreCase = true))
                }
            } else {
                null
            }

            if (verifiedCustomer == null) continue

            val prevKwh = if (obj.has("previousKwh") && !obj.isNull("previousKwh")) obj.getDouble("previousKwh") else null
            val unitsConsumed = if (obj.has("unitsConsumed") && !obj.isNull("unitsConsumed")) obj.getDouble("unitsConsumed") else null

            readingsList.add(
                MeterReading(
                    id = obj.optLong("id", 0L),
                    customerId = verifiedCustomer.id,
                    readingDate = obj.getString("readingDate"),
                    readingTime = obj.getString("readingTime"),
                    timestamp = obj.getLong("timestamp"),
                    kwh = obj.getDouble("kwh"),
                    kvarh = obj.getDouble("kvarh"),
                    kvah = obj.getDouble("kvah"),
                    powerFactor = obj.getDouble("powerFactor"),
                    isPfAutoCalculated = obj.optBoolean("isPfAutoCalculated", true),
                    previousKwh = prevKwh,
                    unitsConsumed = unitsConsumed,
                    remarks = obj.optString("remarks", "")
                )
            )
        }

        return BackupParsedData(exportDate, customersList, readingsList)
    }

    fun createBackupFile(context: Context, jsonString: String): File {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(reportsDir, "MeterRead_FullBackup_$timestamp.json")

        FileOutputStream(backupFile).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(jsonString)
                writer.flush()
            }
        }
        return backupFile
    }

    fun readTextFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } ?: throw IllegalArgumentException("Could not open file stream")
    }
}
