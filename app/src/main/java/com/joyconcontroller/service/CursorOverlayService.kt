package com.joyconcontroller.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.joyconcontroller.R
import com.joyconcontroller.ui.CursorView
import com.joyconcontroller.ui.MainActivity

/**
 * Foreground service that hosts the [CursorView] in a system overlay window.
 *
 * The overlay window is:
 *  - TYPE_ACCESSIBILITY_OVERLAY (works without SYSTEM_ALERT_WINDOW on Android 10+
 *    when running within an AccessibilityService context)
 *  - Full-screen, touch pass-through (FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE)
 *  - Transparent background
 *
 * Position updates arrive via [updatePosition] companion method.
 */
class CursorOverlayService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "joycon_cursor_overlay"
        private const val NOTIFICATION_ID = 1001

        // Singleton reference for efficient position updates from GestureDispatcher
        private var cursorView: CursorView? = null

        /** Called by GestureDispatcher on every cursor movement (main thread). */
        fun updatePosition(x: Float, y: Float) {
            cursorView?.updatePosition(x, y)
        }

        /** Called when a tap is dispatched to show tap animation. */
        fun onTap() {
            cursorView?.animateTap()
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: CursorView? = null

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        addOverlayWindow()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlayWindow()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    // ─── Overlay window ───────────────────────────────────────────────────────

    private fun addOverlayWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager!!.defaultDisplay.getRealMetrics(metrics)

        val params = WindowManager.LayoutParams(
            metrics.widthPixels,
            metrics.heightPixels,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val view = CursorView(this).also {
            // Start cursor at screen centre
            it.updatePosition(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
        }

        windowManager!!.addView(view, params)
        overlayView = view
        cursorView = view
    }

    private fun removeOverlayWindow() {
        overlayView?.let { view ->
            windowManager?.removeView(view)
        }
        overlayView = null
        cursorView = null
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Joy-Con Cursor Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows cursor overlay while Joy-Con is active"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Joy-Con Controller Active")
            .setContentText("Cursor overlay is running")
            .setSmallIcon(R.drawable.ic_joystick)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
