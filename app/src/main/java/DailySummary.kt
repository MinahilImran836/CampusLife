package com.example.campuslife.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey val date: String, // Use date as primary key for simple "one per day" lookups
    val totalTasks: Int,
    val completedTasks: Int,
    val onTimeTasks: Int,
    val topCategory: String,
    val eventsAttended: Int,
    val aiSummary: String? = null // Store the AI response here
)