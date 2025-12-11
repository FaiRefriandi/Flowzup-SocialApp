package com.frzterr.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.frzterr.app.MainActivity
import com.frzterr.app.data.repository.auth.AuthRepository
import com.frzterr.app.databinding.ActivityResetPasswordBinding
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSubmitPassword.setOnClickListener {
            val pass1 = binding.edtNewPassword.text.toString().trim()
            val pass2 = binding.edtConfirmNewPassword.text.toString().trim()

            if (pass1.isEmpty() || pass2.isEmpty()) {
                showToast("Semua field harus diisi")
                return@setOnClickListener
            }

            if (pass1 != pass2) {
                showToast("Password tidak cocok")
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    authRepository.updatePassword(pass1)

                    showToast("Password berhasil diperbarui")
                    redirectToHome()

                } catch (e: Exception) {
                    Log.e("RESET_PASS", e.message ?: "Error")
                    showToast("Gagal reset password: ${e.message}")
                }
            }
        }
    }

    private fun redirectToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}