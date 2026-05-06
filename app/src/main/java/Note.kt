//package com.example.campuslife.data
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//import java.io.Serializable
//
//@Entity(tableName = "notes")
//data class Note(
//    @PrimaryKey(autoGenerate = true) val id: Int = 0,
//    var title: String,
//    var content: String,
//    val date: String,
//    val color: String,
//    val attachmentUris: MutableList<String> = mutableListOf(),
//    val attachmentNames: MutableList<String> = mutableListOf(),
//    val links: MutableList<String> = mutableListOf()
//) : Serializable