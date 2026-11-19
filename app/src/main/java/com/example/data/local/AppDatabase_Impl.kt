package com.example.data.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlin.reflect.KClass

@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
    private val _customerDao: Lazy<CustomerDao> = lazy {
        CustomerDao_Impl(this)
    }

    private val _meterReadingDao: Lazy<MeterReadingDao> = lazy {
        MeterReadingDao_Impl(this)
    }

    protected override fun createOpenDelegate(): RoomOpenDelegate {
        val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(
            1,
            "4d5b0c50162517ce7917023f206f0fa0",
            "2da70a8034b1357e60f3006d3d8d4a01"
        ) {
            public override fun createAllTables(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL, `meterNumber` TEXT NOT NULL, `consumerNumber` TEXT NOT NULL, `phoneNumber` TEXT NOT NULL, `connectionType` TEXT NOT NULL, `sanctionedLoad` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                connection.execSQL("CREATE TABLE IF NOT EXISTS `meter_readings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `readingDate` TEXT NOT NULL, `readingTime` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `kwh` REAL NOT NULL, `kvarh` REAL NOT NULL, `kvah` REAL NOT NULL, `powerFactor` REAL NOT NULL, `isPfAutoCalculated` INTEGER NOT NULL, `previousKwh` REAL, `unitsConsumed` REAL, `remarks` TEXT NOT NULL, FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_meter_readings_customerId` ON `meter_readings` (`customerId`)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_meter_readings_timestamp` ON `meter_readings` (`timestamp`)")
                connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4d5b0c50162517ce7917023f206f0fa0')")
            }

            public override fun dropAllTables(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE IF EXISTS `customers`")
                connection.execSQL("DROP TABLE IF EXISTS `meter_readings`")
            }

            public override fun onCreate(connection: SQLiteConnection) {
            }

            public override fun onOpen(connection: SQLiteConnection) {
                connection.execSQL("PRAGMA foreign_keys = ON")
                internalInitInvalidationTracker(connection)
            }

            public override fun onPreMigrate(connection: SQLiteConnection) {
                dropFtsSyncTriggers(connection)
            }

            public override fun onPostMigrate(connection: SQLiteConnection) {
            }

            public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
                val _columnsCustomers: MutableMap<String, TableInfo.Column> = mutableMapOf()
                _columnsCustomers.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsCustomers.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsCustomers.put("address", TableInfo.Column("address", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsCustomers.put("meterNumber", TableInfo.Column("meterNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsCustomers.put("consumerNumber", TableInfo.Column("consumerNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsCustomers.put("phoneNumber", TableInfo.Column("phoneNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsCustomers.put("connectionType", TableInfo.Column("connectionType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsCustomers.put("sanctionedLoad", TableInfo.Column("sanctionedLoad", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsCustomers.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                val _foreignKeysCustomers: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
                val _indicesCustomers: MutableSet<TableInfo.Index> = mutableSetOf()
                val _infoCustomers: TableInfo = TableInfo("customers", _columnsCustomers, _foreignKeysCustomers, _indicesCustomers)
                val _existingCustomers: TableInfo = read(connection, "customers")
                if (!_infoCustomers.equals(_existingCustomers)) {
                    return RoomOpenDelegate.ValidationResult(
                        false,
                        """
                        |customers(com.example.data.local.Customer).
                        | Expected:
                        |""".trimMargin() + _infoCustomers + """
                        |
                        | Found:
                        |""".trimMargin() + _existingCustomers
                    )
                }
                val _columnsMeterReadings: MutableMap<String, TableInfo.Column> = mutableMapOf()
                _columnsMeterReadings.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("customerId", TableInfo.Column("customerId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("readingDate", TableInfo.Column("readingDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("readingTime", TableInfo.Column("readingTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("kwh", TableInfo.Column("kwh", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("kvarh", TableInfo.Column("kvarh", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("kvah", TableInfo.Column("kvah", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("powerFactor", TableInfo.Column("powerFactor", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("isPfAutoCalculated", TableInfo.Column("isPfAutoCalculated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("previousKwh", TableInfo.Column("previousKwh", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("unitsConsumed", TableInfo.Column("unitsConsumed", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
                _columnsMeterReadings.put("remarks", TableInfo.Column("remarks", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
                val _foreignKeysMeterReadings: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
                _foreignKeysMeterReadings.add(TableInfo.ForeignKey("customers", "CASCADE", "NO ACTION", listOf("customerId"), listOf("id")))
                val _indicesMeterReadings: MutableSet<TableInfo.Index> = mutableSetOf()
                _indicesMeterReadings.add(TableInfo.Index("index_meter_readings_customerId", false, listOf("customerId"), listOf("ASC")))
                _indicesMeterReadings.add(TableInfo.Index("index_meter_readings_timestamp", false, listOf("timestamp"), listOf("ASC")))
                val _infoMeterReadings: TableInfo = TableInfo("meter_readings", _columnsMeterReadings, _foreignKeysMeterReadings, _indicesMeterReadings)
                val _existingMeterReadings: TableInfo = read(connection, "meter_readings")
                if (!_infoMeterReadings.equals(_existingMeterReadings)) {
                    return RoomOpenDelegate.ValidationResult(
                        false,
                        """
                        |meter_readings(com.example.data.local.MeterReading).
                        | Expected:
                        |""".trimMargin() + _infoMeterReadings + """
                        |
                        | Found:
                        |""".trimMargin() + _existingMeterReadings
                    )
                }
                return RoomOpenDelegate.ValidationResult(true, null)
            }
        }
        return _openDelegate
    }

    protected override fun createInvalidationTracker(): InvalidationTracker {
        val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
        val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
        return InvalidationTracker(this, _shadowTablesMap, _viewTables, "customers", "meter_readings")
    }

    public override fun clearAllTables() {
        super.performClear(true, "customers", "meter_readings")
    }

    protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
        val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
        _typeConvertersMap.put(CustomerDao::class, CustomerDao_Impl.getRequiredConverters())
        _typeConvertersMap.put(MeterReadingDao::class, MeterReadingDao_Impl.getRequiredConverters())
        return _typeConvertersMap
    }

    public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
        val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
        return _autoMigrationSpecsSet
    }

    public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
        val _autoMigrations: MutableList<Migration> = mutableListOf()
        return _autoMigrations
    }

    public override fun customerDao(): CustomerDao = _customerDao.value

    public override fun meterReadingDao(): MeterReadingDao = _meterReadingDao.value
}
