package com.islamic.dashboard

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Calendar
import kotlin.math.*

// ==================== SMALL WIDGET ====================
class IslamicWidgetSmall : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateSmallWidget(ctx, mgr, id) }
    }
    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, IslamicWidgetSmall::class.java))
            ids.forEach { id -> updateSmallWidget(ctx, mgr, id) }
        }
    }
}

// ==================== MEDIUM WIDGET ====================
class IslamicWidgetMedium : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateMediumWidget(ctx, mgr, id) }
    }
    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, IslamicWidgetMedium::class.java))
            ids.forEach { id -> updateMediumWidget(ctx, mgr, id) }
        }
    }
}

// ==================== LARGE WIDGET ====================
class IslamicWidgetLarge : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateLargeWidget(ctx, mgr, id) }
    }
    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, IslamicWidgetLarge::class.java))
            ids.forEach { id -> updateLargeWidget(ctx, mgr, id) }
        }
    }
}

// ==================== CONSTANTS ====================
const val ACTION_UPDATE_WIDGET = "com.islamic.dashboard.UPDATE_WIDGET"

// ==================== UPDATE FUNCTIONS ====================

fun updateSmallWidget(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
    val views = RemoteViews(ctx.packageName, R.layout.widget_small)
    val now = Calendar.getInstance()

    // Time
    var hr = now.get(Calendar.HOUR_OF_DAY)
    val mn = now.get(Calendar.MINUTE)
    val ampm = if (hr >= 12) "مساءً" else "صباحاً"
    if (hr > 12) hr -= 12; if (hr == 0) hr = 12
    views.setTextViewText(R.id.widget_small_time, "%02d:%02d".format(hr, mn))

    // Hijri
    val hijri = toHijri(now.time)
    views.setTextViewText(R.id.widget_small_hijri, "${hijri.day} ${HIJRI_MONTHS[hijri.month - 1]} ${hijri.year} هـ")

    // Open app on click
    val launchIntent = Intent(ctx, MainActivity::class.java)
    val pi = PendingIntent.getActivity(ctx, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.widget_small_time, pi)

    mgr.updateAppWidget(widgetId, views)

    // Fetch prayer times async
    CoroutineScope(Dispatchers.IO).launch {
        val data = getSavedLocation(ctx)
        val prayers = fetchPrayerTimes(data.lat, data.lon, data.method)
        withContext(Dispatchers.Main) {
            if (prayers != null) {
                val next = getNextPrayer(prayers)
                views.setTextViewText(R.id.widget_small_prayer_name, PRAYER_NAMES_AR[next.name] ?: next.name)
                views.setTextViewText(R.id.widget_small_prayer_time, next.time)
                views.setTextViewText(R.id.widget_small_countdown, getCountdown(next.time))
                views.setTextViewText(R.id.widget_small_prayer_icon, PRAYER_ICONS[next.name] ?: "🕌")
                mgr.updateAppWidget(widgetId, views)
            }
        }
    }

    scheduleNextUpdate(ctx)
}

