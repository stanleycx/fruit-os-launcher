package com.stanleycx.fruitos.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.layoutDataStore by preferencesDataStore(name = "launcher_layout")
private val LAYOUT_KEY = stringPreferencesKey("layout_data")

/**
 * Sauvegarde / chargement du LauncherLayout (DataStore).
 *
 * Format JSON (nouveau) — supporte apps ET dossiers.
 * Repli automatique sur l'ancien format texte (apps seulement) si besoin.
 */
class LayoutRepository(private val context: Context) {

    suspend fun load(): LauncherLayout {
        val raw = context.layoutDataStore.data
            .map { prefs: Preferences -> prefs[LAYOUT_KEY] ?: "" }
            .first()

        return if (raw.isBlank()) LauncherLayout.Empty else deserialize(raw)
    }

    suspend fun save(layout: LauncherLayout) {
        val serialized = serialize(layout)
        context.layoutDataStore.edit { prefs -> prefs[LAYOUT_KEY] = serialized }
    }

    // ── Sérialisation JSON ───────────────────────────────────────────────────

    private fun serialize(layout: LauncherLayout): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("dock", JSONArray(layout.dock))
        root.put("hidden", JSONArray(layout.hidden.toList()))

        val pagesArr = JSONArray()
        for (page in layout.pages) {
            val pageObj = JSONObject()
            for ((slot, item) in page) {
                val itemObj = JSONObject()
                when (item) {
                    is LayoutItem.App -> {
                        itemObj.put("t", "app")
                        itemObj.put("p", item.packageName)
                    }
                    is LayoutItem.Folder -> {
                        itemObj.put("t", "folder")
                        itemObj.put("id", item.id)
                        itemObj.put("name", item.name)
                        itemObj.put("apps", JSONArray(item.packageNames))
                    }
                }
                pageObj.put(slot.toString(), itemObj)
            }
            pagesArr.put(pageObj)
        }
        root.put("pages", pagesArr)
        return root.toString()
    }

    private fun deserialize(raw: String): LauncherLayout {
        return try {
            if (raw.trimStart().startsWith("{")) parseJson(raw) else parseLegacy(raw)
        } catch (e: Exception) {
            try { parseLegacy(raw) } catch (e2: Exception) { LauncherLayout.Empty }
        }
    }

    private fun parseJson(raw: String): LauncherLayout {
        val root = JSONObject(raw)

        val dock = root.optJSONArray("dock")
            ?.let { arr -> List(arr.length()) { arr.getString(it) } } ?: emptyList()

        val hidden = root.optJSONArray("hidden")
            ?.let { arr -> List(arr.length()) { arr.getString(it) } }?.toSet() ?: emptySet()

        val pages = mutableListOf<Map<Int, LayoutItem>>()
        val pagesArr = root.optJSONArray("pages")
        if (pagesArr != null) {
            for (i in 0 until pagesArr.length()) {
                val pageObj = pagesArr.getJSONObject(i)
                val page = mutableMapOf<Int, LayoutItem>()
                val keys = pageObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val slot = k.toIntOrNull() ?: continue
                    val itemObj = pageObj.getJSONObject(k)
                    when (itemObj.optString("t")) {
                        "app" -> {
                            val p = itemObj.optString("p")
                            if (p.isNotBlank()) page[slot] = LayoutItem.App(p)
                        }
                        "folder" -> {
                            val id = itemObj.optString("id")
                            val name = itemObj.optString("name")
                            val appsArr = itemObj.optJSONArray("apps")
                            val pkgs = appsArr?.let { a -> List(a.length()) { a.getString(it) } }
                                ?: emptyList()
                            if (id.isNotBlank() && pkgs.isNotEmpty()) {
                                page[slot] = LayoutItem.Folder(id, name, pkgs)
                            }
                        }
                    }
                }
                pages.add(page)
            }
        }

        return LauncherLayout(
            pages = if (pages.isEmpty()) listOf(emptyMap()) else pages,
            dock = dock,
            hidden = hidden
        )
    }

    // ── Ancien format texte (rétrocompat lecture seule) ───────────────────────

    private fun parseLegacy(raw: String): LauncherLayout {
        val dock = mutableListOf<String>()
        val pages = mutableListOf<MutableMap<Int, LayoutItem>>()
        val hidden = mutableSetOf<String>()

        var currentSection: String? = null
        var legacyCounter = 0

        for (line in raw.lines()) {
            when (line) {
                "DOCK" -> currentSection = "DOCK"
                "PAGE" -> { currentSection = "PAGE"; pages.add(mutableMapOf()); legacyCounter = 0 }
                "HIDDEN" -> currentSection = "HIDDEN"
                else -> {
                    if (line.isBlank()) continue
                    when (currentSection) {
                        "DOCK" -> dock.add(line)
                        "PAGE" -> {
                            val page = pages.lastOrNull() ?: continue
                            val colonIndex = line.indexOf(':')
                            if (colonIndex > 0) {
                                val slot = line.substring(0, colonIndex).toIntOrNull()
                                val pkg = line.substring(colonIndex + 1)
                                if (slot != null && pkg.isNotBlank()) page[slot] = LayoutItem.App(pkg)
                            } else {
                                page[legacyCounter] = LayoutItem.App(line)
                                legacyCounter++
                            }
                        }
                        "HIDDEN" -> hidden.add(line)
                    }
                }
            }
        }

        return LauncherLayout(
            pages = if (pages.isEmpty()) listOf(emptyMap()) else pages,
            dock = dock,
            hidden = hidden
        )
    }
}