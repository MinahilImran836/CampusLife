package com.example.campuslife

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // This is where you put your "Not Cringe" messages!
        val messages = listOf(
            "Future you will thank you for doing those tasks now.",
            "New day, new Tasks. Update your list?",
        )
        val randomMessage = messages.random()

        NotificationHelper.sendNotification(
            context,
            "Campus Life Reminder ",
            randomMessage
        )
    }
}