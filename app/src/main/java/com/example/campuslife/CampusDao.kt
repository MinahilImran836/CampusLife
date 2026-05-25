package com.example.campuslife.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CampusDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllNotesStatic(): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<NoteChunk>)

    @Query("DELETE FROM note_chunks WHERE noteId = :noteId")
    suspend fun deleteChunksByNoteId(noteId: Int)

    @Query("SELECT * FROM note_chunks")
    suspend fun getAllChunksStatic(): List<NoteChunk>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isCompleted ASC, timeInMillis DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM events WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isCompleted ASC, timeInMillis DESC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedTasksForSummary(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)

    @Update
    suspend fun updateTask(task: Task)

    @Update
    suspend fun updateNote(note: Note)

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteTask(task: Task)

    @Delete
    suspend fun deleteNote(note: Note)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("SELECT * FROM tasks WHERE isDeleted = 1")
    suspend fun getDeletedTasks(): List<Task>

    @Query("SELECT * FROM notes WHERE isDeleted = 1")
    suspend fun getDeletedNotes(): List<Note>

    @Query("SELECT * FROM events WHERE isDeleted = 1")
    suspend fun getDeletedEvents(): List<Event>

    @Query("DELETE FROM tasks WHERE timeInMillis < :todayMillis AND isCompleted = 1")
    suspend fun deleteOldCompletedTasks(todayMillis: Long)

    @Query("DELETE FROM tasks WHERE isDeleted = 1")
    suspend fun permanentlyDeleteAllTasks()

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun permanentlyDeleteAllNotes()

    @Query("DELETE FROM events WHERE isDeleted = 1")
    suspend fun permanentlyDeleteAllEvents()
}