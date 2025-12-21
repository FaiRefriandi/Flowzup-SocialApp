package com.frzterr.app.ui.common

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class BaseEdgeToEdgeBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // Fix Keyboard Overlap: INFO keys -> Adjust Resize
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            
            // Find design_bottom_sheet dynamically
            var bottomSheetId = resources.getIdentifier("design_bottom_sheet", "id", "com.google.android.material")
            if (bottomSheetId == 0) {
                // Fallback to app package if not found in material
                bottomSheetId = resources.getIdentifier("design_bottom_sheet", "id", requireContext().packageName)
            }
            
            val bottomSheet = bottomSheetDialog.findViewById<View>(bottomSheetId)
            bottomSheet?.let { sheet ->
                // Make background transparent for rounded corners
                sheet.setBackgroundResource(android.R.color.transparent)
                
                // Default behavior: Expanded
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        return dialog
    }
}
