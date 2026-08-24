package com.example.lab05notificationdemo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val title = intent.getStringExtra(MainActivity.EXTRA_TITLE)
        val message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE)

        findViewById<TextView>(R.id.tvDetailTitle).text = title
        findViewById<TextView>(R.id.tvDetailMessage).text = message

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}
