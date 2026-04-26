package com.joyconcontroller.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.joyconcontroller.R
import com.joyconcontroller.utils.AppPreferences

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

/**
 * PreferenceFragment that renders all settings from XML and syncs with [AppPreferences].
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        bindSummaries()
    }

    /**
     * Make SeekBar preferences show their current value as summary.
     */
    private fun bindSummaries() {
        // Cursor speed
        findPreference<SeekBarPreference>(AppPreferences.KEY_CURSOR_SPEED)?.apply {
            summary = "Current: $value"
            setOnPreferenceChangeListener { pref, newValue ->
                pref.summary = "Current: $newValue"
                true
            }
        }

        // Dead zone
        findPreference<SeekBarPreference>(AppPreferences.KEY_DEAD_ZONE)?.apply {
            summary = "Current: ${value / 100f} (default 0.08)"
            setOnPreferenceChangeListener { pref, newValue ->
                val f = (newValue as Int) / 100f
                pref.summary = "Current: $f"
                true
            }
        }

        // Scroll speed
        findPreference<SeekBarPreference>(AppPreferences.KEY_SCROLL_SPEED)?.apply {
            summary = "Current: $value"
            setOnPreferenceChangeListener { pref, newValue ->
                pref.summary = "Current: $newValue"
                true
            }
        }

        // Scroll mode
        findPreference<ListPreference>(AppPreferences.KEY_SCROLL_MODE)?.apply {
            summary = entry
            setOnPreferenceChangeListener { pref, _ ->
                pref as ListPreference
                pref.summary = pref.entry
                true
            }
        }
    }
}
