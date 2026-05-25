package com.example.campuslife

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.campuslife.data.AppDatabase
import com.example.campuslife.data.CampusDao
import com.example.campuslife.data.Note
import com.example.campuslife.data.NoteChunk
import com.example.campuslife.network.NotePayload
import com.example.campuslife.network.RetrofitClient
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class NoteFormActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnSave: Button
    private lateinit var btnAttach: ImageView
    private lateinit var btnAddLink: ImageView
    private lateinit var containerFiles: LinearLayout
    private lateinit var containerLinks: LinearLayout

    private lateinit var campusDao: CampusDao
    private val selectedUris = mutableListOf<String>()
    private val selectedNames = mutableListOf<String>()
    private val enteredLinks = mutableListOf<String>()

    private var currentNoteId: Int = 0
    private var existingEmbedding: String? = null

    private val PICK_FILE_RESULT_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This automatically closes the activity when clicking outside the dialog area
        setFinishOnTouchOutside(true)

        setContentView(R.layout.activity_note_form)

        campusDao = AppDatabase.getDatabase(this).campusDao()

        etTitle = findViewById(R.id.edtNoteTitle)
        etContent = findViewById(R.id.edtNoteContent)
        btnSave = findViewById(R.id.btnSaveNote)
        btnAttach = findViewById(R.id.btnAttachFile)
        btnAddLink = findViewById(R.id.btnAddLink)
        containerFiles = findViewById(R.id.containerFilePreviews)
        containerLinks = findViewById(R.id.containerLinkPreviews)

        val existingNote = intent.getSerializableExtra("noteData") as? Note
        existingNote?.let {
            currentNoteId = it.id
            etTitle.setText(it.title)
            etContent.setText(it.content)
            selectedUris.addAll(it.attachmentUris)
            selectedNames.addAll(it.attachmentNames)
            enteredLinks.addAll(it.links)
            existingEmbedding = it.embedding
            refreshPreviews()
            btnSave.text = "Update Note ✨"
        }

        btnAttach.setOnClickListener {
            if (selectedUris.size < 3) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*"))
                }
                startActivityForResult(intent, PICK_FILE_RESULT_CODE)
            } else {
                Toast.makeText(this, "Max 3 files allowed!", Toast.LENGTH_SHORT).show()
            }
        }

        btnAddLink.setOnClickListener {
            if (enteredLinks.size < 3) showLinkDialog()
            else Toast.makeText(this, "Max 3 links allowed!", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener { processAndSaveNote() }
    }

    private fun refreshPreviews() {
        containerFiles.removeAllViews()
        selectedUris.forEachIndexed { index, _ ->
            addPillToContainer(containerFiles, "📎 ${selectedNames[index]}", "#CDE990") {
                selectedUris.removeAt(index)
                selectedNames.removeAt(index)
                refreshPreviews()
            }
        }

        containerLinks.removeAllViews()
        enteredLinks.forEachIndexed { index, link ->
            addPillToContainer(containerLinks, "🔗 $link", "#FFD4D4") {
                enteredLinks.removeAt(index)
                refreshPreviews()
            }
        }
    }

    private fun addPillToContainer(container: LinearLayout, text: String, colorHex: String, onRemove: () -> Unit) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_attachment_pill, container, false)
        val textView = view.findViewById<TextView>(R.id.txtPillText)
        textView.text = text

        view.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))

        view.findViewById<ImageView>(R.id.btnRemovePill).setOnClickListener { onRemove() }
        container.addView(view)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                selectedUris.add(uri.toString())
                selectedNames.add(getFileName(uri))
                refreshPreviews()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "File"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = cursor.getString(index)
            }
        }
        return name
    }

    private fun showLinkDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val input = dialogView.findViewById<EditText>(R.id.dialogInput)
        val btnAdd = dialogView.findViewById<TextView>(R.id.btnConfirm)

        input.visibility = View.VISIBLE
        btnAdd.setOnClickListener {
            val link = input.text.toString().trim()
            if (link.isNotEmpty()) {
                enteredLinks.add(link)
                refreshPreviews()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun processAndSaveNote() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this).apply {
            setMessage("Extracting chunks and processing vector mappings...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val initialNote = Note(
                    id = currentNoteId,
                    title = title,
                    content = content,
                    date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()),
                    color = "#FFFAEC",
                    attachmentUris = selectedUris,
                    attachmentNames = selectedNames,
                    links = enteredLinks,
                    isDeleted = false,
                    embedding = null
                )

                val generatedId = campusDao.insertNote(initialNote).toInt()
                val targetNoteId = if (currentNoteId == 0) generatedId else currentNoteId

                if (currentNoteId != 0) {
                    campusDao.deleteChunksByNoteId(currentNoteId)
                }

                val chunkList = mutableListOf<NoteChunk>()

                if (content.isNotEmpty()) {
                    try {
                        val textBlock = "Note: $title\nDescription: $content"
                        val response = RetrofitClient.apiService.getNoteEmbedding(NotePayload(text = textBlock))
                        chunkList.add(
                            NoteChunk(
                                noteId = targetNoteId,
                                chunkText = textBlock,
                                embedding = response.embedding.toString(),
                                pageNumber = 0
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("CAMPUS_LIFE_RAG", "Failed description block mapping", e)
                    }
                }

                for (uriString in selectedUris) {
                    val uri = Uri.parse(uriString)
                    val mimeType = contentResolver.getType(uri)

                    if (mimeType == "application/pdf") {
                        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                        if (parcelFileDescriptor != null) {
                            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
                            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                            var slidingWindowText = StringBuilder()
                            var windowStartPage = 1

                            for (i in 0 until pdfRenderer.pageCount) {
                                val page = pdfRenderer.openPage(i)
                                val scaleFactor = 2.0f
                                val bitmap = Bitmap.createBitmap((page.width * scaleFactor).toInt(), (page.height * scaleFactor).toInt(), Bitmap.Config.ARGB_8888)
                                val matrix = Matrix().apply { setScale(scaleFactor, scaleFactor) }
                                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val image = InputImage.fromBitmap(bitmap, 0)
                                try {
                                    val result = recognizer.process(image).await()
                                    val cleanText = result.text.trim()

                                    if (cleanText.isNotEmpty()) {
                                        slidingWindowText.append(" [Slide ${i + 1}]: $cleanText\n")
                                    }
                                } catch (e: Exception) {
                                    Log.e("CAMPUS_LIFE_RAG", "MLKit failed on page index $i", e)
                                } finally {
                                    page.close()
                                }

                                if ((i + 1) % 3 == 0 || (i + 1) == pdfRenderer.pageCount) {
                                    val compiledChunkString = slidingWindowText.toString().trim()
                                    if (compiledChunkString.isNotEmpty()) {
                                        val finalChunkText = "Document: $title | Pages $windowStartPage-${i + 1}\n$compiledChunkString"
                                        try {
                                            val response = RetrofitClient.apiService.getNoteEmbedding(NotePayload(text = finalChunkText))
                                            chunkList.add(
                                                NoteChunk(
                                                    noteId = targetNoteId,
                                                    chunkText = finalChunkText,
                                                    embedding = response.embedding.toString(),
                                                    pageNumber = windowStartPage
                                                )
                                            )
                                        } catch (e: Exception) {
                                            Log.e("CAMPUS_LIFE_RAG", "Network vector call failed for chunk", e)
                                        }
                                    }
                                    slidingWindowText = StringBuilder()
                                    windowStartPage = i + 2
                                }
                            }
                            pdfRenderer.close()
                            parcelFileDescriptor.close()
                        }
                    } else {
                        try {
                            val image = InputImage.fromFilePath(this@NoteFormActivity, uri)
                            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                            val result = recognizer.process(image).await()
                            val cleanText = result.text.trim()
                            if (cleanText.isNotEmpty()) {
                                val formattedText = "Document Image: $title\n$cleanText"
                                val response = RetrofitClient.apiService.getNoteEmbedding(NotePayload(text = formattedText))
                                chunkList.add(
                                    NoteChunk(
                                        noteId = targetNoteId,
                                        chunkText = formattedText,
                                        embedding = response.embedding.toString(),
                                        pageNumber = 1
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("CAMPUS_LIFE_RAG", "Image file extraction error", e)
                        }
                    }
                }

                if (chunkList.isNotEmpty()) {
                    campusDao.insertChunks(chunkList)
                }

                val noteToReturn = initialNote.copy(id = targetNoteId)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    val resultIntent = Intent().apply { putExtra("noteData", noteToReturn) }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }

            } catch (e: Exception) {
                Log.e("CAMPUS_LIFE_RAG", "Global save execution failed", e)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@NoteFormActivity, "Error saving structural note properties.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}