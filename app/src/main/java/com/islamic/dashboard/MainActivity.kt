package com.islamic.dashboard

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.Date

// ===================== MAIN ACTIVITY =====================

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private val tabs = listOf("home","prayer","cosmos","events","converter")
    private val webViews = mutableMapOf<String, WebView>()
    private var currentTab = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("islamic_app", Context.MODE_PRIVATE)
        val container = findViewById<android.widget.FrameLayout>(R.id.webViewContainer)
        tabs.forEach { tab ->
            val wv = createWebView(tab)
            container.addView(wv)
            webViews[tab] = wv
            wv.loadUrl("file:///android_asset/$tab.html")
        }
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            val tab = when (item.itemId) {
                R.id.nav_home -> "home"
                R.id.nav_prayer -> "prayer"
                R.id.nav_cosmos -> "cosmos"
                R.id.nav_events -> "events"
                R.id.nav_converter -> "converter"
                else -> "home"
            }
            showTab(tab); true
        }
        showTab("home")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(tab: String): WebView {
        return WebView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(-1, -1)
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
                override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                    callback.invoke(origin, true, false)
                }
            }
            webViewClient = WebViewClient()
            addJavascriptInterface(AndroidBridge(this@MainActivity, prefs), "Android")
        }
    }

    private fun showTab(tab: String) {
        currentTab = tab
        webViews.forEach { (k, v) -> v.visibility = if (k == tab) View.VISIBLE else View.GONE }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val wv = webViews[currentTab]
        if (wv?.canGoBack() == true) wv.goBack() else super.onBackPressed()
    }
}

// ===================== JAVASCRIPT BRIDGE =====================

class AndroidBridge(private val ctx: Context, private val prefs: SharedPreferences) {
    @JavascriptInterface
    fun saveLocation(lat: Double, lon: Double, city: String, method: String) {
        prefs.edit().putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat())
            .putString("city", city).putString("method", method).apply()
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
        android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}

// ===================== WIDGET HELPERS =====================

private val HIJRI_MONTHS = listOf(
    "محرم","صفر","ربيع الأول","ربيع الآخر","جمادى الأولى","جمادى الآخرة",
    "رجب","شعبان","رمضان","شوال","ذو القعدة","ذو الحجة"
)

internal fun widgetTime(): String {
    val c = Calendar.getInstance()
    var h = c.get(Calendar.HOUR_OF_DAY)
    val m = c.get(Calendar.MINUTE)
    if (h > 12) h -= 12; if (h == 0) h = 12
    return "%02d:%02d".format(h, m)
}

internal fun widgetHijri(): String {
    val jd = (Date().time / 86400000 + 2440587.5).toLong()
    var l = jd - 1948440 + 10632
    val n = (l - 1) / 10631; l = l - 10631 * n + 354
    val j = ((10985-l)/5316)*((50*l)/17719)+(l/5670)*((43*l)/15238)
    l = l-((30-j)/15)*((17719*j)/50)-(j/16)*((15238*j)/43)+29
    val mo = ((24*l)/709).toInt()
    val dy = (l-(709*mo)/24).toInt()
    val yr = (30*n+j-30).toInt()
    return "$dy ${HIJRI_MONTHS.getOrElse(mo-1){""}} $yr هـ"
}

internal fun widgetMoon(): String {
    @Suppress("DEPRECATION")
    val diff = (Date().time - Date(124,0,11).time) / 86400000.0
    val pct = ((diff % 29.53059) + 29.53059) % 29.53059 / 29.53059
    return when {
        pct < 0.03 || pct > 0.97 -> "🌑 المحاق"
        pct < 0.25 -> "🌒 الهلال"
        pct < 0.28 -> "🌓 التربيع الأول"
        pct < 0.47 -> "🌔 الأحدب المتزايد"
        pct < 0.53 -> "🌕 البدر"
        pct < 0.72 -> "🌖 الأحدب المتناقص"
        pct < 0.78 -> "🌗 التربيع الأخير"
        else -> "🌘 الهلال الأخير"
    }
}

private fun pi(ctx: Context, code: Int) = PendingIntent.getActivity(
    ctx, code, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
)

// ===================== WIDGETS =====================

class IslamicWidgetSmall : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach {
            val v = RemoteViews(ctx.packageName, R.layout.widget_small)
            v.setTextViewText(R.id.widget_small_time, widgetTime())
            v.setTextViewText(R.id.widget_small_hijri, widgetHijri())
            v.setOnClickPendingIntent(R.id.widget_small_time, pi(ctx, 0))
            mgr.updateAppWidget(it, v)
        }
    }
}

class IslamicWidgetMedium : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach {
            val v = RemoteViews(ctx.packageName, R.layout.widget_medium)
            v.setTextViewText(R.id.widget_med_time, widgetTime())
            v.setTextViewText(R.id.widget_med_hijri, widgetHijri())
            v.setTextViewText(R.id.widget_med_moon, widgetMoon())
            v.setOnClickPendingIntent(R.id.widget_med_time, pi(ctx, 1))
            mgr.updateAppWidget(it, v)
        }
    }
}

class IslamicWidgetLarge : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach {
            val v = RemoteViews(ctx.packageName, R.layout.widget_large)
            v.setTextViewText(R.id.widget_lg_time, widgetTime())
            v.setTextViewText(R.id.widget_lg_hijri, widgetHijri())
            v.setTextViewText(R.id.widget_lg_moon_phase, widgetMoon())
            v.setOnClickPendingIntent(R.id.widget_lg_time, pi(ctx, 2))
            mgr.updateAppWidget(it, v)
        }
    }
}

const val ACTION_UPDATE_WIDGET = "com.islamic.dashboard.UPDATE_WIDGET"