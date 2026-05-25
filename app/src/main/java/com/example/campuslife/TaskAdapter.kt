package com.example.campuslife.adapters

import android.graphics.Color
import android.graphics.Paint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campuslife.R
import com.example.campuslife.data.Task

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val listener: OnTaskClickListener
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    interface OnTaskClickListener {
        fun onTaskClick(task: Task, position: Int)
        fun onTaskLongClick(task: Task, position: Int)
        fun onTaskToggled(task: Task, wasLate: Boolean)
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.txtTaskTitle)
        val description: TextView = itemView.findViewById(R.id.txtTaskDesc)
        val categoryBadge: TextView = itemView.findViewById(R.id.txtCategoryBadge)
        val dateTime: TextView = itemView.findViewById(R.id.txtDateTime)
        val priorityStar: TextView = itemView.findViewById(R.id.txtPriorityStar)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val t = tasks[position]

        holder.title.text = t.title
        holder.description.text = t.description
        holder.categoryBadge.text = t.category
        holder.dateTime.text = "${t.date} | ${t.time}"

        holder.description.maxLines = 2
        holder.description.ellipsize = TextUtils.TruncateAt.END
        holder.description.setOnClickListener {
            holder.description.maxLines = if (holder.description.maxLines == 2) Int.MAX_VALUE else 2
        }

        holder.itemView.translationX = 0f

        val thirtyMinutesInMillis = 30 * 60 * 1000L
        val isTaskOverdue = !t.isCompleted && (System.currentTimeMillis() > (t.timeInMillis + thirtyMinutesInMillis))

        holder.checkBox.isChecked = t.isCompleted

        if (t.isCompleted) {
            holder.title.paintFlags = holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.itemView.alpha = 0.5f
            holder.title.setTextColor(Color.GRAY)
            holder.dateTime.setTextColor(Color.GRAY)
        } else {
            holder.title.paintFlags = holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.itemView.alpha = 1.0f

            if (isTaskOverdue) {
                holder.title.setTextColor(Color.RED)
                holder.dateTime.setTextColor(Color.RED)
            } else {
                holder.title.setTextColor(Color.BLACK)
                holder.dateTime.setTextColor(Color.parseColor("#888888"))
            }
        }

        holder.checkBox.setOnClickListener {
            val isChecked = holder.checkBox.isChecked
            t.isCompleted = isChecked

            // Evaluate if it is past deadline + 30 mins grace period right now
            val wasLate = System.currentTimeMillis() > (t.timeInMillis + thirtyMinutesInMillis)

            if (isChecked && wasLate) {
                // Task is late -> Run the slide away translation animation
                holder.itemView.animate()
                    .alpha(0f)
                    .translationX(100f)
                    .setDuration(500)
                    .withEndAction {
                        // Safely pull the adapter position to avoid indexing mismatched items
                        val currentPos = holder.adapterPosition
                        if (currentPos != RecyclerView.NO_POSITION && currentPos < tasks.size) {
                            tasks.removeAt(currentPos)
                            notifyItemRemoved(currentPos)
                            listener.onTaskToggled(t, true)
                        }
                    }.start()
            } else {
                // Task is on time -> Simply fall to bottom through notifyItemChanged/Room collection
                notifyItemChanged(holder.adapterPosition)
                listener.onTaskToggled(t, false)
            }
        }

        holder.priorityStar.visibility = if (t.isImportant) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { listener.onTaskClick(t, holder.adapterPosition) }
        holder.itemView.setOnLongClickListener {
            listener.onTaskLongClick(t, holder.adapterPosition)
            true
        }
    }
}