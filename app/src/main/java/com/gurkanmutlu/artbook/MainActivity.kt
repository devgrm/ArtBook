package com.gurkanmutlu.artbook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.gurkanmutlu.artbook.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var artList: ArrayList<art>
    private lateinit var ArtAdapter : ArtAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) //etkin hale getirdik
        val view = binding.root
        setContentView(view)

        artList = ArrayList<art>()

        ArtAdapter = ArtAdapter(artList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = ArtAdapter


        try {
            val database = this.openOrCreateDatabase("arts", MODE_PRIVATE, null)

            val cursor = database.rawQuery("SELECT * FROM arts ",null)
            val artNameIx = cursor.getColumnIndex("artname")
            val idIx = cursor.getColumnIndex("id")

            while (cursor.moveToNext()) {
                val name = cursor.getString(artNameIx)
                val id = cursor.getInt(idIx)
                val art = art(name, id)
                artList.add(art)
            }

            ArtAdapter.notifyDataSetChanged()

            cursor.close()

        }catch (e: Exception) {

        }

    }

    // baglama işlemi
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        //inflater xlm ile bağlamada yazılır
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.art_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }


    // tıklama yapıldığında ne olacağı
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.add_art_item){
            val intent = Intent(this@MainActivity,ArtActivity::class.java)
            intent.putExtra("info", "new" )
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}