package com.stanleycx.fruitos.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.stanleycx.fruitos.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.Executors

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWeatherWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWeatherWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = getWidgetPrefs(context)
        val lat = prefs.getString("weather_lat_$appWidgetId", null)?.toDoubleOrNull() ?: 48.8566
        val lon = prefs.getString("weather_lon_$appWidgetId", null)?.toDoubleOrNull() ?: 2.3522
        val city = prefs.getString("weather_city_$appWidgetId", null) ?: "Paris"
        val light = getTheme(context, appWidgetId) == THEME_LIGHT

        // État « chargement » immédiat.
        val loading = RemoteViews(context.packageName, R.layout.widget_weather)
        applyTheme(loading, light)
        loading.setTextViewText(R.id.location, city)
        loading.setTextViewText(R.id.temp, "…")
        loading.setTextViewText(R.id.desc, "Mise à jour…")
        loading.setTextViewText(R.id.weather_icon, "⌛")
        loading.setTextViewText(R.id.max_temp, "")
        loading.setTextViewText(R.id.min_temp, "")
        appWidgetManager.updateAppWidget(appWidgetId, loading)

        Executors.newSingleThreadExecutor().execute {
            val data = try { fetchWeather(lat, lon) } catch (e: Exception) { null }
            Handler(Looper.getMainLooper()).post {
                val views = RemoteViews(context.packageName, R.layout.widget_weather)
                applyTheme(views, light)
                views.setTextViewText(R.id.location, city)
                if (data != null) {
                    views.setTextViewText(R.id.temp, "${data.temp}°")
                    views.setTextViewText(R.id.desc, data.description)
                    views.setTextViewText(R.id.weather_icon, data.icon)
                    views.setTextViewText(R.id.max_temp, "↑ ${data.maxTemp}°")
                    views.setTextViewText(R.id.min_temp, "↓ ${data.minTemp}°")
                } else {
                    views.setTextViewText(R.id.temp, "—")
                    views.setTextViewText(R.id.desc, "Indisponible")
                    views.setTextViewText(R.id.weather_icon, "☁️")
                    views.setTextViewText(R.id.max_temp, "")
                    views.setTextViewText(R.id.min_temp, "")
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    data class WeatherData(
        val temp: Int, val description: String, val icon: String, val maxTemp: Int, val minTemp: Int
    )

    companion object {
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"

        private fun getWidgetPrefs(context: Context): SharedPreferences =
            context.getSharedPreferences("launcher_widgets", Context.MODE_PRIVATE)

        fun getTheme(context: Context, id: Int): String =
            getWidgetPrefs(context).getString("weather_theme_$id", THEME_DARK) ?: THEME_DARK

        fun saveTheme(context: Context, id: Int, theme: String) {
            getWidgetPrefs(context).edit().putString("weather_theme_$id", theme).apply()
        }

        /** Applique le thème (clair/sombre) au RemoteViews météo. */
        private fun applyTheme(views: RemoteViews, light: Boolean) {
            views.setInt(
                R.id.weather_root, "setBackgroundResource",
                if (light) R.drawable.widget_rounded_bg_light else R.drawable.widget_rounded_bg
            )
            val primary = if (light) Color.parseColor("#1C1C1E") else Color.WHITE
            val secondary = if (light) Color.parseColor("#6C6C70") else Color.parseColor("#C7C7CC")
            views.setTextColor(R.id.location, primary)
            views.setTextColor(R.id.temp, primary)
            views.setTextColor(R.id.desc, secondary)
            views.setTextColor(R.id.max_temp, if (light) Color.parseColor("#FF9500") else Color.parseColor("#FF9F0A"))
            views.setTextColor(R.id.min_temp, if (light) Color.parseColor("#0A84FF") else Color.parseColor("#64D2FF"))
        }

        /** Sauve une localisation (lat/lon + nom) géocodée à la config. */
        fun saveLocation(context: Context, appWidgetId: Int, lat: Double, lon: Double, name: String) {
            getWidgetPrefs(context).edit()
                .putString("weather_lat_$appWidgetId", lat.toString())
                .putString("weather_lon_$appWidgetId", lon.toString())
                .putString("weather_city_$appWidgetId", name)
                .apply()
        }

        data class GeoResult(val name: String, val admin: String, val lat: Double, val lon: Double)

        /** Géocodage Open-Meteo (sans clé). À appeler hors thread principal. */
        fun geocodeCity(query: String): List<GeoResult> {
            return try {
                val q = URLEncoder.encode(query, "UTF-8")
                val url = "https://geocoding-api.open-meteo.com/v1/search?name=$q&count=6&language=fr"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"; connectTimeout = 6000; readTimeout = 6000
                    setRequestProperty("User-Agent", "FruitOSLauncher/1.0")
                }
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val results = json.optJSONArray("results") ?: return emptyList()
                (0 until results.length()).map { i ->
                    val o = results.getJSONObject(i)
                    GeoResult(
                        name = o.optString("name"),
                        admin = listOfNotNull(
                            o.optString("admin1").ifBlank { null },
                            o.optString("country").ifBlank { null }
                        ).joinToString(", "),
                        lat = o.getDouble("latitude"),
                        lon = o.getDouble("longitude")
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        private fun fetchWeather(lat: Double, lon: Double): WeatherData? {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 6000; readTimeout = 6000
                setRequestProperty("User-Agent", "FruitOSLauncher/1.0")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val current = json.optJSONObject("current") ?: return null
            val temp = current.optDouble("temperature_2m", Double.NaN)
            if (temp.isNaN()) return null
            val code = current.optInt("weather_code", 0)
            val daily = json.optJSONObject("daily")
            val maxArr = daily?.optJSONArray("temperature_2m_max")
            val minArr = daily?.optJSONArray("temperature_2m_min")
            val maxT = if (maxArr != null && maxArr.length() > 0) maxArr.getDouble(0).toInt() else temp.toInt()
            val minT = if (minArr != null && minArr.length() > 0) minArr.getDouble(0).toInt() else temp.toInt()
            return WeatherData(temp.toInt(), codeToDesc(code), codeToIcon(code), maxT, minT)
        }

        private fun codeToDesc(code: Int): String = when (code) {
            0 -> "Dégagé"
            1, 2 -> "Peu nuageux"
            3 -> "Couvert"
            45, 48 -> "Brouillard"
            51, 53, 55 -> "Bruine"
            56, 57 -> "Bruine verglaçante"
            61, 63, 65 -> "Pluie"
            66, 67 -> "Pluie verglaçante"
            71, 73, 75, 77 -> "Neige"
            80, 81, 82 -> "Averses"
            85, 86 -> "Averses de neige"
            95 -> "Orage"
            96, 99 -> "Orage de grêle"
            else -> "Variable"
        }

        private fun codeToIcon(code: Int): String = when (code) {
            0 -> "☀️"
            1, 2 -> "🌤️"
            3 -> "☁️"
            45, 48 -> "🌫️"
            51, 53, 55, 56, 57 -> "🌦️"
            61, 63, 65, 66, 67 -> "🌧️"
            71, 73, 75, 77, 85, 86 -> "❄️"
            80, 81, 82 -> "🌦️"
            95, 96, 99 -> "⛈️"
            else -> "🌡️"
        }

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
            val intent = android.content.Intent(context, WeatherWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }

        /** Met à jour un seul widget (après config). */
        fun updateOne(context: Context, appWidgetId: Int) {
            val intent = android.content.Intent(context, WeatherWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            context.sendBroadcast(intent)
        }
    }
}
