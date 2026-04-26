package com.joyconcontroller.utils

import android.content.Context
import android.util.TypedValue
import kotlin.math.abs
import kotlin.math.sign

/** Convert dp to pixels */
fun Context.dpToPx(dp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

/**
 * Apply deadzone to a raw axis value.
 * Values inside [-deadzone, deadzone] become 0.
 * Values outside are rescaled to [0, 1] so there's no jump at the boundary.
 */
fun applyDeadZone(value: Float, deadZone: Float): Float {
    if (abs(value) < deadZone) return 0f
    // Rescale: (|v| - dz) / (1 - dz) * sign
    return ((abs(value) - deadZone) / (1f - deadZone)) * sign(value)
}

/**
 * Apply acceleration curve to a normalised axis value [-1, 1].
 * Low input → precise movement; high input → accelerated movement.
 * Uses a power curve: output = input^exponent
 */
fun applyAcceleration(value: Float, exponent: Float = 1.8f): Float {
    if (value == 0f) return 0f
    val magnitude = abs(value)
    return Math.pow(magnitude.toDouble(), exponent.toDouble()).toFloat() * sign(value)
}

/** Clamp a float to [min, max] */
fun Float.clamp(min: Float, max: Float): Float = maxOf(min, minOf(max, this))
