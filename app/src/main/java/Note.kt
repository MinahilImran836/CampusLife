package com.example.campuslife.model

import java.io.Serializable

data class Note(
    var title: String,
    var content: String,
    val date: String,
    val color: String,
    val attachmentUris: MutableList<String> = mutableListOf(),
    val attachmentNames: MutableList<String> = mutableListOf(),
    val links: MutableList<String> = mutableListOf()
) : Serializable