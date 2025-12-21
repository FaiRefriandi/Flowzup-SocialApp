package com.frzterr.app.ui.create

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.frzterr.app.R
import com.frzterr.app.data.local.ProfileLocalStore
import com.frzterr.app.data.repository.auth.AuthRepository
import com.frzterr.app.data.repository.post.PostRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

class CreatePostActivity : AppCompatActivity() {

    private lateinit var authRepo: AuthRepository
    private lateinit var postRepo: PostRepository

    private lateinit var btnCancel: TextView
    private lateinit var btnPost: MaterialButton
    private lateinit var imgAvatar: ShapeableImageView
    private lateinit var tvUsername: TextView
    private lateinit var etContent: EditText
    private lateinit var btnAddImage: ImageView
    private lateinit var rvImagePreview: androidx.recyclerview.widget.RecyclerView
    private lateinit var loadingContainer: View
    
    // Adapter
    private lateinit var imageAdapter: ImagePreviewAdapter

    private var selectedImageUris = mutableListOf<Uri>()

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updateImagePreview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        authRepo = AuthRepository()
        postRepo = PostRepository()

        bindViews()
        setupUI()
        setupListeners()
    }

    private fun bindViews() {
        btnCancel = findViewById(R.id.btnCancel)
        btnPost = findViewById(R.id.btnPost)
        imgAvatar = findViewById(R.id.imgAvatar)
        tvUsername = findViewById(R.id.tvUsername)
        etContent = findViewById(R.id.etContent)
        btnAddImage = findViewById(R.id.btnAddImage)
        rvImagePreview = findViewById(R.id.rvImagePreview)
        loadingContainer = findViewById(R.id.loadingContainer)
    }

    private fun setupUI() {
        // Load user info from local storage
        val (name, avatarPath, username) = ProfileLocalStore.load(this)

        tvUsername.text = username ?: "username"

        // Load avatar logic: Local File -> URL -> Placeholder
        var isAvatarLoaded = false
        val localAvatarPath = ProfileLocalStore.loadLocalAvatarPath(this)
        
        if (localAvatarPath != null) {
            val file = File(localAvatarPath)
            if (file.exists()) {
                imgAvatar.setImageURI(Uri.fromFile(file))
                isAvatarLoaded = true
            }
        }

        // If local file failed/missing, try loading from URL
        if (!isAvatarLoaded && !avatarPath.isNullOrEmpty()) {
            imgAvatar.load(avatarPath) {
                crossfade(true)
                placeholder(R.drawable.ic_user_placeholder)
                error(R.drawable.ic_user_placeholder)
            }
        }
        
        // Ensure background is set for circular shape if needed
        imgAvatar.background = ContextCompat.getDrawable(this, R.drawable.bg_circle)
        
        // Setup Recycler View
        rvImagePreview.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )
        imageAdapter = ImagePreviewAdapter { uriToRemove ->
            selectedImageUris.remove(uriToRemove)
            updateImagePreview()
        }
        rvImagePreview.adapter = imageAdapter

        // Focus on content input
        etContent.requestFocus()
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener {
            finish()
        }

        btnPost.setOnClickListener {
            createPost()
        }

        btnAddImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Enable/disable post button based on content
        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Also check if images exist? For now just text or maybe let images only post?
                // Standard: Text is required or image is required? 
                // Original code required text. Let's keep it requiring text OR images.
                // But simplified: keep original logic (text required)
                btnPost.isEnabled = !s.isNullOrBlank() || selectedImageUris.isNotEmpty()
            }
        })
    }

    private fun updateImagePreview() {
        imageAdapter.submitList(selectedImageUris)
        rvImagePreview.visibility = if (selectedImageUris.isNotEmpty()) View.VISIBLE else View.GONE
        
        // Verify button state again just in case (e.g. text empty but image added)
        val content = etContent.text.toString().trim()
        btnPost.isEnabled = content.isNotBlank() || selectedImageUris.isNotEmpty()
    }

    private fun createPost() {
        val content = etContent.text.toString().trim()
        
        // If empty content AND no images, show error
        if (content.isBlank() && selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Harap isi konten atau pilih gambar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                loadingContainer.visibility = View.VISIBLE
                btnPost.isEnabled = false

                val currentUser = authRepo.getCurrentUser()
                if (currentUser == null) {
                    Toast.makeText(this@CreatePostActivity, "Belum login", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Upload images
                val uploadedImageUrls = mutableListOf<String>()
                
                if (selectedImageUris.isNotEmpty()) {
                    for (uri in selectedImageUris) {
                        try {
                            val imageBytes = compressImage(uri)
                            val uploadResult = postRepo.uploadPostImage(currentUser.id, imageBytes)
                            
                            if (uploadResult.isSuccess) {
                                uploadResult.getOrNull()?.let { uploadedImageUrls.add(it) }
                            } else {
                                Toast.makeText(
                                    this@CreatePostActivity,
                                    "Gagal mengupload salah satu gambar",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Continue or abort? Let's abort to be safe
                                return@launch
                            }
                        } catch (e: Exception) {
                            Log.e("CreatePost", "Error uploading image", e)
                             Toast.makeText(
                                this@CreatePostActivity,
                                "Error memproses gambar",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }
                    }
                }

                // Create post
                val result = postRepo.createPost(currentUser.id, content, uploadedImageUrls)

                if (result.isSuccess) {
                    Toast.makeText(this@CreatePostActivity, "Postingan berhasil dibuat!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@CreatePostActivity,
                        "Gagal membuat postingan",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@CreatePostActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                loadingContainer.visibility = View.GONE
                btnPost.isEnabled = true
            }
        }
    }

    private suspend fun compressImage(uri: Uri): ByteArray {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                contentResolver,
                uri
            )

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            stream.toByteArray()
        }
    }
}
