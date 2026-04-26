package com.joyconcontroller.input

import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Hardware constants for Nintendo Switch Joy-Con (Right).
 *
 * Joy-Con presents itself as a standard Bluetooth HID gamepad.
 * Android maps its buttons to standard GameController keycodes.
 *
 * Right Joy-Con button layout:
 *  A = BUTTON_A (96)
 *  B = BUTTON_B (97)
 *  X = BUTTON_X (99)
 *  Y = BUTTON_Y (100)
 *  R = BUTTON_R1 (103)
 *  ZR = BUTTON_R2 (104) or AXIS_RTRIGGER
 *  + (Plus) = BUTTON_START (108)
 *  HOME = BUTTON_MODE (110)
 *  RS (stick click) = BUTTON_THUMBR (107)
 *  SL = BUTTON_SELECT (109)  [side button]
 *  SR = varies by firmware
 *
 * Right stick axes:
 *  X = AXIS_Z
 *  Y = AXIS_RZ
 *
 * Note: Left Joy-Con uses:
 *  Left stick X = AXIS_X, Y = AXIS_Y
 *  - (Minus) = BUTTON_SELECT (109)
 */
object JoyConConstants {

    // ── Button keycodes ──────────────────────────────────────────────────────
    const val BTN_A = KeyEvent.KEYCODE_BUTTON_A          // 96
    const val BTN_B = KeyEvent.KEYCODE_BUTTON_B          // 97
    const val BTN_X = KeyEvent.KEYCODE_BUTTON_X          // 99
    const val BTN_Y = KeyEvent.KEYCODE_BUTTON_Y          // 100
    const val BTN_R = KeyEvent.KEYCODE_BUTTON_R1         // 103
    const val BTN_ZR = KeyEvent.KEYCODE_BUTTON_R2        // 104
    const val BTN_PLUS = KeyEvent.KEYCODE_BUTTON_START   // 108
    const val BTN_HOME = KeyEvent.KEYCODE_BUTTON_MODE    // 110
    const val BTN_RS = KeyEvent.KEYCODE_BUTTON_THUMBR    // 107  (stick click)
    const val BTN_MINUS = KeyEvent.KEYCODE_BUTTON_SELECT // 109  (left joy-con -)
    const val BTN_SL = KeyEvent.KEYCODE_BUTTON_SELECT    // 109  same fallback
    const val BTN_SR = KeyEvent.KEYCODE_BUTTON_START     // firmware-dependent

    // ── Axes ─────────────────────────────────────────────────────────────────
    const val AXIS_RIGHT_STICK_X = MotionEvent.AXIS_Z    // right stick horizontal
    const val AXIS_RIGHT_STICK_Y = MotionEvent.AXIS_RZ   // right stick vertical
    const val AXIS_LEFT_STICK_X = MotionEvent.AXIS_X     // left stick horizontal
    const val AXIS_LEFT_STICK_Y = MotionEvent.AXIS_Y     // left stick vertical

    // ── Timing (ms) ──────────────────────────────────────────────────────────
    const val LONG_PRESS_DURATION_MS = 600L
    const val DOUBLE_TAP_INTERVAL_MS = 80L
    const val RS_CLICK_DEBOUNCE_MS = 200L   // prevent accidental RS tap during stick motion
    const val TICK_INTERVAL_MS = 16L        // ~60fps input polling interval

    // ── Speed multipliers ─────────────────────────────────────────────────────
    const val BOOST_MULTIPLIER = 2.5f
}
