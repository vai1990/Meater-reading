package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.Customer
import com.example.data.local.MeterReading
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.shadows.ShadowLog

class TeamSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private lateinit var repository: TeamSyncRepository

    @Before
    override fun setUp() {
        super.setUp()
        ShadowLog.stream = System.out
        repository = TeamSyncRepository(db)
    }

    @Test
    fun `owner can create workspace and colleague can join via invite code and sync readings`() = runEmulatorTest {
        val ownerUid = signInTestUser("vaibhav@example.com")

        // 1. Owner creates workspace TEAM101
        val createResult = repository.createOrGetWorkspace(
            inviteCode = "TEAM101",
            teamName = "Vaibhav Electrical Team",
            userDisplayName = "Vaibhav"
        )
        assertTrue("Create workspace should succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val createdWorkspace = createResult.getOrThrow()
        assertEquals("TEAM101", createdWorkspace.inviteCode)
        assertTrue(ownerUid in createdWorkspace.memberUids)

        // 2. Colleague signs in and joins TEAM101
        val colleagueUid = signInTestUser("rahul@example.com")
        val joinResult = repository.joinWorkspaceByCode(
            inviteCode = "TEAM101",
            colleagueName = "Rahul"
        )
        assertTrue("Colleague join should succeed: ${joinResult.exceptionOrNull()}", joinResult.isSuccess)
        val joinedWorkspace = joinResult.getOrThrow()
        assertEquals(2, joinedWorkspace.memberUids.size)
        assertTrue(ownerUid in joinedWorkspace.memberUids)
        assertTrue(colleagueUid in joinedWorkspace.memberUids)

        // 3. Colleague pushes a customer and meter reading to the shared workspace
        val customer = Customer(
            id = 1L,
            name = "Anand Textiles",
            address = "MIDC Plot 45",
            meterNumber = "LT-449201",
            consumerNumber = "CA-98721455",
            phoneNumber = "9876543210",
            connectionType = "3-Phase Industrial",
            sanctionedLoad = "65 kW"
        )
        val custPush = repository.pushCustomerToCloud(
            workspace = joinedWorkspace,
            customer = customer,
            createdByName = "Rahul"
        )
        assertTrue("Customer push should succeed: ${custPush.exceptionOrNull()}", custPush.isSuccess)

        val reading = MeterReading(
            id = 10L,
            customerId = 1L,
            readingDate = "2026-11-15",
            readingTime = "11:30 AM",
            timestamp = 1763206200000L,
            kwh = 1540.5,
            kvarh = 310.0,
            kvah = 1571.4,
            powerFactor = 0.98,
            isPfAutoCalculated = true,
            previousKwh = 1500.0,
            unitsConsumed = 40.5,
            remarks = "Colleague site visit"
        )
        val readPush = repository.pushReadingToCloud(
            workspace = joinedWorkspace,
            customer = customer,
            reading = reading,
            createdByName = "Rahul"
        )
        assertTrue("Reading push should succeed: ${readPush.exceptionOrNull()}", readPush.isSuccess)

        // 4. Verify real-time Flows emit the synced workspace, customer, and reading
        val sharedWorkspaces = repository.observeUserWorkspaces().first { it.isNotEmpty() }
        assertEquals(1, sharedWorkspaces.size)
        assertEquals("TEAM101", sharedWorkspaces.first().inviteCode)

        val sharedCustomers = repository.observeSharedCustomers().first { it.isNotEmpty() }
        assertEquals(1, sharedCustomers.size)
        assertEquals("LT-449201", sharedCustomers.first().meterNumber)
        assertEquals("Rahul", sharedCustomers.first().createdByName)

        val sharedReadings = repository.observeSharedReadings().first { it.isNotEmpty() }
        assertEquals(1, sharedReadings.size)
        assertEquals(1540.5, sharedReadings.first().kwh, 0.01)
        assertEquals("Rahul", sharedReadings.first().createdByName)
    }

    @Test
    fun `outsider not in workspace memberUids is denied reading shared records`() = runEmulatorTest {
        val ownerUid = signInTestUser("owner@example.com")
        val ws = repository.createOrGetWorkspace("TEAM202", "Owner Team", "Owner").getOrThrow()

        val customer = Customer(
            id = 1L,
            name = "Private Factory",
            address = "Pune",
            meterNumber = "MTR-999"
        )
        val docId = repository.pushCustomerToCloud(ws, customer, "Owner").getOrThrow()
        assertNotNull(ownerUid)

        // Sign in as an outsider who has NOT joined TEAM202
        signInTestUser("outsider@example.com")
        val directReadResult = runCatching {
            db.collection("cloud_customers").document(docId).get().await()
        }
        assertTrue("Outsider direct read must fail", directReadResult.isFailure)
        val ex = directReadResult.exceptionOrNull() as? FirebaseFirestoreException
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun `unauthenticated user is denied creating workspace or pushing readings`() = runEmulatorTest {
        auth.signOut()

        val createResult = repository.createOrGetWorkspace("TEAM303", "Anon Team", "Anon")
        assertTrue("Unauthenticated workspace creation must fail", createResult.isFailure)

        val rawWriteResult = runCatching {
            db.collection("workspaces").document("TEAM303").set(
                mapOf(
                    "ownerId" to "anon",
                    "name" to "Anon Team",
                    "inviteCode" to "TEAM303",
                    "memberUids" to listOf("anon"),
                    "memberNames" to listOf("Anon")
                )
            ).await()
        }
        assertTrue("Unauthenticated raw Firestore write must fail", rawWriteResult.isFailure)
        val ex = rawWriteResult.exceptionOrNull() as? FirebaseFirestoreException
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }
}
