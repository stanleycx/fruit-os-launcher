package com.stanleycx.fruitos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore by preferencesDataStore(name = "launcher_widgets")
private val WIDGET_KEY = stringPreferencesKey("widget_layout")

class WidgetRepository(private val context: Context) {

    suspend fun load(): WidgetLayout {
        val raw = context.widgetDataStore.data
            .map { it[WIDGET_KEY] ?: "" }
            .first()
        return widgetLayoutFromJson(raw)
    }

    suspend fun save(layout: WidgetLayout) {
        context.widgetDataStore.edit { prefs ->
            prefs[WIDGET_KEY] = widgetLayoutToJson(layout)
        }
    }
}
