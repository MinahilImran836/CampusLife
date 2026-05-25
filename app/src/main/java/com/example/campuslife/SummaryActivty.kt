package com.example.campuslife

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.campuslife.data.AppDatabase
import com.example.campuslife.data.Task
import com.example.campuslife.data.Event
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val campusDao = AppDatabase.getDatabase(this).campusDao()

        lifecycleScope.launch {
            val tasks = campusDao.getAllTasks().first()
            val events = campusDao.getAllEvents().first()

            // --- 1. QUANTITATIVE ENGINE (Local) ---
            val completed = tasks.filter { it.isCompleted }
            val percent = if (tasks.isNotEmpty()) (completed.size.toFloat() / tasks.size * 100).toInt() else 0

            findViewById<ProgressBar>(R.id.summaryChart).progress = percent
            findViewById<TextView>(R.id.txtBigPercentage).text = "$percent%"
            findViewById<TextView>(R.id.txtStatDetails).text = "Tasks Completed: ${completed.size}/${tasks.size}"

            // Populate metric pills immediately
            findViewById<TextView>(R.id.txtFocusMetric).text = "%.1f".format(tasks.size.toFloat())
            findViewById<TextView>(R.id.txtPunctualityMetric).text = "$percent%"
            findViewById<TextView>(R.id.txtLocationMetric).text = "${events.filter { it.isCompleted }.size}"

            // --- 2. ANALYTICAL ENGINE (AI) ---
            fetchActualAiSummary(tasks, events)
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    private suspend fun fetchActualAiSummary(tasks: List<Task>, events: List<Event>) {
        val total = tasks.size
        val done = tasks.count { it.isCompleted }
        val late = tasks.count { it.isCompleted && it.completedAt > it.timeInMillis }
        val categories = tasks.groupBy { it.category }.mapValues { it.value.size }

        val prompt = """
            You are a senior productivity coach. 
            Data: Total Tasks: $total, Completed: $done, Late: $late, Category Distribution: $categories.
            
            Perform a professional diagnostic. Return EXACTLY 3 blocks separated by '|||'.
            Format: [Archetype] ::: [Analysis] ||| [Bottleneck] ::: [Analysis] ||| [Strategy] ::: [Advice]
            - Archetype: Identify user persona based on late/done ratio.
            - Bottleneck: Which category is being neglected?
            - Strategy: One specific data-driven tip to reduce late tasks.
        """.trimIndent()

        try {
            val generativeModel = GenerativeModel("gemini-2.5-flash", "YOUR_API_KEY")
            val response = generativeModel.generateContent(prompt).text ?: ""
            val blocks = response.split("|||")

            if (blocks.size >= 3) {
                // Parse and update UI
                updateUiBlock(R.id.txtAiPersona, blocks[0])
                updateUiBlock(R.id.txtAiPunctuality, blocks[1])
                updateUiBlock(R.id.txtAiLocations, blocks[2])
            }
        } catch (e: Exception) {
            findViewById<TextView>(R.id.txtAiPersona).text = "Persona analysis pending."
            findViewById<TextView>(R.id.txtAiPunctuality).text = "Category bottlenecks calculating."
            findViewById<TextView>(R.id.txtAiLocations).text = "Strategic forecasting offline."
        }
    }

    private fun updateUiBlock(textViewId: Int, block: String) {
        val parts = block.split(":::")
        if (parts.size >= 2) {
            findViewById<TextView>(textViewId).text = parts[1].trim()
        } else {
            findViewById<TextView>(textViewId).text = block.trim()
        }
    }
}