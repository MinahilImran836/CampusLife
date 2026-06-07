package com.example.campuslife

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campuslife.data.AppDatabase
import com.example.campuslife.data.CampusDao
import com.example.campuslife.data.ChatMessage
import com.example.campuslife.network.AgentRequest
import com.example.campuslife.network.NotePayload
import com.example.campuslife.network.RetrofitClient
import com.example.campuslife.utils.VectorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var edtInput: EditText
    private lateinit var btnSend: ImageView
    private lateinit var campusDao: CampusDao
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        campusDao = AppDatabase.getDatabase(this).campusDao()
        rvChat = findViewById(R.id.rvChatHistory)
        edtInput = findViewById(R.id.edtChatInput)
        btnSend = findViewById(R.id.btnSendChat)
        chatAdapter = ChatAdapter(chatMessages)
        rvChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvChat.adapter = chatAdapter
        rvChat.itemAnimator = null
        btnSend.setOnClickListener {
            val userText = edtInput.text.toString().trim()
            if (userText.isNotEmpty()) executeChatPipeline(userText)
        }
    }

    private fun executeChatPipeline(query: String) {
        appendMessage(ChatMessage(text = query, isUser = true))
        edtInput.text.clear()
        appendMessage(ChatMessage(text = "Thinking...", isUser = false))

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("DEBUG_RAG", "Starting pipeline for: $query")
                val embeddingResponse = RetrofitClient.apiService.getNoteEmbedding(NotePayload(text = query))
                val queryVector = embeddingResponse.embedding.toFloatArray()
                val allStoredChunks = campusDao.getAllChunksStatic()
                Log.d("DEBUG_RAG", "Database chunks loaded: ${allStoredChunks.size}")

                var contextualMatches = allStoredChunks.mapNotNull { chunk ->
                    if (chunk.embedding.isEmpty()) return@mapNotNull null
                    try {
                        val chunkVector = VectorUtils.parseStringToFloatArray(chunk.embedding)
                        val score = VectorUtils.calculateCosineSimilarity(queryVector, chunkVector)
                        if (score >= 0.28f) Pair(chunk, score) else null
                    } catch (e: Exception) { null }
                }.sortedByDescending { it.second }.take(4)

                if (contextualMatches.isEmpty()) {
                    Log.d("DEBUG_RAG", "No semantic matches found, using fallback.")
                    contextualMatches = allStoredChunks.take(3).map { Pair(it, 0.0f) }
                }

                val contextStringBuilder = StringBuilder()
                contextualMatches.forEach { (chunk, score) ->
                    Log.d("DEBUG_RAG", "Adding chunk (score: $score): ${chunk.chunkText.take(50)}...")
                    contextStringBuilder.append(chunk.chunkText).append("\n---\n")
                }

                val cleanContextString = contextStringBuilder.toString().trim()
                Log.d("DEBUG_RAG", "Final context string length: ${cleanContextString.length}")

                val backendResponse = RetrofitClient.apiService.askAgent(AgentRequest(query, cleanContextString))
                withContext(Dispatchers.Main) {
                    removeLastMessage()
                    appendMessage(ChatMessage(text = backendResponse.answer, isUser = false))
                }
            } catch (e: Exception) {
                Log.e("DEBUG_RAG", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    removeLastMessage()
                    appendMessage(ChatMessage(text = "❌ Error: ${e.message}", isUser = false))
                }
            }
        }
    }

    private fun appendMessage(message: ChatMessage) {
        chatMessages.add(message)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        rvChat.scrollToPosition(chatMessages.size - 1)
    }

    private fun removeLastMessage() {
        if (chatMessages.isNotEmpty()) {
            val index = chatMessages.size - 1
            chatMessages.removeAt(index)
            chatAdapter.notifyItemRemoved(index)
        }
    }

    // ViewHolders and Adapter omitted for brevity but remain unchanged from your original file
    inner class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) { val txtMessage: TextView = view.findViewById(R.id.txtUserMessage) }
    inner class AssistantMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) { val txtMessage: TextView = view.findViewById(R.id.txtAssistantMessage) }
    inner class ChatAdapter(val items: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemViewType(position: Int): Int = if (items[position].isUser) 1 else 0
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = if (viewType == 1) UserMessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bubble_user, parent, false)) else AssistantMessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bubble_assistant, parent, false))
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) { val item = items[position]; if (holder is UserMessageViewHolder) holder.txtMessage.text = item.text else if (holder is AssistantMessageViewHolder) (holder as AssistantMessageViewHolder).txtMessage.text = item.text }
    }
}