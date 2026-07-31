package com.example.lab_02_lifecycledemo

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.lab_02_lifecycledemo.ui.theme.Lab02LifecycleDemoTheme

class MainActivity : ComponentActivity() {

    private val TAG = "LifecycleDemo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logAndToast("onCreate called - Sunny, USN: 25MCAR0230")
        enableEdgeToEdge()
        setContent {
            Lab02LifecycleDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Sunny",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        logAndToast("onStart called")
    }

    override fun onResume() {
        super.onResume()
        logAndToast("onResume called")
    }

    override fun onPause() {
        super.onPause()
        logAndToast("onPause called")
    }

    override fun onStop() {
        super.onStop()
        logAndToast("onStop called")
    }

    override fun onRestart() {
        super.onRestart()
        logAndToast("onRestart called")
    }

    override fun onDestroy() {
        super.onDestroy()
        logAndToast("onDestroy called")
    }

    private fun logAndToast(message: String) {
        Log.d(TAG, message)
        showCustomToast(message)
    }

    private fun showCustomToast(message: String) {
        val textView = TextView(this).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(40, 24, 40, 24)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6200EE"))
                cornerRadius = 24f
            }
        }

        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            view = textView
            show()
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "USN: 25MCAR0230",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Lab02LifecycleDemoTheme {
        Greeting("Sunny")
    }
}