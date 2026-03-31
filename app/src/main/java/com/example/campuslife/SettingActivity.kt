package com.example.campuslife

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // Find the button in your settings XML
        val btnSetReminder = findViewById<Button>(R.id.btnSetReminder)

        btnSetReminder.setOnClickListener {
            openTimePicker()
        }
    }

    private fun openTimePicker() {
        val calendar = Calendar.getInstance()

        // 1. Try to load the PREVIOUSLY saved time (default to current time if none)
        val prefs = getSharedPreferences("CampusLifePrefs", Context.MODE_PRIVATE)
        val savedHour = prefs.getInt("reminder_hour", calendar.get(Calendar.HOUR_OF_DAY))
        val savedMinute = prefs.getInt("reminder_minute", calendar.get(Calendar.MINUTE))

        val timePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->

            // 2. SAVE the new time to the "Permanent Notepad"
            val editor = prefs.edit()
            editor.putInt("reminder_hour", selectedHour)
            editor.putInt("reminder_minute", selectedMinute)
            editor.apply() // This saves it to the phone's storage!

            // 3. Schedule the alarm
            NotificationHelper.scheduleReminder(this, selectedHour, selectedMinute)

            val timeStr = String.format("%02d:%02d", selectedHour, selectedMinute)
            Toast.makeText(this, "Reminder saved for $timeStr! ✨", Toast.LENGTH_SHORT).show()

        }, savedHour, savedMinute, true)

        timePicker.show()
    }
}