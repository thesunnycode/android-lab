package com.example.lab04intentdemo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val username = intent.getStringExtra("USERNAME")
        findViewById<TextView>(R.id.tvWelcome).text = "Welcome, $username!"

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            finish()
        }
    }
}