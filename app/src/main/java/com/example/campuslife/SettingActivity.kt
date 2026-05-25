package com.example.campuslife

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnSetReminder: Button
    private lateinit var switchNotifications: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        btnSetReminder = findViewById(R.id.btnSetReminder)
        switchNotifications = findViewById(R.id.switchNotifications)

        val prefs = getSharedPreferences("CampusLifePrefs", Context.MODE_PRIVATE)

        val isEnabled = prefs.getBoolean("reminder_enabled", false)
        switchNotifications.isChecked = isEnabled
        updateButtonState(isEnabled)

        btnSetReminder.setOnClickListener {
            openTimePicker()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val editor = prefs.edit()
            editor.putBoolean("reminder_enabled", isChecked)
            editor.apply()

            updateButtonState(isChecked)

            if (isChecked) {
                val h = prefs.getInt("reminder_hour", 9)
                val m = prefs.getInt("reminder_minute", 0)
                NotificationHelper.scheduleDailyReminder(this, h, m)
                Toast.makeText(this, "Daily reminders ON", Toast.LENGTH_SHORT).show()
            } else {

                NotificationHelper.cancelDailyReminder(this)
                Toast.makeText(this, "Reminders OFF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateButtonState(isEnabled: Boolean) {
        btnSetReminder.isEnabled = isEnabled
        btnSetReminder.alpha = if (isEnabled) 1.0f else 0.4f
    }

    private fun openTimePicker() {
        val calendar = Calendar.getInstance()
        val prefs = getSharedPreferences("CampusLifePrefs", Context.MODE_PRIVATE)

        val savedHour = prefs.getInt("reminder_hour", calendar.get(Calendar.HOUR_OF_DAY))
        val savedMinute = prefs.getInt("reminder_minute", calendar.get(Calendar.MINUTE))

        val timePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val editor = prefs.edit()
            editor.putInt("reminder_hour", selectedHour)
            editor.putInt("reminder_minute", selectedMinute)
            editor.apply()

            NotificationHelper.scheduleDailyReminder(this, selectedHour, selectedMinute)

            val timeStr = String.format("%02d:%02d", selectedHour, selectedMinute)
            Toast.makeText(this, "Reminder scheduled for $timeStr! ⏰", Toast.LENGTH_SHORT).show()

        }, savedHour, savedMinute, true)

        timePicker.show()
    }
}