package com.stanleycx.fruitos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Slider style Fruit OS : piste fine arrondie, pouce circulaire blanc ombré, teinte active bleue.
 *
 * Gère le geste lui-même via `awaitFirstDown` + `drag` (consommés) → la valeur suit le doigt
 * DÈS l'appui ET en continu pendant le glissement (plus de « réagit seulement au tap »), sans se
 * faire voler le geste par un scroll ou détecteur parent.
 */
@Composable
fun FruitSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    trackHeight: Dp = 4.dp,
    thumbSize: Dp = 28.dp,
    activeColor: Color = Color(0xFF007AFF),
    inactiveColor: Color = Color(0xFFD9D9DE)
) {
    val span = (valueRange.endInclusive - valueRange.start).let { if (it == 0f) 1f else it }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(thumbSize)) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val thumbPx = with(density) { thumbSize.toPx() }
        val trackHPx = with(density) { trackHeight.toPx() }
        val travel = (widthPx - thumbPx).coerceAtLeast(1f)

        fun valueFromX(x: Float): Float {
            val f = ((x - thumbPx / 2f) / travel).coerceIn(0f, 1f)
            return valueRange.start + f * span
        }

        val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
        val thumbX = thumbPx / 2f + fraction * travel

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbSize)
                .pointerInput(valueRange, travel) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        onValueChange(valueFromX(down.position.x))
                        drag(down.id) { change ->
                            change.consume()
                            onValueChange(valueFromX(change.position.x))
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(thumbSize)) {
                val cy = size.height / 2f
                val left = thumbPx / 2f
                val right = size.width - thumbPx / 2f
                val radius = trackHPx / 2f
                // Piste inactive
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(left, cy - trackHPx / 2f),
                    size = Size((right - left).coerceAtLeast(0f), trackHPx),
                    cornerRadius = CornerRadius(radius, radius)
                )
                // Piste active
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(left, cy - trackHPx / 2f),
                    size = Size((thumbX - left).coerceAtLeast(0f), trackHPx),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
            // Pouce
            Box(
                modifier = Modifier
                    .offset { IntOffset((thumbX - thumbPx / 2f).roundToInt(), 0) }
                    .size(thumbSize)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(0.5.dp, Color.Black.copy(alpha = 0.06f), CircleShape)
            )
        }
    }
}
