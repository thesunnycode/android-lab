package com.example.lab08menuswebviewdemo

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

// Exposed to the page's JavaScript as `window.Android`.
class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
