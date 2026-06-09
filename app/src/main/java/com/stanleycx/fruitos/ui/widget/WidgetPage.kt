package com.stanleycx.fruitos.ui.widget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.data.WidgetLayout
import com.stanleycx.fruitos.data.collidesOnPage
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.GlossLevel
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.glass
import com.stanleycx.fruitos.ui.home.EditModeState
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

/** Index pager de la page dédiée widgets. */
const val WIDGET_PAGE_INDEX = 0

/** Hauteur d'une rangée de la grille sur la page widgets. */
private val WIDGET_CELL_HEIGHT = 100.dp

/** Hauteur max d'un widget (en cellules) sur la page widgets — généreuse car la page scrolle. */
private const val WIDGET_PAGE_MAX_ROW_SPAN = 12

@Composable
fun WidgetPage(
    widgetLayout: WidgetLayout,
    editMode: EditModeState,
    resizeState: WidgetResizeState,
    hazeState: HazeState,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
    onAddWidget: () -> Unit,
    onRemoveWidget: (String) -> Unit,
    onResizeWidget: (String, WidgetGridRect) -> Unit,
    onReorderWidgets: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onSwipeBack: () -> Unit = {},
    onWidgetLongPress: (widgetId: String, touchPosInRoot: Offset) -> Unit = { _, _ -> },
    // La page peut être PRÉ-COMPOSÉE hors écran (pour une ouverture fluide) alors qu'on n'est
    // pas dessus. Le BackHandler ne doit s'activer que lorsqu'elle est réellement affichée,
    // sinon il capterait le geste/bouton Retour depuis l'écran d'accueil.
    navEnabled: Boolean = true
) {
    BackHandler(enabled = navEnabled) { onSwipeBack() }

    val density = LocalDensity.current
    val context = LocalContext.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val cellWidthDp = (screenWidthDp - 24.dp) / 4
    val cellHeightDp = WIDGET_CELL_HEIGHT

    // rememberUpdatedState pour widgetLayout : évite le stale closure dans onWidgetDragEnd
    val currentLayout by rememberUpdatedState(widgetLayout)

    val widgets = remember(widgetLayout) {
        widgetLayout.placements
            .filter { it.pageIndex == WIDGET_PAGE_INDEX }
            .sortedBy { it.row }
    }

    // État du drag sur la grille widget (déplacement 2D)
    var draggedWidgetId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Hauteur totale de la grille
    val totalRows = widgets.maxOfOrNull { (it.row + it.rowSpan).toInt() } ?: 0
    val gridHeight = cellHeightDp * totalRows.coerceAtLeast(3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(editMode.isEditing) {
                if (editMode.isEditing) {
                    // En édition : un tap dans le vide (entre les widgets ou sur les côtés)
                    // quitte l'édition. Tapoter un widget/poignée/FAB ne déclenche rien ici
                    // (ces enfants consomment le geste). Pas de swipe inter-pages en édition.
                    detectTapGestures(onTap = { editMode.exit() })
                } else {
                    // Hors édition : swipe horizontal (vers la gauche) → retour à la page d'apps.
                    var totalDx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDx = 0f },
                        onDragEnd = {
                            if (totalDx < -80f) onSwipeBack()
                            totalDx = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            totalDx += dragAmount
                            change.consume()
                        }
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState(), enabled = draggedWidgetId == null)
                .padding(horizontal = 12.dp)
                .padding(top = statusBarTop + 16.dp, bottom = navBottom + 32.dp)
        ) {
            Text(
                text = "Widgets",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Grille 4×N avec positionnement absolu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
            ) {
                if (widgets.isEmpty()) {
                    EmptyWidgetPlaceholder()
                }

                // Indicateur de snap pendant le drag
                draggedWidgetId?.let { dragId ->
                    val w = widgets.find { it.widgetId == dragId }
                    w?.let {
                        val cWPx = with(density) { cellWidthDp.toPx() }
                        val cHPx = with(density) { cellHeightDp.toPx() }
                        val snapCol = (it.col + dragOffset.x / cWPx)
                            .roundToInt().coerceIn(0, 4 - it.colSpan).toFloat()
                        val snapRow = (it.row + dragOffset.y / cHPx)
                            .roundToInt().coerceAtLeast(0).toFloat()
                        // Feedback collision : rouge si la cible chevauche un autre widget.
                        val snapCollides = currentLayout.collidesOnPage(
                            dragId, it.copy(col = snapCol, row = snapRow)
                        )
                        val snapColor = if (snapCollides) Color(0xFFFF3B30) else Color.White
                        Box(
                            modifier = Modifier
                                .offset(x = cellWidthDp * snapCol, y = cellHeightDp * snapRow)
                                .size(cellWidthDp * it.colSpan, cellHeightDp * it.rowSpan)
                                .clip(RoundedCornerShape(32.dp))
                                .background(snapColor.copy(alpha = 0.12f))
                                .border(2.dp, snapColor.copy(alpha = if (snapCollides) 0.6f else 0.35f), RoundedCornerShape(32.dp))
                        )
                    }
                }

                widgets.forEach { widget ->
                    // key() par widgetId : chaque WidgetCard (et son AppWidgetHostView) reste lié
                    // à SON widget. Sans ça, supprimer un widget recyclerait les vues par position
                    // → un autre widget s'afficherait/serait supprimé à la place.
                    key(widget.widgetId) {
                    val isBeingDragged = draggedWidgetId == widget.widgetId
                    val dxDp = if (isBeingDragged) with(density) { dragOffset.x.toDp() } else 0.dp
                    val dyDp = if (isBeingDragged) with(density) { dragOffset.y.toDp() } else 0.dp

                    WidgetCard(
                        placement = widget,
                        editMode = editMode,
                        resizeState = resizeState,
                        cellWidthDp = cellWidthDp,
                        cellHeightDp = cellHeightDp,
                        maxRowSpan = WIDGET_PAGE_MAX_ROW_SPAN,
                        isDragged = isBeingDragged,
                        onRemove = { onRemoveWidget(widget.widgetId) },
                        onResizeCommit = { rect ->
                            // Anti-superposition : un resize qui chevauche un autre widget est annulé (snap-back).
                            val candidate = widget.copy(
                                col = rect.col, row = rect.row,
                                colSpan = rect.colSpan, rowSpan = rect.rowSpan
                            )
                            if (currentLayout.collidesOnPage(widget.widgetId, candidate)) {
                                com.stanleycx.fruitos.ui.components.Haptics.light(context)
                            } else {
                                onResizeWidget(widget.widgetId, rect)
                            }
                        },
                        onLongPress = { offset -> onWidgetLongPress(widget.widgetId, offset) },
                        onWidgetDragStart = {
                            draggedWidgetId = widget.widgetId
                            dragOffset = Offset.Zero
                        },
                        onWidgetDrag = { delta ->
                            if (draggedWidgetId == widget.widgetId) dragOffset += delta
                        },
                        onWidgetDragEnd = {
                            if (draggedWidgetId == widget.widgetId) {
                                // Utilise currentLayout pour éviter le stale closure
                                val fresh = currentLayout.placements.find { it.widgetId == widget.widgetId }
                                if (fresh != null) {
                                    val cWPx = with(density) { cellWidthDp.toPx() }
                                    val cHPx = with(density) { cellHeightDp.toPx() }
                                    val newCol = (fresh.col + dragOffset.x / cWPx)
                                        .roundToInt().coerceIn(0, 4 - fresh.colSpan).toFloat()
                                    val newRow = (fresh.row + dragOffset.y / cHPx)
                                        .roundToInt().coerceAtLeast(0).toFloat()
                                    if (newCol != fresh.col || newRow != fresh.row) {
                                        // Anti-superposition : déplacement sur un autre widget → snap-back.
                                        val candidate = fresh.copy(col = newCol, row = newRow)
                                        if (currentLayout.collidesOnPage(widget.widgetId, candidate)) {
                                            com.stanleycx.fruitos.ui.components.Haptics.light(context)
                                        } else {
                                            onResizeWidget(
                                                widget.widgetId,
                                                WidgetGridRect(newCol, newRow, fresh.colSpan, fresh.rowSpan)
                                            )
                                        }
                                    }
                                }
                                draggedWidgetId = null
                                dragOffset = Offset.Zero
                            }
                        },
                        modifier = Modifier.offset(
                            x = cellWidthDp * widget.col + dxDp,
                            y = cellHeightDp * widget.row + dyDp
                        )
                    )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AddWidgetFab(
                    hazeState = hazeState,
                    glassLevel = glassLevel,
                    glassTint = glassTint,
                    customTintColor = customTintColor,
                    loupeLevel = loupeLevel,
                    glossLevel = glossLevel,
                    onClick = onAddWidget
                )
            }
        }
    }
}

@Composable
private fun EmptyWidgetPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.07f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Appuie sur + pour ajouter un widget",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AddWidgetFab(
    hazeState: HazeState,
    glassLevel: GlassLevel,
    glassTint: GlassTint,
    customTintColor: Color?,
    loupeLevel: LoupeLevel,
    glossLevel: GlossLevel,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .shadow(8.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .glass(
                hazeState = hazeState,
                level = glassLevel,
                glassTint = glassTint,
                customTintColor = customTintColor,
                shape = CircleShape,
                loupeLevel = loupeLevel,
                glossLevel = glossLevel
            )
            .size(56.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Ajouter un widget",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
