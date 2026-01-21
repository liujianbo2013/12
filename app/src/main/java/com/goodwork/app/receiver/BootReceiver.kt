package com.goodwork.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.goodwork.app.service.UnlockMonitorService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Boot completed, starting service")
            
            // 检查服务是否启用
            val prefs = context.getSharedPreferences("GoodWorkPrefs", Context.MODE_PRIVATE)
            val serviceEnabled = prefs.getBoolean("service_enabled", false)
            
            if (serviceEnabled) {
                val serviceIntent = Intent(context, UnlockMonitorService::class.java)
                context.startForegroundService(serviceIntent)
                Log.d("BootReceiver", "Service started")
            } else {
                Log.d("BootReceiver", "Service not enabled, skipping start")
            }
        }
    }
}