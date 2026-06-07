package com.example.campuslife

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.campuslife.data.AppDatabase
import com.example.campuslife.data.DailySummary
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val campusDao = AppDatabase.getDatabase(this).campusDao()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            val tasks = campusDao.getAllTasks().first()
            val events = campusDao.getAllEvents().first()

            // Calculate stats
            val completed = tasks.filter { it.isCompleted }
            val onTime = completed.filter { it.completedAt <= it.timeInMillis }
            val percent = if (tasks.isNotEmpty()) (completed.size.toFloat() / tasks.size * 100).toInt() else 0
            val topCategory = tasks.groupBy { it.category }.maxByOrNull { it.value.size }?.key ?: "N/A"
            val attendedEvents = events.filter { it.isCompleted }.size

            // UI Update (Static Stats)
            findViewById<ProgressBar>(R.id.summaryChart).progress = percent
            findViewById<TextView>(R.id.txtBigPercentage).text = "$percent%"
            findViewById<TextView>(R.id.txtStatDetails).text = "Tasks Completed: ${completed.size}/${tasks.size}"
            findViewById<TextView>(R.id.txtFocusMetric).text = "${tasks.size}"
            findViewById<TextView>(R.id.txtPunctualityMetric).text = "$percent%"
            findViewById<TextView>(R.id.txtLocationMetric).text = "$attendedEvents"

            // Logic: Fetch existing or generate new
            val existingSummary = campusDao.getSummaryByDate(today) // You need this function in DAO

            if (existingSummary != null && !existingSummary.aiSummary.isNullOrEmpty()) {
                // Use cached AI data
                parseAndDisplayAi(existingSummary.aiSummary!!)
            } else {
                // Generate new AI data and save it
                val history = campusDao.getSummaryHistory().first()
                val newSummary = DailySummary(
                    date = today,
                    totalTasks = tasks.size,
                    completedTasks = completed.size,
                    onTimeTasks = onTime.size,
                    topCategory = topCategory,
                    eventsAttended = attendedEvents
                )

                val aiResponse = generateAiSummary(history)
                val summaryWithAi = newSummary.copy(aiSummary = aiResponse)
                campusDao.insertDailySummary(summaryWithAi)
                parseAndDisplayAi(aiResponse)
            }
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    private suspend fun generateAiSummary(history: List<DailySummary>): String {
        val prompt = "Analyze this history: $history. Return 3 blocks with '|||' format: Archetype:::Analysis ||| Bottleneck:::Analysis ||| Strategy:::Advice"
        val generativeModel = GenerativeModel("gemini-2.5-flash", "")
        return generativeModel.generateContent(prompt).text ?: "Analysis unavailable."
    }

    private fun parseAndDisplayAi(response: String) {
        val blocks = response.split("|||")
        if (blocks.size >= 3) {
            updateUiBlock(R.id.txtAiPersona, blocks[0])
            updateUiBlock(R.id.txtAiPunctuality, blocks[1])
            updateUiBlock(R.id.txtAiLocations, blocks[2])
        }
    }

    private fun updateUiBlock(textViewId: Int, block: String) {
        val parts = block.split(":::")
        findViewById<TextView>(textViewId).text = if (parts.size >= 2) parts[1].trim() else block.trim()
    }
}