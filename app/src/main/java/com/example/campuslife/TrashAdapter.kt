package com.example.campuslife.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campuslife.model.Task
import com.example.campuslife.model.Note
import com.example.campuslife.model.Event

class TrashAdapter(
    private val items: List<Any>,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<TrashAdapter.TrashViewHolder>() {

    val selectedItems = mutableSetOf<Any>()
    var isSelectionMode = false

    class TrashViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(android.R.id.text1)
        val subtitle: TextView = view.findViewById(android.R.id.text2)
        val root: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return TrashViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        val item = items[position]

        when (item) {
            is Task -> { holder.title.text = "📌 [TASK] ${item.title}"; holder.subtitle.text = item.date }
            is Note -> { holder.title.text = "📝 [NOTE] ${item.title}"; holder.subtitle.text = item.content }
            is Event -> { holder.title.text = "📅 [EVENT] ${item.title}"; holder.subtitle.text = "${item.date} ${item.time}" }
        }

        // 2. Visual Feedback for selection
        holder.root.setBackgroundColor(if (selectedItems.contains(item)) Color.parseColor("#FFD4D4") else Color.TRANSPARENT)

        // 3. Click Logic
        holder.root.setOnLongClickListener {
            isSelectionMode = true
            toggleSelection(item)
            true
        }

        holder.root.setOnClickListener {
            if (isSelectionMode) toggleSelection(item)
        }
    }

    private fun toggleSelection(item: Any) {
        if (selectedItems.contains(item)) selectedItems.remove(item)
        else selectedItems.add(item)

        if (selectedItems.isEmpty()) isSelectionMode = false
        notifyDataSetChanged()
        onSelectionChanged(selectedItems.size)
    }

    override fun getItemCount() = items.size
}