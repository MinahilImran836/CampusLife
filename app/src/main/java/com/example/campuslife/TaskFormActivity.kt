package com.example.campuslife

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.campuslife.adapters.SpinnerAdapter
import com.example.campuslife.data.AppDatabase
import com.example.campuslife.data.CampusDao
import com.example.campuslife.data.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class TaskFormActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var btnCategorySelector: TextView
    private lateinit var switchImportant: androidx.appcompat.widget.SwitchCompat
    private lateinit var btnSave: Button

    private var selectedCalendar = Calendar.getInstance()

    private val baselineCategories = listOf("Study", "Work", "Personal", "Fitness")
    private val categories = arrayListOf<String>()
    private var selectedCategory = "Study"

    private var existingTask: Task? = null

    private lateinit var database: AppDatabase
    private lateinit var campusDao: CampusDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This handles the closing of the dialog when clicking outside
        setFinishOnTouchOutside(true)

        setContentView(R.layout.activity_task_form)

        database = AppDatabase.getDatabase(this)
        campusDao = database.campusDao()

        etTitle = findViewById(R.id.edtTaskTitle)
        etDescription = findViewById(R.id.edtTaskDescription)
        etDate = findViewById(R.id.edtTaskDate)
        etTime = findViewById(R.id.edtTaskTime)
        btnCategorySelector = findViewById(R.id.btnCategorySelector)
        switchImportant = findViewById(R.id.switchImportant)
        btnSave = findViewById(R.id.btnSaveTask)

        existingTask = intent.getSerializableExtra("taskData") as? Task
        existingTask?.let {
            etTitle.setText(it.title)
            etDescription.setText(it.description)
            etDate.setText(it.date)
            etTime.setText(it.time)
            btnCategorySelector.text = it.category
            selectedCategory = it.category
            switchImportant.isChecked = it.isImportant
            selectedCalendar.timeInMillis = it.timeInMillis
            btnSave.text = "Update Task"
        }

        loadDatabaseCategories()
        setupListeners()
    }

    private fun loadDatabaseCategories() {
        lifecycleScope.launch {
            val allTasks = campusDao.getAllTasks().first()
            val savedCustomCategories = allTasks.map { it.category }
                .distinct()
                .filter { it.isNotEmpty() && !baselineCategories.contains(it) }

            categories.clear()
            categories.addAll(baselineCategories)
            categories.addAll(savedCustomCategories)
            categories.add("+ Add New")

            if (existingTask != null) {
                btnCategorySelector.text = selectedCategory
            }
        }
    }

    private fun setupListeners() {
        etDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedCalendar.set(Calendar.YEAR, y)
                selectedCalendar.set(Calendar.MONTH, m)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, d)
                etDate.setText("$d/${m + 1}/$y")
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        etTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, h)
                selectedCalendar.set(Calendar.MINUTE, m)
                selectedCalendar.set(Calendar.SECOND, 0)

                val ampm = if (h < 12) "AM" else "PM"
                val hour = if (h % 12 == 0) 12 else h % 12
                etTime.setText(String.format("%02d:%02d %s", hour, m, ampm))
            }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), false).show()
        }

        btnCategorySelector.setOnClickListener { showCutesyMenu(it) }
        btnSave.setOnClickListener { validateAndSave() }
    }

    private fun showCutesyMenu(anchor: View) {
        val popup = ListPopupWindow(this)
        val adapter = SpinnerAdapter(this, categories)
        popup.setAdapter(adapter)
        popup.anchorView = anchor
        popup.setBackgroundDrawable(resources.getDrawable(R.drawable.bg_dropdown_menu, theme))

        popup.setOnItemClickListener { _, _, pos, _ ->
            if (categories[pos] == "+ Add New") {
                showAddNewCategoryDialog()
            } else {
                selectedCategory = categories[pos]
                btnCategorySelector.text = selectedCategory
            }
            popup.dismiss()
        }

        popup.show()

        popup.listView?.setOnItemLongClickListener { _, _, pos, _ ->
            val clickedCategory = categories[pos]

            if (clickedCategory == "+ Add New") return@setOnItemLongClickListener true

            if (baselineCategories.contains(clickedCategory)) {
                Toast.makeText(this, "🌸 '$clickedCategory' is a built-in category!", Toast.LENGTH_SHORT).show()
                return@setOnItemLongClickListener true
            }

            AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete '$clickedCategory'? Existing tasks under this label will reset to 'Personal'.")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        val allTasks = campusDao.getAllTasks().first()
                        allTasks.forEach { task ->
                            if (task.category == clickedCategory) {
                                val fallbackTask = task.copy(category = "Personal")
                                campusDao.updateTask(fallbackTask)
                            }
                        }

                        categories.removeAt(pos)
                        adapter.notifyDataSetChanged()

                        if (selectedCategory == clickedCategory) {
                            selectedCategory = "Personal"
                            btnCategorySelector.text = "Personal"
                        }

                        Toast.makeText(this@TaskFormActivity, "Deleted '$clickedCategory'", Toast.LENGTH_SHORT).show()
                        popup.dismiss()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()

            true
        }
    }

    private fun showAddNewCategoryDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setContentView(R.layout.dialog_new_category)
        val et = dialog.findViewById<EditText>(R.id.edtNewCategoryName)
        dialog.findViewById<Button>(R.id.btnDialogAdd).setOnClickListener {
            val inputName = et.text.toString().trim()
            if (inputName.isNotEmpty()) {
                if (categories.contains(inputName)) {
                    Toast.makeText(this, "Category already exists!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val insertIndex = if (categories.size > 0) categories.size - 1 else 0
                categories.add(insertIndex, inputName)

                selectedCategory = inputName
                btnCategorySelector.text = selectedCategory
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun validateAndSave() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) { etTitle.error = "Required!"; return }

        val task = Task(
            id = existingTask?.id ?: 0,
            title = title,
            category = selectedCategory,
            date = etDate.text.toString(),
            time = etTime.text.toString(),
            description = etDescription.text.toString(),
            isImportant = switchImportant.isChecked,
            timeInMillis = selectedCalendar.timeInMillis,
            isCompleted = existingTask?.isCompleted ?: false,
            isDeleted = existingTask?.isDeleted ?: false,
            isArchived = existingTask?.isArchived ?: false,
            completedAt = existingTask?.completedAt ?: 0L
        )

        val intent = Intent().apply { putExtra("taskData", task) }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}