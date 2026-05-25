package com.example.campuslife.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String,
    val date: String,
    val time: String,
    val timeInMillis: Long,
    val isImportant: Boolean,
    var isCompleted: Boolean = false,
    var isDeleted: Boolean = false,
    var isArchived: Boolean = false,
    var completedAt: Long = 0L
) : Serializable