package com.frzterr.app.ui.home

import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.frzterr.app.R
import com.frzterr.app.data.model.Post
import com.frzterr.app.ui.common.BaseCustomBottomSheet

class EditPostBottomSheet(
    private val post: Post,
    private val onSaveClick: (String) -> Unit
) : BaseCustomBottomSheet() {

    override fun getLayoutResId(): Int = R.layout.dialog_edit_post

    override fun onSheetCreated(view: View) {
        val etContent = view.findViewById<EditText>(R.id.etContent)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val btnSave = view.findViewById<TextView>(R.id.btnSave)

        // Pre-fill content
        etContent.setText(post.content)
        etContent.setSelection(post.content.length)
        etContent.requestFocus()

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            val newContent = etContent.text.toString().trim()
            if (newContent.isNotBlank()) {
                if (newContent != post.content) {
                    onSaveClick(newContent)
                }
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Konten tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val TAG = "EditPostBottomSheet"
    }
}
