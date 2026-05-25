package com.example.campuslife.data



import androidx.room.Entity

import androidx.room.PrimaryKey

import java.io.Serializable



@Entity(tableName = "events")

data class Event(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val title: String,

    val location: String,

    val date: String,

    val time: String,

    val timeInMillis: Long,

    var isCompleted: Boolean = false,

    var isDeleted: Boolean = false,

    var isArchived: Boolean = false

) : Serializable

