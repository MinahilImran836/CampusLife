package com.example.campuslife

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campuslife.adapters.TaskAdapter
import com.example.campuslife.adapters.NoteAdapter
import com.example.campuslife.adapters.EventAdapter
import com.example.campuslife.model.Task
import com.example.campuslife.model.Note
import com.example.campuslife.model.Event
import com.google.android.material.navigation.NavigationView
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity(),
    TaskAdapter.OnTaskClickListener,
    NoteAdapter.OnNoteClickListener,
    EventAdapter.OnEventClickListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var btnMenu: ImageView

    private lateinit var btnAdd: ImageView
    private lateinit var btnTaskWrapper: ConstraintLayout
    private lateinit var btnNoteWrapper: ConstraintLayout
    private lateinit var btnEventWrapper: ConstraintLayout
    private lateinit var btnTaskMini: ImageView
    private lateinit var btnNoteMini: ImageView
    private lateinit var btnEventMini: ImageView
    private lateinit var txtEmptyPlaceholder: TextView

    private var isExpanded = false

    private lateinit var rvMainToday: RecyclerView
    private lateinit var rvTasksTab: RecyclerView
    private lateinit var rvNotesTab: RecyclerView
    private lateinit var rvEventsTab: RecyclerView

    private lateinit var tabTasks: Button
    private lateinit var tabNotes: Button
    private lateinit var tabEvents: Button

    private lateinit var todayAdapter: TaskAdapter
    private lateinit var taskTabAdapter: TaskAdapter
    private lateinit var notesAdapter: NoteAdapter
    private lateinit var eventAdapter: EventAdapter

    private val REQ_ADD_TASK = 2001
    private val REQ_EDIT_TASK = 2002
    private val REQ_NOTE = 3001
    private val REQ_EVENT = 4001

    private var editingPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        NotificationHelper.createNotificationChannel(this)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        btnMenu = findViewById(R.id.btnMenu)
        navigationView.itemIconTintList = null

        tabTasks = findViewById(R.id.tabTasks)
        tabNotes = findViewById(R.id.tabNotes)
        tabEvents = findViewById(R.id.tabEvents)
        txtEmptyPlaceholder = findViewById(R.id.txtEmptyPlaceholder)

        btnAdd = findViewById(R.id.btnAdd)
        btnTaskWrapper = findViewById(R.id.btnTaskWrapper)
        btnNoteWrapper = findViewById(R.id.btnNoteWrapper)
        btnEventWrapper = findViewById(R.id.btnEventWrapper)
        btnTaskMini = findViewById(R.id.btnAddTaskMini)
        btnNoteMini = findViewById(R.id.btnAddNoteMini)
        btnEventMini = findViewById(R.id.btnAddEventMini)

        rvMainToday = findViewById(R.id.recyclerViewMain)
        rvTasksTab = findViewById(R.id.recyclerViewTasksTab)
        rvNotesTab = findViewById(R.id.recyclerViewNotesTab)
        rvEventsTab = findViewById(R.id.recyclerViewEventsTab)

        rvMainToday.layoutManager = LinearLayoutManager(this)
        rvTasksTab.layoutManager = LinearLayoutManager(this)
        rvNotesTab.layoutManager = LinearLayoutManager(this)
        rvEventsTab.layoutManager = LinearLayoutManager(this)

        // 🔥 FIX 1: Pointing all adapters to SharedData global lists
        todayAdapter = TaskAdapter(SharedData.activeTasks, this)
        taskTabAdapter = TaskAdapter(SharedData.activeTasks, this)
        notesAdapter = NoteAdapter(SharedData.activeNotes, this)
        eventAdapter = EventAdapter(SharedData.activeEvents, this)

        rvMainToday.adapter = todayAdapter
        rvTasksTab.adapter = taskTabAdapter
        rvNotesTab.adapter = notesAdapter
        rvEventsTab.adapter = eventAdapter
        setupListeners()
        setupDrawer()
    }

    // 🔥 FIX 2: Refresh lists when returning from TrashActivity
    override fun onResume() {
        super.onResume()
        refreshAllData()
    }

    private fun refreshAllData() {
        todayAdapter.notifyDataSetChanged()
        taskTabAdapter.notifyDataSetChanged()
        notesAdapter.notifyDataSetChanged()
        eventAdapter.notifyDataSetChanged()
        updatePlaceholderVisibility()
    }

    private fun setupDrawer() {
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_trash -> startActivity(Intent(this, TrashActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupListeners() {
        tabTasks.setOnClickListener { showTab(rvTasksTab) }
        tabNotes.setOnClickListener { showTab(rvNotesTab) }
        tabEvents.setOnClickListener { showTab(rvEventsTab) }

        btnAdd.setOnClickListener { if (!isExpanded) expandMenu() else collapseMenu() }

        btnTaskMini.setOnClickListener {
            collapseMenu()
            startActivityForResult(Intent(this, TaskFormActivity::class.java), REQ_ADD_TASK)
        }
        btnNoteMini.setOnClickListener {
            collapseMenu()
            startActivityForResult(Intent(this, NoteFormActivity::class.java), REQ_NOTE)
        }
        btnEventMini.setOnClickListener {
            collapseMenu()
            startActivityForResult(Intent(this, EventFormActivity::class.java), REQ_EVENT)
        }
    }

    private fun updatePlaceholderVisibility() {
        val isEmpty = when {
            rvTasksTab.visibility == View.VISIBLE -> SharedData.activeTasks.isEmpty()
            rvNotesTab.visibility == View.VISIBLE -> SharedData.activeNotes.isEmpty()
            rvEventsTab.visibility == View.VISIBLE -> SharedData.activeEvents.isEmpty()
            else -> SharedData.activeTasks.isEmpty()
        }
        txtEmptyPlaceholder.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    // --- RECYCLERVIEW ACTIONS (Updated to use SharedData) ---
    override fun onTaskClick(task: Task, position: Int) {
        editingPosition = position
        val intent = Intent(this, TaskFormActivity::class.java).apply { putExtra("taskData", task) }
        startActivityForResult(intent, REQ_EDIT_TASK)
    }

    override fun onTaskLongClick(task: Task, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task?")
            .setPositiveButton("Yes") { _, _ ->
                SharedData.activeTasks.remove(task)
                SharedData.deletedTasks.add(task)
                refreshAllData()
                Toast.makeText(this, "Moved to Trash 🗑️", Toast.LENGTH_SHORT).show()
            }.show()
    }

    override fun onNoteClick(note: Note, position: Int) {
        editingPosition = position
        val intent = Intent(this, NoteFormActivity::class.java).apply { putExtra("noteData", note) }
        startActivityForResult(intent, REQ_NOTE)
    }

    override fun onNoteLongClick(note: Note, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Note?")
            .setPositiveButton("Yes") { _, _ ->
                SharedData.activeNotes.remove(note)
                SharedData.deletedNotes.add(note)
                refreshAllData()
            }.show()
    }

    override fun onEventClick(event: Event, position: Int) {
        editingPosition = position
        val intent = Intent(this, EventFormActivity::class.java).apply { putExtra("eventData", event) }
        startActivityForResult(intent, REQ_EVENT)
    }

    override fun onEventLongClick(event: Event, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Event?")
            .setPositiveButton("Yes") { _, _ ->
                SharedData.activeEvents.remove(event)
                SharedData.deletedEvents.add(event)
                refreshAllData()
            }.show()
    }

    private fun showTab(viewToShow: RecyclerView) {
        rvMainToday.visibility = View.GONE
        rvTasksTab.visibility = View.GONE
        rvNotesTab.visibility = View.GONE
        rvEventsTab.visibility = View.GONE
        viewToShow.visibility = View.VISIBLE
        updatePlaceholderVisibility()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQ_ADD_TASK, REQ_EDIT_TASK -> {
                val task = data?.getSerializableExtra("taskData") as? Task
                task?.let {
                    if (requestCode == REQ_ADD_TASK) SharedData.activeTasks.add(it)

                    else if (editingPosition != -1) SharedData.activeTasks[editingPosition] = it
                    NotificationHelper.sendNotification(this, "New Task Slayed! ✨", "Added: ${it.title}")
                    showTab(rvTasksTab)
                }
            }
            REQ_NOTE -> {
                val note = data?.getSerializableExtra("noteData") as? Note
                note?.let {
                    if (editingPosition != -1) SharedData.activeNotes[editingPosition] = it
                    else SharedData.activeNotes.add(it)
                    showTab(rvNotesTab)
                }
            }
            REQ_EVENT -> {
                val event = data?.getSerializableExtra("eventData") as? Event
                event?.let {
                    if (editingPosition != -1) SharedData.activeEvents[editingPosition] = it
                    else SharedData.activeEvents.add(it)
                    showTab(rvEventsTab)
                }
            }
        }
        refreshAllData()
    }

    private fun expandMenu() {
        isExpanded = true
        val radius = 280f
        val wrappers = listOf(btnTaskWrapper, btnNoteWrapper, btnEventWrapper)
        val angles = listOf(90f, 135f, 180f)
        btnAdd.animate().rotation(45f).setDuration(300).start()
        wrappers.forEachIndexed { idx, wrapper ->
            wrapper.visibility = View.VISIBLE
            wrapper.alpha = 0f
            val rad = Math.toRadians(angles[idx].toDouble())
            wrapper.animate()
                .translationX(radius * cos(rad).toFloat())
                .translationY(-radius * sin(rad).toFloat())
                .alpha(1f).setDuration(300L).setInterpolator(OvershootInterpolator()).start()
        }
    }

    private fun collapseMenu() {
        isExpanded = false
        val wrappers = listOf(btnTaskWrapper, btnNoteWrapper, btnEventWrapper)
        btnAdd.animate().rotation(0f).setDuration(300).start()
        wrappers.forEach { wrapper ->
            wrapper.animate().translationX(0f).translationY(0f).alpha(0f).setDuration(250L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { wrapper.visibility = View.INVISIBLE }.start()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()
    }
}