fun updateMediumWidget(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
    val views = RemoteViews(ctx.packageName, R.layout.widget_medium)
    val now = Calendar.getInstance()

    // Time
    var hr = now.get(Calendar.HOUR_OF_DAY)
    val mn = now.get(Calendar.MINUTE)
    if (hr > 12) hr -= 12; if (hr == 0) hr = 12
    views.setTextViewText(R.id.widget_med_time, "%02d:%02d".format(hr, mn))

    // Dates
    val hijri = toHijri(now.time)
    views.setTextViewText(R.id.widget_med_hijri, "${hijri.day} ${HIJRI_MONTHS[hijri.month - 1]} ${hijri.year} هـ")
    views.setTextViewText(R.id.widget_med_greg, "${now.get(Calendar.DAY_OF_MONTH)} ${GREG_MONTHS[now.get(Calendar.MONTH)]} ${now.get(Calendar.YEAR)}")

    // Moon phase
    val moon = getMoonPhase()
    views.setTextViewText(R.id.widget_med_moon, "${MOON_ICONS[moon.phase]} ${moon.name}")

    // Open app on click
    val launchIntent = Intent(ctx, MainActivity::class.java)
    val pi = PendingIntent.getActivity(ctx, 1, launchIntent, PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.widget_med_time, pi)

    mgr.updateAppWidget(widgetId, views)

    CoroutineScope(Dispatchers.IO).launch {
        val data = getSavedLocation(ctx)

        // Weather
        val weather = fetchWeather(data.lat, data.lon)
        withContext(Dispatchers.Main) {
            if (weather != null) {
                views.setTextViewText(R.id.widget_med_temp, "${weather.temp}°")
                views.setTextViewText(R.id.widget_med_weather, weather.desc)
                mgr.updateAppWidget(widgetId, views)
            }
        }

        // Prayer times
        val prayers = fetchPrayerTimes(data.lat, data.lon, data.method)
        withContext(Dispatchers.Main) {
            if (prayers != null) {
                views.setTextViewText(R.id.widget_med_fajr, prayers["Fajr"] ?: "--:--")
                views.setTextViewText(R.id.widget_med_dhuhr, prayers["Dhuhr"] ?: "--:--")
                views.setTextViewText(R.id.widget_med_asr, prayers["Asr"] ?: "--:--")
                views.setTextViewText(R.id.widget_med_maghrib, prayers["Maghrib"] ?: "--:--")
                views.setTextViewText(R.id.widget_med_isha, prayers["Isha"] ?: "--:--")
                val next = getNextPrayer(prayers)
                views.setTextViewText(R.id.widget_med_next_name, PRAYER_NAMES_AR[next.name] ?: next.name)
                views.setTextViewText(R.id.widget_med_next_time, next.time)
                views.setTextViewText(R.id.widget_med_countdown, getCountdown(next.time))
                mgr.updateAppWidget(widgetId, views)
            }
        }
    }

    scheduleNextUpdate(ctx)
}

fun updateLargeWidget(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
    val views = RemoteViews(ctx.packageName, R.layout.widget_large)
    val now = Calendar.getInstance()

    // Time
    var hr = now.get(Calendar.HOUR_OF_DAY)
    val mn = now.get(Calendar.MINUTE)
    val ampm = if (hr >= 12) "مساءً" else "صباحاً"
    if (hr > 12) hr -= 12; if (hr == 0) hr = 12
    views.setTextViewText(R.id.widget_lg_time, "%02d:%02d".format(hr, mn))
    views.setTextViewText(R.id.widget_lg_ampm, ampm)

    // Dates
    val hijri = toHijri(now.time)
    views.setTextViewText(R.id.widget_lg_hijri, "${hijri.day} ${HIJRI_MONTHS[hijri.month - 1]} ${hijri.year} هـ")
    views.setTextViewText(R.id.widget_lg_greg, "${DAYS_AR[now.get(Calendar.DAY_OF_WEEK) - 1]}، ${now.get(Calendar.DAY_OF_MONTH)} ${GREG_MONTHS[now.get(Calendar.MONTH)]}")

    // Moon phase
    val moon = getMoonPhase()
    views.setTextViewText(R.id.widget_lg_moon_icon, MOON_ICONS[moon.phase] ?: "🌙")
    views.setTextViewText(R.id.widget_lg_moon_phase, moon.name)
    views.setTextViewText(R.id.widget_lg_moon_day, "اليوم ${moon.day} من الشهر")

    // Open app on click
    val launchIntent = Intent(ctx, MainActivity::class.java)
    val pi = PendingIntent.getActivity(ctx, 2, launchIntent, PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.widget_lg_time, pi)

    mgr.updateAppWidget(widgetId, views)

    CoroutineScope(Dispatchers.IO).launch {
        val data = getSavedLocation(ctx)

        // Weather
        val weather = fetchWeather(data.lat, data.lon)
        withContext(Dispatchers.Main) {
            if (weather != null) {
                views.setTextViewText(R.id.widget_lg_temp, "${weather.temp}°")
                views.setTextViewText(R.id.widget_lg_weather, weather.desc)
                mgr.updateAppWidget(widgetId, views)
            }
        }

        // Prayers
        val prayers = fetchPrayerTimes(data.lat, data.lon, data.method)
        withContext(Dispatchers.Main) {
            if (prayers != null) {
                views.setTextViewText(R.id.widget_lg_fajr, prayers["Fajr"] ?: "--:--")
                views.setTextViewText(R.id.widget_lg_sunrise, prayers["Sunrise"] ?: "--:--")
                views.setTextViewText(R.id.widget_lg_dhuhr, prayers["Dhuhr"] ?: "--:--")
                views.setTextViewText(R.id.widget_lg_asr, prayers["Asr"] ?: "--:--")
                views.setTextViewText(R.id.widget_lg_maghrib, prayers["Maghrib"] ?: "--:--")
                views.setTextViewText(R.id.widget_lg_isha, prayers["Isha"] ?: "--:--")
                val next = getNextPrayer(prayers)
                views.setTextViewText(R.id.widget_lg_next_name, PRAYER_NAMES_AR[next.name] ?: next.name)
                views.setTextViewText(R.id.widget_lg_countdown, getCountdown(next.time))
                mgr.updateAppWidget(widgetId, views)
            }
        }
    }

    scheduleNextUpdate(ctx)
}

