package com.goodwork.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.goodwork.app.database.Contact
import com.goodwork.app.database.DatabaseHelper

class SmsSender(
    private val context: Context,
    private val databaseHelper: DatabaseHelper
) {

    fun sendAlertToAllContacts(
        onSuccess: (Int) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!hasSmsPermission()) {
            onFailure(SecurityException("SMS permission not granted"))
            return
        }

        val contacts = databaseHelper.getAllContacts()
        if (contacts.isEmpty()) {
            onFailure(IllegalStateException("No contacts configured"))
            return
        }

        val template = databaseHelper.getSetting(DatabaseHelper.KEY_SMS_TEMPLATE)
            ?: "[好活] 用户已5小时未操作手机，请确认安全"

        var successCount = 0
        var failureCount = 0

        contacts.forEach { contact ->
            try {
                sendSms(contact.phone, template)
                successCount++
                Log.d("SmsSender", "SMS sent to ${contact.name} (${contact.phone})")
            } catch (e: Exception) {
                failureCount++
                Log.e("SmsSender", "Failed to send SMS to ${contact.name}: ${e.message}")
            }
        }

        if (successCount > 0) {
            onSuccess(successCount)
        } else {
            onFailure(Exception("Failed to send SMS to all contacts"))
        }
    }

    fun sendSms(phoneNumber: String, message: String) {
        if (!hasSmsPermission()) {
            throw SecurityException("SMS permission not granted")
        }

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                null,
                null
            )
        } catch (e: Exception) {
            Log.e("SmsSender", "Error sending SMS: ${e.message}")
            throw e
        }
    }

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}