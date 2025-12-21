package com.frzterr.app.ui.viewer

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.frzterr.app.R
import com.github.chrisbanes.photoview.PhotoView

class ImageViewerDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_IMAGES = "arg_images"
        private const val ARG_POSITION = "arg_position"
        const val TAG = "ImageViewerDialogFragment"

        fun newInstance(images: List<String>, position: Int): ImageViewerDialogFragment {
            val fragment = ImageViewerDialogFragment()
            val args = Bundle()
            args.putStringArrayList(ARG_IMAGES, ArrayList(images))
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
        
        // Convenience for single image (needed for compatibility or easy call)
        fun newInstance(imageUrl: String): ImageViewerDialogFragment {
            return newInstance(listOf(imageUrl), 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_image_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // If shown as normal fragment (for Shared Element Transition), ensure background is set
        // (Dialog usually handles window background, but Fragment needs view background)
        if (!showsDialog) {
            view.setBackgroundColor(Color.BLACK)
        }

        val images = arguments?.getStringArrayList(ARG_IMAGES) ?: emptyList<String>()
        val startPosition = arguments?.getInt(ARG_POSITION, 0) ?: 0
        
        val vpFullScreen: ViewPager2 = view.findViewById(R.id.vpFullScreen)
        val btnClose: ImageView = view.findViewById(R.id.btnClose)
        
        // Adjust Close button position to avoid status bar overlap
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(btnClose) { v, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = bars.top + (16 * resources.displayMetrics.density).toInt()
            v.layoutParams = params
            insets
        }

        // Postpone transition until image is loaded
        postponeEnterTransition()

        if (images.isNotEmpty()) {
            val adapter = FullScreenImageAdapter(images)
            vpFullScreen.adapter = adapter
            vpFullScreen.setCurrentItem(startPosition, false)
        } else {
             startPostponedEnterTransition()
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    // Override dismiss to handle Fragment mode
    override fun dismiss() {
        if (showsDialog) {
            super.dismiss()
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
    }

    // Inner Adapter Class
    inner class FullScreenImageAdapter(private val hiddenImages: List<String>) : RecyclerView.Adapter<FullScreenImageAdapter.ImageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_full, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.bind(hiddenImages[position])
        }

        override fun getItemCount(): Int = hiddenImages.size

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val photoView: PhotoView = itemView.findViewById(R.id.photoView)

            fun bind(url: String) {
                // Set transition name to match source
                androidx.core.view.ViewCompat.setTransitionName(photoView, url)
                
                photoView.load(url) {
                    crossfade(false) // Disable crossfade for smooth shared element transition
                    listener(
                        onSuccess = { _, _ ->
                            startPostponedEnterTransition()
                        },
                        onError = { _, _ ->
                            startPostponedEnterTransition()
                        }
                    )
                }
            }
        }
    }
}
