package com.example.campuslife.adapters

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campuslife.R
import com.example.campuslife.data.Event

class EventAdapter(
    private val events: MutableList<Event>,
    private val listener: OnEventClickListener
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    interface OnEventClickListener {
        fun onEventClick(event: Event, position: Int)
        fun onEventLongClick(event: Event, position: Int)
        fun onEventToggled(event: Event, wasLate: Boolean)
    }

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle: TextView = itemView.findViewById(R.id.txtEventTitle)
        val txtDateTime: TextView = itemView.findViewById(R.id.txtEventDateTime)
        val txtLocation: TextView = itemView.findViewById(R.id.txtEventLocation)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        holder.txtTitle.text = event.title
        holder.txtDateTime.text = "${event.date} • ${event.time}"
        holder.txtLocation.text = event.location
        holder.checkBox.isChecked = event.isCompleted

        holder.itemView.translationX = 0f

        val thirtyMinutesInMillis = 30 * 60 * 1000L
        val isEventOverdue = !event.isCompleted && (System.currentTimeMillis() > (event.timeInMillis + thirtyMinutesInMillis))

        if (event.isCompleted) {
            holder.txtTitle.paintFlags = holder.txtTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.itemView.alpha = 0.5f
            holder.txtTitle.setTextColor(Color.GRAY)
            holder.txtDateTime.setTextColor(Color.GRAY)
        } else {
            holder.txtTitle.paintFlags = holder.txtTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.itemView.alpha = 1.0f

            if (isEventOverdue) {
                holder.txtTitle.setTextColor(Color.RED)
                holder.txtDateTime.setTextColor(Color.RED)
            } else {
                holder.txtTitle.setTextColor(Color.BLACK)
                holder.txtDateTime.setTextColor(Color.parseColor("#888888"))
            }
        }

        holder.checkBox.setOnClickListener {
            val isChecked = holder.checkBox.isChecked
            event.isCompleted = isChecked

            val wasLate = System.currentTimeMillis() > (event.timeInMillis + thirtyMinutesInMillis)

            if (isChecked && wasLate) {
                // Event is late -> Run sliding out animation wrapper
                holder.itemView.animate()
                    .alpha(0f)
                    .translationX(100f)
                    .setDuration(500)
                    .withEndAction {
                        // Safely pull the adapter position to avoid indexing mismatched items
                        val currentPos = holder.adapterPosition
                        if (currentPos != RecyclerView.NO_POSITION && currentPos < events.size) {
                            events.removeAt(currentPos)
                            notifyItemRemoved(currentPos)
                            listener.onEventToggled(event, true)
                        }
                    }.start()
            } else {
                // Event is within timeframe -> Reload list positions cleanly
                notifyItemChanged(holder.adapterPosition)
                listener.onEventToggled(event, false)
            }
        }

        holder.itemView.setOnClickListener { listener.onEventClick(event, holder.adapterPosition) }
        holder.itemView.setOnLongClickListener {
            listener.onEventLongClick(event, holder.adapterPosition)
            true
        }
    }

    override fun getItemCount(): Int = events.size
}