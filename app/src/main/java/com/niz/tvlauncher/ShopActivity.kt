package com.niz.tvlauncher

import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * Opens the Corner Shop Boys website full-screen, inside the launcher, so there's no
 * separate browser to install. You scroll with the D-pad. BACK returns to the home screen.
 */
class ShopActivity : AppCompatActivity() {

    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#EFE6D2"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            isFocusable = true
            isFocusableInTouchMode = true
            webViewClient = WebViewClient()   // keep navigation inside the app
        }
        setContentView(web)
        web.loadUrl(getString(R.string.shop_url))
        web.requestFocus()
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
