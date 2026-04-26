package com.joyconcontroller.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.PointF
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.joyconcontroller.input.GestureDispatcher
import com.joyconcontroller.input.JoyConInputHandler
import com.joyconcontroller.input.JoyConState
import com.joyconcontroller.utils.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Core AccessibilityService that:
 *  1. Receives Joy-Con Bluetooth HID key/motion events (via onKeyEvent / onMotionEvent)
 *  2. Feeds them to [JoyConInputHandler]
 *  3. Runs [GestureDispatcher] to convert state → gestures
 *  4. Starts / stops [CursorOverlayService] for the visual cursor
 *
 * Must be enabled by the user in Android Accessibility settings.
 */
class JoyConAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JoyConA11yService"

        // Broadcast action sent to MainActivity for status updates
        const val ACTION_STATUS = "com.joyconcontroller.STATUS"
        const val EXTRA_CONNECTED = "connected"

        // Static reference so other components can check running state
        var instance: JoyConAccessibilityService? = null
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var prefs: AppPreferences
    private lateinit var inputHandler: JoyConInputHandler
    private lateinit var gestureDispatcher: GestureDispatcher

    private val cursorPosition = PointF()
    private var screenWidth = 1080
    private var screenHeight = 1920

    // Track last state for the dispatcher
    private var latestState = JoyConState()

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service connected")

        prefs = AppPreferences(applicationContext)

        // Get real screen dimensions
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        // Centre cursor on start
        cursorPosition.set(screenWidth / 2f, screenHeight / 2f)

        // Configure what events we receive
        serviceInfo = serviceInfo.apply {
            flags = flags or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }

        inputHandler = JoyConInputHandler(prefs)

        gestureDispatcher = GestureDispatcher(
            service = this,
            prefs = prefs,
            cursorPosition = cursorPosition,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            onCursorMoved = { pos ->
                CursorOverlayService.updatePosition(pos.x, pos.y)
            }
        )

        // Observe state changes → button handling
        serviceScope.launch {
            inputHandler.state.collectLatest { state ->
                latestState = state
                gestureDispatcher.onStateChanged(state)
            }
        }

        // Start cursor movement tick loop
        gestureDispatcher.startTick { latestState }

        // Start overlay cursor service
        startCursorOverlay()

        broadcastStatus(connected = true)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        gestureDispatcher.stopTick()
        serviceScope.cancel()
        stopCursorOverlay()
        instance = null
        broadcastStatus(connected = false)
    }

    // ─── Accessibility events (required override, we don't need them) ─────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used — we operate on gesture injection and HID events only
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    // ─── HID Input events ─────────────────────────────────────────────────────

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return inputHandler.handleKeyEvent(event)
    }

    // Note: onGenericMotionEvent is called for joystick events routed through
    // the accessibility service on Android 9+. For older paths the overlay
    // window in CursorOverlayService also captures them.
    fun dispatchJoyConMotion(event: MotionEvent): Boolean {
        return inputHandler.handleMotionEvent(event)
    }

    // ─── Overlay service management ───────────────────────────────────────────

    private fun startCursorOverlay() {
        val intent = Intent(this, CursorOverlayService::class.java)
        startForegroundService(intent)
    }

    private fun stopCursorOverlay() {
        stopService(Intent(this, CursorOverlayService::class.java))
    }

    private fun broadcastStatus(connected: Boolean) {
        sendBroadcast(Intent(ACTION_STATUS).putExtra(EXTRA_CONNECTED, connected))
    }
}
