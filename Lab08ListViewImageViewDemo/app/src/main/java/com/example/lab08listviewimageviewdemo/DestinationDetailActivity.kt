package com.example.lab08listviewimageviewdemo

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DestinationDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_HERO_RES = "extra_hero_res"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_destination_detail)

        val ivDetailImage = findViewById<ImageView>(R.id.ivDetailImage)
        val tvDetailCategory = findViewById<TextView>(R.id.tvDetailCategory)
        val tvDetailName = findViewById<TextView>(R.id.tvDetailName)
        val tvDetailDescription = findViewById<TextView>(R.id.tvDetailDescription)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        val heroRes = intent.getIntExtra(EXTRA_HERO_RES, R.drawable.img_manali_hero)
        ivDetailImage.setImageResource(heroRes)
        tvDetailCategory.text = intent.getStringExtra(EXTRA_CATEGORY)
        tvDetailName.text = intent.getStringExtra(EXTRA_NAME)
        tvDetailDescription.text = intent.getStringExtra(EXTRA_DESCRIPTION)

        btnBack.setOnClickListener { finish() }
    }
}
