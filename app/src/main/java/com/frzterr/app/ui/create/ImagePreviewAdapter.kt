package com.frzterr.app.ui.create

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.frzterr.app.R
import com.google.android.material.imageview.ShapeableImageView

class ImagePreviewAdapter(
    private val onRemoveClick: (Uri) -> Unit
) : RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder>() {

    private val images = mutableListOf<Uri>()

    fun submitList(newImages: List<Uri>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_create_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPreview: ShapeableImageView = itemView.findViewById(R.id.imgPreview)
        private val btnRemove: ImageView = itemView.findViewById(R.id.btnDataRemove)

        fun bind(uri: Uri) {
            imgPreview.load(uri) {
                crossfade(true)
            }

            btnRemove.setOnClickListener {
                onRemoveClick(uri)
            }
        }
    }
}
