package com.example.campuslife.data


data class ChatMessage(
    val text: String,
    val isUser: Boolean, // true if sent by user, false if sent by AI Assistant
    val timestamp: Long = System.currentTimeMillis()
)