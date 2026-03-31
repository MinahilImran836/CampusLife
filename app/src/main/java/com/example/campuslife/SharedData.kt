package com.example.campuslife

import com.example.campuslife.model.Task
import com.example.campuslife.model.Note
import com.example.campuslife.model.Event

object SharedData {
    // Live Items
    val activeTasks = mutableListOf<Task>()
    val activeNotes = mutableListOf<Note>()
    val activeEvents = mutableListOf<Event>()

    // Trash Items
    val deletedTasks = mutableListOf<Task>()
    val deletedNotes = mutableListOf<Note>()
    val deletedEvents = mutableListOf<Event>()
}