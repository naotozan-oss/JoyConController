package com.joyconcontroller.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.joyconcontroller.utils.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Parses raw Android KeyEvent / MotionEvent from a connected Joy-Con
 * and exposes the current state as a [StateFlow<JoyConState>].
 *
 * Thread-safety: KeyEvent/MotionEvent callbacks arrive on the main thread.
 * The StateFlow emission is therefore also on main. Consumers should
 * observe on an appropriate dispatcher.
 */
class JoyConInputHandler(private val prefs: AppPreferences) {

    private val _state = MutableStateFlow(JoyConState())
    val state: StateFlow<JoyConState> = _state.asStateFlow()

    // Mutable working copy; only committed via copy-and-emit
    private var current = JoyConState()

    // Track which buttons are currently pressed
    private val pressedButtons = mutableSetOf<Int>()

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Call from [onKeyDown] / [onKeyUp] in the AccessibilityService or Activity.
     * @return true if the event was consumed by Joy-Con handling
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (!isJoyConKey(code)) return false

        val pressed = event.action == KeyEvent.ACTION_DOWN
        if (pressed) pressedButtons.add(code) else pressedButtons.remove(code)

        current = current.copy(
            btnA = JoyConConstants.BTN_A in pressedButtons,
            btnB = JoyConConstants.BTN_B in pressedButtons,
            btnX = JoyConConstants.BTN_X in pressedButtons,
            btnY = JoyConConstants.BTN_Y in pressedButtons,
            btnR = JoyConConstants.BTN_R in pressedButtons,
            btnZR = JoyConConstants.BTN_ZR in pressedButtons,
            btnPlus = JoyConConstants.BTN_PLUS in pressedButtons,
            btnHome = JoyConConstants.BTN_HOME in pressedButtons,
            btnRS = JoyConConstants.BTN_RS in pressedButtons,
            btnMinus = JoyConConstants.BTN_MINUS in pressedButtons,
            boostActive = computeBoost()
        )
        emit()
        return true
    }

    /**
     * Call from [onGenericMotionEvent] for joystick axis updates.
     * @return true if the event was consumed
     */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK == 0 &&
            event.source and InputDevice.SOURCE_GAMEPAD == 0
        ) return false

        // Process all historical positions to avoid input lag on batched events
        for (pos in 0 until event.historySize) {
            updateAxes(
                rx = event.getHistoricalAxisValue(JoyConConstants.AXIS_RIGHT_STICK_X, pos),
                ry = event.getHistoricalAxisValue(JoyConConstants.AXIS_RIGHT_STICK_Y, pos),
                lx = event.getHistoricalAxisValue(JoyConConstants.AXIS_LEFT_STICK_X, pos),
                ly = event.getHistoricalAxisValue(JoyConConstants.AXIS_LEFT_STICK_Y, pos)
            )
        }
        // Current position
        updateAxes(
            rx = event.getAxisValue(JoyConConstants.AXIS_RIGHT_STICK_X),
            ry = event.getAxisValue(JoyConConstants.AXIS_RIGHT_STICK_Y),
            lx = event.getAxisValue(JoyConConstants.AXIS_LEFT_STICK_X),
            ly = event.getAxisValue(JoyConConstants.AXIS_LEFT_STICK_Y)
        )
        return true
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun updateAxes(rx: Float, ry: Float, lx: Float, ly: Float) {
        current = current.copy(
            rightStickX = rx,
            rightStickY = ry,
            leftStickX = lx,
            leftStickY = ly,
            boostActive = computeBoost()
        )
        emit()
    }

    private fun computeBoost(): Boolean {
        return if (prefs.boostToggle) {
            // Toggle mode: R button toggles boost (handled in gesture dispatcher)
            current.boostActive
        } else {
            // Hold mode: boost active while R is held
            JoyConConstants.BTN_R in pressedButtons
        }
    }

    private fun emit() {
        _state.value = current
    }

    private fun isJoyConKey(keyCode: Int): Boolean = keyCode in setOf(
        JoyConConstants.BTN_A,
        JoyConConstants.BTN_B,
        JoyConConstants.BTN_X,
        JoyConConstants.BTN_Y,
        JoyConConstants.BTN_R,
        JoyConConstants.BTN_ZR,
        JoyConConstants.BTN_PLUS,
        JoyConConstants.BTN_HOME,
        JoyConConstants.BTN_RS,
        JoyConConstants.BTN_MINUS
    )
}
