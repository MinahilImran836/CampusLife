package com.example.campuslife.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class CommunityPost(
    @get:Exclude @set:Exclude var id: String = "",
    var author: String = "",       // Matches "author" in hashMap
    var authorUid: String = "",    // Matches "authorUid" in hashMap
    val title: String = "",
    val content: String = "",
    var timestamp: Timestamp? = null,
    val attachmentData: List<String> = emptyList(),
    val attachmentNames: List<String> = emptyList()
)