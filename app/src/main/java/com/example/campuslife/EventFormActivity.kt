package com.example.campuslife

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.campuslife.data.Event
import java.util.*

class EventFormActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etLocation: EditText
    private lateinit var btnPickDate: EditText
    private lateinit var btnPickTime: EditText
    private lateinit var btnSave: Button
    private var selectedCalendar = Calendar.getInstance()
    private var existingEvent: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // THIS IS THE KEY:
        // This makes the activity close automatically when a touch is detected
        // outside the bounds of the floating dialog window.
        setFinishOnTouchOutside(true)

        setContentView(R.layout.activity_event_form)

        etTitle = findViewById(R.id.edtEventTitle)
        etLocation = findViewById(R.id.edtEventLocation)
        btnPickDate = findViewById(R.id.btnPickDate)
        btnPickTime = findViewById(R.id.btnPickTime)
        btnSave = findViewById(R.id.btnSaveEvent)

        existingEvent = intent.getSerializableExtra("eventData") as? Event
        existingEvent?.let {
            etTitle.setText(it.title)
            etLocation.setText(it.location)
            btnPickDate.setText(it.date)
            btnPickTime.setText(it.time)
            selectedCalendar.timeInMillis = it.timeInMillis
            btnSave.text = "Update Event"
        }

        btnPickDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedCalendar.set(Calendar.YEAR, y)
                selectedCalendar.set(Calendar.MONTH, m)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, d)
                btnPickDate.setText("$d/${m+1}/$y")
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnPickTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, h)
                selectedCalendar.set(Calendar.MINUTE, m)
                selectedCalendar.set(Calendar.SECOND, 0)
                btnPickTime.setText(String.format("%02d:%02d", h, m))
            }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), true).show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val date = btnPickDate.text.toString().trim()
            val time = btnPickTime.text.toString().trim()

            if (title.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Title and Date are required!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val event = Event(
                id = existingEvent?.id ?: 0,
                title = title,
                location = location,
                date = date,
                time = time,
                timeInMillis = selectedCalendar.timeInMillis,
                isCompleted = existingEvent?.isCompleted ?: false,
                isDeleted = existingEvent?.isDeleted ?: false,
                isArchived = existingEvent?.isArchived ?: false
            )

            val resultIntent = Intent()
            resultIntent.putExtra("eventData", event)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}