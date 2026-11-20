package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class TeamSyncRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val workspacesCollection = db.collection("workspaces")
    private val customersCollection = db.collection("cloud_customers")
    private val readingsCollection = db.collection("cloud_readings")

    private fun sanitizeDocId(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return if (cleaned.isEmpty()) "doc_${System.currentTimeMillis()}" else cleaned.take(100)
    }

    fun generateDefaultInviteCode(uid: String): String {
        val prefix = uid.replace(Regex("[^a-zA-Z0-9]"), "").uppercase(Locale.US).take(6)
        return "TEAM_${if (prefix.length >= 3) prefix else "101A"}"
    }

    fun observeUserWorkspaces(): Flow<List<TeamWorkspace>> {
        val uid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return emptyFlow()

        return workspacesCollection
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TeamWorkspace::class.java)
                }
            }
            .catch { e ->
                throw handleFirestoreError(e, OperationType.LIST, "workspaces")
            }
    }

    fun observeSharedCustomers(): Flow<List<CloudCustomer>> {
        val uid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return emptyFlow()

        return customersCollection
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CloudCustomer::class.java)
                }
            }
            .catch { e ->
                throw handleFirestoreError(e, OperationType.LIST, "cloud_customers")
            }
    }

    fun observeSharedReadings(): Flow<List<CloudReading>> {
        val uid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return emptyFlow()

        return readingsCollection
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CloudReading::class.java)
                }
            }
            .catch { e ->
                throw handleFirestoreError(e, OperationType.LIST, "cloud_readings")
            }
    }

    suspend fun getWorkspaceById(workspaceId: String): Result<TeamWorkspace?> = runCatching {
        val cleanId = sanitizeDocId(workspaceId.uppercase(Locale.US))
        try {
            val snapshot = workspacesCollection.document(cleanId).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(TeamWorkspace::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            throw handleFirestoreError(e, OperationType.GET, "workspaces/$cleanId")
        }
    }

    suspend fun createOrGetWorkspace(
        inviteCode: String,
        teamName: String,
        userDisplayName: String
    ): Result<TeamWorkspace> = runCatching {
        val currentUser = Firebase.auth.currentUser
            ?: throw handleFirestoreError(
                IllegalStateException("User not signed in"),
                OperationType.CREATE,
                "workspaces"
            )
        val uid = currentUser.uid
        val cleanCode = sanitizeDocId(inviteCode.uppercase(Locale.US))
        val docRef = workspacesCollection.document(cleanCode)

        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existing = existingSnap.toObject(TeamWorkspace::class.java)
                if (existing != null) {
                    if (uid in existing.memberUids) {
                        return@runCatching existing
                    } else if (existing.memberUids.size < 10) {
                        val updatedUids = (existing.memberUids + uid).distinct().take(10)
                        val updatedNames = (existing.memberNames + userDisplayName.ifBlank { "Colleague" }).distinct().take(10)
                        docRef.update(
                            mapOf(
                                "memberUids" to updatedUids,
                                "memberNames" to updatedNames,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        val joined = existing.copy(
                            memberUids = updatedUids,
                            memberNames = updatedNames
                        )
                        return@runCatching joined
                    }
                }
            }

            val workspace = TeamWorkspace(
                id = cleanCode,
                ownerId = uid,
                name = teamName.trim().ifBlank { "MeterRead Team" }.take(100),
                inviteCode = cleanCode,
                memberUids = listOf(uid),
                memberNames = listOf(userDisplayName.trim().ifBlank { "Team Admin" }.take(100))
            )
            docRef.set(workspace).await()
            workspace
        } catch (e: Exception) {
            throw handleFirestoreError(e, OperationType.CREATE, "workspaces/$cleanCode")
        }
    }

    suspend fun joinWorkspaceByCode(
        inviteCode: String,
        colleagueName: String
    ): Result<TeamWorkspace> = runCatching {
        val currentUser = Firebase.auth.currentUser
            ?: throw handleFirestoreError(
                IllegalStateException("User not signed in"),
                OperationType.UPDATE,
                "workspaces"
            )
        val uid = currentUser.uid
        val cleanCode = sanitizeDocId(inviteCode.uppercase(Locale.US))
        val docRef = workspacesCollection.document(cleanCode)

        try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                throw IllegalArgumentException("टीम कोड '$cleanCode' सापडला नाही (Team code not found)")
            }
            val workspace = snapshot.toObject(TeamWorkspace::class.java)
                ?: throw IllegalStateException("Invalid workspace data")

            if (uid in workspace.memberUids) {
                return@runCatching workspace
            }
            if (workspace.memberUids.size >= 10) {
                throw IllegalStateException("या टीममध्ये कमाल १० सदस्य पूर्ण झाले आहेत (Team is full)")
            }

            val newMemberUids = workspace.memberUids + uid
            val safeName = colleagueName.trim().ifBlank { "Colleague" }.take(100)
            val newMemberNames = (workspace.memberNames + safeName).take(10)

            docRef.update(
                mapOf(
                    "memberUids" to newMemberUids,
                    "memberNames" to newMemberNames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            workspace.copy(
                memberUids = newMemberUids,
                memberNames = newMemberNames
            )
        } catch (e: Exception) {
            throw handleFirestoreError(e, OperationType.UPDATE, "workspaces/$cleanCode")
        }
    }

    suspend fun pushCustomerToCloud(
        workspace: TeamWorkspace,
        customer: Customer,
        createdByName: String
    ): Result<String> = runCatching {
        val currentUser = Firebase.auth.currentUser
            ?: throw handleFirestoreError(
                IllegalStateException("User not signed in"),
                OperationType.CREATE,
                "cloud_customers"
            )
        val uid = currentUser.uid
        val cleanWorkspaceId = sanitizeDocId(workspace.inviteCode.ifBlank { workspace.id })
        val cleanMeter = sanitizeDocId(customer.meterNumber)
        val docId = sanitizeDocId("${cleanWorkspaceId}_${cleanMeter}")
        val docRef = customersCollection.document(docId)

        val memberUids = (workspace.memberUids + uid).distinct().take(10)
        val cloudCustomer = CloudCustomer(
            id = docId,
            workspaceId = cleanWorkspaceId,
            ownerId = uid,
            createdByName = createdByName.trim().ifBlank { "Team Member" }.take(100),
            memberUids = memberUids,
            name = customer.name.trim().ifBlank { "Customer" }.take(150),
            address = customer.address.trim().take(300),
            meterNumber = customer.meterNumber.trim().ifBlank { "MTR-01" }.take(64),
            consumerNumber = customer.consumerNumber.trim().take(64),
            phoneNumber = customer.phoneNumber.trim().take(32),
            connectionType = customer.connectionType.trim().ifBlank { "3-Phase Industrial" }.take(64),
            sanctionedLoad = customer.sanctionedLoad.trim().take(64)
        )

        try {
            try {
                docRef.set(cloudCustomer).await()
            } catch (_: Exception) {
                docRef.update(
                    mapOf(
                        "memberUids" to memberUids,
                        "name" to cloudCustomer.name,
                        "address" to cloudCustomer.address,
                        "meterNumber" to cloudCustomer.meterNumber,
                        "consumerNumber" to cloudCustomer.consumerNumber,
                        "phoneNumber" to cloudCustomer.phoneNumber,
                        "connectionType" to cloudCustomer.connectionType,
                        "sanctionedLoad" to cloudCustomer.sanctionedLoad,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            docId
        } catch (e: Exception) {
            throw handleFirestoreError(e, OperationType.WRITE, "cloud_customers/$docId")
        }
    }

    suspend fun pushReadingToCloud(
        workspace: TeamWorkspace,
        customer: Customer,
        reading: MeterReading,
        createdByName: String
    ): Result<String> = runCatching {
        val currentUser = Firebase.auth.currentUser
            ?: throw handleFirestoreError(
                IllegalStateException("User not signed in"),
                OperationType.CREATE,
                "cloud_readings"
            )
        val uid = currentUser.uid
        val cleanWorkspaceId = sanitizeDocId(workspace.inviteCode.ifBlank { workspace.id })
        val cleanMeter = sanitizeDocId(customer.meterNumber)
        val docId = sanitizeDocId("${cleanWorkspaceId}_${cleanMeter}_${reading.timestamp}")
        val docRef = readingsCollection.document(docId)

        val memberUids = (workspace.memberUids + uid).distinct().take(10)
        val safeKwh = if (reading.kwh > 0.0) reading.kwh else 0.01
        val safeKvarh = if (reading.kvarh >= 0.0) reading.kvarh else 0.0
        val safeKvah = if (reading.kvah > 0.0) reading.kvah else safeKwh
        val safePf = reading.powerFactor.coerceIn(0.0, 1.0)
        val safePrevKwh = (reading.previousKwh ?: 0.0).coerceAtLeast(0.0)
        val safeConsumed = (reading.unitsConsumed ?: 0.0).coerceAtLeast(0.0)

        val cloudReading = CloudReading(
            id = docId,
            workspaceId = cleanWorkspaceId,
            ownerId = uid,
            createdByName = createdByName.trim().ifBlank { "Team Member" }.take(100),
            memberUids = memberUids,
            customerMeterNumber = customer.meterNumber.trim().ifBlank { "MTR-01" }.take(64),
            customerName = customer.name.trim().ifBlank { "Customer" }.take(150),
            readingDate = reading.readingDate.trim().ifBlank { "2026-11-15" }.take(32),
            readingTime = reading.readingTime.trim().ifBlank { "10:00 AM" }.take(32),
            readingTimestamp = reading.timestamp.coerceAtLeast(0L),
            kwh = safeKwh,
            kvarh = safeKvarh,
            kvah = safeKvah,
            powerFactor = safePf,
            isPfAutoCalculated = reading.isPfAutoCalculated,
            previousKwh = safePrevKwh,
            unitsConsumed = safeConsumed,
            remarks = reading.remarks.trim().take(300)
        )

        try {
            try {
                docRef.set(cloudReading).await()
            } catch (_: Exception) {
                docRef.update(
                    mapOf(
                        "memberUids" to memberUids,
                        "customerMeterNumber" to cloudReading.customerMeterNumber,
                        "customerName" to cloudReading.customerName,
                        "readingDate" to cloudReading.readingDate,
                        "readingTime" to cloudReading.readingTime,
                        "readingTimestamp" to cloudReading.readingTimestamp,
                        "kwh" to cloudReading.kwh,
                        "kvarh" to cloudReading.kvarh,
                        "kvah" to cloudReading.kvah,
                        "powerFactor" to cloudReading.powerFactor,
                        "isPfAutoCalculated" to cloudReading.isPfAutoCalculated,
                        "previousKwh" to cloudReading.previousKwh,
                        "unitsConsumed" to cloudReading.unitsConsumed,
                        "remarks" to cloudReading.remarks,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            docId
        } catch (e: Exception) {
            throw handleFirestoreError(e, OperationType.WRITE, "cloud_readings/$docId")
        }
    }

    suspend fun propagateWorkspaceMembersToExistingRecords(workspace: TeamWorkspace) {
        val currentUser = try {
            Firebase.auth.currentUser
        } catch (_: Exception) {
            null
        } ?: return

        val uid = currentUser.uid
        val updatedMembers = workspace.memberUids.distinct().take(10)
        if (updatedMembers.size <= 1) return

        try {
            val custSnap = customersCollection
                .whereArrayContains("memberUids", uid)
                .get()
                .await()
            for (doc in custSnap.documents) {
                val cust = doc.toObject(CloudCustomer::class.java) ?: continue
                if (cust.workspaceId == workspace.inviteCode && cust.memberUids != updatedMembers) {
                    doc.reference.update(
                        mapOf(
                            "memberUids" to updatedMembers,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val readSnap = readingsCollection
                .whereArrayContains("memberUids", uid)
                .get()
                .await()
            for (doc in readSnap.documents) {
                val read = doc.toObject(CloudReading::class.java) ?: continue
                if (read.workspaceId == workspace.inviteCode && read.memberUids != updatedMembers) {
                    doc.reference.update(
                        mapOf(
                            "memberUids" to updatedMembers,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (_: Exception) {
            // Best-effort member propagation
        }
    }

    suspend fun deleteCustomerFromCloud(
        customerName: String,
        meterNumber: String
    ): Result<Int> = runCatching {
        val uid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return@runCatching 0

        val targetName = customerName.trim()
        val targetMeter = meterNumber.trim()
        var deletedCount = 0

        val custSnap = customersCollection
            .whereArrayContains("memberUids", uid)
            .get()
            .await()
        for (doc in custSnap.documents) {
            val cust = doc.toObject(CloudCustomer::class.java) ?: continue
            val nameMatches = targetName.isNotEmpty() && cust.name.trim().equals(targetName, ignoreCase = true)
            val meterMatches = targetMeter.isNotEmpty() &&
                !targetMeter.startsWith("SHEET-MTR-", ignoreCase = true) &&
                !targetMeter.equals("MTR-01", ignoreCase = true) &&
                cust.meterNumber.trim().equals(targetMeter, ignoreCase = true)
            if (nameMatches || meterMatches) {
                runCatching { doc.reference.delete().await(); deletedCount++ }
            }
        }

        val readSnap = readingsCollection
            .whereArrayContains("memberUids", uid)
            .get()
            .await()
        for (doc in readSnap.documents) {
            val read = doc.toObject(CloudReading::class.java) ?: continue
            val nameMatches = targetName.isNotEmpty() && read.customerName.trim().equals(targetName, ignoreCase = true)
            val meterMatches = targetMeter.isNotEmpty() &&
                !targetMeter.startsWith("SHEET-MTR-", ignoreCase = true) &&
                !targetMeter.equals("MTR-01", ignoreCase = true) &&
                read.customerMeterNumber.trim().equals(targetMeter, ignoreCase = true)
            if (nameMatches || meterMatches) {
                runCatching { doc.reference.delete().await(); deletedCount++ }
            }
        }
        deletedCount
    }

    suspend fun deleteReadingFromCloud(
        customerName: String,
        readingDate: String,
        timestamp: Long,
        kwh: Double
    ): Result<Int> = runCatching {
        val uid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return@runCatching 0

        val targetName = customerName.trim()
        var deletedCount = 0

        val readSnap = readingsCollection
            .whereArrayContains("memberUids", uid)
            .get()
            .await()
        for (doc in readSnap.documents) {
            val read = doc.toObject(CloudReading::class.java) ?: continue
            val nameMatches = targetName.isEmpty() || read.customerName.trim().equals(targetName, ignoreCase = true)
            val timeMatches = timestamp > 0L && kotlin.math.abs(read.readingTimestamp - timestamp) < 60_000L
            val dateKwhMatches = read.readingDate.trim().equals(readingDate.trim(), ignoreCase = true) &&
                kotlin.math.abs(read.kwh - kwh) < 0.01
            if (nameMatches && (timeMatches || dateKwhMatches)) {
                runCatching { doc.reference.delete().await(); deletedCount++ }
            }
        }
        deletedCount
    }

    suspend fun clearAllUserCloudData(): Result<Int> = runCatching {
        val uid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return@runCatching 0

        var deletedCount = 0
        val custSnap = customersCollection
            .whereArrayContains("memberUids", uid)
            .get()
            .await()
        for (doc in custSnap.documents) {
            runCatching { doc.reference.delete().await(); deletedCount++ }
        }

        val readSnap = readingsCollection
            .whereArrayContains("memberUids", uid)
            .get()
            .await()
        for (doc in readSnap.documents) {
            runCatching { doc.reference.delete().await(); deletedCount++ }
        }
        deletedCount
    }
}
