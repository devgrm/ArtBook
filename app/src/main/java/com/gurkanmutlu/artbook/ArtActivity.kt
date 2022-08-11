package com.gurkanmutlu.artbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.gurkanmutlu.artbook.databinding.ActivityArtBinding
import java.io.ByteArrayOutputStream

class ArtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtBinding
    private lateinit var ActivityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissonLaucher :  ActivityResultLauncher<String>
    var selectedBitmap : Bitmap? = null
    private lateinit var database: SQLiteDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("arts", MODE_PRIVATE, null)

        registerLauncher()


        val intent = intent
        val info = intent.getStringExtra("info")
        if (info.equals("new")) {

            //Resetliyoruz
            binding.artNameText.setText("")
            binding.artistName.setText("")
            binding.yearText.setText("")
            binding.saveButton.visibility = View.VISIBLE
            binding.imageView.setImageResource(R.drawable.selectimage)
        }else{
            binding.saveButton.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id", 1 )
            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistName")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.artNameText.setText(cursor.getString(artNameIx))
                binding.artistName.setText(cursor.getString(artistNameIx))
                binding.yearText.setText(cursor.getString(yearIx))

                 val byteArray = cursor.getBlob(imageIx)
                 val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                 binding.imageView.setImageBitmap(bitmap)
            }
            cursor.close()
        }


    }

    fun saveClicked(view: View) {


        val artName = binding.artNameText.text.toString()
        val artistName = binding.artistName.text.toString()
        val year = binding.yearText.text.toString()

        if (selectedBitmap != null){
            val  smallBitmap = makeSmallerBitmap(selectedBitmap!!, 300)


            // görseli veriye çevirme
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                //val database = this.openOrCreateDatabase("arts", MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artName VARCHAR, artistName VARCHAR, year VARCHAR, image BLOB)")
                val sqlString = "INSERT INTO arts(artName, artistName, year, image) VALUES (?,?,?,?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()

            }catch (e: Exception){
                e.printStackTrace()
            }
            val intent = Intent(this@ArtActivity,ArtActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)//bundan önceki aktiviteleri kapat ve mainactivity geri dönüyoruz
            startActivity(intent)

        }

    }


    //bitmap küçültme fonksiyonu
    private fun makeSmallerBitmap(image: Bitmap, maximumSize: Int) : Bitmap {

        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()

        if (bitmapRatio > 1) {
            // landscape
            width = maximumSize
            val scaleHeight = width / bitmapRatio
            height = scaleHeight.toInt()

        }else{
            //portrait
            height = maximumSize
            val scaleWith = height * bitmapRatio
            width = scaleWith.toInt()
        }

        return  Bitmap.createScaledBitmap(image,width,height, true)

    }

    fun selectImage(view: View) {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){ // izin alma mantığını kullanıcıya gösterme
                 //Rationale
                Snackbar.make(view,"Permisson needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permisson",View.OnClickListener
                {}).show() //kullanıcı onay verene kadar ekranda bekle
                permissonLaucher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }else{
                // request permisson
                permissonLaucher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }else{
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            ActivityResultLauncher.launch(intentToGallery)

            // TELEFONDA NEREDE KAYITLI OLDUĞU BULUYORUZ
        }

    }

    // Galeriye gitmek ve galeriden fotograf seçmek
    private fun registerLauncher() {
        ActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
               // println("result ok") // çalıştığını kontrol ettik
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    val imageData = intentFromResult.data
                   // binding.imageView.setImageURI(imageData)
                    if (imageData != null) {
                       println("ok") //çalıştığını kontrol ettik
                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                val source = ImageDecoder.createSource(this@ArtActivity.contentResolver,imageData)
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }else{
                                selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver,imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }
                    }catch (e: Exception) {
                            e.printStackTrace()

                      }
                    }
                }
            }
        }

        permissonLaucher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if (result) {
                // permisson granted
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                   ActivityResultLauncher.launch(intentToGallery)

            }else{
                //permisson denied
                Toast.makeText(this@ArtActivity,"Permisson neded",Toast.LENGTH_LONG).show()
            }
        }
    }
}