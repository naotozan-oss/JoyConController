package com.joyconcontroller.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.joyconcontroller.R
import com.joyconcontroller.databinding.ActivityMainBinding
import com.joyconcontroller.input.JoyConInputHandler
import com.joyconcontroller.service.JoyConAccessibilityService
import com.joyconcontroller.settings.SettingsActivity
import com.joyconcontroller.utils.AppPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppPreferences
    private lateinit var inputHandler: JoyConInputHandler

    // Receive status broadcasts from service
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connected = intent.getBooleanExtra(
                JoyConAccessibilityService.EXTRA_CONNECTED, false
            )
            updateServiceStatus(connected)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        prefs = AppPreferences(this)
        inputHandler = JoyConInputHandler(prefs)

        setupUI()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            statusReceiver,
            IntentFilter(JoyConAccessibilityService.ACTION_STATUS),
            RECEIVER_NOT_EXPORTED
        )
        // Refresh status on resume
        updateServiceStatus(JoyConAccessibilityService.instance != null)
        updateBluetoothStatus()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(statusReceiver)
    }

    // ─── UI Setup ─────────────────────────────────────────────────────────────

    private fun setupUI() {
        // Accessibility service button
        binding.btnEnableAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }

        // Bluetooth pair button
        binding.btnPairJoyCon.setOnClickListener {
            openBluetoothSettings()
        }

        // Test connection button
        binding.btnTestInput.setOnClickListener {
            Snackbar.make(
                binding.root,
                "Press any Joy-Con button to test",
                Snackbar.LENGTH_LONG
            ).show()
        }

        // Settings shortcut
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkPermissions() {
        updateBluetoothStatus()
    }

    private fun updateServiceStatus(connected: Boolean) {
        binding.statusIndicator.setBackgroundResource(
            if (connected) R.drawable.ic_status_on else R.drawable.ic_status_off
        )
        binding.tvServiceStatus.text = if (connected) {
            "✅ Accessibility Service: Active"
        } else {
            "❌ Accessibility Service: Inactive"
        }
        binding.btnEnableAccessibility.isVisible = !connected
        binding.cardJoyConStatus.isVisible = connected
    }

    private fun updateBluetoothStatus() {
        val bm = getSystemService(BluetoothManager::class.java)
        val btEnabled = bm?.adapter?.isEnabled == true
        binding.tvBluetoothStatus.text = if (btEnabled) {
            "✅ Bluetooth: On"
        } else {
            "❌ Bluetooth: Off"
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Snackbar.make(
            binding.root,
            "Find 'Joy-Con Controller' and enable it",
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun openBluetoothSettings() {
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    // ─── Pass Joy-Con events to handler for testing ───────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (inputHandler.handleKeyEvent(event)) {
            binding.tvLastInput.text = "Last input: Key ${keyCode}"
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return inputHandler.handleKeyEvent(event) || super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (inputHandler.handleMotionEvent(event)) return true
        return super.onGenericMotionEvent(event)
    }

    // ─── Options menu ─────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
