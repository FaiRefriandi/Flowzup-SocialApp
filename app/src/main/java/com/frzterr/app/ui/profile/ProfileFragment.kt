package com.frzterr.app.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.frzterr.app.R
import com.frzterr.app.data.local.ProfileLocalStore
import com.frzterr.app.data.remote.supabase.SupabaseManager
import com.frzterr.app.data.repository.auth.AuthRepository
import com.frzterr.app.data.repository.user.AppUser
import com.frzterr.app.data.repository.user.UserRepository
import com.frzterr.app.databinding.FragmentProfileBinding
import com.frzterr.app.ui.auth.AuthActivity
import com.yalantis.ucrop.UCrop
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()
    private val profileVM: ProfileViewModel by activityViewModels()

    // ================= IMAGE PICK =================
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { startCrop(it) }
        }

    private val cropResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                UCrop.getOutput(result.data!!)?.let { uploadAvatar(it) }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        // ======================================================
        // 🔥 FIRST FRAME — AVATAR LOCAL (SYNC, NO DELAY)
        // ======================================================
        val localAvatarPath = ProfileLocalStore.loadLocalAvatarPath(requireContext())
        if (localAvatarPath != null) {
            val file = File(localAvatarPath)
            if (file.exists()) {
                binding.imgAvatar.setImageURI(Uri.fromFile(file))
                binding.imgAvatar.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle)
            }
        }

        binding.imgAvatar.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    binding.imgAvatar.viewTreeObserver.removeOnPreDrawListener(this)

                    val localPath = ProfileLocalStore.loadLocalAvatarPath(requireContext())
                    if (localPath != null) {
                        val file = File(localPath)
                        if (file.exists()) {
                            binding.imgAvatar.setImageURI(Uri.fromFile(file))
                        }
                    }
                    return true
                }
            }
        )

        // ======================================================
        // 🔥 LOAD LOCAL STATE JIKA VIEWMODEL KOSONG
        // ======================================================
        if (profileVM.cachedUser == null) {
            val (name, avatarUrl) = ProfileLocalStore.load(requireContext())
            if (name != null || avatarUrl != null) {
                profileVM.cachedUser = AppUser(
                    id = "local",
                    fullName = name,
                    avatarUrl = avatarUrl
                )
            }
        }

        // ======================================================
        // 🔥 BIND STATE SECEPAT MUNGKIN
        // ======================================================
        profileVM.cachedUser?.let {
            bindProfile(it)
        }

        // ======================================================
        // 🔄 SYNC DB DI BACKGROUND (TANPA LOADING)
        // ======================================================
        if (profileVM.cachedUser == null || profileVM.cachedUser?.id == "local") {
            loadProfile(force = true, showLoading = false)
        }

        binding.root.post { updateStatusBarIconColor() }

        binding.swipeRefresh.setOnRefreshListener {
            loadProfile(force = true, showLoading = true)
        }

        binding.imgAvatar.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnLogout.setOnClickListener {
            ProfileLocalStore.clear(requireContext())
            lifecycleScope.launch {
                authRepo.signOut()
                startActivity(
                    Intent(requireContext(), AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            }
        }
    }

    // ================= LOAD PROFILE =================
    private fun loadProfile(
        force: Boolean = false,
        showLoading: Boolean = false
    ) {
        if (!force && profileVM.cachedUser != null) return

        lifecycleScope.launch {
            try {
                if (showLoading) {
                    binding.swipeRefresh.isRefreshing = true
                }

                val authUser = authRepo.getCurrentUser() ?: return@launch
                val dbUser = userRepo.getUserByIdForce(authUser.id) ?: return@launch

                profileVM.cachedUser = dbUser
                bindProfile(dbUser)

                ProfileLocalStore.save(
                    requireContext(),
                    dbUser.fullName,
                    dbUser.avatarUrl
                )

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show()
            } finally {
                if (showLoading) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    // ================= BIND UI =================
    private fun bindProfile(user: AppUser) {
        binding.tvName.text = user.fullName ?: ""

        binding.imgAvatar.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle)

        // ⛔ COIL BUKAN FIRST RENDER
        binding.imgAvatar.load(user.avatarUrl) {
            crossfade(false)
            size(256)

            if (user.avatarUrl == null) {
                placeholder(R.drawable.ic_user_placeholder)
                error(R.drawable.ic_user_placeholder)
            } else {
                placeholder(null)
                error(null)
            }
        }
    }

    // ================= START CROP =================
    private fun startCrop(source: Uri) {
        val dest = Uri.fromFile(
            File(requireContext().cacheDir, "crop_${UUID.randomUUID()}.jpg")
        )

        val intent = UCrop.of(source, dest)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
            .getIntent(requireContext())

        cropResult.launch(intent)
    }

    // ================= UPLOAD AVATAR =================
    private fun uploadAvatar(uri: Uri) {
        lifecycleScope.launch {
            try {
                val user = authRepo.getCurrentUser() ?: return@launch
                val compressed = compressImage(uri)
                val fileName = "${user.id}.jpg"

                // 🔥 SIMPAN FILE LOKAL UNTUK FIRST RENDER
                val localFile = File(
                    requireContext().filesDir,
                    "avatar_${user.id}.jpg"
                )
                localFile.writeBytes(compressed)

                ProfileLocalStore.saveLocalAvatarPath(
                    requireContext(),
                    localFile.absolutePath
                )

                SupabaseManager.client.storage
                    .from("avatars")
                    .upload(
                        path = fileName,
                        data = compressed
                    ) {
                        upsert = true
                        contentType = io.ktor.http.ContentType.Image.JPEG
                    }

                val baseUrl = SupabaseManager.client.storage
                    .from("avatars")
                    .publicUrl(fileName)

                val versionedUrl = "$baseUrl?v=${System.currentTimeMillis()}"

                // DB = SINGLE SOURCE OF TRUTH
                authRepo.updateCustomAvatar(versionedUrl)
                userRepo.updateAvatarUrl(user.id, versionedUrl)

                profileVM.cachedUser =
                    profileVM.cachedUser?.copy(avatarUrl = versionedUrl)

                ProfileLocalStore.save(
                    requireContext(),
                    profileVM.cachedUser?.fullName,
                    versionedUrl
                )

                bindProfile(profileVM.cachedUser!!)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal upload avatar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ================= COMPRESS IMAGE =================
    private suspend fun compressImage(uri: Uri): ByteArray =
        withContext(Dispatchers.IO) {
            val bitmap =
                android.provider.MediaStore.Images.Media.getBitmap(
                    requireContext().contentResolver,
                    uri
                )

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
            stream.toByteArray()
        }

    // ================= STATUS BAR =================
    private fun updateStatusBarIconColor() {
        val activity = activity ?: return
        val window = activity.window

        val bgColor = (binding.root.background as? ColorDrawable)?.color ?: Color.BLACK
        val isDark = isColorDark(bgColor)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (
                0.299 * Color.red(color) +
                        0.587 * Color.green(color) +
                        0.114 * Color.blue(color)
                ) / 255
        return darkness >= 0.5
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
