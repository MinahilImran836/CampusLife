package com.example.campuslife

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Route actions cleanly based on explicit intent filtering strings
        when (intent.action) {
            NotificationHelper.ACTION_TASK_REMINDER -> {
                val taskTitle = intent.getStringExtra("task_title") ?: "Task Reminder"
                NotificationHelper.sendNotification(
                    context,
                    "Task Deadline!",
                    "Time to work on: $taskTitle"
                )
            }
            NotificationHelper.ACTION_DAILY_REMINDER -> {
                val prefs = context.getSharedPreferences("CampusLifePrefs", Context.MODE_PRIVATE)
                if (prefs.getBoolean("reminder_enabled", true)) {
                    val messages = listOf(
                        "Future you will thank you for doing those tasks now.",
                        "New day, new Tasks. Time to update your list?",
                        "Stay organized, stay focused."
                    )

                    NotificationHelper.sendNotification(
                        context,
                        "Campus Life Daily Check-in",
                        messages.random()
                    )

                    // Reschedule for tomorrow smoothly
                    val hour = prefs.getInt("reminder_hour", 9)
                    val minute = prefs.getInt("reminder_minute", 0)
                    NotificationHelper.scheduleDailyReminder(context, hour, minute)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Device rebooted; reschedule the daily system loop
                val prefs = context.getSharedPreferences("CampusLifePrefs", Context.MODE_PRIVATE)
                if (prefs.getBoolean("reminder_enabled", true)) {
                    val hour = prefs.getInt("reminder_hour", 9)
                    val minute = prefs.getInt("reminder_minute", 0)
                    NotificationHelper.scheduleDailyReminder(context, hour, minute)
                }
            }
        }
    }
}