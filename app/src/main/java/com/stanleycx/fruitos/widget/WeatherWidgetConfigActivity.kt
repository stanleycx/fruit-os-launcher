package com.stanleycx.fruitos.widget

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.ui.theme.FruitOSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        setContent {
            FruitOSTheme {
                val ok = {
                    setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                    finish()
                }
                WeatherConfigScreen(
                    initialTheme = WeatherWidgetProvider.getTheme(this, appWidgetId),
                    onSelect = { geo, theme ->
                        WeatherWidgetProvider.saveLocation(this, appWidgetId, geo.lat, geo.lon, geo.name)
                        WeatherWidgetProvider.saveTheme(this, appWidgetId, theme)
                        WeatherWidgetProvider.updateOne(this, appWidgetId)
                        ok()
                    },
                    onApplyTheme = { theme ->
                        WeatherWidgetProvider.saveTheme(this, appWidgetId, theme)
                        WeatherWidgetProvider.updateOne(this, appWidgetId)
                        ok()
                    },
                    onCancel = { setResult(Activity.RESULT_CANCELED); finish() }
                )
            }
        }
    }
}

@Composable
private fun WeatherConfigScreen(
    initialTheme: String,
    onSelect: (WeatherWidgetProvider.Companion.GeoResult, String) -> Unit,
    onApplyTheme: (String) -> Unit,
    onCancel: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherWidgetProvider.Companion.GeoResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var theme by remember { mutableStateOf(initialTheme) }
    val scope = rememberCoroutineScope()

    fun search() {
        if (query.isBlank()) return
        loading = true
        scope.launch {
            val r = withContext(Dispatchers.IO) { WeatherWidgetProvider.geocodeCity(query.trim()) }
            results = r
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Météo", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Text("THÈME", color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        ThemeSegmented(selected = theme, onSelect = { theme = it })
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { onApplyTheme(theme) }, modifier = Modifier.fillMaxWidth()) {
            Text("Appliquer le thème")
        }

        Spacer(Modifier.height(20.dp))
        Text("VILLE", color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Ville (ex. Paris, Lyon…)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF0A84FF), cursorColor = Color(0xFF0A84FF)
            )
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { search() }, modifier = Modifier.weight(1f)) { Text("Rechercher") }
            OutlinedButton(onClick = onCancel) { Text("Annuler") }
        }

        Spacer(Modifier.height(16.dp))
        if (loading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { geo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onSelect(geo, theme) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text(geo.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        if (geo.admin.isNotBlank()) {
                            Text(geo.admin, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSegmented(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        WeatherWidgetProvider.THEME_DARK to "Sombre",
        WeatherWidgetProvider.THEME_LIGHT to "Clair"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSel) Color(0xFF0A84FF) else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = Color.White, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}
