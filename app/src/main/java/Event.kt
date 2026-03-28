package com.example.campuslife.model

import java.io.Serializable

data class Event(
    val title: String,
    val location: String,
    val date: String,
    val time: String
) : Serializable