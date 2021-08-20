package com.example.memorygame

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.MemoryCard
import kotlin.math.min

class CustomAdapter(
        private val context: Context,
        private val boardSize: BoardSize,
        private val cards: List<MemoryCard>,
        private val cardClickListener: CardClickListener
) :
    RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    companion object{
        private const val MARGIN_SIZE=10
        private const val TAG="CustomAdapter"
    }
    interface CardClickListener{
        fun onCardClicked(position: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var cardwidth=parent.width/boardSize.getWidth()-2* MARGIN_SIZE
        var cardheight=parent.height/boardSize.getHeight()-2* MARGIN_SIZE
        var cardsidelength=min(cardheight,cardwidth)
        var view=LayoutInflater.from(context).inflate(R.layout.memory_card,parent,false)
        var layoutparams=view.findViewById<CardView>(R.id.cardview).layoutParams as ViewGroup.MarginLayoutParams
        layoutparams.width=cardsidelength
        layoutparams.height=cardsidelength
        layoutparams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun getItemCount()=boardSize.numCards

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }
    inner class ViewHolder(itemView:View): RecyclerView.ViewHolder(itemView){
        private val imageButton =itemView.findViewById<ImageButton>(R.id.imageButton)    
        fun bind(position: Int) {
            val memoryCard=cards[position]
            if(memoryCard.isFaceUp){
                if(memoryCard.imageUrl!=null){
                    Glide.with(context)
                            .load(memoryCard.imageUrl)
                            .placeholder(R.drawable.ic_image)
                            .into(imageButton)
                }
                else {
                    imageButton.setImageResource(memoryCard.identifier)
                }
            }
            else {
                imageButton.setImageResource(R.drawable.ic_launcher_background)
            }
            imageButton.alpha=if(memoryCard.isMatched) .4f else 1.0f
            var colorStateList=if(memoryCard.isMatched)ContextCompat.getColorStateList(context,R.color.gray)else null
            ViewCompat.setBackgroundTintList(imageButton,colorStateList)
            imageButton.setOnClickListener{
                Log.i(TAG,"clicked on posstion $position")
                cardClickListener.onCardClicked(position)
            }
        }

    }
}
