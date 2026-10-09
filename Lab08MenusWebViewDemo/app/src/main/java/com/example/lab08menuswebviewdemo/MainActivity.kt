package com.example.lab08menuswebviewdemo

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvUrl: TextView
    private lateinit var progress: LinearProgressIndicator

    // Link under the finger when the context menu was opened (null = not a link).
    private var longPressedLink: String? = null

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            webView.goBack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Keep content clear of the status and navigation bars (edge-to-edge is enforced).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        toolbar = findViewById(R.id.toolbar)
        tvUrl = findViewById(R.id.tvUrl)
        progress = findViewById(R.id.progress)
        webView = findViewById(R.id.webView)

        // 1. Options menu lives in this Toolbar.
        setSupportActionBar(toolbar)

        setUpWebView()

        // 2. Popup menu anchored to the "Quick Links" button.
        findViewById<MaterialButton>(R.id.btnQuickLinks).setOnClickListener { showQuickLinks(it) }

        // 3. Floating context menu on long-press inside the WebView.
        registerForContextMenu(webView)

        onBackPressedDispatcher.addCallback(this, backCallback)

        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webViewClient = object : WebViewClient() {
            // Keep http(s) pages inside the app; hand other schemes (mailto:, tel:, intent:) to the system.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme
                if (scheme == "http" || scheme == "https" || scheme == "file") return false
                openExternally(request.url.toString())
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
                tvUrl.text = url
            }

            override fun onPageFinished(view: WebView, url: String) {
                progress.visibility = View.INVISIBLE
                tvUrl.text = if (url == HOME_URL) "studyhub://home" else url
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                backCallback.isEnabled = view.canGoBack()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    view.loadDataWithBaseURL(null, offlinePage(error.description.toString()), "text/html", "utf-8", null)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.setProgressCompat(newProgress, true)
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                toolbar.subtitle = title
            }
        }
    }

    // ---------- Options menu ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_back).isEnabled = webView.canGoBack()
        menu.findItem(R.id.action_forward).isEnabled = webView.canGoForward()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_home -> webView.loadUrl(HOME_URL)
            R.id.action_refresh -> webView.reload()
            R.id.action_back -> webView.goBack()
            R.id.action_forward -> webView.goForward()
            R.id.action_share -> share(webView.title.orEmpty(), currentUrl())
            R.id.action_open_external -> openExternally(currentUrl())
            R.id.text_small, R.id.text_normal, R.id.text_large -> {
                item.isChecked = true
                webView.settings.textZoom = when (item.itemId) {
                    R.id.text_small -> 80
                    R.id.text_large -> 130
                    else -> 100
                }
                toast("Text size: ${item.title}")
            }
            R.id.action_javascript -> {
                item.isChecked = !item.isChecked
                webView.settings.javaScriptEnabled = item.isChecked
                toast(if (item.isChecked) "JavaScript enabled" else "JavaScript disabled")
                webView.reload()
            }
            R.id.action_about -> showAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // ---------- Popup menu ----------

    private fun showQuickLinks(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_quick_links, menu)
            setOnMenuItemClickListener { item ->
                val url = when (item.itemId) {
                    R.id.link_android -> "https://developer.android.com/"
                    R.id.link_kotlin -> "https://kotlinlang.org/docs/home.html"
                    R.id.link_material -> "https://m3.material.io/"
                    R.id.link_wikipedia -> "https://en.m.wikipedia.org/wiki/Android_(operating_system)"
                    else -> return@setOnMenuItemClickListener false
                }
                toast("Opening ${item.title}")
                webView.loadUrl(url)
                true
            }
            show()
        }
    }

    // ---------- Context menu ----------

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val hit = webView.hitTestResult
        val isLink = hit.type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
            hit.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE

        if (isLink) {
            longPressedLink = hit.extra
            menu.setHeaderTitle(hit.extra)
            menuInflater.inflate(R.menu.menu_context_link, menu)
        } else {
            longPressedLink = null
            menu.setHeaderTitle(webView.title)
            menuInflater.inflate(R.menu.menu_context_page, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val link = longPressedLink
        when (item.itemId) {
            R.id.ctx_open -> link?.let { webView.loadUrl(it) }
            R.id.ctx_copy -> link?.let { copyToClipboard(it) }
            R.id.ctx_share -> link?.let { share("Study link", it) }
            R.id.ctx_reload -> webView.reload()
            R.id.ctx_copy_page -> copyToClipboard(currentUrl())
            else -> return super.onContextItemSelected(item)
        }
        return true
    }

    // ---------- Helpers ----------

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun currentUrl(): String = webView.url ?: HOME_URL

    private fun share(subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    private fun openExternally(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toast("No app can open this link")
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("URL", text))
        toast("Copied: $text")
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun offlinePage(reason: String) = """
        <html><body style="font-family:sans-serif;text-align:center;padding:48px 24px;color:#1B2635">
        <h2>Page unavailable</h2><p style="color:#5B6B80">$reason</p>
        <p>Check the internet connection, then use <b>Refresh</b> or <b>Home</b> from the menu.</p>
        </body></html>
    """.trimIndent()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    companion object {
        const val HOME_URL = "file:///android_asset/home.html"
    }
}
