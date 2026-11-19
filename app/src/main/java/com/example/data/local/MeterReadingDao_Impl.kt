package com.example.data.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MeterReadingDao_Impl(
    __db: RoomDatabase,
) : MeterReadingDao {
    private val __db: RoomDatabase

    private val __insertAdapterOfMeterReading: EntityInsertAdapter<MeterReading>

    private val __deleteAdapterOfMeterReading: EntityDeleteOrUpdateAdapter<MeterReading>

    private val __updateAdapterOfMeterReading: EntityDeleteOrUpdateAdapter<MeterReading>

    init {
        this.__db = __db
        this.__insertAdapterOfMeterReading = object : EntityInsertAdapter<MeterReading>() {
            protected override fun createQuery(): String =
                "INSERT OR REPLACE INTO `meter_readings` (`id`,`customerId`,`readingDate`,`readingTime`,`timestamp`,`kwh`,`kvarh`,`kvah`,`powerFactor`,`isPfAutoCalculated`,`previousKwh`,`unitsConsumed`,`remarks`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)"

            protected override fun bind(statement: SQLiteStatement, entity: MeterReading) {
                statement.bindLong(1, entity.id)
                statement.bindLong(2, entity.customerId)
                statement.bindText(3, entity.readingDate)
                statement.bindText(4, entity.readingTime)
                statement.bindLong(5, entity.timestamp)
                statement.bindDouble(6, entity.kwh)
                statement.bindDouble(7, entity.kvarh)
                statement.bindDouble(8, entity.kvah)
                statement.bindDouble(9, entity.powerFactor)
                val _tmp: Int = if (entity.isPfAutoCalculated) 1 else 0
                statement.bindLong(10, _tmp.toLong())
                val _tmpPreviousKwh: Double? = entity.previousKwh
                if (_tmpPreviousKwh == null) {
                    statement.bindNull(11)
                } else {
                    statement.bindDouble(11, _tmpPreviousKwh)
                }
                val _tmpUnitsConsumed: Double? = entity.unitsConsumed
                if (_tmpUnitsConsumed == null) {
                    statement.bindNull(12)
                } else {
                    statement.bindDouble(12, _tmpUnitsConsumed)
                }
                statement.bindText(13, entity.remarks)
            }
        }
        this.__deleteAdapterOfMeterReading = object : EntityDeleteOrUpdateAdapter<MeterReading>() {
            protected override fun createQuery(): String = "DELETE FROM `meter_readings` WHERE `id` = ?"

            protected override fun bind(statement: SQLiteStatement, entity: MeterReading) {
                statement.bindLong(1, entity.id)
            }
        }
        this.__updateAdapterOfMeterReading = object : EntityDeleteOrUpdateAdapter<MeterReading>() {
            protected override fun createQuery(): String =
                "UPDATE OR ABORT `meter_readings` SET `id` = ?,`customerId` = ?,`readingDate` = ?,`readingTime` = ?,`timestamp` = ?,`kwh` = ?,`kvarh` = ?,`kvah` = ?,`powerFactor` = ?,`isPfAutoCalculated` = ?,`previousKwh` = ?,`unitsConsumed` = ?,`remarks` = ? WHERE `id` = ?"

            protected override fun bind(statement: SQLiteStatement, entity: MeterReading) {
                statement.bindLong(1, entity.id)
                statement.bindLong(2, entity.customerId)
                statement.bindText(3, entity.readingDate)
                statement.bindText(4, entity.readingTime)
                statement.bindLong(5, entity.timestamp)
                statement.bindDouble(6, entity.kwh)
                statement.bindDouble(7, entity.kvarh)
                statement.bindDouble(8, entity.kvah)
                statement.bindDouble(9, entity.powerFactor)
                val _tmp: Int = if (entity.isPfAutoCalculated) 1 else 0
                statement.bindLong(10, _tmp.toLong())
                val _tmpPreviousKwh: Double? = entity.previousKwh
                if (_tmpPreviousKwh == null) {
                    statement.bindNull(11)
                } else {
                    statement.bindDouble(11, _tmpPreviousKwh)
                }
                val _tmpUnitsConsumed: Double? = entity.unitsConsumed
                if (_tmpUnitsConsumed == null) {
                    statement.bindNull(12)
                } else {
                    statement.bindDouble(12, _tmpUnitsConsumed)
                }
                statement.bindText(13, entity.remarks)
                statement.bindLong(14, entity.id)
            }
        }
    }

    public override suspend fun insertReading(reading: MeterReading): Long = performSuspending(__db, false, true) { _connection ->
        __insertAdapterOfMeterReading.insertAndReturnId(_connection, reading)
    }

    public override suspend fun deleteReading(reading: MeterReading): Unit = performSuspending(__db, false, true) { _connection ->
        __deleteAdapterOfMeterReading.handle(_connection, reading)
    }

    public override suspend fun updateReading(reading: MeterReading): Unit = performSuspending(__db, false, true) { _connection ->
        __updateAdapterOfMeterReading.handle(_connection, reading)
    }

    private fun readMeterReadingRow(_stmt: SQLiteStatement): MeterReading {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCustomerId: Int = getColumnIndexOrThrow(_stmt, "customerId")
        val _columnIndexOfReadingDate: Int = getColumnIndexOrThrow(_stmt, "readingDate")
        val _columnIndexOfReadingTime: Int = getColumnIndexOrThrow(_stmt, "readingTime")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfKwh: Int = getColumnIndexOrThrow(_stmt, "kwh")
        val _columnIndexOfKvarh: Int = getColumnIndexOrThrow(_stmt, "kvarh")
        val _columnIndexOfKvah: Int = getColumnIndexOrThrow(_stmt, "kvah")
        val _columnIndexOfPowerFactor: Int = getColumnIndexOrThrow(_stmt, "powerFactor")
        val _columnIndexOfIsPfAutoCalculated: Int = getColumnIndexOrThrow(_stmt, "isPfAutoCalculated")
        val _columnIndexOfPreviousKwh: Int = getColumnIndexOrThrow(_stmt, "previousKwh")
        val _columnIndexOfUnitsConsumed: Int = getColumnIndexOrThrow(_stmt, "unitsConsumed")
        val _columnIndexOfRemarks: Int = getColumnIndexOrThrow(_stmt, "remarks")

        val _tmpId: Long = _stmt.getLong(_columnIndexOfId)
        val _tmpCustomerId: Long = _stmt.getLong(_columnIndexOfCustomerId)
        val _tmpReadingDate: String = _stmt.getText(_columnIndexOfReadingDate)
        val _tmpReadingTime: String = _stmt.getText(_columnIndexOfReadingTime)
        val _tmpTimestamp: Long = _stmt.getLong(_columnIndexOfTimestamp)
        val _tmpKwh: Double = _stmt.getDouble(_columnIndexOfKwh)
        val _tmpKvarh: Double = _stmt.getDouble(_columnIndexOfKvarh)
        val _tmpKvah: Double = _stmt.getDouble(_columnIndexOfKvah)
        val _tmpPowerFactor: Double = _stmt.getDouble(_columnIndexOfPowerFactor)
        val _tmpIsPfAutoCalculated: Boolean = _stmt.getLong(_columnIndexOfIsPfAutoCalculated).toInt() != 0
        val _tmpPreviousKwh: Double? = if (_stmt.isNull(_columnIndexOfPreviousKwh)) null else _stmt.getDouble(_columnIndexOfPreviousKwh)
        val _tmpUnitsConsumed: Double? = if (_stmt.isNull(_columnIndexOfUnitsConsumed)) null else _stmt.getDouble(_columnIndexOfUnitsConsumed)
        val _tmpRemarks: String = _stmt.getText(_columnIndexOfRemarks)

        return MeterReading(
            _tmpId,
            _tmpCustomerId,
            _tmpReadingDate,
            _tmpReadingTime,
            _tmpTimestamp,
            _tmpKwh,
            _tmpKvarh,
            _tmpKvah,
            _tmpPowerFactor,
            _tmpIsPfAutoCalculated,
            _tmpPreviousKwh,
            _tmpUnitsConsumed,
            _tmpRemarks
        )
    }

    public override fun getReadingsForCustomer(customerId: Long): Flow<List<MeterReading>> {
        val _sql: String = "SELECT * FROM meter_readings WHERE customerId = ? ORDER BY timestamp DESC"
        return createFlow(__db, false, arrayOf("meter_readings")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, customerId)
                val _result: MutableList<MeterReading> = mutableListOf()
                while (_stmt.step()) {
                    _result.add(readMeterReadingRow(_stmt))
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getReadingsForCustomerList(customerId: Long): List<MeterReading> {
        val _sql: String = "SELECT * FROM meter_readings WHERE customerId = ? ORDER BY timestamp DESC"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, customerId)
                val _result: MutableList<MeterReading> = mutableListOf()
                while (_stmt.step()) {
                    _result.add(readMeterReadingRow(_stmt))
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getLatestReading(customerId: Long): MeterReading? {
        val _sql: String = "SELECT * FROM meter_readings WHERE customerId = ? ORDER BY timestamp DESC LIMIT 1"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, customerId)
                if (_stmt.step()) {
                    readMeterReadingRow(_stmt)
                } else {
                    null
                }
            } finally {
                _stmt.close()
            }
        }
    }

    public override fun getAllReadings(): Flow<List<MeterReading>> {
        val _sql: String = "SELECT * FROM meter_readings ORDER BY timestamp DESC"
        return createFlow(__db, false, arrayOf("meter_readings")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                val _result: MutableList<MeterReading> = mutableListOf()
                while (_stmt.step()) {
                    _result.add(readMeterReadingRow(_stmt))
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getReadingsBetween(
        customerId: Long,
        startTime: Long,
        endTime: Long,
    ): List<MeterReading> {
        val _sql: String = "SELECT * FROM meter_readings WHERE customerId = ? AND timestamp BETWEEN ? AND ? ORDER BY timestamp ASC"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, customerId)
                _stmt.bindLong(2, startTime)
                _stmt.bindLong(3, endTime)
                val _result: MutableList<MeterReading> = mutableListOf()
                while (_stmt.step()) {
                    _result.add(readMeterReadingRow(_stmt))
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getAllReadingsList(): List<MeterReading> {
        val _sql: String = "SELECT * FROM meter_readings ORDER BY timestamp ASC"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                val _result: MutableList<MeterReading> = mutableListOf()
                while (_stmt.step()) {
                    _result.add(readMeterReadingRow(_stmt))
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getTotalReadingsCount(): Int {
        val _sql: String = "SELECT COUNT(*) FROM meter_readings"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                if (_stmt.step()) {
                    _stmt.getLong(0).toInt()
                } else {
                    0
                }
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun deleteAllReadingsForCustomer(customerId: Long) {
        val _sql: String = "DELETE FROM meter_readings WHERE customerId = ?"
        return performSuspending(__db, false, true) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, customerId)
                _stmt.step()
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun deleteAllReadings() {
        val _sql: String = "DELETE FROM meter_readings"
        return performSuspending(__db, false, true) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.step()
            } finally {
                _stmt.close()
            }
        }
    }

    public companion object {
        public fun getRequiredConverters(): List<KClass<*>> = emptyList()
    }
}
