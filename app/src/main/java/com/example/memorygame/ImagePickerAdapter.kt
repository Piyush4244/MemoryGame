package com.example.memorygame

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.memorygame.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
        private val context: Context,
        private val imagesUris: List<Uri>,
        private val boardSize: BoardSize,
        private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view=LayoutInflater.from(context).inflate(R.layout.card_image,parent,false)
        val cardWidth=parent.width/boardSize.getWidth()
        val cardHeight=parent.height/boardSize.getHeight()
        val cardSideLength=min(cardHeight,cardWidth)
        val layoutParams=view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width=cardSideLength
        layoutParams.height=cardSideLength
        return ViewHolder(view)
    }

    override fun getItemCount()=boardSize.getNumPairs()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position<imagesUris.size){
            holder.bind(imagesUris[position])
        }
        else{
            holder.bind()
        }
    }

    interface ImageClickListener{
        fun onImageClicked()
    }

    inner class ViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView){
        private val imageView=itemView.findViewById<ImageView>(R.id.ivCustomImage)
        fun bind(uri:Uri){
            //imageView.setImageURI(uri)
            Glide.with(context).load(uri).into(imageView)
            imageView.setOnClickListener(null)
        }
        fun bind(){
            imageView.setOnClickListener{
                imageClickListener.onImageClicked()
            }
        }
    }

}
