package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meter_readings",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("customerId"),
        Index("timestamp")
    ]
)
data class MeterReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val readingDate: String,
    val readingTime: String,
    val timestamp: Long,
    val kwh: Double,
    val kvarh: Double,
    val kvah: Double,
    val powerFactor: Double,
    val isPfAutoCalculated: Boolean = true,
    val previousKwh: Double? = null,
    val unitsConsumed: Double? = null,
    val remarks: String = ""
)
