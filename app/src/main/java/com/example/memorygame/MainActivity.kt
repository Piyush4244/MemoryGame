package com.example.memorygame

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.MemoryGame
import com.example.memorygame.models.UserImageList
import com.example.memorygame.utils.EXTRA_BOARD_SIZE
import com.example.memorygame.utils.EXTRA_GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG="MainActivity"
        private const val CREATE_REQUEST_CODE=222
    }

    private var customGameImages:List<String>?=null
    private lateinit var clroot: CoordinatorLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var pairs: TextView
    private lateinit var moves:TextView
    private var boardSize:BoardSize=BoardSize.EASY
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter:CustomAdapter
    private var gameName:String?=null
    private val db=Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvBoard=findViewById(R.id.rvBoard)
        pairs=findViewById(R.id.pairs)
        moves=findViewById(R.id.moves)
        clroot=findViewById(R.id.clroot)

        setUpBoard();
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mi_refresh->{
                if(memoryGame.getNumMoves()>0&&memoryGame.numPairsFound!=boardSize.getNumPairs()){
                    showAlertDialog("Quit your current game",null,View.OnClickListener{
                        setUpBoard()
                    })
                }
                else
                setUpBoard()
                return true
            }
            R.id.mi_new_size->{
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom->{
                showCreationDialog()
                return true
            }
            R.id.mi_download->{
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode== CREATE_REQUEST_CODE&&resultCode==Activity.RESULT_OK){
            val customGameName=data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName==null){
                Log.e(TAG,"null game name")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document->
            val userImageList=document.toObject(UserImageList::class.java)
            if(userImageList?.images==null){
                Log.e(TAG,"invalid data from firestore")
                Snackbar.make(clroot,"Sorry could not find game,$customGameName",Snackbar.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val numCard=userImageList.images.size*2;
            boardSize=BoardSize.getByValue(numCard)
            gameName=customGameName
            customGameImages=userImageList.images
            for(imageUrl in customGameImages!!){
                Glide.with(this).load(imageUrl).preload()
            }
            setUpBoard()
            Snackbar.make(clroot,"you are now playing $gameName",Snackbar.LENGTH_LONG).show()
        }.addOnFailureListener{
            Log.e(TAG,"error retrieving game",it)
        }
    }


    private fun showDownloadDialog() {
        val downloadView=LayoutInflater.from(this).inflate(R.layout.dialog_download_game,null)
        showAlertDialog("download the custom game",downloadView,View.OnClickListener {
            val etDownloadGameName=downloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload=etDownloadGameName.text.toString()
            downloadGame(gameToDownload)
        })
    }

    private fun showCreationDialog() {
        var boardSizeView=LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        var radioGroup=boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board",boardSizeView,View.OnClickListener{
            var desiredBoardSize=when(radioGroup.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.MEDIUM
            }
            val intent=Intent(this@MainActivity,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE,desiredBoardSize)
            startActivityForResult(intent,CREATE_REQUEST_CODE)
        })
    }

    private fun showNewSizeDialog() {
        var boardSizeView=LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        var radioGroup=boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY->radioGroup.check(R.id.rbEasy)
            BoardSize.MEDIUM->radioGroup.check(R.id.rbMedium)
            BoardSize.HARD->radioGroup.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size",boardSizeView,View.OnClickListener{
            boardSize=when(radioGroup.checkedRadioButtonId){
                R.id.rbEasy->BoardSize.EASY
                R.id.rbHard->BoardSize.HARD
                else->BoardSize.MEDIUM
            }
            setUpBoard()
        })
    }

    private fun showAlertDialog(title:String, view:View?, positiveButtonClickListener:View.OnClickListener) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Ok"){ _,_ -> positiveButtonClickListener.onClick(null)}
                .show()
    }

    private fun setUpBoard() {
        supportActionBar?.title=gameName?:getString(R.string.app_name)
        when(boardSize){
            BoardSize.EASY->{
                pairs.text="Pairs: 0/4"
                moves.text="EASY 4 x 2"
            }
            BoardSize.MEDIUM->{
                pairs.text="Pairs: 0/9"
                moves.text="MEDIUM 6 x 3"
            }
            BoardSize.HARD->{
                pairs.text="Pairs: 0/12"
                moves.text="HARD 6 x 4"
            }
        }
        memoryGame=MemoryGame(boardSize,customGameImages)
        pairs.setTextColor(ContextCompat.getColor(this,R.color.progress_none))
        adapter=CustomAdapter(this,boardSize,memoryGame.cards,object : CustomAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGame(position)
            }

        })
        rvBoard.adapter=adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager=GridLayoutManager(this,boardSize.getWidth())
    }

    private fun updateGame(position: Int) {
        if(memoryGame.numPairsFound==boardSize.getNumPairs()){
            Snackbar.make(clroot,"You already won!! ",Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.cards[position].isFaceUp){
            Snackbar.make(clroot,"Invalid move",Snackbar.LENGTH_SHORT).show()
            return
        }
        memoryGame.flipCard(position)
        adapter.notifyDataSetChanged()
        if(memoryGame.cards[position].isMatched){
            var color=ArgbEvaluator().evaluate(
                    memoryGame.numPairsFound.toFloat()/boardSize.getNumPairs(),
                    ContextCompat.getColor(this,R.color.progress_none),
                    ContextCompat.getColor(this,R.color.progress_full)
            ) as Int
            pairs.setTextColor(color)
            pairs.text="Pairs: ${memoryGame.numPairsFound}/${boardSize.getNumPairs()}"
            if(memoryGame.numPairsFound==boardSize.getNumPairs()){

                CommonConfetti.explosion(clroot,4,2, intArrayOf(Color.BLACK,Color.BLUE,Color.CYAN,Color.RED,Color.YELLOW)).infinite()
                Snackbar.make(clroot,"You won,Congratulation",Snackbar.LENGTH_LONG).show()
                return
            }
        }
        moves.text="Moves: ${memoryGame.getNumMoves()}"
    }

}