// ==================== HELPERS ====================

data class LocationData(val lat: Double, val lon: Double, val method: String)
data class WeatherData(val temp: Int, val desc: String)
data class NextPrayer(val name: String, val time: String)
data class HijriDate(val day: Int, val month: Int, val year: Int)
data class MoonPhase(val phase: String, val name: String, val day: Int)

fun getSavedLocation(ctx: Context): LocationData {
    val prefs = ctx.getSharedPreferences("islamic_app", Context.MODE_PRIVATE)
    return LocationData(
        lat = prefs.getFloat("lat", 21.3891f).toDouble(),
        lon = prefs.getFloat("lon", 39.8579f).toDouble(),
        method = prefs.getString("method", "4") ?: "4"
    )
}

suspend fun fetchPrayerTimes(lat: Double, lon: Double, method: String): Map<String, String>? {
    return try {
        val cal = Calendar.getInstance()
        val url = "https://api.aladhan.com/v1/timings/${cal.get(Calendar.DAY_OF_MONTH)}" +
                "-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.YEAR)}" +
                "?latitude=$lat&longitude=$lon&method=$method"
        val response = URL(url).readText()
        val json = JSONObject(response)
        val timings = json.getJSONObject("data").getJSONObject("timings")
        mapOf(
            "Fajr" to timings.getString("Fajr"),
            "Sunrise" to timings.getString("Sunrise"),
            "Dhuhr" to timings.getString("Dhuhr"),
            "Asr" to timings.getString("Asr"),
            "Maghrib" to timings.getString("Maghrib"),
            "Isha" to timings.getString("Isha")
        )
    } catch (e: Exception) { null }
}

suspend fun fetchWeather(lat: Double, lon: Double): WeatherData? {
    return try {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weathercode&timezone=auto"
        val response = URL(url).readText()
        val json = JSONObject(response).getJSONObject("current")
        val temp = json.getDouble("temperature_2m").toInt()
        val code = json.getInt("weathercode")
        WeatherData(temp, weatherDesc(code))
    } catch (e: Exception) { null }
}

fun weatherDesc(code: Int): String = when {
    code == 0 -> "☀️ صافٍ"
    code <= 3 -> "⛅ غائم"
    code <= 49 -> "🌫️ ضبابي"
    code <= 69 -> "🌧️ أمطار"
    code <= 79 -> "❄️ ثلوج"
    code <= 99 -> "⛈️ عاصفة"
    else -> "🌡️ متغير"
}

