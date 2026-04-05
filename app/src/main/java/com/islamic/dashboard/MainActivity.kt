package com.islamic.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.islamic.dashboard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private val tabs = listOf("home", "prayer", "cosmos", "events", "converter")
    private val webViews = mutableMapOf<String, WebView>()
    private var currentTab = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = getSharedPreferences("islamic_app", Context.MODE_PRIVATE)
        initWebViews()
        setupBottomNav()
        showTab("home")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebViews() {
        val container = binding.webViewContainer
        tabs.forEach { tab ->
            val wv = WebView(this).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                visibility = View.GONE
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(false)
                    setGeolocationEnabled(true)
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String,
                        callback: GeolocationPermissions.Callback
                    ) {
                        callback.invoke(origin, true, false)
                    }
                }
                webViewClient = WebViewClient()
                addJavascriptInterface(AndroidBridge(this@MainActivity, prefs), "Android")
            }
            container.addView(wv)
            webViews[tab] = wv
            wv.loadUrl("file:///android_asset/$tab.html")
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val tab = when (item.itemId) {
                R.id.nav_home -> "home"
                R.id.nav_prayer -> "prayer"
                R.id.nav_cosmos -> "cosmos"
                R.id.nav_events -> "events"
                R.id.nav_converter -> "converter"
                else -> "home"
            }
            showTab(tab)
            true
        }
    }

    private fun showTab(tab: String) {
        currentTab = tab
        webViews.forEach { (key, wv) ->
            wv.visibility = if (key == tab) View.VISIBLE else View.GONE
        }
    }

    override fun onBackPressed() {
        val wv = webViews[currentTab]
        if (wv?.canGoBack() == true) {
            wv.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

class AndroidBridge(
    private val context: Context,
    private val prefs: SharedPreferences
) {
    @JavascriptInterface
    fun saveLocation(lat: Double, lon: Double, city: String, method: String) {
        prefs.edit()
            .putFloat("lat", lat.toFloat())
            .putFloat("lon", lon.toFloat())
            .putString("city", city)
            .putString("method", method)
            .apply()
    }

    @JavascriptInterface
    fun getLocationData(): String {
        val lat = prefs.getFloat("lat", 21.3891f).toDouble()
        val lon = prefs.getFloat("lon", 39.8579f).toDouble()
        val city = prefs.getString("city", "جدة") ?: "جدة"
        val method = prefs.getString("method", "4") ?: "4"
        return """{"lat":$lat,"lon":$lon,"city":"$city","method":"$method"}"""
    }

    @JavascriptInterface
    fun showToast(msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}