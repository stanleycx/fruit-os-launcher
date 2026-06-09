package com.stanleycx.fruitos.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stanleycx.fruitos.ui.components.IconOverride
import org.json.JSONObject

/**
 * Persistance des personnalisations d'icône PAR APP (fond de couleur, couleur de logo, zoom).
 * SharedPreferences "icon_overrides", clé "overrides" = objet JSON { pkg: {bg?, logo?, zoom} }.
 */
object IconOverrideStore {
    private const val PREFS = "icon_overrides"
    private const val KEY = "overrides"

    fun load(context: Context): Map<String, IconOverride> = runCatching {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyMap()
        val obj = JSONObject(json)
        buildMap {
            for (pkg in obj.keys()) {
                val o = obj.getJSONObject(pkg)
                put(
                    pkg,
                    IconOverride(
                        bgColor = if (o.has("bg")) Color(o.getInt("bg")) else null,
                        logoColor = if (o.has("logo")) Color(o.getInt("logo")) else null,
                        logoTint = if (o.has("tint")) Color(o.getInt("tint")) else null,
                        logoBrightness = o.optDouble("bri", 1.0).toFloat(),
                        logoZoom = o.optDouble("zoom", 1.0).toFloat()
                    )
                )
            }
        }
    }.getOrDefault(emptyMap())

    fun save(context: Context, overrides: Map<String, IconOverride>) {
        val obj = JSONObject()
        overrides.forEach { (pkg, ov) ->
            if (ov.isEmpty) return@forEach
            val o = JSONObject()
            ov.bgColor?.let { o.put("bg", it.toArgb()) }
            ov.logoColor?.let { o.put("logo", it.toArgb()) }
            ov.logoTint?.let { o.put("tint", it.toArgb()) }
            if (ov.logoBrightness != 1f) o.put("bri", ov.logoBrightness.toDouble())
            if (ov.logoZoom != 1f) o.put("zoom", ov.logoZoom.toDouble())
            obj.put(pkg, o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, obj.toString()).apply()
    }
}
