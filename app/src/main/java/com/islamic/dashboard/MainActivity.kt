package com.islamic.dashboard

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar
import java.util.Date

class IslamicWidgetSmall : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateSmallWidget(ctx, mgr, it) }
    }
}

class IslamicWidgetMedium : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateMediumWidget(ctx, mgr, it) }
    }
}

class IslamicWidgetLarge : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateLargeWidget(ctx, mgr, it) }
    }
}

const val ACTION_UPDATE_WIDGET = "com.islamic.dashboard.UPDATE_WIDGET"

private val HIJRI_MONTHS = listOf(
    "محرم","صفر","ربيع الأول","ربيع الآخر","جمادى الأولى","جمادى الآخرة",
    "رجب","شعبان","رمضان","شوال","ذو القعدة","ذو الحجة"
)

private fun getTimeString(): String {
    val now = Calendar.getInstance()
    var h = now.get(Calendar.HOUR_OF_DAY)
    val m = now.get(Calendar.MINUTE)
    if (h > 12) h -= 12
    if (h == 0) h = 12
    return "%02d:%02d".format(h, m)
}

private fun getHijriDate(): String {
    val ts = Date().time
    val jd = (ts / 86400000 + 2440587.5).toLong()
    var l = jd - 1948440 + 10632
    val n = (l - 1) / 10631
    l = l - 10631 * n + 354
    val j = ((10985 - l) / 5316) * ((50 * l) / 17719) +
            (l / 5670) * ((43 * l) / 15238)
    l = l - ((30 - j) / 15) * ((17719 * j) / 50) -
            (j / 16) * ((15238 * j) / 43) + 29
    val month = ((24 * l) / 709).toInt()
    val day = (l - (709 * month) / 24).toInt()
    val year = (30 * n + j - 30).toInt()
    val mName = HIJRI_MONTHS.getOrElse(month - 1) { "" }
    return "$day $mName $year هـ"
}

private fun getMoonPhase(): String {
    val now = Date()
    @Suppress("DEPRECATION")
    val knownNew = Date(124, 0, 11)
    val diff = (now.time - knownNew.time) / (1000.0 * 60 * 60 * 24)
    val cycle = 29.53059
    val pct = ((diff % cycle) + cycle) % cycle / cycle
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

private fun getLaunchIntent(ctx: Context, reqCode: Int): PendingIntent {
    val i = Intent(ctx, MainActivity::class.java)
    return PendingIntent.getActivity(ctx, reqCode, i, PendingIntent.FLAG_IMMUTABLE)
}

fun updateSmallWidget(ctx: Context, mgr: AppWidgetManager, id: Int) {
    val v = RemoteViews(ctx.packageName, R.layout.widget_small)
    v.setTextViewText(R.id.widget_small_time, getTimeString())
    v.setTextViewText(R.id.widget_small_hijri, getHijriDate())
    v.setOnClickPendingIntent(R.id.widget_small_time, getLaunchIntent(ctx, 0))
    mgr.updateAppWidget(id, v)
}

fun updateMediumWidget(ctx: Context, mgr: AppWidgetManager, id: Int) {
    val v = RemoteViews(ctx.packageName, R.layout.widget_medium)
    v.setTextViewText(R.id.widget_med_time, getTimeString())
    v.setTextViewText(R.id.widget_med_hijri, getHijriDate())
    v.setTextViewText(R.id.widget_med_moon, getMoonPhase())
    v.setOnClickPendingIntent(R.id.widget_med_time, getLaunchIntent(ctx, 1))
    mgr.updateAppWidget(id, v)
}

fun updateLargeWidget(ctx: Context, mgr: AppWidgetManager, id: Int) {
    val v = RemoteViews(ctx.packageName, R.layout.widget_large)
    v.setTextViewText(R.id.widget_lg_time, getTimeString())
    v.setTextViewText(R.id.widget_lg_hijri, getHijriDate())
    v.setTextViewText(R.id.widget_lg_moon_phase, getMoonPhase())
    v.setOnClickPendingIntent(R.id.widget_lg_time, getLaunchIntent(ctx, 2))
    mgr.updateAppWidget(id, v)
}