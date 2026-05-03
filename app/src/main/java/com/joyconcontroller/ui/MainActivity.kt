package com.joyconcontroller.ui

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    // Runtime permission launcher for BLUETOOTH_CONNECT (Android 12+)
    private val btPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateBluetoothStatus()
        if (!granted) {
            Snackbar.make(binding.root,
                "Bluetooth permission needed to show BT status",
                Snackbar.LENGTH_LONG).show()
        }
    }

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
        // Android 14 (API 34)+ requires explicit exported flag on registerReceiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                statusReceiver,
                IntentFilter(JoyConAccessibilityService.ACTION_STATUS),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                statusReceiver,
                IntentFilter(JoyConAccessibilityService.ACTION_STATUS)
            )
        }
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
        // BLUETOOTH_CONNECT is a runtime permission on Android 12 (API 31)+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
        }
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
        val btEnabled = try {
            // On Android 12+, accessing adapter without BLUETOOTH_CONNECT throws SecurityException
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                null // Permission not granted yet
            } else {
                val bm = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
                bm?.adapter?.isEnabled
            }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }

        binding.tvBluetoothStatus.text = when (btEnabled) {
            true  -> "✅ Bluetooth: On"
            false -> "❌ Bluetooth: Off"
            null  -> "⚠️ Bluetooth: Permission needed"
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
