package com.example.campuslife

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isSpecific = intent.getBooleanExtra("is_specific", false)

        if (isSpecific) {
            // This is a Specific Task or Event reminder
            val taskTitle = intent.getStringExtra("task_title") ?: "Task Reminder"
            NotificationHelper.sendNotification(
                context,
                "Time to Slay! ✨",
                "Start working on: $taskTitle"
            )
        } else {
            // Standard Daily Check-in logic
            val prefs = context.getSharedPreferences("CampusLifePrefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("reminder_enabled", false)) {
                val messages = listOf(
                    "Future you will thank you for doing those tasks now. ✨",
                    "New day, new Tasks. Time to update your list? 📝",
                    "Stay organized, stay legendary. 🎓"
                )

                NotificationHelper.sendNotification(
                    context,
                    "Campus Life Daily Check-in",
                    messages.random()
                )

                // RE-SCHEDULE DAILY
                val hour = prefs.getInt("reminder_hour", 9)
                val minute = prefs.getInt("reminder_minute", 0)
                NotificationHelper.scheduleDailyReminder(context, hour, minute)
            }
        }
    }
}