package com.goodwork.app.manager

import android.content.Context
import com.goodwork.app.database.Contact
import com.goodwork.app.database.DatabaseHelper

class ContactManager(private val context: Context) {

    private val databaseHelper = DatabaseHelper(context)

    fun addContact(name: String, phone: String, isDefault: Boolean = false): Long {
        return databaseHelper.addContact(name, phone, isDefault)
    }

    fun getAllContacts(): List<Contact> {
        return databaseHelper.getAllContacts()
    }

    fun updateContact(id: Int, name: String, phone: String, isDefault: Boolean = false): Int {
        return databaseHelper.updateContact(id, name, phone, isDefault)
    }

    fun deleteContact(id: Int): Int {
        return databaseHelper.deleteContact(id)
    }

    fun getContactById(id: Int): Contact? {
        return databaseHelper.getContactById(id)
    }

    fun hasContacts(): Boolean {
        return getAllContacts().isNotEmpty()
    }
}