fun getNextPrayer(prayers: Map<String, String>): NextPrayer {
    val now = Calendar.getInstance()
    val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val order = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
    for (name in order) {
        val time = prayers[name] ?: continue
        val (h, m) = time.split(":").map { it.trim().toInt() }
        if (h * 60 + m > nowMin) return NextPrayer(name, time)
    }
    return NextPrayer("Fajr", prayers["Fajr"] ?: "--:--")
}

fun getCountdown(prayerTime: String): String {
    val now = Calendar.getInstance()
    val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val parts = prayerTime.split(":").map { it.trim().toIntOrNull() ?: 0 }
    val pMin = parts[0] * 60 + parts[1]
    var diff = pMin - nowMin
    if (diff < 0) diff += 1440
    val h = diff / 60; val m = diff % 60
    return "%02d:%02d".format(h, m)
}

fun toHijri(date: java.util.Date): HijriDate {
    val jd = (date.time / 86400000 + 2440587.5).toLong()
    var l = jd - 1948440 + 10632
    val n = (l - 1) / 10631
    l = l - 10631 * n + 354
    val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
    l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
    val month = (24 * l) / 709
    val day = l - (709 * month) / 24
    val year = 30 * n + j - 30
    return HijriDate(day.toInt(), month.toInt(), year.toInt())
}

fun getMoonPhase(): MoonPhase {
    val now = java.util.Date()
    val knownNew = java.util.Date(124, 0, 11) // Jan 11, 2024
    val diff = (now.time - knownNew.time) / (1000.0 * 60 * 60 * 24)
    val cycle = 29.53059
    val phase = ((diff % cycle) + cycle) % cycle
    val pct = phase / cycle
    val day = phase.roundToInt()

    val (name, key) = when {
        pct < 0.03 || pct > 0.97 -> Pair("المحاق", "new")
        pct < 0.22 -> Pair("الهلال", "waxing_crescent")
        pct < 0.28 -> Pair("التربيع الأول", "first_quarter")
        pct < 0.47 -> Pair("الأحدب المتزايد", "waxing_gibbous")
        pct < 0.53 -> Pair("البدر", "full")
        pct < 0.72 -> Pair("الأحدب المتناقص", "waning_gibbous")
        pct < 0.78 -> Pair("التربيع الأخير", "last_quarter")
        else -> Pair("الهلال الأخير", "waning_crescent")
    }
    return MoonPhase(key, name, day)
}

fun Double.roundToInt(): Int = kotlin.math.roundToInt(this)

fun scheduleNextUpdate(ctx: Context) {
    val alarmMgr = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(ctx, IslamicWidgetSmall::class.java).apply {
        action = ACTION_UPDATE_WIDGET
    }
    val pi = PendingIntent.getBroadcast(ctx, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val nextMinute = Calendar.getInstance().apply {
        add(Calendar.MINUTE, 1)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    try {
        alarmMgr.setExact(AlarmManager.RTC, nextMinute.timeInMillis, pi)
    } catch (e: SecurityException) {
        alarmMgr.set(AlarmManager.RTC, nextMinute.timeInMillis, pi)
    }
}

// ==================== CONSTANTS ====================

val HIJRI_MONTHS = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
    "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
    "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
)

val GREG_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
)

val DAYS_AR = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

val PRAYER_NAMES_AR = mapOf(
    "Fajr" to "الفجر", "Sunrise" to "الشروق", "Dhuhr" to "الظهر",
    "Asr" to "العصر", "Maghrib" to "المغرب", "Isha" to "العشاء"
)

val PRAYER_ICONS = mapOf(
    "Fajr" to "🌙", "Sunrise" to "🌅", "Dhuhr" to "☀️",
    "Asr" to "🌤", "Maghrib" to "🌇", "Isha" to "⭐"
)

val MOON_ICONS = mapOf(
    "new" to "🌑", "waxing_crescent" to "🌒", "first_quarter" to "🌓",
    "waxing_gibbous" to "🌔", "full" to "🌕", "waning_gibbous" to "🌖",
    "last_quarter" to "🌗", "waning_crescent" to "🌘"
)
