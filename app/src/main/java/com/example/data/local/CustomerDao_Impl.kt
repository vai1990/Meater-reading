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
public class CustomerDao_Impl(
    __db: RoomDatabase,
) : CustomerDao {
    private val __db: RoomDatabase

    private val __insertAdapterOfCustomer: EntityInsertAdapter<Customer>

    private val __deleteAdapterOfCustomer: EntityDeleteOrUpdateAdapter<Customer>

    private val __updateAdapterOfCustomer: EntityDeleteOrUpdateAdapter<Customer>

    init {
        this.__db = __db
        this.__insertAdapterOfCustomer = object : EntityInsertAdapter<Customer>() {
            protected override fun createQuery(): String =
                "INSERT OR REPLACE INTO `customers` (`id`,`name`,`address`,`meterNumber`,`consumerNumber`,`phoneNumber`,`connectionType`,`sanctionedLoad`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

            protected override fun bind(statement: SQLiteStatement, entity: Customer) {
                statement.bindLong(1, entity.id)
                statement.bindText(2, entity.name)
                statement.bindText(3, entity.address)
                statement.bindText(4, entity.meterNumber)
                statement.bindText(5, entity.consumerNumber)
                statement.bindText(6, entity.phoneNumber)
                statement.bindText(7, entity.connectionType)
                statement.bindText(8, entity.sanctionedLoad)
                statement.bindLong(9, entity.createdAt)
            }
        }
        this.__deleteAdapterOfCustomer = object : EntityDeleteOrUpdateAdapter<Customer>() {
            protected override fun createQuery(): String = "DELETE FROM `customers` WHERE `id` = ?"

            protected override fun bind(statement: SQLiteStatement, entity: Customer) {
                statement.bindLong(1, entity.id)
            }
        }
        this.__updateAdapterOfCustomer = object : EntityDeleteOrUpdateAdapter<Customer>() {
            protected override fun createQuery(): String =
                "UPDATE OR ABORT `customers` SET `id` = ?,`name` = ?,`address` = ?,`meterNumber` = ?,`consumerNumber` = ?,`phoneNumber` = ?,`connectionType` = ?,`sanctionedLoad` = ?,`createdAt` = ? WHERE `id` = ?"

            protected override fun bind(statement: SQLiteStatement, entity: Customer) {
                statement.bindLong(1, entity.id)
                statement.bindText(2, entity.name)
                statement.bindText(3, entity.address)
                statement.bindText(4, entity.meterNumber)
                statement.bindText(5, entity.consumerNumber)
                statement.bindText(6, entity.phoneNumber)
                statement.bindText(7, entity.connectionType)
                statement.bindText(8, entity.sanctionedLoad)
                statement.bindLong(9, entity.createdAt)
                statement.bindLong(10, entity.id)
            }
        }
    }

    public override suspend fun insertCustomer(customer: Customer): Long = performSuspending(__db, false, true) { _connection ->
        __insertAdapterOfCustomer.insertAndReturnId(_connection, customer)
    }

    public override suspend fun deleteCustomer(customer: Customer): Unit = performSuspending(__db, false, true) { _connection ->
        __deleteAdapterOfCustomer.handle(_connection, customer)
    }

    public override suspend fun updateCustomer(customer: Customer): Unit = performSuspending(__db, false, true) { _connection ->
        __updateAdapterOfCustomer.handle(_connection, customer)
    }

    public override fun getAllCustomers(): Flow<List<Customer>> {
        val _sql: String = "SELECT * FROM customers ORDER BY createdAt DESC"
        return createFlow(__db, false, arrayOf("customers")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
                val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
                val _columnIndexOfMeterNumber: Int = getColumnIndexOrThrow(_stmt, "meterNumber")
                val _columnIndexOfConsumerNumber: Int = getColumnIndexOrThrow(_stmt, "consumerNumber")
                val _columnIndexOfPhoneNumber: Int = getColumnIndexOrThrow(_stmt, "phoneNumber")
                val _columnIndexOfConnectionType: Int = getColumnIndexOrThrow(_stmt, "connectionType")
                val _columnIndexOfSanctionedLoad: Int = getColumnIndexOrThrow(_stmt, "sanctionedLoad")
                val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
                val _result: MutableList<Customer> = mutableListOf()
                while (_stmt.step()) {
                    val _tmpId: Long = _stmt.getLong(_columnIndexOfId)
                    val _tmpName: String = _stmt.getText(_columnIndexOfName)
                    val _tmpAddress: String = _stmt.getText(_columnIndexOfAddress)
                    val _tmpMeterNumber: String = _stmt.getText(_columnIndexOfMeterNumber)
                    val _tmpConsumerNumber: String = _stmt.getText(_columnIndexOfConsumerNumber)
                    val _tmpPhoneNumber: String = _stmt.getText(_columnIndexOfPhoneNumber)
                    val _tmpConnectionType: String = _stmt.getText(_columnIndexOfConnectionType)
                    val _tmpSanctionedLoad: String = _stmt.getText(_columnIndexOfSanctionedLoad)
                    val _tmpCreatedAt: Long = _stmt.getLong(_columnIndexOfCreatedAt)
                    _result.add(
                        Customer(
                            _tmpId,
                            _tmpName,
                            _tmpAddress,
                            _tmpMeterNumber,
                            _tmpConsumerNumber,
                            _tmpPhoneNumber,
                            _tmpConnectionType,
                            _tmpSanctionedLoad,
                            _tmpCreatedAt
                        )
                    )
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override fun getCustomerById(id: Long): Flow<Customer?> {
        val _sql: String = "SELECT * FROM customers WHERE id = ? LIMIT 1"
        return createFlow(__db, false, arrayOf("customers")) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, id)
                val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
                val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
                val _columnIndexOfMeterNumber: Int = getColumnIndexOrThrow(_stmt, "meterNumber")
                val _columnIndexOfConsumerNumber: Int = getColumnIndexOrThrow(_stmt, "consumerNumber")
                val _columnIndexOfPhoneNumber: Int = getColumnIndexOrThrow(_stmt, "phoneNumber")
                val _columnIndexOfConnectionType: Int = getColumnIndexOrThrow(_stmt, "connectionType")
                val _columnIndexOfSanctionedLoad: Int = getColumnIndexOrThrow(_stmt, "sanctionedLoad")
                val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
                if (_stmt.step()) {
                    Customer(
                        _stmt.getLong(_columnIndexOfId),
                        _stmt.getText(_columnIndexOfName),
                        _stmt.getText(_columnIndexOfAddress),
                        _stmt.getText(_columnIndexOfMeterNumber),
                        _stmt.getText(_columnIndexOfConsumerNumber),
                        _stmt.getText(_columnIndexOfPhoneNumber),
                        _stmt.getText(_columnIndexOfConnectionType),
                        _stmt.getText(_columnIndexOfSanctionedLoad),
                        _stmt.getLong(_columnIndexOfCreatedAt)
                    )
                } else {
                    null
                }
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getCustomerByIdDirect(id: Long): Customer? {
        val _sql: String = "SELECT * FROM customers WHERE id = ? LIMIT 1"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                _stmt.bindLong(1, id)
                val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
                val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
                val _columnIndexOfMeterNumber: Int = getColumnIndexOrThrow(_stmt, "meterNumber")
                val _columnIndexOfConsumerNumber: Int = getColumnIndexOrThrow(_stmt, "consumerNumber")
                val _columnIndexOfPhoneNumber: Int = getColumnIndexOrThrow(_stmt, "phoneNumber")
                val _columnIndexOfConnectionType: Int = getColumnIndexOrThrow(_stmt, "connectionType")
                val _columnIndexOfSanctionedLoad: Int = getColumnIndexOrThrow(_stmt, "sanctionedLoad")
                val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
                if (_stmt.step()) {
                    Customer(
                        _stmt.getLong(_columnIndexOfId),
                        _stmt.getText(_columnIndexOfName),
                        _stmt.getText(_columnIndexOfAddress),
                        _stmt.getText(_columnIndexOfMeterNumber),
                        _stmt.getText(_columnIndexOfConsumerNumber),
                        _stmt.getText(_columnIndexOfPhoneNumber),
                        _stmt.getText(_columnIndexOfConnectionType),
                        _stmt.getText(_columnIndexOfSanctionedLoad),
                        _stmt.getLong(_columnIndexOfCreatedAt)
                    )
                } else {
                    null
                }
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getAllCustomersList(): List<Customer> {
        val _sql: String = "SELECT * FROM customers ORDER BY id ASC"
        return performSuspending(__db, true, false) { _connection ->
            val _stmt: SQLiteStatement = _connection.prepare(_sql)
            try {
                val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
                val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
                val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
                val _columnIndexOfMeterNumber: Int = getColumnIndexOrThrow(_stmt, "meterNumber")
                val _columnIndexOfConsumerNumber: Int = getColumnIndexOrThrow(_stmt, "consumerNumber")
                val _columnIndexOfPhoneNumber: Int = getColumnIndexOrThrow(_stmt, "phoneNumber")
                val _columnIndexOfConnectionType: Int = getColumnIndexOrThrow(_stmt, "connectionType")
                val _columnIndexOfSanctionedLoad: Int = getColumnIndexOrThrow(_stmt, "sanctionedLoad")
                val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
                val _result: MutableList<Customer> = mutableListOf()
                while (_stmt.step()) {
                    _result.add(
                        Customer(
                            _stmt.getLong(_columnIndexOfId),
                            _stmt.getText(_columnIndexOfName),
                            _stmt.getText(_columnIndexOfAddress),
                            _stmt.getText(_columnIndexOfMeterNumber),
                            _stmt.getText(_columnIndexOfConsumerNumber),
                            _stmt.getText(_columnIndexOfPhoneNumber),
                            _stmt.getText(_columnIndexOfConnectionType),
                            _stmt.getText(_columnIndexOfSanctionedLoad),
                            _stmt.getLong(_columnIndexOfCreatedAt)
                        )
                    )
                }
                _result
            } finally {
                _stmt.close()
            }
        }
    }

    public override suspend fun getCustomerCount(): Int {
        val _sql: String = "SELECT COUNT(*) FROM customers"
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

    public override suspend fun deleteAllCustomers() {
        val _sql: String = "DELETE FROM customers"
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
