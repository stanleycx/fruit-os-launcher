package com.stanleycx.fruitos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.data.AppInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.stanleycx.fruitos.ui.components.AppIcon
import com.stanleycx.fruitos.ui.components.GlassGlyphStyle
import com.stanleycx.fruitos.ui.components.GlassTintSource
import com.stanleycx.fruitos.ui.components.IconStyle
import com.stanleycx.fruitos.ui.components.IconStyleMode
import com.stanleycx.fruitos.ui.components.LocalIconHazeState
import com.stanleycx.fruitos.ui.components.LocalIconStyle
import dev.chrisbanes.haze.HazeState

/** Palette de couleurs proposée pour les modes Teinté et Verre. */
private val IconTintPalette = listOf(
    Color(0xFF5B9BD5), // bleu
    Color(0xFF2ECC71), // vert
    Color(0xFF9B59B6), // violet
    Color(0xFFFF69B4), // rose
    Color(0xFFE67E22), // orange
    Color(0xFFE74C3C), // rouge
    Color(0xFF1ABC9C), // teal
    Color(0xFFF1C40F), // or
    Color(0xFFFFFFFF), // blanc
    Color(0xFF8E8E93)  // gris
)

@Composable
fun IconStyleScreen(
    current: IconStyle,
    onChange: (IconStyle) -> Unit,
    onBack: () -> Unit,
    hazeState: HazeState,
    previewApps: List<AppInfo>,
    modifier: Modifier = Modifier
) {
    // Le mode Verre reprend le matériau verre du système → pas de couleur propre ici.
    val showPalette = current.mode == IconStyleMode.Tinted

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
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
                text = "Style des icônes",
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // === APERÇU live ===
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
                .height(140.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF3A3A3C))
        ) {
            // Le style sélectionné est fourni aux AppIcon de l'aperçu → rendu en direct.
            CompositionLocalProvider(
                LocalIconStyle provides current,
                LocalIconHazeState provides hazeState
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (previewApps.isEmpty()) {
                        Text(
                            text = "Aucune app à prévisualiser",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    } else {
                        previewApps.take(4).forEach { app ->
                            AppIcon(
                                app = app,
                                onClick = {},
                                showLabel = false,
                                iconSize = 60.dp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // === MODES ===
        Text(
            text = "STYLE",
            color = Color(0xFF6C6C70),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconStyleMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (mode == current.mode) Color(0xFFE5F1FF) else Color.White)
                        .clickable { onChange(current.copy(mode = mode)) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (mode) {
                                IconStyleMode.Default -> "Défaut"
                                IconStyleMode.Dark    -> "Sombre"
                                IconStyleMode.Tinted  -> "Teinté"
                                IconStyleMode.Glass   -> "Verre"
                            },
                            color = Color.Black,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (mode) {
                                IconStyleMode.Default -> "Couleurs d'origine des apps"
                                IconStyleMode.Dark    -> "Fond foncé, glyphe conservé"
                                IconStyleMode.Tinted  -> "Glyphe monochrome recoloré"
                                IconStyleMode.Glass   -> "Matériau verre (réglages verre du système)"
                            },
                            color = Color.Black.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                    }
                    if (mode == current.mode) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // === LIGHT BORDER TOGGLE (always visible, any style) ===
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { onChange(current.copy(lightBorder = !current.lightBorder)) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF007AFF)),
                contentAlignment = Alignment.Center
            ) {
                // Visual hint for border
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.5.dp, Color.White, RoundedCornerShape(3.dp))
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bordure légère",
                    color = Color.Black,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Comme sur le dock et les éléments en glass",
                    color = Color(0xFF8E8E93),
                    fontSize = 14.sp
                )
            }
            // Fruit OS-style toggle
            Box(
                modifier = Modifier
                    .size(width = 51.dp, height = 31.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (current.lightBorder) Color(0xFF34C759) else Color(0xFFE9E9EA))
                    .clickable { onChange(current.copy(lightBorder = !current.lightBorder)) },
                contentAlignment = if (current.lightBorder) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(27.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                )
            }
        }

        // === PALETTE (Teinté / Verre) ===
        if (showPalette) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "COULEUR",
                color = Color(0xFF6C6C70),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconTintPalette.forEach { color ->
                        val selected = colorsClose(color, current.tintColor)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) Color(0xFF007AFF) else Color.Black.copy(alpha = 0.12f),
                                    shape = CircleShape
                                )
                                .clickable { onChange(current.copy(tintColor = color)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (color == Color.White) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // === LOGO (mode Verre) : rendu du glyphe ===
        if (current.mode == IconStyleMode.Glass) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "LOGO",
                color = Color(0xFF6C6C70),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassGlyphStyle.entries.forEach { glyph ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (glyph == current.glassGlyph) Color(0xFFE5F1FF) else Color.White)
                            .clickable { onChange(current.copy(glassGlyph = glyph)) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (glyph) {
                                GlassGlyphStyle.SystemTint -> "Adapté à la teinte du système"
                                GlassGlyphStyle.Original   -> "Couleur de base"
                                GlassGlyphStyle.Mono       -> "Noir & blanc"
                                GlassGlyphStyle.CustomTint -> "Teinte personnalisée"
                            },
                            color = Color.Black,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (glyph == current.glassGlyph) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Sélecteur de couleur du logo (si « Teinte personnalisée »).
            if (current.glassGlyph == GlassGlyphStyle.CustomTint) {
                Spacer(modifier = Modifier.height(12.dp))
                InlineColorPicker(
                    initial = current.glassGlyphTintColor,
                    default = Color(0xFF5B9BD5),
                    onColorChange = { onChange(current.copy(glassGlyphTintColor = it)) }
                )
            }

            // === TEINTE DU VERRE : système ou personnalisée ===
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "TEINTE DU VERRE",
                color = Color(0xFF6C6C70),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassTintSource.entries.forEach { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (source == current.glassTintSource) Color(0xFFE5F1FF) else Color.White)
                            .clickable { onChange(current.copy(glassTintSource = source)) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (source) {
                                GlassTintSource.System -> "Teinte du système"
                                GlassTintSource.Custom -> "Teinte personnalisée"
                            },
                            color = Color.Black,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (source == current.glassTintSource) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
            if (current.glassTintSource == GlassTintSource.Custom) {
                Spacer(modifier = Modifier.height(12.dp))
                InlineColorPicker(
                    initial = current.glassCustomTint,
                    default = Color(0xFF5B9BD5),
                    onColorChange = { onChange(current.copy(glassCustomTint = it)) }
                )
            }

            // Opacité + luminosité du logo (réglables séparément).
            Spacer(modifier = Modifier.height(24.dp))
            LabeledSlider(
                label = "OPACITÉ",
                value = current.glassGlyphOpacity,
                valueRange = 0f..1f,
                valueLabel = "${(current.glassGlyphOpacity * 100).toInt()} %",
                defaultValue = 0.75f,
                onValueChange = { onChange(current.copy(glassGlyphOpacity = it)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            LabeledSlider(
                label = "LUMINOSITÉ",
                value = current.glassGlyphBrightness,
                valueRange = 0f..2f,
                valueLabel = "${(current.glassGlyphBrightness * 100).toInt()} %",
                defaultValue = 1f,
                onValueChange = { onChange(current.copy(glassGlyphBrightness = it)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Le style s'applique à toutes les icônes d'apps (accueil, dock, dossiers, App Library, Spotlight).",
            color = Color(0xFF8E8E93),
            fontSize = 13.sp,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        )
    }
}

/** Deux couleurs « identiques » à epsilon près (comparaison de swatches). */
private fun colorsClose(a: Color, b: Color): Boolean {
    fun close(x: Float, y: Float) = kotlin.math.abs(x - y) < 0.01f
    return close(a.red, b.red) && close(a.green, b.green) && close(a.blue, b.blue)
}

