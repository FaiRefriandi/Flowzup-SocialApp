package com.frzterr.app.ui.home

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.frzterr.app.R

class PostOptionsBottomSheet(
    private val isOwner: Boolean,
    private val onCopyClick: () -> Unit,
    private val onEditClick: () -> Unit,
    private val onDeleteClick: () -> Unit,
    private val onNotInterestedClick: () -> Unit
) : com.frzterr.app.ui.common.BaseCustomBottomSheet() {

    override fun getLayoutResId(): Int = R.layout.bottom_sheet_post_options

    override fun onSheetCreated(view: View) {
        val btnCopy = view.findViewById<LinearLayout>(R.id.btnCopy)
        val btnEdit = view.findViewById<LinearLayout>(R.id.btnEdit)
        val btnDelete = view.findViewById<LinearLayout>(R.id.btnDelete)
        val btnNotInterested = view.findViewById<LinearLayout>(R.id.btnNotInterested)

        // 1. Copy (Always Visible)
        btnCopy.setOnClickListener {
            dismiss()
            onCopyClick()
        }

        // 2. Owner-Only Options
        if (isOwner) {
            btnEdit.visibility = View.VISIBLE
            btnDelete.visibility = View.VISIBLE
            btnNotInterested.visibility = View.GONE
            
            btnEdit.setOnClickListener {
                dismiss()
                onEditClick()
            }
            btnDelete.setOnClickListener {
                dismiss()
                onDeleteClick()
            }
        } else {
            // 3. Non-Owner Options
            btnEdit.visibility = View.GONE
            btnDelete.visibility = View.GONE
            btnNotInterested.visibility = View.VISIBLE
            
            btnNotInterested.setOnClickListener {
                dismiss()
                onNotInterestedClick()
            }
        }
    }

    companion object {
        const val TAG = "PostOptionsBottomSheet"
    }
}
