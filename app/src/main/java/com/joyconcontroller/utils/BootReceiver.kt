package com.joyconcontroller.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives BOOT_COMPLETED to restore overlay service after device restart.
 * The AccessibilityService itself is managed by Android settings.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed received")
            // Overlay service will be started when user opens app or
            // when AccessibilityService connects to a Joy-Con.
        }
    }
}
