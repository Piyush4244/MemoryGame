package com.example.memorygame.models

import com.example.memorygame.utils.DEFAULT_ICON

class MemoryGame(private val boardSize: BoardSize, customImages: List<String>?) {

    val cards:List<MemoryCard>
    var numPairsFound=0

    private var indexOfUnmatchedCard=-1
    private var numCardFlip=0
    init {
        cards = if(customImages==null){
            val chosenImages= DEFAULT_ICON.shuffled().take(boardSize.getNumPairs())
            val randomizedImages=(chosenImages+chosenImages).shuffled()
            randomizedImages.map { MemoryCard(it) }
        }
        else {
            val randomizedImages=(customImages+customImages).shuffled()
            randomizedImages.map { MemoryCard(it.hashCode(),it) }
        }
    }

    fun flipCard(position: Int) {
        numCardFlip++
        val card=cards[position]
        card.isFaceUp=!card.isFaceUp
        if(indexOfUnmatchedCard!=-1){
            if(cards[indexOfUnmatchedCard].identifier==card.identifier){
                cards[indexOfUnmatchedCard].isMatched=true
                card.isMatched=true
                numPairsFound++
                indexOfUnmatchedCard=-1
            }
            else {
                cards[indexOfUnmatchedCard].isFaceUp=false
                indexOfUnmatchedCard=position
            }
        }
        else indexOfUnmatchedCard=position
    }

    fun getNumMoves(): Int {
        return numCardFlip/2
    }
}