package com.example.campuslife.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class CommunityPost(
    @get:Exclude @set:Exclude var id: String = "",
    var author: String = "",
    var authorUid: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "General",
    var timestamp: Timestamp? = null,
    val attachmentData: List<String> = emptyList(),
    val attachmentNames: List<String> = emptyList()
)