package com.frzterr.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.frzterr.app.R
import com.frzterr.app.ui.create.CreatePostActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

import com.frzterr.app.data.repository.auth.AuthRepository

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()
    private val authRepo = AuthRepository()
    private lateinit var adapter: PostAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvPosts = view.findViewById<RecyclerView>(R.id.rvPosts)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val fabCreatePost = view.findViewById<FloatingActionButton>(R.id.fabCreatePost)
        val emptyState = view.findViewById<View>(R.id.emptyState)
        val shimmerViewContainer = view.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerViewContainer)

        // Setup adapter
        val currentUser = authRepo.getCurrentUser()
        adapter = PostAdapter(
            currentUserId = currentUser?.id,
            onLikeClick = { postWithUser ->
                viewModel.toggleLike(postWithUser.post.id, postWithUser.isLiked)
            },
            onCommentClick = { postWithUser ->
                val commentsBottomSheet = com.frzterr.app.ui.comments.CommentsBottomSheet(
                    postId = postWithUser.post.id,
                    postOwnerId = postWithUser.post.userId,
                    onCommentAdded = {
                        viewModel.refresh()
                    }
                )
                commentsBottomSheet.show(
                    requireActivity().supportFragmentManager,
                    com.frzterr.app.ui.comments.CommentsBottomSheet.TAG
                )
            },
            onRepostClick = { postWithUser ->
                viewModel.toggleRepost(postWithUser.post.id, postWithUser.isReposted)
            },
            onUserClick = { postWithUser ->
                // TODO: Navigate to user profile
                Toast.makeText(
                    requireContext(),
                    "Profile: @${postWithUser.user.username}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onOptionClick = { postWithUser ->
                val isOwner = currentUser?.id == postWithUser.post.userId
                
                PostOptionsBottomSheet(
                    isOwner = isOwner,
                    onCopyClick = {
                        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Post Content", postWithUser.post.content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireContext(), "Teks disalin", Toast.LENGTH_SHORT).show()
                    },
                    onEditClick = {
                        EditPostBottomSheet(
                            post = postWithUser.post,
                            onSaveClick = { newContent ->
                                viewModel.editPost(postWithUser.post, newContent)
                            }
                        ).show(requireActivity().supportFragmentManager, EditPostBottomSheet.TAG)
                    },
                    onDeleteClick = {
                        viewModel.deletePost(postWithUser.post)
                    },
                    onNotInterestedClick = {
                        viewModel.hidePost(postWithUser.post.id)
                        Toast.makeText(requireContext(), "Postingan disembunyikan", Toast.LENGTH_SHORT).show()
                    }
                ).show(requireActivity().supportFragmentManager, PostOptionsBottomSheet.TAG)
            },
            onImageClick = { images, position, view ->
                val fragment = com.frzterr.app.ui.viewer.ImageViewerDialogFragment.newInstance(images, position)
                
                // Shared Element Transition Logic
                fragment.sharedElementEnterTransition = androidx.transition.TransitionInflater.from(requireContext())
                    .inflateTransition(android.R.transition.move)
                fragment.enterTransition = androidx.transition.Fade()
                fragment.exitTransition = androidx.transition.Fade()
                
                requireActivity().supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .addSharedElement(view, androidx.core.view.ViewCompat.getTransitionName(view) ?: "")
                    .add(android.R.id.content, fragment, com.frzterr.app.ui.viewer.ImageViewerDialogFragment.TAG)
                    .addToBackStack(com.frzterr.app.ui.viewer.ImageViewerDialogFragment.TAG)
                    .commit()
            }
        )

        rvPosts.adapter = adapter

        // Observe posts
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            emptyState.visibility = if (posts.isEmpty() && !viewModel.isLoading.value!!) View.VISIBLE else View.GONE
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                shimmerViewContainer.visibility = View.VISIBLE
                shimmerViewContainer.startShimmer()
                rvPosts.visibility = View.GONE
                emptyState.visibility = View.GONE
            } else {
                shimmerViewContainer.stopShimmer()
                shimmerViewContainer.visibility = View.GONE
                rvPosts.visibility = View.VISIBLE
                swipeRefresh.isRefreshing = false
                
                // Check empty state again after loading finishes
                if (adapter.currentList.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                }
            }
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        // Swipe to refresh
        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        // FAB click
        fabCreatePost.setOnClickListener {
            startActivity(Intent(requireContext(), CreatePostActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh posts when returning to this fragment
        viewModel.refresh()
    }
}
