package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterReadingDao {
    @Query("SELECT * FROM meter_readings WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getReadingsForCustomer(customerId: Long): Flow<List<MeterReading>>

    @Query("SELECT * FROM meter_readings WHERE customerId = :customerId ORDER BY timestamp DESC")
    suspend fun getReadingsForCustomerList(customerId: Long): List<MeterReading>

    @Query("SELECT * FROM meter_readings WHERE customerId = :customerId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestReading(customerId: Long): MeterReading?

    @Query("SELECT * FROM meter_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<MeterReading>>

    @Query("SELECT * FROM meter_readings WHERE customerId = :customerId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getReadingsBetween(customerId: Long, startTime: Long, endTime: Long): List<MeterReading>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReading): Long

    @Update
    suspend fun updateReading(reading: MeterReading)

    @Delete
    suspend fun deleteReading(reading: MeterReading)

    @Query("SELECT * FROM meter_readings ORDER BY timestamp ASC")
    suspend fun getAllReadingsList(): List<MeterReading>

    @Query("SELECT COUNT(*) FROM meter_readings")
    suspend fun getTotalReadingsCount(): Int

    @Query("DELETE FROM meter_readings WHERE customerId = :customerId")
    suspend fun deleteAllReadingsForCustomer(customerId: Long)

    @Query("DELETE FROM meter_readings")
    suspend fun deleteAllReadings()
}

