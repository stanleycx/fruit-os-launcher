package com.stanleycx.fruitos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.ui.components.FruitSlider

/** Slider Fruit OS avec libellé, valeur affichée et bouton reset (partagé entre écrans de réglages). */
@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    defaultValue: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF6C6C70), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(valueLabel, color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Réinitialiser",
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onValueChange(defaultValue) }
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        FruitSlider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

/**
 * Sélecteur de couleur façon Photoshop : un carré Saturation×Luminosité (tap/glisse) + une barre
 * de teinte. Aperçu + reset. Partagé entre les écrans de réglages.
 */
@Composable
fun InlineColorPicker(initial: Color, default: Color, onColorChange: (Color) -> Unit) {
    val hsv0 = remember { approxHsv(initial) }
    var hue by remember { mutableFloatStateOf(hsv0.first) }
    var sat by remember { mutableFloatStateOf(hsv0.second) }
    var value by remember { mutableFloatStateOf(hsv0.third) }
    val color = Color.hsv(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    val emit = { onColorChange(Color.hsv(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Réinitialiser",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable {
                            val d = approxHsv(default)
                            hue = d.first; sat = d.second; value = d.third
                            onColorChange(default)
                        }
                )
            }
            Spacer(Modifier.height(14.dp))
            SatValSquare(hue = hue, sat = sat, value = value) { s, v -> sat = s; value = v; emit() }
            Spacer(Modifier.height(14.dp))
            HueBar(hue = hue) { h -> hue = h; emit() }
        }
    }
}

/** Carré Saturation (x) × Luminosité (y) — pipette : tap/glisse pour choisir, comme Photoshop. */
@Composable
private fun SatValSquare(hue: Float, sat: Float, value: Float, onChange: (Float, Float) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                fun update(pos: Offset) {
                    val s = (pos.x / size.width).coerceIn(0f, 1f)
                    val v = (1f - pos.y / size.height).coerceIn(0f, 1f)
                    onChange(s, v)
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    update(down.position)
                    drag(down.id) { ch -> ch.consume(); update(ch.position) }
                }
            }
    ) {
        val density = LocalDensity.current
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        // Couches : teinte pure → dégradé blanc (saturation) → dégradé noir (luminosité).
        Box(Modifier.fillMaxSize().background(Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.White, Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
        // Pointeur (anneau blanc cerclé de noir).
        val tx = with(density) { (sat.coerceIn(0f, 1f) * wPx).toDp() } - 9.dp
        val ty = with(density) { ((1f - value.coerceIn(0f, 1f)) * hPx).toDp() } - 9.dp
        Box(
            modifier = Modifier
                .offset(x = tx, y = ty)
                .size(18.dp)
                .border(2.dp, Color.White, CircleShape)
                .border(3.5.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
        )
    }
}

/** Barre de teinte horizontale (tap/glisse). */
@Composable
private fun HueBar(hue: Float, onHueChange: (Float) -> Unit) {
    val spectrum = remember {
        listOf(0, 60, 120, 180, 240, 300, 360).map { Color.hsv(it.toFloat(), 1f, 1f) }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.horizontalGradient(spectrum))
            .pointerInput(Unit) {
                fun update(x: Float) = onHueChange((x / size.width).coerceIn(0f, 1f) * 360f)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    update(down.position.x)
                    drag(down.id) { ch -> ch.consume(); update(ch.position.x) }
                }
            }
    ) {
        val density = LocalDensity.current
        val wPx = constraints.maxWidth.toFloat()
        val tx = with(density) { (hue.coerceIn(0f, 360f) / 360f * wPx).toDp() } - 13.dp
        Box(
            modifier = Modifier
                .offset(x = tx)
                .align(Alignment.CenterStart)
                .size(26.dp)
                .border(3.dp, Color.White, CircleShape)
                .border(4.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
        )
    }
}

/** Color → HSV approximatif pour initialiser les sliders. */
private fun approxHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b); val delta = max - min
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
