package com.uspn.maps.ui.theme

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.uspn.maps.ImageBatiment
import com.uspn.maps.R

class ImagePagerAdapter (
    private val context: Context,
    private val images: List<ImageBatiment>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                //400
            ).apply { bottomMargin = 24 }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return ImageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {

        // Gerer le cas ou l'image est enregistree avec l'extension
        val imageName = images[position].image.substringBeforeLast('.')

        val resId = context.resources.getIdentifier(
            imageName,
            "drawable",
            context.packageName
        )

        Glide.with(context)
            .load(if (resId != 0) resId else R.drawable.default_img)
            .centerCrop()
            .placeholder(R.drawable.default_img)
            .error(R.drawable.default_img)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = images.size
}