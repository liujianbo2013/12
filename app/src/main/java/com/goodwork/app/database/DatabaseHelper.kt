package com.goodwork.app.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "goodwork.db"
        private const val DATABASE_VERSION = 1

        // Contacts表
        private const val TABLE_CONTACTS = "contacts"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_PHONE = "phone"
        private const val COLUMN_IS_DEFAULT = "is_default"

        // Settings表
        private const val TABLE_SETTINGS = "settings"
        private const val COLUMN_KEY = "key"
        private const val COLUMN_VALUE = "value"

        // Settings keys
        const val KEY_SMS_TEMPLATE = "sms_template"
        const val KEY_SERVICE_ENABLED = "service_enabled"
        const val KEY_LAST_UNLOCK_TIME = "last_unlock_time"
        const val KEY_LAST_SMS_SENT_TIME = "last_sms_sent_time"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建Contacts表
        val createContactsTable = """
            CREATE TABLE $TABLE_CONTACTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_PHONE TEXT NOT NULL,
                $COLUMN_IS_DEFAULT INTEGER DEFAULT 0
            )
        """.trimIndent()

        // 创建Settings表
        val createSettingsTable = """
            CREATE TABLE $TABLE_SETTINGS (
                $COLUMN_KEY TEXT PRIMARY KEY,
                $COLUMN_VALUE TEXT
            )
        """.trimIndent()

        db.execSQL(createContactsTable)
        db.execSQL(createSettingsTable)

        // 插入默认设置
        insertDefaultSettings(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONTACTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        onCreate(db)
    }

    private fun insertDefaultSettings(db: SQLiteDatabase) {
        val defaultTemplate = "[好活] 用户已5小时未操作手机，请确认安全"
        
        db.insert(TABLE_SETTINGS, null, ContentValues().apply {
            put(COLUMN_KEY, KEY_SMS_TEMPLATE)
            put(COLUMN_VALUE, defaultTemplate)
        })
        
        db.insert(TABLE_SETTINGS, null, ContentValues().apply {
            put(COLUMN_KEY, KEY_SERVICE_ENABLED)
            put(COLUMN_VALUE, "false")
        })
        
        db.insert(TABLE_SETTINGS, null, ContentValues().apply {
            put(COLUMN_KEY, KEY_LAST_UNLOCK_TIME)
            put(COLUMN_VALUE, "0")
        })
        
        db.insert(TABLE_SETTINGS, null, ContentValues().apply {
            put(COLUMN_KEY, KEY_LAST_SMS_SENT_TIME)
            put(COLUMN_VALUE, "0")
        })
    }

    // Contacts CRUD操作
    fun addContact(name: String, phone: String, isDefault: Boolean = false): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_PHONE, phone)
            put(COLUMN_IS_DEFAULT, if (isDefault) 1 else 0)
        }
        return db.insert(TABLE_CONTACTS, null, values)
    }

    fun getAllContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_CONTACTS,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_PHONE, COLUMN_IS_DEFAULT),
            null, null, null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                contacts.add(
                    Contact(
                        it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                        it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        it.getString(it.getColumnIndexOrThrow(COLUMN_PHONE)),
                        it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_DEFAULT)) == 1
                    )
                )
            }
        }
        return contacts
    }

    fun updateContact(id: Int, name: String, phone: String, isDefault: Boolean = false): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_PHONE, phone)
            put(COLUMN_IS_DEFAULT, if (isDefault) 1 else 0)
        }
        return db.update(
            TABLE_CONTACTS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun deleteContact(id: Int): Int {
        val db = writableDatabase
        return db.delete(
            TABLE_CONTACTS,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun getContactById(id: Int): Contact? {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_CONTACTS,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_PHONE, COLUMN_IS_DEFAULT),
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return Contact(
                    it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_PHONE)),
                    it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_DEFAULT)) == 1
                )
            }
        }
        return null
    }

    // Settings操作
    fun getSetting(key: String): String? {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_SETTINGS,
            arrayOf(COLUMN_VALUE),
            "$COLUMN_KEY = ?",
            arrayOf(key),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(COLUMN_VALUE))
            }
        }
        return null
    }

    fun setSetting(key: String, value: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_VALUE, value)
        }
        
        val rowsUpdated = db.update(
            TABLE_SETTINGS,
            values,
            "$COLUMN_KEY = ?",
            arrayOf(key)
        )
        
        if (rowsUpdated == 0) {
            values.put(COLUMN_KEY, key)
            db.insert(TABLE_SETTINGS, null, values)
        }
    }

    fun getLongSetting(key: String, defaultValue: Long = 0L): Long {
        return getSetting(key)?.toLongOrNull() ?: defaultValue
    }

    fun setLongSetting(key: String, value: Long) {
        setSetting(key, value.toString())
    }

    fun getBooleanSetting(key: String, defaultValue: Boolean = false): Boolean {
        return getSetting(key)?.toBooleanOrNull() ?: defaultValue
    }

    fun setBooleanSetting(key: String, value: Boolean) {
        setSetting(key, value.toString())
    }
}

data class Contact(
    val id: Int,
    val name: String,
    val phone: String,
    val isDefault: Boolean
)