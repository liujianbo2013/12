package com.goodwork.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.goodwork.app.service.UnlockMonitorService

class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("ScreenStateReceiver", "Screen OFF")
                updateLockState(context, true)
            }
            Intent.ACTION_SCREEN_ON -> {
                Log.d("ScreenStateReceiver", "Screen ON")
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d("ScreenStateReceiver", "User Present (Unlocked)")
                updateLockState(context, false)
            }
        }
    }

    private fun updateLockState(context: Context, isLocked: Boolean) {
        val serviceIntent = Intent(context, UnlockMonitorService::class.java).apply {
            action = if (isLocked) {
                UnlockMonitorService.ACTION_SCREEN_LOCKED
            } else {
                UnlockMonitorService.ACTION_SCREEN_UNLOCKED
            }
        }
        context.startService(serviceIntent)
    }
}