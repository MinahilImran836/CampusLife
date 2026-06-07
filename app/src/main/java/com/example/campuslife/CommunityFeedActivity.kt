package com.example.campuslife

import android.app.AlertDialog
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campuslife.data.AppDatabase
import com.example.campuslife.data.CommunityPost
import com.example.campuslife.data.Note
import com.example.campuslife.data.NoteChunk
import com.example.campuslife.network.NotePayload
import com.example.campuslife.network.RetrofitClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CommunityFeedActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rvCommunityFeed: RecyclerView
    private lateinit var fabNewPost: FloatingActionButton
    private lateinit var adapter: CommunityAdapter
    private lateinit var searchView: SearchView
    private lateinit var categorySpinner: Spinner

    private var allPosts = mutableListOf<CommunityPost>()

    private val pendingFileUris = mutableListOf<Uri>()
    private val pendingFileNames = mutableListOf<String>()
    private var dialogFileContainer: LinearLayout? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { safeUri ->
            if (pendingFileUris.contains(safeUri)) return@let
            if (pendingFileUris.size < 3) {
                pendingFileUris.add(safeUri)
                val fileName = getFileNameFromUri(safeUri)
                pendingFileNames.add(fileName)
                dialogFileContainer?.let { container ->
                    val preview = LayoutInflater.from(this).inflate(R.layout.view_post_attachment_row, container, false)
                    preview.findViewById<TextView>(R.id.txtRowAttachmentName).text = fileName
                    preview.findViewById<ImageView>(R.id.btnRowRemove).visibility = View.VISIBLE
                    preview.findViewById<Button>(R.id.btnRowDownload).visibility = View.GONE

                    preview.findViewById<ImageView>(R.id.btnRowRemove).setOnClickListener {
                        val index = container.indexOfChild(preview)
                        if (index != -1) {
                            container.removeViewAt(index)
                            pendingFileUris.removeAt(index)
                            pendingFileNames.removeAt(index)
                        }
                    }
                    container.addView(preview)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_feed)

        rvCommunityFeed = findViewById(R.id.rvCommunityFeed)
        fabNewPost = findViewById(R.id.fabNewPost)
        searchView = findViewById(R.id.searchView)
        categorySpinner = findViewById(R.id.categorySpinner)

        rvCommunityFeed.layoutManager = LinearLayoutManager(this)

        adapter = CommunityAdapter(
            onDownloadClick = { post, index -> downloadAndVectorize(post, index) },
            onEditClick = { post -> editPost(post) },
            onDeleteClick = { post -> deletePost(post) },
            onSaveToNotesClick = { post -> saveToMyNotes(post) }
        )
        rvCommunityFeed.adapter = adapter

        setupFilters()
        loadFeedFromFirestore()
        fabNewPost.setOnClickListener { showPostCreationFlow() }
    }

    private fun setupFilters() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterPosts(newText, categorySpinner.selectedItem.toString())
                return true
            }
        })

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterPosts(searchView.query.toString(), categorySpinner.selectedItem.toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterPosts(query: String?, category: String) {
        val filtered = allPosts.filter { post ->
            (query.isNullOrEmpty() || post.title.contains(query, true) || post.content.contains(query, true)) &&
                    (category == "General" || post.category == category)
        }
        adapter.submitList(filtered)
    }

    private fun showPostCreationFlow() {
        pendingFileUris.clear()
        pendingFileNames.clear()
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null)
        val spinner = view.findViewById<Spinner>(R.id.spinnerCategory)
        dialogFileContainer = view.findViewById(R.id.containerFilePreviews)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        view.findViewById<ImageView>(R.id.btnSelectAttachment).setOnClickListener { filePickerLauncher.launch("*/*") }
        view.findViewById<Button>(R.id.btnPublish).setOnClickListener {
            val title = view.findViewById<EditText>(R.id.edtDialogTitle).text.toString()
            val content = view.findViewById<EditText>(R.id.edtDialogContent).text.toString()
            val category = spinner.selectedItem.toString()
            if (title.isNotEmpty()) {
                uploadPost(title, content, category)
                dialog.dismiss()
            }
        }
    }

    private fun uploadPost(title: String, content: String, category: String) {
        val attachmentData = mutableListOf<String>()
        for (uri in pendingFileUris) {
            contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                attachmentData.add(Base64.encodeToString(bytes, Base64.DEFAULT))
            }
        }

        val post = hashMapOf(
            "title" to title,
            "content" to content,
            "category" to category,
            "author" to (FirebaseAuth.getInstance().currentUser?.email ?: "Anonymous"),
            "authorUid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
            "timestamp" to FieldValue.serverTimestamp(),
            "attachmentNames" to pendingFileNames,
            "attachmentData" to attachmentData
        )

        db.collection("posts").add(post)
            .addOnSuccessListener { Toast.makeText(this, "Published!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun editPost(post: CommunityPost) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null)
        val edtTitle = view.findViewById<EditText>(R.id.edtDialogTitle)
        val edtContent = view.findViewById<EditText>(R.id.edtDialogContent)
        val btnPublish = view.findViewById<Button>(R.id.btnPublish)
        edtTitle.setText(post.title)
        edtContent.setText(post.content)
        btnPublish.text = "Update Post"
        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        btnPublish.setOnClickListener {
            val newTitle = edtTitle.text.toString()
            val newContent = edtContent.text.toString()
            db.collection("posts").document(post.id)
                .update(mapOf("title" to newTitle, "content" to newContent))
                .addOnSuccessListener {
                    Toast.makeText(this, "Updated successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
        }
    }

    private fun deletePost(post: CommunityPost) {
        AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("posts").document(post.id).delete()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveToMyNotes(post: CommunityPost) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val localNote = Note(
            title = post.title,
            content = post.content,
            date = Date().toString(),
            color = "#FFFFFF",
            attachmentNames = post.attachmentNames.toMutableList(),
            attachmentUris = post.attachmentData.toMutableList()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(this@CommunityFeedActivity).campusDao()
                val noteId = dao.insertNote(localNote).toInt()
                val chunkList = mutableListOf<NoteChunk>()

                // 1. Vectorize the main text content
                if (post.content.isNotEmpty()) {
                    val textBlock = "Note: ${post.title}\nDescription: ${post.content}"
                    val response = RetrofitClient.apiService.getNoteEmbedding(NotePayload(text = textBlock))

                    chunkList.add(NoteChunk(
                        noteId = noteId,
                        chunkText = textBlock,
                        embedding = response.embedding.toString(),
                        pageNumber = 0
                    ))
                }

                // 2. Process Attachments (Base64 images)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                for (i in post.attachmentData.indices) {
                    try {
                        val bytes = Base64.decode(post.attachmentData[i], Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        val image = InputImage.fromBitmap(bitmap, 0)

                        val result = recognizer.process(image).await()
                        val extractedText = result.text.trim()

                        if (extractedText.isNotEmpty()) {
                            val formattedText = "Attachment [${post.attachmentNames[i]}]:\n$extractedText"
                            val response = RetrofitClient.apiService.getNoteEmbedding(NotePayload(text = formattedText))

                            chunkList.add(NoteChunk(
                                noteId = noteId,
                                chunkText = formattedText,
                                embedding = response.embedding.toString(),
                                pageNumber = i + 1
                            ))
                        }
                    } catch (e: Exception) {
                        Log.e("CommunityRAG", "Failed to process attachment ${post.attachmentNames[i]}", e)
                    }
                }

                // 3. Save all chunks
                if (chunkList.isNotEmpty()) {
                    dao.insertChunks(chunkList)
                    Log.d("DEBUG_RAG", "Successfully indexed ${chunkList.size} chunks for note $noteId")
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CommunityFeedActivity, "Saved and Vectorized!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CommunityFeedActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadAndVectorize(post: CommunityPost, index: Int) {
        try {
            val bytes = Base64.decode(post.attachmentData[index], Base64.DEFAULT)
            val fileName = post.attachmentNames[index]
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, fileName); put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf"); put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) }
                contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.let { uri -> contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
            } else File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName).writeBytes(bytes)
            Toast.makeText(this, "Downloaded", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show() }
    }

    private fun loadFeedFromFirestore() {
        db.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { s, _ ->
                allPosts = s?.map { d -> d.toObject(CommunityPost::class.java).apply { id = d.id } }?.toMutableList() ?: mutableListOf()
                filterPosts(searchView.query.toString(), categorySpinner.selectedItem.toString())
            }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return "File_${System.currentTimeMillis()}"
    }
}