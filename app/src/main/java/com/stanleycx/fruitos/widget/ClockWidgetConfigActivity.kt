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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.ui.theme.FruitOSTheme

class ClockWidgetConfigActivity : ComponentActivity() {

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
                ClockConfigScreen(
                    initialStyle = ClockWidgetProvider.getStyle(this, appWidgetId),
                    initialTheme = ClockWidgetProvider.getTheme(this, appWidgetId),
                    onSave = { style, theme ->
                        ClockWidgetProvider.saveConfig(this, appWidgetId, style, theme)
                        ClockWidgetProvider.updateOne(this, appWidgetId)
                        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                        finish()
                    },
                    onCancel = { setResult(Activity.RESULT_CANCELED); finish() }
                )
            }
        }
    }
}

@Composable
private fun ClockConfigScreen(
    initialStyle: String,
    initialTheme: String,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var style by remember { mutableStateOf(initialStyle) }
    var theme by remember { mutableStateOf(initialTheme) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1E)).padding(24.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Horloge", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Text("STYLE", color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        SegmentedRow(
            options = listOf(
                ClockWidgetProvider.STYLE_ANALOG to "Analogique",
                ClockWidgetProvider.STYLE_DIGITAL to "Numérique"
            ),
            selected = style,
            onSelect = { style = it }
        )

        Spacer(Modifier.height(24.dp))
        Text("THÈME", color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        SegmentedRow(
            options = listOf(
                ClockWidgetProvider.THEME_DARK to "Sombre",
                ClockWidgetProvider.THEME_LIGHT to "Clair"
            ),
            selected = theme,
            onSelect = { theme = it }
        )

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Annuler") }
            Button(onClick = { onSave(style, theme) }, modifier = Modifier.weight(1f)) { Text("Enregistrer") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SegmentedRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
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
