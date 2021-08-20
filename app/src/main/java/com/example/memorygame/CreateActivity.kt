package com.example.memorygame

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object{
        private  const val TAG="CreateActivity"
        private const val PIC_PHOTO_CODE=111
        private const val READ_EXTERNAL_PHOTO_CODE=133
        private const val READ_PHOTOS_PERMISSION=android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    private lateinit var boardSize: BoardSize
    private var numImageRequired=-1
    private var chosenImagesUris= mutableListOf<Uri>()

    private  lateinit var adapter:ImagePickerAdapter
    private lateinit var rvImagePicker:RecyclerView
    private lateinit var etGameName:EditText
    private lateinit var btnSave:Button
    private val storage=Firebase.storage
    private val db=Firebase.firestore
    private lateinit var pbUploading:ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker=findViewById(R.id.rvImagePicker)
        etGameName=findViewById(R.id.etGamenName)
        btnSave=findViewById(R.id.btnSave)
        pbUploading=findViewById(R.id.pbUploading)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize=intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImageRequired=boardSize.getNumPairs()
        supportActionBar?.title="Choose pics(0/$numImageRequired)"

        btnSave.setOnClickListener{
            saveDataToFirebase()
        }
        etGameName.filters= arrayOf(InputFilter.LengthFilter(14))
        etGameName.addTextChangedListener(object :TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                btnSave.isEnabled=shouldEnableSaveBtn()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        adapter=ImagePickerAdapter(this,chosenImagesUris,boardSize,object :ImagePickerAdapter.ImageClickListener{
            override fun onImageClicked() {
                if(isPermissionGranted(this@CreateActivity,READ_PHOTOS_PERMISSION)){
                    launchIntentForPhotos()
                }
                else {
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTO_CODE)
                }
            }

        })
        rvImagePicker.adapter=adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager=GridLayoutManager(this,boardSize.getWidth())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode== READ_EXTERNAL_PHOTO_CODE){
            if(grantResults.isNotEmpty()&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                launchIntentForPhotos()
            }
            else {
                Toast.makeText(this,"In order to create custom game allow the fkin permission chutiye",Toast.LENGTH_LONG);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode!= PIC_PHOTO_CODE||resultCode!=Activity.RESULT_OK||data==null){
            Log.w(TAG,"did not get data bc")
            return
        }
        val selectedUri=data.data;
        val clipData=data.clipData
        if(clipData!=null){
            Log.i(TAG,"clipData numImages ${clipData.itemCount}: $clipData")
            for(i in 0 until clipData.itemCount){
                val clipItem=clipData.getItemAt(i)
                if(chosenImagesUris.size<numImageRequired){
                    chosenImagesUris.add(clipItem.uri)
                }
            }
        }
        else if(selectedUri!=null){
            Log.i(TAG,"data:: $selectedUri")
            chosenImagesUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title="Choose Pics ${chosenImagesUris.size}/$numImageRequired"
        btnSave.isEnabled=shouldEnableSaveBtn()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun shouldEnableSaveBtn(): Boolean {
        if(chosenImagesUris.size!=numImageRequired){
            return false
        }
        if(etGameName.text.isBlank()||etGameName.text.length<3){
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        //val intent=Intent(Intent.ACTION_PICK)
        val intent=Intent(Intent.ACTION_GET_CONTENT)
        intent.type="image/*"
        //intent.putExtra(Intent.ACTION_SEND_MULTIPLE,true)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        startActivityForResult(Intent.createChooser(intent,"ch00se Pics"),PIC_PHOTO_CODE)
    }

    private fun saveDataToFirebase() {
        val customGameName=etGameName.text.toString()
        Log.i(TAG,"saveDataToFirebase")
        btnSave.isEnabled=false
        db.collection("games").document(customGameName).get()
                .addOnSuccessListener{document->
                    if(document!=null && document.data!=null){
                        AlertDialog.Builder(this)
                                .setTitle("game already exist")
                                .setPositiveButton("ok",null)
                                .show()
                        btnSave.isEnabled=true
                    }
                    else {
                        handleImageUploading(customGameName)
                    }
                }.addOnFailureListener{exception ->  
                    Log.e(TAG,"error saving data",exception)
                    Toast.makeText(this,"failed to save images",Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled=true
                }

    }

    private fun handleImageUploading(customGameName: String) {
        pbUploading.visibility= View.VISIBLE
        var didEncounterError=false
        val uploadedImageUrls= mutableListOf<String>()
        for (i in chosenImagesUris.indices){
            val imageByteArray=getImageByteArray(chosenImagesUris[i])
            val filePath="images/$customGameName/${System.currentTimeMillis()}-$i.jpg"
            val photoReference=storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                    .continueWithTask{photoUploadTask ->
                        Log.i(TAG,"uploaded bytes${photoUploadTask.result?.bytesTransferred}")
                        photoReference.downloadUrl
                    }.addOnCompleteListener { downloadUrlTask->
                        if(!downloadUrlTask.isSuccessful){
                            Log.e(TAG,"failed to download/upload url",downloadUrlTask.exception)
                            Toast.makeText(this,"failed to upload images",Toast.LENGTH_SHORT).show()
                            didEncounterError=true
                            pbUploading.visibility=View.GONE
                            return@addOnCompleteListener
                        }
                        if(didEncounterError){
                            pbUploading.visibility=View.GONE
                            return@addOnCompleteListener
                        }
                        val downloadedUrl=downloadUrlTask.result.toString()
                        uploadedImageUrls.add((downloadedUrl))
                        pbUploading.progress=uploadedImageUrls.size*100/chosenImagesUris.size
                        Log.i(TAG,"finished uploading url $downloadedUrl,total uploaded ${uploadedImageUrls.size}")
                        if(uploadedImageUrls.size==chosenImagesUris.size){
                            handleImagesUploaded(customGameName,uploadedImageUrls)
                        }
                    }
        }
    }

    private fun handleImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        db.collection("games").document(gameName)
                .set(mapOf("images" to imageUrls))
                .addOnCompleteListener { gameCreationTask->
                    pbUploading.visibility=View.GONE
                    if(!gameCreationTask.isSuccessful){
                        Log.e(TAG,"exception with game creation",gameCreationTask.exception)
                        Toast.makeText(this,"failed game creation",Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }
                    Log.i(TAG,"game $gameName created successfully")
                    AlertDialog.Builder(this)
                            .setTitle("upload complete! lets play game $gameName")
                            .setPositiveButton("ok"){_,_->
                                val resultData=Intent();
                                resultData.putExtra(EXTRA_GAME_NAME,gameName)
                                setResult(Activity.RESULT_OK,resultData)
                                finish()
                            }.show()
                }
    }

    private fun getImageByteArray(uri: Uri): ByteArray {
        val originalBitmap=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
            val source=ImageDecoder.createSource(contentResolver,uri)
            ImageDecoder.decodeBitmap(source)
        }
        else {
            MediaStore.Images.Media.getBitmap(contentResolver,uri)
        }
        Log.i(TAG,"original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap=BitmapScaler.scaleToFitHeight(originalBitmap,150)
        Log.i(TAG,"Scaled Width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream=ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG,60,byteOutputStream)
        return byteOutputStream.toByteArray()
    }

}