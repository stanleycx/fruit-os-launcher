package com.stanleycx.fruitos.widget

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.ui.theme.FruitOSTheme

class PhotoFrameWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            FruitOSTheme {
                PhotoConfigScreen(
                    onSave = { uris, folderUri, cycleMs ->
                        // Persist for individual photos
                        uris.forEach { uri ->
                            try {
                                contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (e: Exception) { }
                        }

                        // Persist folder if selected (efface les photos individuelles, mode exclusif)
                        folderUri?.let { treeUri ->
                            try {
                                // modeFlags = mode d'ACCÈS (READ) uniquement ; le bit PERSISTABLE
                                // n'est pas un mode valide ici et faisait échouer la prise de droit.
                                contentResolver.takePersistableUriPermission(
                                    treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (e: Exception) { }
                            PhotoFrameWidgetProvider.savePhotoFolderUri(this, appWidgetId, treeUri.toString())
                            PhotoFrameWidgetProvider.clearPhotoUris(this, appWidgetId)
                        } ?: run {
                            PhotoFrameWidgetProvider.clearPhotoFolderUri(this, appWidgetId)
                            if (uris.isNotEmpty()) {
                                PhotoFrameWidgetProvider.savePhotoUris(this, appWidgetId, uris.map { it.toString() })
                            }
                        }

                        PhotoFrameWidgetProvider.savePhotoCycleDuration(this, appWidgetId, cycleMs)
                        PhotoFrameWidgetProvider.applyCycle(this, appWidgetId)
                        PhotoFrameWidgetProvider.updateOne(this, appWidgetId)

                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(Activity.RESULT_OK, resultValue)
                        finish()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun PhotoConfigScreen(
    onSave: (List<Uri>, Uri?, Long) -> Unit,
    onCancel: () -> Unit
) {
    var selectedUris by remember { mutableStateOf(listOf<Uri>()) }
    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDurationMs by remember { mutableStateOf(0L) }

    // OpenDocument (ACTION_OPEN_DOCUMENT) → URI PERSISTABLE (lisible plus tard par le widget),
    // contrairement à GetContent dont l'URI n'est valable que le temps de la config.
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && !selectedUris.contains(uri)) {
            selectedUris = selectedUris + uri
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFolderUri = uri
            selectedUris = emptyList()
        }
    }

    // AlarmManager limite le défilement des widgets à ~1 min minimum.
    val durations = listOf(
        0L to "Off", 60000L to "1 min", 300000L to "5 min", 900000L to "15 min", 3600000L to "1 h"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Cadre Photo / Carousel",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Ajoutez des photos ou sélectionnez un dossier/album pour le carousel.",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { photoPickerLauncher.launch(arrayOf("image/*")) }) {
                Text("Ajouter photo")
            }
            Button(onClick = { folderPickerLauncher.launch(null) }) {
                Text("Choisir dossier/album")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (selectedFolderUri != null) {
            Text("Dossier sélectionné ✓", color = Color(0xFF4ECDC4), fontSize = 14.sp)
        } else if (selectedUris.isNotEmpty()) {
            Text("${selectedUris.size} photo(s) sélectionnée(s)", color = Color.White, fontSize = 14.sp)
        }

        Spacer(Modifier.height(20.dp))

        Text("Défilement automatique :", color = Color.Gray, fontSize = 13.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            durations.forEach { (ms, label) ->
                FilterChip(
                    selected = selectedDurationMs == ms,
                    onClick = { selectedDurationMs = ms },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onCancel) {
                Text("Annuler")
            }
            Button(
                onClick = { onSave(selectedUris, selectedFolderUri, selectedDurationMs) },
                enabled = selectedUris.isNotEmpty() || selectedFolderUri != null
            ) {
                Text("Enregistrer")
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Une seule photo = cadre fixe. Plusieurs photos ou un dossier = diaporama (défilement ≥ 1 min).",
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}