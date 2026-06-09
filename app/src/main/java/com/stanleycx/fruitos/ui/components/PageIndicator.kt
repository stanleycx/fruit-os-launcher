package com.stanleycx.fruitos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import dev.chrisbanes.haze.HazeState
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.glass
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.GlossLevel

/**
 * Indicateur de page style Fruit OS.
 *
 * Simples petits points (dots classiques). Le point actif est simplement plus visible.
 * Pendant le swipe, le highlight passe doucement d'un point à l'autre grâce à pageOffsetFraction.
 *
 * @param pageCount Nombre total de pages
 * @param currentPage Page actuelle (entière)
 * @param pageOffsetFraction Décalage en cours de swipe (fractionnaire)
 */
@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    pageOffsetFraction: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    if (pageCount < 1) return   // allow 1 page (single dot in the pill)

    // Dots style Fruit OS classiques (simples petits cercles, pas de ligne ni de transformation en capsule).
    // Le point actif est simplement plus visible (opacité plus élevée), le highlight glisse pendant le swipe.
    val dotSize = 6.dp
    val dotSpacing = 6.dp
    val rowWidth = dotSize * pageCount + dotSpacing * (pageCount - 1).coerceAtLeast(0)

    // PERF : la position du pager (pageOffsetFraction) change à CHAQUE frame pendant un swipe.
    // On la lit via lambda DANS le Canvas (phase de dessin) → seul le dessin des points se ré-exécute,
    // PAS la recomposition de l'indicateur ni de la pilule en verre qui l'entoure.
    Canvas(modifier = modifier.size(width = rowWidth, height = dotSize)) {
        val dotPx = dotSize.toPx()
        val spacingPx = dotSpacing.toPx()
        val r = dotPx / 2f
        val cy = size.height / 2f
        val activePosition = currentPage + pageOffsetFraction()
        for (index in 0 until pageCount) {
            // Proximité du point avec la position active (1 = exactement dessus, 0 = loin)
            val distance = abs(index - activePosition).coerceIn(0f, 1f)
            val proximity = 1f - distance
            // Opacité : les points inactifs sont discrets, le point actif est bien visible
            val alpha = 0.3f + 0.7f * proximity
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = r,
                center = Offset(r + index * (dotPx + spacingPx), cy)
            )
        }
    }
}

/**
 * Page indicator dots wrapped in a glass pill (same visual style as SpotlightTriggerButton).
 * The pill width automatically adapts to the number of pages (more dots = wider capsule).
 */
@Composable
fun PageIndicatorPill(
    pageCount: Int,
    currentPage: Int,
    pageOffsetFraction: () -> Float = { 0f },
    onPageClick: (Int) -> Unit = {},
    hazeState: HazeState,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
    modifier: Modifier = Modifier
) {
    if (pageCount < 1) return

    val dotSize = 6.dp
    val dotSpacing = 6.dp
    val horizontalPadding = 10.dp   // breathing room inside the pill, similar feel to search button

    // Calculate the natural width needed for the dots row
    val dotsContentWidth = if (pageCount == 1) {
        dotSize
    } else {
        dotSize * pageCount + dotSpacing * (pageCount - 1)
    }

    val pillWidth = dotsContentWidth + horizontalPadding * 2

    Box(
        modifier = modifier
            .size(width = pillWidth, height = 32.dp)
            .clip(CircleShape)
            .glass(
                hazeState = hazeState,
                level = glassLevel,
                glassTint = glassTint,
                customTintColor = customTintColor,
                shape = CircleShape,
                loupeLevel = loupeLevel, glossLevel = glossLevel
            )
            .pointerInput(pageCount) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val contentStartX = horizontalPadding.toPx()
                        val slotWidth = (dotSize + dotSpacing).toPx()
                        val relativeX = (offset.x - contentStartX).coerceAtLeast(0f)
                        val index = (relativeX / slotWidth).toInt().coerceIn(0, pageCount - 1)
                        onPageClick(index)
                    },
                    onDrag = { change, _ ->
                        val contentStartX = horizontalPadding.toPx()
                        val slotWidth = (dotSize + dotSpacing).toPx()
                        val relativeX = (change.position.x - contentStartX).coerceAtLeast(0f)
                        val index = (relativeX / slotWidth).toInt().coerceIn(0, pageCount - 1)
                        onPageClick(index)
                        change.consume()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        PageIndicator(
            pageCount = pageCount,
            currentPage = currentPage,
            pageOffsetFraction = pageOffsetFraction,
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )
    }
}