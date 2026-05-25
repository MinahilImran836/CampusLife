package com.example.campuslife

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.*

object NotificationHelper {
    private const val CHANNEL_ID = "campus_life_reminders"

    // Separate explicit action strings to prevent system mixing
    const val ACTION_DAILY_REMINDER = "com.example.campuslife.ACTION_DAILY_REMINDER"
    const val ACTION_TASK_REMINDER = "com.example.campuslife.ACTION_TASK_REMINDER"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Campus Life Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for tasks and events"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
            putExtra("is_specific", false)
        }
        // Unique request code 100 for global daily reminder
        val pendingIntent = PendingIntent.getBroadcast(
            context, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun scheduleSpecificReminder(context: Context, title: String, timeInMillis: Long) {
        if (timeInMillis < System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra("is_specific", true)
            putExtra("task_title", title)
        }

        // Unique dynamic ID based on task title hash code
        val requestId = title.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }

    fun sendNotification(context: Context, title: String, message: String) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun cancelDailyReminder(context: Context) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 100, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}