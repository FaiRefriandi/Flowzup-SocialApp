package com.frzterr.app.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frzterr.app.data.model.PostWithUser
import com.frzterr.app.data.repository.auth.AuthRepository
import com.frzterr.app.data.repository.post.PostRepository
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val postRepo = PostRepository()
    private val authRepo = AuthRepository()

    private val _posts = MutableLiveData<List<PostWithUser>>()
    val posts: LiveData<List<PostWithUser>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadPosts()
    }

    fun loadPosts(showLoading: Boolean = true) {
        viewModelScope.launch {
            try {
                if (showLoading) {
                    _isLoading.value = true
                }
                _error.value = null

                // Retry mechanism for getCurrentUser (handle session loading race condition)
                var currentUser = authRepo.getCurrentUser()
                var retries = 0
                while (currentUser == null && retries < 3) {
                    kotlinx.coroutines.delay(300) // Wait for session to load
                    currentUser = authRepo.getCurrentUser()
                    retries++
                }
                
                if (currentUser == null) {
                    // Session genuinely not available - likely actually logged out
                    _error.value = null // Don't show error, let MainActivity handle redirect
                    return@launch
                }

                val fetchedPosts = postRepo.getAllPosts(currentUser.id)
                _posts.value = fetchedPosts

            } catch (e: Exception) {
                _error.value = "Failed to load posts: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLike(postId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            try {
                val currentUser = authRepo.getCurrentUser() ?: return@launch

                // ⚡ INSTANT Optimistic update - NO DELAY
                val optimisticPosts = _posts.value?.map { postWithUser ->
                    if (postWithUser.post.id == postId) {
                        val updatedPost = postWithUser.post.copy(
                            likeCount = if (currentlyLiked) 
                                (postWithUser.post.likeCount - 1).coerceAtLeast(0) 
                            else 
                                postWithUser.post.likeCount + 1
                        )
                        postWithUser.copy(
                            post = updatedPost,
                            isLiked = !currentlyLiked
                        )
                    } else {
                        postWithUser
                    }
                }
                _posts.value = optimisticPosts ?: emptyList()

                // Background sync - fire and forget
                launch {
                    val result = postRepo.toggleLike(postId, currentUser.id, currentlyLiked)
                    if (result.isFailure) {
                        // Revert on failure
                        loadPosts(showLoading = false)
                    }
                }

            } catch (e: Exception) {
                loadPosts(showLoading = false)
            }
        }
    }

    fun refresh() {
        loadPosts(showLoading = false)
    }

    fun toggleRepost(postId: String, currentlyReposted: Boolean) {
        viewModelScope.launch {
            try {
                val currentUser = authRepo.getCurrentUser() ?: return@launch

                // ⚡ INSTANT Optimistic update - NO DELAY
                val optimisticPosts = _posts.value?.map { postWithUser ->
                    if (postWithUser.post.id == postId) {
                        val updatedPost = postWithUser.post.copy(
                            repostCount = if (currentlyReposted) 
                                (postWithUser.post.repostCount - 1).coerceAtLeast(0) 
                            else 
                                postWithUser.post.repostCount + 1
                        )
                        postWithUser.copy(
                            post = updatedPost,
                            isReposted = !currentlyReposted
                        )
                    } else {
                        postWithUser
                    }
                }
                _posts.value = optimisticPosts ?: emptyList()

                // Background sync - fire and forget
                launch {
                    val result = postRepo.toggleRepost(postId, currentUser.id, currentlyReposted)
                    if (result.isFailure) {
                        // Revert on failure
                        loadPosts(showLoading = false)
                    }
                }

            } catch (e: Exception) {
                loadPosts(showLoading = false)
            }
        }
    }
    fun deletePost(post: com.frzterr.app.data.model.Post) {
        val oldList = _posts.value

        // ⚡ INSTANT Optimistic update
        _posts.value = oldList?.filter { it.post.id != post.id }

        viewModelScope.launch {
            try {
                val currentUser = authRepo.getCurrentUser() ?: return@launch
                val result = postRepo.deletePost(post.id, currentUser.id)

                if (result.isFailure) {
                    _error.value = "Gagal menghapus postingan"
                    _posts.value = oldList // Revert
                }
            } catch (e: Exception) {
                _error.value = "Gagal menghapus postingan"
                _posts.value = oldList // Revert
            }
        }
    }

    fun editPost(post: com.frzterr.app.data.model.Post, newContent: String) {
        val oldList = _posts.value

        // ⚡ INSTANT Optimistic update
        val updatedList = oldList?.map { postWithUser ->
            if (postWithUser.post.id == post.id) {
                postWithUser.copy(
                    post = postWithUser.post.copy(content = newContent)
                )
            } else {
                postWithUser
            }
        }
        _posts.value = updatedList ?: emptyList()

        viewModelScope.launch {
            try {
                val currentUser = authRepo.getCurrentUser() ?: return@launch
                val result = postRepo.updatePost(post.id, currentUser.id, newContent)

                if (result.isFailure) {
                    _error.value = "Gagal mengupdate postingan"
                    _posts.value = oldList // Revert
                }
            } catch (e: Exception) {
                _error.value = "Gagal mengupdate postingan"
                _posts.value = oldList // Revert
            }
        }
    }
    fun hidePost(postId: String) {
        val oldList = _posts.value
        _posts.value = oldList?.filter { it.post.id != postId }
    }
}
