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

        // Use structured initialization parameters for smooth rendering
        rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChat.adapter = chatAdapter

        // CRITICAL FIX: Kill the default item animator to prevent drop-shadow overlapping/flickering
        rvChat.itemAnimator = null
        rvChat.setHasFixedSize(true)

        btnSend.setOnClickListener {
            val userText = edtInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                executeChatPipeline(userText)
            }
        }
    }

    private fun executeChatPipeline(query: String) {
        appendMessage(ChatMessage(text = query, isUser = true))
        edtInput.text.clear()

        val loadingMessage = ChatMessage(text = "Thinking...", isUser = false)
        appendMessage(loadingMessage)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val embeddingResponse = RetrofitClient.apiService.getNoteEmbedding(NotePayload(text = query))
                val queryVector = embeddingResponse.embedding.toFloatArray()

                val allStoredChunks = campusDao.getAllChunksStatic()

                var contextualMatches = allStoredChunks.mapNotNull { chunk ->
                    if (chunk.embedding.isEmpty()) return@mapNotNull null

                    try {
                        val chunkVector = VectorUtils.parseStringToFloatArray(chunk.embedding)
                        if (chunkVector.isEmpty()) return@mapNotNull null

                        val score = VectorUtils.calculateCosineSimilarity(queryVector, chunkVector)
                        Pair(chunk, score)
                    } catch (e: Exception) {
                        null
                    }
                }.filter { it.second >= 0.28f }
                    .sortedByDescending { it.second }
                    .take(4)

                if (contextualMatches.isEmpty()) {
                    val lowerQuery = query.lowercase(Locale.getDefault())
                    val queryWords = lowerQuery.split("\\s+".toRegex()).filter { it.length > 3 }

                    val fallbackMatches = allStoredChunks.filter { chunk ->
                        val lowerText = chunk.chunkText.lowercase(Locale.getDefault())
                        queryWords.any { word -> lowerText.contains(word) }
                    }.take(3).map { Pair(it, 0.27f) }

                    contextualMatches = fallbackMatches
                }

                val contextStringBuilder = StringBuilder()
                if (contextualMatches.isNotEmpty()) {
                    contextStringBuilder.append("Extracted slide references found in local device database storage:\n\n")
                    contextualMatches.forEach { (chunk, _) ->
                        contextStringBuilder.append(chunk.chunkText).append("\n---\n")
                    }
                }

                val cleanContextString = contextStringBuilder.toString().trim()

                val agentRequestPayload = AgentRequest(
                    query = query,
                    context = cleanContextString
                )

                val backendResponse = RetrofitClient.apiService.askAgent(agentRequestPayload)

                withContext(Dispatchers.Main) {
                    removeLastMessage()
                    appendMessage(ChatMessage(text = backendResponse.answer, isUser = false))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    removeLastMessage()
                    appendMessage(ChatMessage(text = "❌ Error reaching backend server instances.", isUser = false))
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

    // Custom ViewHolders for structural layout bubble mapping
    private inner class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.txtUserMessage)
    }

    private inner class AssistantMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.txtAssistantMessage)
    }

    private inner class ChatAdapter(val items: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int = if (items[position].isUser) 1 else 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 1) {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bubble_user, parent, false)
                UserMessageViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bubble_assistant, parent, false)
                AssistantMessageViewHolder(view)
            }
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            if (holder is UserMessageViewHolder) {
                holder.txtMessage.text = item.text
            } else if (holder is AssistantMessageViewHolder) {
                holder.txtMessage.text = item.text
            }
        }
    }
}