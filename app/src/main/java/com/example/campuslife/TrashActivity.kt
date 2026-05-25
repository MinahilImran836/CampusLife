package com.example.campuslife

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campuslife.adapters.TrashAdapter
import com.example.campuslife.data.AppDatabase
import com.example.campuslife.data.CampusDao
import com.example.campuslife.data.Event
import com.example.campuslife.data.Note
import com.example.campuslife.data.Task
import kotlinx.coroutines.launch

class TrashActivity : AppCompatActivity() {

    private lateinit var rvTrash: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var btnAction: Button
    private lateinit var btnRestore: Button
    private lateinit var adapter: TrashAdapter
    private val allDeletedItems = mutableListOf<Any>()

    private lateinit var database: AppDatabase
    private lateinit var campusDao: CampusDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        database = AppDatabase.getDatabase(this)
        campusDao = database.campusDao()

        rvTrash = findViewById(R.id.rvTrash)
        txtEmpty = findViewById(R.id.txtEmptyTrash)
        btnAction = findViewById(R.id.btnClearTrash)
        btnRestore = findViewById(R.id.btnRestoreSelected)

        rvTrash.layoutManager = LinearLayoutManager(this)

        btnAction.setOnClickListener {
            if (::adapter.isInitialized && adapter.isSelectionMode) deleteSelected() else clearAll()
        }

        btnRestore.setOnClickListener {
            restoreSelected()
        }

        setupTrashList()
    }

    private fun setupTrashList() {
        lifecycleScope.launch {
            allDeletedItems.clear()
            allDeletedItems.addAll(campusDao.getDeletedTasks())
            allDeletedItems.addAll(campusDao.getDeletedNotes())
            allDeletedItems.addAll(campusDao.getDeletedEvents())

            if (allDeletedItems.isEmpty()) {
                txtEmpty.visibility = View.VISIBLE
                rvTrash.visibility = View.GONE
                btnAction.visibility = View.GONE
                btnRestore.visibility = View.GONE
            } else {
                txtEmpty.visibility = View.GONE
                rvTrash.visibility = View.VISIBLE
                btnAction.visibility = View.VISIBLE
                btnAction.text = "Clear All"

                adapter = TrashAdapter(allDeletedItems) { selectedCount ->
                    if (selectedCount > 0) {
                        btnAction.text = "Delete ($selectedCount)"
                        btnRestore.visibility = View.VISIBLE
                    } else {
                        btnAction.text = "Clear All"
                        btnRestore.visibility = View.GONE
                    }
                }
                rvTrash.adapter = adapter
            }
        }
    }

    private fun restoreSelected() {
        lifecycleScope.launch {
            adapter.selectedItems.forEach { item ->
                when (item) {
                    is Task -> campusDao.updateTask(item.apply { isDeleted = false })
                    is Note -> campusDao.updateNote(item.apply { isDeleted = false })
                    is Event -> campusDao.updateEvent(item.apply { isDeleted = false })
                }
            }
            Toast.makeText(this@TrashActivity, "Restored successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun deleteSelected() {
        AlertDialog.Builder(this)
            .setTitle("Delete Permanently?")
            .setMessage("Selected items will be gone forever!")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    // Fixed: Performs hard data removals from database storage
                    adapter.selectedItems.forEach { item ->
                        when (item) {
                            is Task -> campusDao.deleteTask(item)
                            is Note -> campusDao.deleteNote(item)
                            is Event -> campusDao.deleteEvent(item)
                        }
                    }
                    Toast.makeText(this@TrashActivity, "Selected items deleted permanently", Toast.LENGTH_SHORT).show()
                    setupTrashList()
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun clearAll() {
        AlertDialog.Builder(this)
            .setTitle("Clear Trash?")
            .setMessage("This cannot be undone!")
            .setPositiveButton("Clear All") { _, _ ->
                lifecycleScope.launch {
                    // Fixed: Calls all three custom clean up queries to empty the entire trash
                    campusDao.permanentlyDeleteAllTasks()
                    campusDao.permanentlyDeleteAllNotes()
                    campusDao.permanentlyDeleteAllEvents()
                    Toast.makeText(this@TrashActivity, "Trash Cleared!", Toast.LENGTH_SHORT).show()
                    setupTrashList()
                }
            }.setNegativeButton("Cancel", null).show()
    }
}