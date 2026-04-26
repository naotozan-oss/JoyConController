package com.joyconcontroller.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.joyconcontroller.utils.AppPreferences
import com.joyconcontroller.utils.applyAcceleration
import com.joyconcontroller.utils.applyDeadZone
import com.joyconcontroller.utils.clamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Translates [JoyConState] snapshots into:
 *  - Cursor position updates (sent to CursorOverlayService)
 *  - AccessibilityService gestures (tap, swipe, long-press)
 *  - System actions (back, home, recents, notifications)
 *
 * Call [onStateChanged] every time a new [JoyConState] arrives.
 * Call [startTick] / [stopTick] to drive the cursor movement loop.
 */
class GestureDispatcher(
    private val service: AccessibilityService,
    private val prefs: AppPreferences,
    private val cursorPosition: PointF,          // shared mutable cursor position
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val onCursorMoved: (PointF) -> Unit  // notify overlay to redraw
) {
    private val tag = "GestureDispatcher"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)

    // State tracking for button edge detection
    private var prevState = JoyConState()
    private var tickJob: Job? = null

    // Button state for long-press tracking
    private var longPressJob: Job? = null
    private var rsTapStartTime = 0L

    // Boost toggle state (used when prefs.boostToggle == true)
    private var boostToggled = false

    // ─── Tick loop ────────────────────────────────────────────────────────────

    fun startTick(stateProvider: () -> JoyConState) {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                val s = stateProvider()
                moveCursor(s)
                delay(JoyConConstants.TICK_INTERVAL_MS)
            }
        }
    }

    fun stopTick() {
        tickJob?.cancel()
        tickJob = null
    }

    // ─── State change handler (called on every new JoyConState) ───────────────

    fun onStateChanged(new: JoyConState) {
        handleButtons(prev = prevState, curr = new)
        prevState = new
    }

    // ─── Cursor movement ──────────────────────────────────────────────────────

    private fun moveCursor(state: JoyConState) {
        val scrollMode = prefs.scrollMode
        val rHeld = state.btnR

        // Determine if right stick is used for scroll or cursor
        val useRightForScroll = scrollMode == "r_button" && rHeld

        // Cursor movement from right stick (unless being used for scroll)
        if (!useRightForScroll) {
            val rawX = state.rightStickX
            val rawY = state.rightStickY
            val dz = prefs.deadZone

            var nx = applyDeadZone(rawX, dz)
            var ny = applyDeadZone(rawY, dz)

            if (prefs.accelerationEnabled) {
                nx = applyAcceleration(nx)
                ny = applyAcceleration(ny)
            }

            val boost = if (prefs.boostToggle) boostToggled else rHeld
            val speedMult = if (boost) JoyConConstants.BOOST_MULTIPLIER else 1f
            val speed = prefs.cursorSpeed * speedMult

            cursorPosition.x = (cursorPosition.x + nx * speed)
                .clamp(0f, screenWidth.toFloat())
            cursorPosition.y = (cursorPosition.y + ny * speed)
                .clamp(0f, screenHeight.toFloat())

            if (nx != 0f || ny != 0f) {
                onCursorMoved(cursorPosition)
            }
        }

        // Scroll from left stick (A-mode)
        if (scrollMode == "left_stick") {
            val lx = applyDeadZone(state.leftStickX, prefs.deadZone)
            val ly = applyDeadZone(state.leftStickY, prefs.deadZone)
            if (lx != 0f || ly != 0f) {
                performScroll(ly, lx)
            }
        }

        // Scroll from right stick while R held (B-mode)
        if (useRightForScroll) {
            val rx = applyDeadZone(state.rightStickX, prefs.deadZone)
            val ry = applyDeadZone(state.rightStickY, prefs.deadZone)
            if (rx != 0f || ry != 0f) {
                performScroll(ry, rx)
            }
        }
    }

    // ─── Button handling ──────────────────────────────────────────────────────

    private fun handleButtons(prev: JoyConState, curr: JoyConState) {
        val cx = cursorPosition.x
        val cy = cursorPosition.y

        // A → tap
        if (!prev.btnA && curr.btnA) {
            performTap(cx, cy)
        }

        // RS (right stick click) → tap (with debounce for accidental presses)
        if (!prev.btnRS && curr.btnRS) {
            rsTapStartTime = SystemClock.uptimeMillis()
        }
        if (prev.btnRS && !curr.btnRS) {
            val held = SystemClock.uptimeMillis() - rsTapStartTime
            if (held < JoyConConstants.RS_CLICK_DEBOUNCE_MS) {
                performTap(cx, cy)
            }
        }

        // B → back
        if (!prev.btnB && curr.btnB) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }

        // HOME → home
        if (!prev.btnHome && curr.btnHome) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        }

        // + → recents
        if (!prev.btnPlus && curr.btnPlus) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        }

        // - → notification panel
        if (!prev.btnMinus && curr.btnMinus) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        }

        // X → long press (start on press, cancel on release)
        if (!prev.btnX && curr.btnX) {
            startLongPress(cx, cy)
        }
        if (prev.btnX && !curr.btnX) {
            cancelLongPress()
        }

        // ZR → double tap
        if (!prev.btnZR && curr.btnZR) {
            performDoubleTap(cx, cy)
        }

        // R boost toggle (only when prefs.boostToggle == true)
        if (prefs.boostToggle && !prev.btnR && curr.btnR) {
            boostToggled = !boostToggled
            Log.d(tag, "Boost toggled: $boostToggled")
        }
    }

    // ─── Gesture builders ─────────────────────────────────────────────────────

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 50L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        service.dispatchGesture(gesture, null, null)
        Log.d(tag, "Tap at ($x, $y)")
    }

    private fun performDoubleTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke1 = GestureDescription.StrokeDescription(path, 0L, 50L)
        val stroke2 = GestureDescription.StrokeDescription(path,
            50L + JoyConConstants.DOUBLE_TAP_INTERVAL_MS, 50L)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke1)
            .addStroke(stroke2)
            .build()
        service.dispatchGesture(gesture, null, null)
        Log.d(tag, "Double-tap at ($x, $y)")
    }

    private fun startLongPress(x: Float, y: Float) {
        longPressJob?.cancel()
        longPressJob = scope.launch {
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(
                path, 0L, JoyConConstants.LONG_PRESS_DURATION_MS
            )
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            service.dispatchGesture(gesture, null, null)
            Log.d(tag, "Long-press at ($x, $y)")
        }
    }

    private fun cancelLongPress() {
        longPressJob?.cancel()
        longPressJob = null
    }

    /**
     * Perform a short swipe to simulate scroll.
     * @param vertical  positive = scroll down, negative = scroll up
     * @param horizontal positive = scroll right, negative = scroll left
     */
    private fun performScroll(vertical: Float, horizontal: Float) {
        val cx = cursorPosition.x
        val cy = cursorPosition.y
        val scrollSpeed = prefs.scrollSpeed

        val deltaX = -horizontal * scrollSpeed * 4f
        val deltaY = -vertical * scrollSpeed * 4f

        val path = Path().apply {
            moveTo(cx, cy)
            lineTo(
                (cx + deltaX).clamp(0f, screenWidth.toFloat()),
                (cy + deltaY).clamp(0f, screenHeight.toFloat())
            )
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, JoyConConstants.TICK_INTERVAL_MS)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        service.dispatchGesture(gesture, null, null)
    }
}
