package com.goodwork.app.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class Logger(private val context: Context) {

    companion object {
        private const val LOG_FILE_NAME = "goodwork.log"
        private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }

    private val logFile: File
        get() = File(context.filesDir, LOG_FILE_NAME)

    fun logEvent(event: String, details: String = "") {
        val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
        val logMessage = "[$timestamp] $event: $details"

        // 打印到Logcat
        Log.d("GoodWork", logMessage)

        // 写入文件
        try {
            checkLogFileSize()
            FileWriter(logFile, true).use { writer ->
                writer.append(logMessage).append("\n")
            }
        } catch (e: Exception) {
            Log.e("Logger", "Failed to write log: ${e.message}")
        }
    }

    fun logUnlock() {
        logEvent("UNLOCK", "Device unlocked")
    }

    fun logLock() {
        logEvent("LOCK", "Device locked")
    }

    fun logSmsSent(phoneNumber: String) {
        logEvent("SMS_SENT", "Alert sent to $phoneNumber")
    }

    fun logSmsFailed(phoneNumber: String, error: String) {
        logEvent("SMS_FAILED", "Failed to send to $phoneNumber: $error")
    }

    fun logServiceStarted() {
        logEvent("SERVICE", "UnlockMonitorService started")
    }

    fun logServiceStopped() {
        logEvent("SERVICE", "UnlockMonitorService stopped")
    }

    fun logCheckTriggered(hoursSinceUnlock: Long) {
        logEvent("CHECK", "Unlock status check triggered. Hours since unlock: $hoursSinceUnlock")
    }

    fun logAlertTriggered() {
        logEvent("ALERT", "Alert condition met, preparing to send SMS")
    }

    fun getLogs(): List<String> {
        val logs = mutableListOf<String>()
        try {
            if (logFile.exists()) {
                logFile.forEachLine { line ->
                    logs.add(line)
                }
            }
        } catch (e: Exception) {
            Log.e("Logger", "Failed to read logs: ${e.message}")
        }
        return logs
    }

    fun clearLogs() {
        try {
            if (logFile.exists()) {
                logFile.delete()
            }
        } catch (e: Exception) {
            Log.e("Logger", "Failed to clear logs: ${e.message}")
        }
    }

    private fun checkLogFileSize() {
        if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
            try {
                // 保留最后1000行
                val lines = logFile.readLines().takeLast(1000)
                FileWriter(logFile).use { writer ->
                    lines.forEach { line ->
                        writer.append(line).append("\n")
                    }
                }
            } catch (e: Exception) {
                Log.e("Logger", "Failed to rotate logs: ${e.message}")
            }
        }
    }
}