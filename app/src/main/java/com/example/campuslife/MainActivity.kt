package com.example.campuslife

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campuslife.adapters.EventAdapter
import com.example.campuslife.adapters.NoteAdapter
import com.example.campuslife.adapters.TaskAdapter
import com.example.campuslife.data.AppDatabase
import com.example.campuslife.data.CampusDao
import com.example.campuslife.data.Event
import com.example.campuslife.data.Note
import com.example.campuslife.data.Task
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private lateinit var categoryScrollView: HorizontalScrollView
    private lateinit var categoryPillsLayout: LinearLayout
    private var selectedCategoryFilter = "All"

    private var rawTaskList = listOf<Task>()

    private lateinit var database: AppDatabase
    private lateinit var campusDao: CampusDao

    private var activeTasks = mutableListOf<Task>()
    private var activeNotes = mutableListOf<Note>()
    private var activeEvents = mutableListOf<Event>()

    private var isNotesTabActive = false

    private val REQ_ADD_TASK = 2001
    private val REQ_EDIT_TASK = 2002
    private val REQ_NOTE = 3001
    private val REQ_EVENT = 4001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)
        campusDao = database.campusDao()

        NotificationHelper.createNotificationChannel(this)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        btnMenu = findViewById(R.id.btnMenu)
        navigationView.itemIconTintList = null

        tabTasks = findViewById(R.id.tabTasks)
        tabNotes = findViewById(R.id.tabNotes)
        tabEvents = findViewById(R.id.tabEvents)
        txtEmptyPlaceholder = findViewById(R.id.txtEmptyPlaceholder)

        categoryScrollView = findViewById(R.id.categoryScrollView)
        categoryPillsLayout = findViewById(R.id.categoryPillsLayout)

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

        todayAdapter = TaskAdapter(activeTasks, this)
        taskTabAdapter = TaskAdapter(activeTasks, this)
        notesAdapter = NoteAdapter(activeNotes, this)
        eventAdapter = EventAdapter(activeEvents, this)

        rvMainToday.adapter = todayAdapter
        rvTasksTab.adapter = taskTabAdapter
        rvNotesTab.adapter = notesAdapter
        rvEventsTab.adapter = eventAdapter

        setupListeners()
        setupDrawer()
        observeDatabase()
        cleanupOldTasks()

        categoryScrollView.visibility = View.VISIBLE

        window.decorView.postDelayed({ runFeatureTutorial() }, 600)
    }

    private fun observeDatabase() {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        lifecycleScope.launch {
            campusDao.getAllTasks().collect { tasks ->
                rawTaskList = tasks.filter { task ->
                    !task.isCompleted || (task.timeInMillis >= todayStart)
                }
                generateCategoryPills()
                applyTaskFilter()
            }
        }

        lifecycleScope.launch {
            campusDao.getAllEvents().collect { events ->
                val filteredEvents = events.filter { event ->
                    !event.isCompleted || (event.timeInMillis >= todayStart)
                }
                activeEvents.clear()
                activeEvents.addAll(filteredEvents)
                eventAdapter.notifyDataSetChanged()
                updatePlaceholderVisibility()
            }
        }

        lifecycleScope.launch {
            campusDao.getAllNotes().collect { notes ->
                // Clean structural reactive filtering without manual fetch drops
                val validNotes = notes.filter { !it.isDeleted }
                activeNotes.clear()
                activeNotes.addAll(validNotes)
                if (isNotesTabActive) {
                    notesAdapter.notifyDataSetChanged()
                    updatePlaceholderVisibility()
                }
            }
        }
    }

    private fun updateNotesUi(notes: List<Note>) {
        activeNotes.clear()
        activeNotes.addAll(notes)
        notesAdapter.notifyDataSetChanged()
        updatePlaceholderVisibility()
    }

    private fun generateCategoryPills() {
        categoryPillsLayout.removeAllViews()
        val baselineCategories = arrayListOf("All", "Study", "Work", "Personal", "Fitness")
        val customCategories = rawTaskList.map { it.category }
            .distinct()
            .filter { it.isNotEmpty() && !baselineCategories.contains(it) }

        val fullCategoriesList = mutableListOf<String>().apply {
            addAll(baselineCategories)
            addAll(customCategories)
        }

        if (!fullCategoriesList.contains(selectedCategoryFilter)) {
            selectedCategoryFilter = "All"
        }

        fullCategoriesList.forEach { category ->
            val pillView = layoutInflater.inflate(R.layout.item_category_pill, categoryPillsLayout, false) as Button
            pillView.text = category
            if (category == selectedCategoryFilter) {
                pillView.alpha = 1.0f
                pillView.scaleX = 1.03f
                pillView.scaleY = 1.03f
            } else {
                pillView.alpha = 0.6f
            }
            pillView.setOnClickListener {
                selectedCategoryFilter = category
                generateCategoryPills()
                applyTaskFilter()
            }
            categoryPillsLayout.addView(pillView)
        }
    }

    private fun applyTaskFilter() {
        val filteredList = if (selectedCategoryFilter == "All") {
            rawTaskList
        } else {
            rawTaskList.filter { it.category == selectedCategoryFilter }
        }
        activeTasks.clear()
        activeTasks.addAll(filteredList)
        todayAdapter.notifyDataSetChanged()
        taskTabAdapter.notifyDataSetChanged()
        updatePlaceholderVisibility()
    }

    private fun cleanupOldTasks() {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        lifecycleScope.launch { campusDao.deleteOldCompletedTasks(todayStart) }
    }

    override fun onTaskClick(task: Task, position: Int) {
        val intent = Intent(this, TaskFormActivity::class.java).apply { putExtra("taskData", task) }
        startActivityForResult(intent, REQ_EDIT_TASK)
    }

    override fun onTaskLongClick(task: Task, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Move Task to Trash?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    val updatedTask = task.copy(isDeleted = true)
                    campusDao.updateTask(updatedTask)
                    Toast.makeText(this@MainActivity, "Moved to Trash", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Cancel", null).show()
    }

    override fun onTaskToggled(task: Task, wasLate: Boolean) {
        lifecycleScope.launch {
            if (task.isCompleted) {
                // Set timestamp right at completion event boundary
                task.completedAt = System.currentTimeMillis()
                if (wasLate) {
                    task.isArchived = true
                    Toast.makeText(this@MainActivity, "Task completed late! ⏰ Saved to summary.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Reset parameters gracefully if toggled off
                task.completedAt = 0L
                task.isArchived = false
            }

            // Explicitly call updateTask to mutate the current entry inside SQLite safely
            campusDao.updateTask(task)
        }
    }

    override fun onNoteClick(note: Note, position: Int) {
        val intent = Intent(this, NoteFormActivity::class.java).apply { putExtra("noteData", note) }
        startActivityForResult(intent, REQ_NOTE)
    }

    override fun onNoteLongClick(note: Note, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Move Note to Trash?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    val updatedNote = note.copy(isDeleted = true)
                    campusDao.updateNote(updatedNote)
                    Toast.makeText(this@MainActivity, "Moved to Trash", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Cancel", null).show()
    }

    override fun onEventClick(event: Event, position: Int) {
        val intent = Intent(this, EventFormActivity::class.java).apply { putExtra("eventData", event) }
        startActivityForResult(intent, REQ_EVENT)
    }

    override fun onEventLongClick(event: Event, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Move Event to Trash?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    val updatedEvent = event.copy(isDeleted = true)
                    campusDao.updateEvent(updatedEvent)
                    Toast.makeText(this@MainActivity, "Moved to Trash", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Cancel", null).show()
    }

    override fun onEventToggled(event: Event, wasLate: Boolean) {
        lifecycleScope.launch {
            if (wasLate && event.isCompleted) {
                val updatedEvent = event.copy(isArchived = true)
                campusDao.updateEvent(updatedEvent) // Adjusted to use clean update interfaces
                Toast.makeText(this@MainActivity, "Event completed late! ⏰ Saved to summary.", Toast.LENGTH_SHORT).show()
            } else {
                campusDao.updateEvent(event)
                if (event.isCompleted) {
                    Toast.makeText(this@MainActivity, "Event Attended!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupDrawer() {
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_summary -> startActivity(Intent(this, SummaryActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_trash -> startActivity(Intent(this, TrashActivity::class.java))
                R.id.nav_chat -> startActivity(Intent(this, ChatActivity::class.java))
                R.id.nav_login -> startActivity(Intent(this, AuthActivity::class.java))
                R.id.nav_community -> startActivity(Intent(this, CommunityFeedActivity::class.java))

                R.id.nav_logout -> {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    Toast.makeText(this, "Logged out safely! See ya! 👋", Toast.LENGTH_SHORT).show()
                    navigationView.menu.clear()
                    navigationView.inflateMenu(R.menu.drawer_menu) // Adjust filename if different
                    navigationView.itemIconTintList = null
                    updateDrawerMenuVisibility()
                }

                // ⚠️ NEW: Permanent Account Deletion Engine
                R.id.nav_delete_account -> {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

                    if (currentUser != null) {
                        AlertDialog.Builder(this)
                            .setTitle("Delete Your Account?")
                            .setMessage("This will permanently remove your cloud credentials. Your offline local tasks and notes will remain completely untouched. Proceed?")
                            .setPositiveButton("Delete Forever") { _, _ ->
                                currentUser.delete()
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(this, "Account deleted successfully. Goodbye! 🍃", Toast.LENGTH_SHORT).show()

                                            // Force clear and re-inflate menu to completely refresh layout instantly
                                            navigationView.menu.clear()
                                            navigationView.inflateMenu(R.menu.drawer_menu) // Adjust filename if different
                                            navigationView.itemIconTintList = null
                                            updateDrawerMenuVisibility()
                                        } else {
                                            // Log failure details to Logcat console for quick inspection
                                            Log.e("CampusLifeAuth", "Account deletion encountered a problem", task.exception)
                                            Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateDrawerMenuVisibility()
    }

    // 2. Add this helper function at the bottom of your MainActivity class
    private fun updateDrawerMenuVisibility() {
        val menu = navigationView.menu
        val loginItem = menu.findItem(R.id.nav_login)
        val communityItem = menu.findItem(R.id.nav_community)
        val logoutItem = menu.findItem(R.id.nav_logout)
        val deleteAccountItem = menu.findItem(R.id.nav_delete_account) // ✨ Track delete item pointer

        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // User is logged in: Hide login, show authenticated features
            loginItem?.isVisible = false
            communityItem?.isVisible = true
            logoutItem?.isVisible = true
            deleteAccountItem?.isVisible = true // ✨ Show it!
        } else {
            // User is a guest: Show login, hide authenticated features
            loginItem?.isVisible = true
            communityItem?.isVisible = false
            logoutItem?.isVisible = false
            deleteAccountItem?.isVisible = false // ✨ Hide it!
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
            rvTasksTab.visibility == View.VISIBLE -> activeTasks.isEmpty()
            rvNotesTab.visibility == View.VISIBLE -> activeNotes.isEmpty()
            rvEventsTab.visibility == View.VISIBLE -> activeEvents.isEmpty()
            else -> activeTasks.isEmpty()
        }
        txtEmptyPlaceholder.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showTab(viewToShow: RecyclerView) {
        rvMainToday.visibility = View.GONE
        rvTasksTab.visibility = View.GONE
        rvNotesTab.visibility = View.GONE
        rvEventsTab.visibility = View.GONE
        viewToShow.visibility = View.VISIBLE

        if (viewToShow == rvNotesTab) {
            isNotesTabActive = true
            categoryScrollView.visibility = View.GONE
            // Let the active database observer collection interface refresh the layout seamlessly
            notesAdapter.notifyDataSetChanged()
        } else {
            isNotesTabActive = false
            if (viewToShow == rvTasksTab || viewToShow == rvMainToday) {
                categoryScrollView.visibility = View.VISIBLE
            } else {
                categoryScrollView.visibility = View.GONE
            }
        }
        updatePlaceholderVisibility()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        lifecycleScope.launch(Dispatchers.IO) {
            when (requestCode) {
                REQ_ADD_TASK -> {
                    val task = data?.getSerializableExtra("taskData") as? Task
                    task?.let {
                        campusDao.insertTask(it)
                        NotificationHelper.scheduleSpecificReminder(this@MainActivity, it.title, it.timeInMillis)
                    }
                }
                REQ_EDIT_TASK -> {
                    val task = data?.getSerializableExtra("taskData") as? Task
                    task?.let {
                        campusDao.updateTask(it)
                        NotificationHelper.scheduleSpecificReminder(this@MainActivity, it.title, it.timeInMillis)
                    }
                }
                REQ_NOTE -> {
                    val note = data?.getSerializableExtra("noteData") as? Note
                    note?.let {
                        if (it.id == 0) campusDao.insertNote(it) else campusDao.updateNote(it)
                    }
                }
                REQ_EVENT -> {
                    val event = data?.getSerializableExtra("eventData") as? Event
                    event?.let {
                        if (it.id == 0) campusDao.insertEvent(it) else campusDao.updateEvent(it)
                        NotificationHelper.scheduleSpecificReminder(this@MainActivity, it.title, it.timeInMillis)
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()
    }

    private fun runFeatureTutorial() {
        val prefs = getSharedPreferences("CampusLifePrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("tutorial_done", false)) return

        val customFont = ResourcesCompat.getFont(this, R.font.cute_apricot)
        val cutePink = Color.parseColor("#FFD4D4")
        val cuteGreen = Color.parseColor("#AACB73")

        TapTargetSequence(this)
            .targets(
                TapTarget.forView(btnMenu, "Open Menu ☰", "Check your Progress here!")
                    .outerCircleColorInt(cutePink).outerCircleAlpha(0.92f)
                    .targetCircleColorInt(Color.WHITE)
                    .titleTextSize(28).descriptionTextSize(18).textTypeface(customFont)
                    .titleTextColorInt(Color.BLACK).descriptionTextColorInt(Color.DKGRAY)
                    .targetRadius(40).transparentTarget(true).cancelable(false),

                TapTarget.forView(tabTasks, "Dashboard Tabs", "Switch between Tasks, Notes, and Events.")
                    .outerCircleColorInt(cuteGreen).outerCircleAlpha(0.92f)
                    .targetCircleColorInt(Color.WHITE)
                    .titleTextSize(28).descriptionTextSize(18).textTypeface(customFont)
                    .titleTextColorInt(Color.WHITE).descriptionTextColorInt(Color.WHITE)
                    .targetRadius(50).transparentTarget(true).cancelable(false),

                TapTarget.forView(btnAdd, "The Plus Button", "Tap this to reveal your tools!")
                    .outerCircleColorInt(cutePink).outerCircleAlpha(0.92f)
                    .targetCircleColorInt(Color.WHITE)
                    .titleTextSize(28).descriptionTextSize(18).textTypeface(customFont)
                    .titleTextColorInt(Color.BLACK).descriptionTextColorInt(Color.DKGRAY)
                    .targetRadius(60).transparentTarget(true).id(100).cancelable(false)
            )
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {
                    if (lastTarget?.id() == 100) {
                        expandMenu()
                        btnAdd.postDelayed({ showSubMenuTutorial() }, 500)
                    }
                }
                override fun onSequenceFinish() {}
                override fun onSequenceCanceled(lastTarget: TapTarget?) {}
            }).start()
    }

    private fun showSubMenuTutorial() {
        val customFont = ResourcesCompat.getFont(this, R.font.cute_apricot)
        val cutePink = Color.parseColor("#FFD4D4")
        val cuteGreen = Color.parseColor("#AACB73")

        TapTargetSequence(this)
            .targets(
                TapTarget.forView(btnTaskMini, "Add Task", "Quickly add a new assignment.")
                    .outerCircleColorInt(cuteGreen).outerCircleAlpha(0.92f)
                    .targetCircleColorInt(Color.WHITE)
                    .titleTextSize(28).descriptionTextSize(18).textTypeface(customFont)
                    .titleTextColorInt(Color.WHITE).descriptionTextColorInt(Color.WHITE)
                    .targetRadius(45).transparentTarget(true).cancelable(false),

                TapTarget.forView(btnNoteMini, "New Note", "Keep track of your thoughts.")
                    .outerCircleColorInt(cutePink).outerCircleAlpha(0.92f)
                    .targetCircleColorInt(Color.WHITE)
                    .titleTextSize(28).descriptionTextSize(18).textTypeface(customFont)
                    .titleTextColorInt(Color.BLACK).descriptionTextColorInt(Color.DKGRAY)
                    .targetRadius(45).transparentTarget(true).cancelable(false),

                TapTarget.forView(btnEventMini, "Create Event", "Plan your campus events.")
                    .outerCircleColorInt(cuteGreen).outerCircleAlpha(0.92f)
                    .targetCircleColorInt(Color.WHITE)
                    .titleTextSize(28).descriptionTextSize(18).textTypeface(customFont)
                    .titleTextColorInt(Color.WHITE).descriptionTextColorInt(Color.WHITE)
                    .targetRadius(45).transparentTarget(true).cancelable(false)
            )
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    collapseMenu()
                    val prefs = getSharedPreferences("CampusLifePrefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("tutorial_done", true).apply()
                    Toast.makeText(this@MainActivity, "Ready to Organize!", Toast.LENGTH_SHORT).show()
                }
                override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                override fun onSequenceCanceled(lastTarget: TapTarget?) {}
            }).start()
    }
}