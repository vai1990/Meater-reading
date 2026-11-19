package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val meterNumber: String,
    val consumerNumber: String = "",
    val phoneNumber: String = "",
    val connectionType: String = "3-Phase Commercial",
    val sanctionedLoad: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
