package com.joyconcontroller.input

/**
 * Immutable snapshot of the current Joy-Con input state.
 * Passed between InputHandler → AccessibilityService → CursorOverlay.
 */
data class JoyConState(
    // Right stick (raw, before deadzone/acceleration)
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,

    // Left stick (raw)
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,

    // Buttons currently held down
    val btnA: Boolean = false,
    val btnB: Boolean = false,
    val btnX: Boolean = false,
    val btnY: Boolean = false,
    val btnR: Boolean = false,
    val btnZR: Boolean = false,
    val btnPlus: Boolean = false,
    val btnHome: Boolean = false,
    val btnRS: Boolean = false,   // right stick click
    val btnMinus: Boolean = false,

    // Derived convenience flags
    val boostActive: Boolean = false  // set by InputHandler based on R + prefs
)
