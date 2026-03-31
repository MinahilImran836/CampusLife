package com.example.campuslife

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campuslife.adapters.TrashAdapter
import com.example.campuslife.model.Event
import com.example.campuslife.model.Note
import com.example.campuslife.model.Task

class TrashActivity : AppCompatActivity() {

    private lateinit var rvTrash: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var btnAction: Button
    private lateinit var btnRestore: Button
    private lateinit var adapter: TrashAdapter
    private val allDeletedItems = mutableListOf<Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        rvTrash = findViewById(R.id.rvTrash)
        txtEmpty = findViewById(R.id.txtEmptyTrash)
        btnAction = findViewById(R.id.btnClearTrash)
        btnRestore = findViewById(R.id.btnRestoreSelected)

        rvTrash.layoutManager = LinearLayoutManager(this)

        btnAction.setOnClickListener {
            if (adapter.isSelectionMode) deleteSelected() else clearAll()
        }

        btnRestore.setOnClickListener {
            restoreSelected()
        }

        setupTrashList()
    }

    private fun setupTrashList() {
        allDeletedItems.clear()
        allDeletedItems.addAll(SharedData.deletedTasks)
        allDeletedItems.addAll(SharedData.deletedNotes)
        allDeletedItems.addAll(SharedData.deletedEvents)

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

    private fun restoreSelected() {
        adapter.selectedItems.forEach { item ->
            when (item) {
                is Task -> {
                    SharedData.deletedTasks.remove(item)
                    SharedData.activeTasks.add(item)
                }
                is Note -> {
                    SharedData.deletedNotes.remove(item)
                    SharedData.activeNotes.add(item)
                }
                is Event -> {
                    SharedData.deletedEvents.remove(item)
                    SharedData.activeEvents.add(item)
                }
            }
        }
        Toast.makeText(this, "Restored successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deleteSelected() {
        AlertDialog.Builder(this)
            .setTitle("Delete Permanently?")
            .setMessage("Selected items will be gone forever!")
            .setPositiveButton("Delete") { _, _ ->
                adapter.selectedItems.forEach { item ->
                    when (item) {
                        is Task -> SharedData.deletedTasks.remove(item)
                        is Note -> SharedData.deletedNotes.remove(item)
                        is Event -> SharedData.deletedEvents.remove(item)
                    }
                }
                setupTrashList()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun clearAll() {
        AlertDialog.Builder(this)
            .setTitle("Clear Trash?")
            .setMessage("This cannot be undone!")
            .setPositiveButton("Clear All") { _, _ ->
                SharedData.deletedTasks.clear()
                SharedData.deletedNotes.clear()
                SharedData.deletedEvents.clear()
                setupTrashList()
            }.setNegativeButton("Cancel", null).show()
    }
}