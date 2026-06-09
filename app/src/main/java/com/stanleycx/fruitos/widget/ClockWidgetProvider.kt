package com.stanleycx.fruitos.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.widget.RemoteViews
import com.stanleycx.fruitos.R

/**
 * Horloge unifiée : style ANALOGIQUE ou NUMÉRIQUE + thème CLAIR ou SOMBRE (choisis à la config).
 * Les vues `AnalogClock`/`TextClock` (natives) s'animent seules → aucune update / alarme.
 */
class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, id))
        }
    }

    companion object {
        const val STYLE_ANALOG = "analog"
        const val STYLE_DIGITAL = "digital"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences("launcher_widgets", Context.MODE_PRIVATE)

        fun getStyle(context: Context, id: Int): String =
            prefs(context).getString("clock_style_$id", STYLE_ANALOG) ?: STYLE_ANALOG

        fun getTheme(context: Context, id: Int): String =
            prefs(context).getString("clock_theme_$id", THEME_DARK) ?: THEME_DARK

        fun saveConfig(context: Context, id: Int, style: String, theme: String) {
            prefs(context).edit()
                .putString("clock_style_$id", style)
                .putString("clock_theme_$id", theme)
                .apply()
        }

        private fun buildViews(context: Context, id: Int): RemoteViews {
            val light = getTheme(context, id) == THEME_LIGHT
            return if (getStyle(context, id) == STYLE_DIGITAL) {
                RemoteViews(context.packageName, R.layout.widget_clock_digital).apply {
                    if (light) {
                        setInt(R.id.clock_root, "setBackgroundResource", R.drawable.widget_rounded_bg_light)
                        setTextColor(R.id.clock_time, Color.parseColor("#1C1C1E"))
                        setTextColor(R.id.clock_date, Color.parseColor("#FF9500"))
                    } else {
                        setInt(R.id.clock_root, "setBackgroundResource", R.drawable.widget_rounded_bg)
                        setTextColor(R.id.clock_time, Color.WHITE)
                        setTextColor(R.id.clock_date, Color.parseColor("#FF9F0A"))
                    }
                }
            } else {
                // Analogique : layout dédié par thème (les drawables dial/aiguilles ne sont pas
                // modifiables via RemoteViews).
                RemoteViews(context.packageName, if (light) R.layout.widget_clock_light else R.layout.widget_clock)
            }
        }

        fun updateAll(context: Context) {
            val awm = AppWidgetManager.getInstance(context)
            val ids = awm.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(Intent(context, ClockWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            })
        }

        fun updateOne(context: Context, id: Int) {
            context.sendBroadcast(Intent(context, ClockWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))
            })
        }
    }
}
