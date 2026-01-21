package com.goodwork.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.goodwork.app.R
import com.goodwork.app.database.DatabaseHelper
import java.util.*

class UnlockMonitorService : Service() {

    companion object {
        const val ACTION_SCREEN_LOCKED = "com.goodwork.app.ACTION_SCREEN_LOCKED"
        const val ACTION_SCREEN_UNLOCKED = "com.goodwork.app.ACTION_SCREEN_UNLOCKED"
        const val ACTION_CHECK_UNLOCK_STATUS = "com.goodwork.app.ACTION_CHECK_UNLOCK_STATUS"
        
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "goodwork_channel"
        
        private const val ACTIVE_START_HOUR = 8
        private const val ACTIVE_END_HOUR = 20
        private const val UNLOCK_THRESHOLD_HOURS = 5
    }

    private lateinit var databaseHelper: DatabaseHelper
    private val handler = Handler(Looper.getMainLooper())
    private var checkRunnable: Runnable? = null
    private var smsSender: SmsSender? = null

    override fun onCreate() {
        super.onCreate()
        databaseHelper = DatabaseHelper(this)
        smsSender = SmsSender(this, databaseHelper)
        createNotificationChannel()
        Log.d("UnlockMonitorService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SCREEN_LOCKED -> handleScreenLocked()
            ACTION_SCREEN_UNLOCKED -> handleScreenUnlocked()
            ACTION_CHECK_UNLOCK_STATUS -> checkUnlockStatus()
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startPeriodicCheck()
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopPeriodicCheck()
        Log.d("UnlockMonitorService", "Service destroyed")
    }

    private fun handleScreenLocked() {
        val currentTime = System.currentTimeMillis()
        databaseHelper.setLongSetting(DatabaseHelper.KEY_LAST_UNLOCK_TIME, currentTime)
        Log.d("UnlockMonitorService", "Screen locked at ${Date(currentTime)}")
    }

    private fun handleScreenUnlocked() {
        val currentTime = System.currentTimeMillis()
        databaseHelper.setLongSetting(DatabaseHelper.KEY_LAST_UNLOCK_TIME, currentTime)
        Log.d("UnlockMonitorService", "Screen unlocked at ${Date(currentTime)}")
    }

    private fun checkUnlockStatus() {
        if (!isServiceEnabled()) {
            Log.d("UnlockMonitorService", "Service is disabled, skipping check")
            return
        }

        if (!isInActivePeriod()) {
            Log.d("UnlockMonitorService", "Not in active period (08:00-20:00), skipping check")
            return
        }

        val lastUnlockTime = databaseHelper.getLongSetting(DatabaseHelper.KEY_LAST_UNLOCK_TIME)
        val currentTime = System.currentTimeMillis()
        val hoursSinceLastUnlock = (currentTime - lastUnlockTime) / (1000 * 60 * 60)

        Log.d("UnlockMonitorService", "Hours since last unlock: $hoursSinceLastUnlock")

        if (hoursSinceLastUnlock >= UNLOCK_THRESHOLD_HOURS) {
            val lastSmsSentTime = databaseHelper.getLongSetting(DatabaseHelper.KEY_LAST_SMS_SENT_TIME)
            val hoursSinceLastSms = (currentTime - lastSmsSentTime) / (1000 * 60 * 60)

            // 避免重复发送短信，至少间隔30分钟
            if (hoursSinceLastSms >= 0.5) {
                Log.d("UnlockMonitorService", "Triggering SMS alert")
                sendAlertSms()
            } else {
                Log.d("UnlockMonitorService", "SMS already sent recently, skipping")
            }
        }
    }

    private fun sendAlertSms() {
        smsSender?.sendAlertToAllContacts(
            onSuccess = { sentCount ->
                Log.d("UnlockMonitorService", "Alert SMS sent to $sentCount contacts")
                databaseHelper.setLongSetting(DatabaseHelper.KEY_LAST_SMS_SENT_TIME, System.currentTimeMillis())
            },
            onFailure = { error ->
                Log.e("UnlockMonitorService", "Failed to send alert SMS: ${error.message}")
            }
        )
    }

    private fun isInActivePeriod(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour in ACTIVE_START_HOUR until ACTIVE_END_HOUR
    }

    private fun isServiceEnabled(): Boolean {
        return databaseHelper.getBooleanSetting(DatabaseHelper.KEY_SERVICE_ENABLED)
    }

    private fun startPeriodicCheck() {
        stopPeriodicCheck()
        
        checkRunnable = object : Runnable {
            override fun run() {
                checkUnlockStatus()
                handler.postDelayed(this, 60 * 60 * 1000L) // 每小时检查一次
            }
        }
        
        handler.postDelayed(checkRunnable!!, 60 * 60 * 1000L) // 首次检查延迟1小时
        Log.d("UnlockMonitorService", "Periodic check started")
    }

    private fun stopPeriodicCheck() {
        checkRunnable?.let {
            handler.removeCallbacks(it)
            checkRunnable = null
        }
        Log.d("UnlockMonitorService", "Periodic check stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "好活服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "好活安全守护服务"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("好活 - 安全守护")
            .setContentText("正在监控设备解锁状态")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}