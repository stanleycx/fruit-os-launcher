package com.stanleycx.fruitos.ui.settings

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.data.HomeItem
import com.stanleycx.fruitos.ui.components.FolderIcon
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.FruitSlider
import dev.chrisbanes.haze.HazeState

/**
 * "Nouvelle page" pour choisir une teinte personnalisée avec roue chromatique.
 */
@Composable
fun CustomGlassTintScreen(
    currentLevel: GlassLevel,
    initialColor: Color? = null,
    onApply: (Color) -> Unit,
    onBack: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    // Valeurs initiales basées sur la couleur existante ou un beau bleu par défaut
    val initialHsv = remember(initialColor) {
        initialColor?.let { approximateHsv(it) } ?: Triple(210f, 0.82f, 0.96f)
    }
    var hue by remember(initialColor) { mutableStateOf(initialHsv.first) }
    var saturation by remember(initialColor) { mutableStateOf(initialHsv.second) }
    var value by remember(initialColor) { mutableStateOf(initialHsv.third) }

    val customColor = Color.hsv(hue, saturation, value)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = Color(0xFF007AFF),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBack() }
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Autres couleurs",
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Même preview que dans la liste principale, mais avec la vraie icône de dossier
        // pour voir l'effet du glassLevel (thickness) + teinte custom sur le dossier (comme le Dock).
        Text(
            text = "APERÇU",
            color = Color(0xFF6C6C70),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF1C1C1E))
        ) {
            val previewFolder = remember {
                HomeItem.Folder(
                    id = "preview_custom_folder",
                    name = "Dossier",
                    apps = emptyList()
                )
            }

            FolderIcon(
                folder = previewFolder,
                onOpen = {},
                isEditing = false,
                hazeState = hazeState,
                glassLevel = currentLevel,
                glassTint = GlassTint.Custom,
                customTintColor = customColor,
                showLabel = true,
                iconSize = 78.dp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ROUE CHROMATIQUE",
                color = Color(0xFF6C6C70),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Réinitialiser",
                tint = Color(0xFF007AFF),
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { hue = 210f; saturation = 0.82f; value = 0.96f }
            )
        }

        // === Roue chromatique (sliders HSV Fruit OS) ===
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Teinte", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            FruitSlider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)

            Spacer(Modifier.height(16.dp))

            Text("Saturation", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            FruitSlider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)

            Spacer(Modifier.height(16.dp))

            Text("Luminosité", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            FruitSlider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Bouton Appliquer → sauvegarde la couleur custom + revient
        Button(
            onClick = {
                onApply(customColor)
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text("Appliquer cette couleur")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Conversion approximative Color → HSV (suffisant pour initialiser les sliders depuis une couleur existante).
 */
private fun approximateHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val v = max
    val s = if (max == 0f) 0f else delta / max

    val h = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }

    return Triple(if (h < 0) h + 360f else h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
}