package com.stanleycx.fruitos.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

data class WidgetGridRect(
    val col: Float,
    val row: Float,
    val colSpan: Int,
    val rowSpan: Int
)

class WidgetResizeState {
    var resizingWidgetId: String? by mutableStateOf(null)
        private set
    var originalRect: WidgetGridRect? by mutableStateOf(null)
        private set
    // Offset pixel EFFECTIF (déjà orienté selon le handle) depuis le début du drag
    var rawOffsetX: Float by mutableFloatStateOf(0f)
        private set
    var rawOffsetY: Float by mutableFloatStateOf(0f)
        private set

    private var cellWPx: Float = 1f
    private var cellHPx: Float = 1f

    // Bornes de span imposées par le widget (min/max cellules). Défauts = grille entière
    // pour rester rétro-compatible avec les appelants qui ne les fournissent pas.
    private var minColSpan: Int = 1
    private var maxColSpan: Int = 4
    private var minRowSpan: Int = 1
    private var maxRowSpan: Int = 6

    val isResizing: Boolean get() = resizingWidgetId != null

    /** Appelé UNE FOIS au début du drag. */
    fun startResize(
        widgetId: String,
        currentRect: WidgetGridRect,
        cellWPx: Float,
        cellHPx: Float,
        minColSpan: Int = 1,
        maxColSpan: Int = 4,
        minRowSpan: Int = 1,
        maxRowSpan: Int = 6
    ) {
        resizingWidgetId = widgetId
        originalRect = currentRect
        this.cellWPx = cellWPx
        this.cellHPx = cellHPx
        this.minColSpan = minColSpan.coerceIn(1, 4)
        this.maxColSpan = maxColSpan.coerceIn(this.minColSpan, 4)
        this.minRowSpan = minRowSpan.coerceIn(1, 6)
        this.maxRowSpan = maxRowSpan.coerceIn(this.minRowSpan, 6)
        rawOffsetX = 0f
        rawOffsetY = 0f
    }

    /** Appelé à chaque frame du drag — offset pixel orienté (effectiveDx/Y positif = plus grand). */
    fun updateRawOffset(dx: Float, dy: Float) {
        if (resizingWidgetId != null) {
            rawOffsetX = dx
            rawOffsetY = dy
        }
    }

    /** Snape vers la grille et retourne le rect final. */
    fun commit(): WidgetGridRect? {
        val orig = originalRect ?: return null
        val newCs = (orig.colSpan + (rawOffsetX / cellWPx).roundToInt()).coerceIn(minColSpan, maxColSpan)
        val newRs = (orig.rowSpan + (rawOffsetY / cellHPx).roundToInt()).coerceIn(minRowSpan, maxRowSpan)
        val result = orig.copy(colSpan = newCs, rowSpan = newRs)
        clear()
        return result
    }

    fun cancel() = clear()

    private fun clear() {
        resizingWidgetId = null
        originalRect = null
        rawOffsetX = 0f
        rawOffsetY = 0f
    }
}

@Composable
fun rememberWidgetResizeState(): WidgetResizeState = remember { WidgetResizeState() }
