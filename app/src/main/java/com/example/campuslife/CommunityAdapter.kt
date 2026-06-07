package com.example.campuslife

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campuslife.data.CommunityPost
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class CommunityAdapter(
    private val onDownloadClick: (CommunityPost, Int) -> Unit,
    private val onEditClick: (CommunityPost) -> Unit,
    private val onDeleteClick: (CommunityPost) -> Unit,
    private val onSaveToNotesClick: (CommunityPost) -> Unit
) : RecyclerView.Adapter<CommunityAdapter.PostViewHolder>() {

    private var posts: List<CommunityPost> = emptyList()

    fun submitList(newList: List<CommunityPost>) {
        posts = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder =
        PostViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_community_post, parent, false))

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) =
        holder.bind(posts[position], onDownloadClick, onEditClick, onDeleteClick, onSaveToNotesClick)

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtAuthor: TextView = itemView.findViewById(R.id.txtAuthor)
        private val txtTimestamp: TextView = itemView.findViewById(R.id.txtTimestamp)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtPostTitle)
        private val txtContent: TextView = itemView.findViewById(R.id.txtPostContent)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        private val btnSave: Button = itemView.findViewById(R.id.btnSaveToNotes)
        private val container: LinearLayout = itemView.findViewById(R.id.containerAttachments)

        fun bind(post: CommunityPost, onDownloadClick: (CommunityPost, Int) -> Unit, onEdit: (CommunityPost) -> Unit, onDelete: (CommunityPost) -> Unit, onSave: (CommunityPost) -> Unit) {
            txtAuthor.text = "By: ${post.author.ifEmpty { "Anonymous" }}"
            txtTitle.text = post.title
            txtContent.text = post.content

            val date = post.timestamp?.toDate()
            txtTimestamp.text = date?.let { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it) } ?: "Just now"

            val isOwner = post.authorUid == FirebaseAuth.getInstance().currentUser?.uid
            btnEdit.visibility = if (isOwner) View.VISIBLE else View.GONE
            btnDelete.visibility = if (isOwner) View.VISIBLE else View.GONE

            btnEdit.setOnClickListener { onEdit(post) }
            btnDelete.setOnClickListener { onDelete(post) }
            btnSave.setOnClickListener { onSave(post) }

            container.removeAllViews()
            post.attachmentData.forEachIndexed { i, _ ->
                val view = LayoutInflater.from(itemView.context).inflate(R.layout.view_post_attachment_row, container, false)
                view.findViewById<TextView>(R.id.txtRowAttachmentName).text = post.attachmentNames.getOrNull(i) ?: "File $i"

                // Hide REMOVE, Show SAVE for Feed items
                view.findViewById<ImageView>(R.id.btnRowRemove).visibility = View.GONE
                view.findViewById<Button>(R.id.btnRowDownload).visibility = View.VISIBLE

                view.findViewById<Button>(R.id.btnRowDownload).setOnClickListener { onDownloadClick(post, i) }
                container.addView(view)
            }
        }
    }
}