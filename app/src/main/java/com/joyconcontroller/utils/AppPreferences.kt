package com.joyconcontroller.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Centralized preferences manager for all app settings.
 * All defaults are defined here for single-source-of-truth.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        // Cursor
        const val KEY_CURSOR_SPEED = "cursor_speed"
        const val KEY_DEAD_ZONE = "dead_zone"
        const val KEY_ACCELERATION_ENABLED = "acceleration_enabled"
        const val KEY_MAX_SPEED = "max_speed"

        // Scroll
        const val KEY_SCROLL_MODE = "scroll_mode" // "left_stick" or "r_button"
        const val KEY_SCROLL_SPEED = "scroll_speed"

        // Boost
        const val KEY_BOOST_TOGGLE = "boost_toggle" // false=hold R, true=toggle

        // Button mapping
        const val KEY_BTN_TAP = "btn_tap"
        const val KEY_BTN_BACK = "btn_back"
        const val KEY_BTN_HOME = "btn_home"
        const val KEY_BTN_RECENTS = "btn_recents"
        const val KEY_BTN_NOTIF = "btn_notif"
        const val KEY_BTN_LONG_PRESS = "btn_long_press"
        const val KEY_BTN_DOUBLE_TAP = "btn_double_tap"

        // Defaults
        const val DEFAULT_CURSOR_SPEED = 12f
        const val DEFAULT_DEAD_ZONE = 0.08f
        const val DEFAULT_MAX_SPEED = 40f
        const val DEFAULT_SCROLL_SPEED = 8f
        const val DEFAULT_SCROLL_MODE = "left_stick"
    }

    var cursorSpeed: Float
        get() = prefs.getFloat(KEY_CURSOR_SPEED, DEFAULT_CURSOR_SPEED)
        set(value) = prefs.edit().putFloat(KEY_CURSOR_SPEED, value).apply()

    var deadZone: Float
        get() = prefs.getFloat(KEY_DEAD_ZONE, DEFAULT_DEAD_ZONE)
        set(value) = prefs.edit().putFloat(KEY_DEAD_ZONE, value).apply()

    var accelerationEnabled: Boolean
        get() = prefs.getBoolean(KEY_ACCELERATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ACCELERATION_ENABLED, value).apply()

    var maxSpeed: Float
        get() = prefs.getFloat(KEY_MAX_SPEED, DEFAULT_MAX_SPEED)
        set(value) = prefs.edit().putFloat(KEY_MAX_SPEED, value).apply()

    var scrollMode: String
        get() = prefs.getString(KEY_SCROLL_MODE, DEFAULT_SCROLL_MODE) ?: DEFAULT_SCROLL_MODE
        set(value) = prefs.edit().putString(KEY_SCROLL_MODE, value).apply()

    var scrollSpeed: Float
        get() = prefs.getFloat(KEY_SCROLL_SPEED, DEFAULT_SCROLL_SPEED)
        set(value) = prefs.edit().putFloat(KEY_SCROLL_SPEED, value).apply()

    var boostToggle: Boolean
        get() = prefs.getBoolean(KEY_BOOST_TOGGLE, false)
        set(value) = prefs.edit().putBoolean(KEY_BOOST_TOGGLE, value).apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
