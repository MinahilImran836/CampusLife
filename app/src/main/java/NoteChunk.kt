package com.example.campuslife.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_chunks",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class NoteChunk(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int,
    val chunkText: String,
    val embedding: String, // Stores the 384-dimensional vector string for this specific chunk
    val pageNumber: Int // Tracks the source slide or page index
)