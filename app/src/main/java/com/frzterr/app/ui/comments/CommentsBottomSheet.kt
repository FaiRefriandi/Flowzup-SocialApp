package com.frzterr.app.ui.comments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.frzterr.app.R
import com.frzterr.app.data.model.CommentWithUser
import com.frzterr.app.data.repository.auth.AuthRepository
import com.frzterr.app.data.repository.post.PostRepository
import com.frzterr.app.ui.common.BaseEdgeToEdgeBottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommentsBottomSheet(
    private val postId: String,
    private val postOwnerId: String,
    private val onCommentAdded: () -> Unit = {}
) : BaseEdgeToEdgeBottomSheetDialogFragment() {

    private val postRepo = PostRepository()
    private val authRepo = AuthRepository()
    private lateinit var adapter: CommentAdapter

    private lateinit var rvComments: RecyclerView
    private lateinit var emptyState: View
    private lateinit var shimmerViewContainer: com.facebook.shimmer.ShimmerFrameLayout
    private lateinit var etComment: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnClose: ImageView
    
    private var replyToCommentId: String? = null
    private var allComments: List<CommentWithUser> = emptyList()
    private val expandedCommentIds = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        rvComments = view.findViewById(R.id.rvComments)
        emptyState = view.findViewById(R.id.emptyState)
        shimmerViewContainer = view.findViewById(R.id.shimmerViewContainer)
        etComment = view.findViewById(R.id.etComment)
        btnSend = view.findViewById(R.id.btnSend)
        btnClose = view.findViewById(R.id.btnClose)

        setupAdapter()
        rvComments.adapter = adapter 

        loadComments()

        btnClose.setOnClickListener {
            dismiss()
        }
        
        // Ensure expanded when typing
        etComment.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        etComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnSend.visibility = if (s.isNullOrBlank()) View.GONE else View.VISIBLE
            }
        })

        btnSend.setOnClickListener {
            val content = etComment.text.toString().trim()
            if (content.isNotBlank()) {
                addComment(content)
            }
        }
    }

    private fun setupAdapter() {
        lifecycleScope.launch {
            val currentUser = authRepo.getCurrentUser()
            adapter = CommentAdapter(
                currentUserId = currentUser?.id,
                postOwnerId = postOwnerId,
                onLikeClick = { comment -> toggleLike(comment) },
                onReplyClick = { comment -> replyComment(comment) },
                onDeleteClick = { comment -> confirmDelete(comment) },
                onReplyToggle = { commentId -> toggleReplyExpanded(commentId) }
            )
            rvComments.adapter = adapter
        }
    }
    
    private fun updateDisplayedComments() {
        val mappedComments = allComments.map { commentWithUser ->
            commentWithUser.copy(
                isExpanded = expandedCommentIds.contains(commentWithUser.comment.id)
            )
        }
        
        val filteredList = mappedComments.filter { commentWithUser ->
            val parentId = commentWithUser.comment.parentCommentId
            if (parentId == null) {
                true 
            } else {
                expandedCommentIds.contains(parentId)
            }
        }
        
        adapter.submitList(filteredList)
        emptyState.visibility = if (allComments.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun toggleReplyExpanded(commentId: String) {
        if (expandedCommentIds.contains(commentId)) {
            expandedCommentIds.remove(commentId)
        } else {
            expandedCommentIds.add(commentId)
        }
        updateDisplayedComments()
    }

    private fun loadComments() {
        lifecycleScope.launch {
            try {
                if (adapter.itemCount == 0 && allComments.isEmpty()) {
                    shimmerViewContainer.visibility = View.VISIBLE
                    shimmerViewContainer.startShimmer()
                    rvComments.visibility = View.GONE
                    emptyState.visibility = View.GONE
                }

                val currentUser = authRepo.getCurrentUser()
                allComments = postRepo.getPostComments(postId, currentUser?.id)

                updateDisplayedComments()
                
                shimmerViewContainer.stopShimmer()
                shimmerViewContainer.visibility = View.GONE
                rvComments.visibility = View.VISIBLE
                
            } catch (e: Exception) {
                shimmerViewContainer.stopShimmer()
                shimmerViewContainer.visibility = View.GONE
            }
        }
    }

    private fun addComment(content: String) {
        lifecycleScope.launch {
            try {
                val currentUser = authRepo.getCurrentUser()
                if (currentUser == null) {
                    Toast.makeText(requireContext(), "Belum login", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val parentIdBeforeReset = replyToCommentId
                val result = postRepo.addComment(postId, currentUser.id, content, replyToCommentId)

                if (result.isSuccess) {
                    etComment.text.clear()
                    replyToCommentId = null 
                    Toast.makeText(requireContext(), "Komentar ditambahkan", Toast.LENGTH_SHORT).show()
                    
                    if (parentIdBeforeReset != null) {
                        expandedCommentIds.add(parentIdBeforeReset)
                    }

                    loadComments()

                    kotlinx.coroutines.delay(200)
                    onCommentAdded()
                } else {
                    Toast.makeText(requireContext(), "Gagal menambahkan komentar", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleLike(commentWithUser: CommentWithUser) {
        val index = allComments.indexOfFirst { it.comment.id == commentWithUser.comment.id }
        
        if (index != -1) {
            val currentItem = allComments[index]
            val wasLiked = currentItem.isLiked
            val newLikeCount = if (wasLiked) {
                (currentItem.comment.likeCount - 1).coerceAtLeast(0)
            } else {
                currentItem.comment.likeCount + 1
            }
            
            val newItem = currentItem.copy(
                isLiked = !wasLiked,
                comment = currentItem.comment.copy(likeCount = newLikeCount)
            )
            
            val mutableList = allComments.toMutableList()
            mutableList[index] = newItem
            allComments = mutableList.toList()
            
            updateDisplayedComments()

            lifecycleScope.launch {
                val currentUser = authRepo.getCurrentUser() ?: return@launch
                
                val result = if (wasLiked) {
                    postRepo.unlikeComment(commentWithUser.comment.id, currentUser.id)
                } else {
                    postRepo.likeComment(commentWithUser.comment.id, currentUser.id)
                }

                if (result.isFailure) {
                    val revertList = allComments.toMutableList()
                    revertList[index] = currentItem
                    allComments = revertList.toList()
                    updateDisplayedComments()
                    Toast.makeText(requireContext(), "Gagal update like", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun replyComment(commentWithUser: CommentWithUser) {
        lifecycleScope.launch {
            val currentUser = authRepo.getCurrentUser()
            
            replyToCommentId = commentWithUser.comment.parentCommentId ?: commentWithUser.comment.id
            
            if (currentUser != null && currentUser.id != commentWithUser.comment.userId) {
                val username = commentWithUser.user.username
                val mention = "@$username "
                etComment.setText(mention)
                etComment.setSelection(mention.length)
            }
            
            etComment.requestFocus()
            
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun confirmDelete(commentWithUser: CommentWithUser) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Komentar")
            .setMessage("Apakah Anda yakin ingin menghapus komentar ini?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteComment(commentWithUser)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteComment(commentWithUser: CommentWithUser) {
        lifecycleScope.launch {
            val commentsToRemove = mutableSetOf<String>()
            collectDescendants(commentWithUser.comment.id, commentsToRemove)
            commentsToRemove.add(commentWithUser.comment.id) 

            val oldList = allComments
            val listAfterRemove = allComments.filter { !commentsToRemove.contains(it.comment.id) }
            allComments = listAfterRemove
            
            updateDisplayedComments()

            val currentUser = authRepo.getCurrentUser() ?: return@launch
            
            val result = postRepo.deleteComment(commentWithUser.comment.id, currentUser.id)

            if (result.isFailure) {
                allComments = oldList
                updateDisplayedComments()
                Toast.makeText(requireContext(), "Gagal menghapus komentar", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Komentar dihapus", Toast.LENGTH_SHORT).show()
                onCommentAdded() 
                
                commentsToRemove.remove(commentWithUser.comment.id) 
                if (commentsToRemove.isNotEmpty()) {
                    launch(Dispatchers.IO) {
                        commentsToRemove.forEach { childId ->
                            postRepo.deleteComment(childId, currentUser.id)
                        }
                    }
                }
            }
        }
    }

    private fun collectDescendants(parentId: String, activeSet: MutableSet<String>) {
        val children = allComments.filter { it.comment.parentCommentId == parentId }
        children.forEach { child ->
            if (!activeSet.contains(child.comment.id)) {
                activeSet.add(child.comment.id)
                collectDescendants(child.comment.id, activeSet)
            }
        }
    }

    companion object {
        const val TAG = "CommentsBottomSheet"
    }
}
