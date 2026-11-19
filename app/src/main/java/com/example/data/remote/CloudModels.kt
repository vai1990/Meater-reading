package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TeamWorkspace(
    @DocumentId
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val memberNames: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudCustomer(
    @DocumentId
    val id: String = "",
    val workspaceId: String = "",
    val ownerId: String = "",
    val createdByName: String = "",
    val memberUids: List<String> = emptyList(),
    val name: String = "",
    val address: String = "",
    val meterNumber: String = "",
    val consumerNumber: String = "",
    val phoneNumber: String = "",
    val connectionType: String = "3-Phase Industrial",
    val sanctionedLoad: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudReading(
    @DocumentId
    val id: String = "",
    val workspaceId: String = "",
    val ownerId: String = "",
    val createdByName: String = "",
    val memberUids: List<String> = emptyList(),
    val customerMeterNumber: String = "",
    val customerName: String = "",
    val readingDate: String = "",
    val readingTime: String = "",
    val readingTimestamp: Long = 0L,
    val kwh: Double = 0.0,
    val kvarh: Double = 0.0,
    val kvah: Double = 0.0,
    val powerFactor: Double = 0.0,
    @get:PropertyName("isPfAutoCalculated")
    @set:PropertyName("isPfAutoCalculated")
    var isPfAutoCalculated: Boolean = true,
    val previousKwh: Double = 0.0,
    val unitsConsumed: Double = 0.0,
    val remarks: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
