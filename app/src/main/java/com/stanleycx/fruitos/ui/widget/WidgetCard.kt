package com.stanleycx.fruitos.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.stanleycx.fruitos.data.WidgetPlacement
import com.stanleycx.fruitos.ui.home.EditModeState
import kotlinx.coroutines.withTimeoutOrNull

private val HANDLE_SIZE = 22.dp
private val CORNER_RADIUS = 32.dp
private val WIDGET_HORIZONTAL_INSET = 4.dp  // ~8px par côté sur un écran standard

/** Bornes de redimensionnement d'un widget, exprimées en nombre de cellules. */
private data class ResizeBounds(
    val minColSpan: Int,
    val maxColSpan: Int,
    val minRowSpan: Int,
    val maxRowSpan: Int
)

@Composable
fun WidgetCard(
    placement: WidgetPlacement,
    editMode: EditModeState,
    resizeState: WidgetResizeState,
    cellWidthDp: Dp,
    cellHeightDp: Dp,
    maxRowSpan: Int = 6,   // hauteur max en cellules (6 sur une page d'apps, plus sur la page widgets)
    // Alignement visuel sur les tuiles d'icônes (écran d'accueil) : inset fixe (pas de mesure).
    topInsetDp: Dp = 0.dp,        // bord haut du widget = bord haut de la tuile d'icône voisine
    bottomReserveDp: Dp = 0.dp,   // espace bas réservé (bord bas tuile + zone du label)
    label: String? = null,        // nom affiché sous le widget (écran d'accueil)
    showLabel: Boolean = false,   // afficher le label (jamais sur la page widgets dédiée)
    onRemove: () -> Unit,
    onResizeCommit: (WidgetGridRect) -> Unit,
    onLongPress: (touchPosInRoot: Offset) -> Unit = {},
    onWidgetDragStart: () -> Unit = {},
    onWidgetDrag: (Offset) -> Unit = {},
    onWidgetDragEnd: () -> Unit = {},
    isDragged: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Liberté de taille : on borne UNIQUEMENT par la grille (1..4 colonnes, 1..maxRowSpan
    // lignes), sans liste de tailles prédéfinies ni min/max du provider. N'importe quel
    // spanX/spanY entier est permis tant qu'il tient dans la grille (la non-superposition
    // est gérée séparément au commit par la détection de collision).
    val resizeBounds = remember(maxRowSpan) {
        ResizeBounds(minColSpan = 1, maxColSpan = 4, minRowSpan = 1, maxRowSpan = maxRowSpan)
    }

    // rememberUpdatedState : pointerInput(editMode.isEditing) ne redémarre pas quand les
    // callbacks changent (ex : widget déplacé → nouveau col/row). Ces refs garantissent
    // que le coroutine en cours appelle toujours la dernière version des lambdas.
    val latestOnLongPress by rememberUpdatedState(onLongPress)
    val latestOnWidgetDragStart by rememberUpdatedState(onWidgetDragStart)
    val latestOnWidgetDrag by rememberUpdatedState(onWidgetDrag)
    val latestOnWidgetDragEnd by rememberUpdatedState(onWidgetDragEnd)

    val (widthDp, heightDp) = if (resizeState.resizingWidgetId == placement.widgetId &&
        resizeState.originalRect != null
    ) {
        val orig = resizeState.originalRect!!
        val rawDxDp = with(density) { resizeState.rawOffsetX.toDp() }
        val rawDyDp = with(density) { resizeState.rawOffsetY.toDp() }
        Pair(
            (cellWidthDp * orig.colSpan + rawDxDp).coerceIn(cellWidthDp, cellWidthDp * 4),
            (cellHeightDp * orig.rowSpan + rawDyDp).coerceIn(cellHeightDp, cellHeightDp * maxRowSpan)
        )
    } else {
        Pair(cellWidthDp * placement.colSpan, cellHeightDp * placement.rowSpan)
    }

    var widgetScreenRect by remember { mutableStateOf(Rect.Zero) }

    // Tremblement "jiggle" Fruit OS — même transition partagée LocalJiggleBase que les icônes,
    // mais amplitude ADAPTÉE À LA TAILLE : on vise le même déplacement de coin (~1.6 dp)
    // que sur une icône, donc l'angle décroît quand le widget grandit (un grand widget à
    // 2° bougerait énormément). Clamp pour rester subtil et visible. Désactivé pendant la
    // manipulation (drag/resize) du widget lui-même.
    val isManipulating = isDragged || resizeState.resizingWidgetId == placement.widgetId
    val halfDiagDp = 0.5f * kotlin.math.sqrt(
        widthDp.value * widthDp.value + heightDp.value * heightDp.value
    )
    val widgetJiggleAmplitudeDeg = if (halfDiagDp > 1f) {
        Math.toDegrees((1.6f / halfDiagDp).toDouble()).toFloat().coerceIn(0.25f, 1.1f)
    } else 0.9f
    val jiggleAngle = com.stanleycx.fruitos.ui.home.useJiggleAngle(
        isEditing = editMode.isEditing && !isManipulating,
        seed = placement.widgetId.hashCode(),
        amplitudeDeg = widgetJiggleAmplitudeDeg
    )

    Box(
        modifier = modifier
            .size(width = widthDp, height = heightDp)
            .onGloballyPositioned { coords ->
                widgetScreenRect = Rect(
                    offset = coords.positionInRoot(),
                    size = coords.size.toSize()
                )
            }
            .graphicsLayer {
                alpha = if (isDragged) 0.75f else 1f
                scaleX = if (isDragged) 0.96f else 1f
                scaleY = if (isDragged) 0.96f else 1f
                rotationZ = jiggleAngle
            }
            .pointerInput(editMode.isEditing) {
                if (!editMode.isEditing) {
                    // Long press → menu contextuel.
                    // Annulé si le doigt bouge au-delà du slop (= scroll en cours).
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val slop = viewConfiguration.touchSlop
                        // Timeout RACCOURCI (0.7×) : le menu contextuel du widget s'arme AVANT les
                        // détecteurs de long-press du home (même timeout) → ces derniers voient
                        // widgetContextMenuId != null et n'ouvrent PAS le menu rapide (plus de
                        // « 2 boutons + menu contextuel »).
                        val lifted = withTimeoutOrNull((viewConfiguration.longPressTimeoutMillis * 0.7f).toLong()) {
                            var pressing = true
                            while (pressing) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    pressing = false
                                } else {
                                    val dx = change.position.x - down.position.x
                                    val dy = change.position.y - down.position.y
                                    if (dx * dx + dy * dy > slop * slop) pressing = false
                                }
                            }
                            true
                        }
                        if (lifted == null) {
                            val touchInRoot = Offset(
                                widgetScreenRect.left + down.position.x,
                                widgetScreenRect.top + down.position.y
                            )
                            latestOnLongPress(touchInRoot)
                            // Consomme le reste du geste pour que le widget (calendrier, horloge…)
                            // ne reçoive PAS de clic au relâcher : un long-press ouvre le menu, pas l'app.
                            while (true) {
                                val e = awaitPointerEvent(pass = PointerEventPass.Initial)
                                e.changes.forEach { it.consume() }
                                if (e.changes.all { !it.pressed }) break
                            }
                        }
                    }
                } else {
                    // Edit mode : intercepte en Initial pass avant l'AndroidView.
                    // Laisse passer si le doigt est sur un handle de resize.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val hPx = HANDLE_SIZE.toPx() * 2f
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val p = down.position
                        val onHandle =
                            (p.x < hPx && p.y < hPx) ||
                            (p.x > w - hPx && p.y < hPx) ||
                            (p.x < hPx && p.y > h - hPx) ||
                            (p.x > w - hPx && p.y > h - hPx) ||
                            (p.x in (w / 2f - hPx)..(w / 2f + hPx) && p.y > h - hPx) ||
                            (p.x > w - hPx && p.y in (h / 2f - hPx)..(h / 2f + hPx))
                        if (onHandle) return@awaitEachGesture

                        down.consume()
                        var started = false
                        var accDx = 0f
                        var accDy = 0f
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                if (started) latestOnWidgetDragEnd()
                                break
                            }
                            val dx = change.position.x - change.previousPosition.x
                            val dy = change.position.y - change.previousPosition.y
                            accDx += dx; accDy += dy
                            val slopSq = viewConfiguration.touchSlop.let { it * it }
                            if (!started && accDx * accDx + accDy * accDy > slopSq) {
                                started = true
                                latestOnWidgetDragStart()
                            }
                            if (started) {
                                latestOnWidgetDrag(Offset(dx, dy))
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        // Petite marge visuelle autour du widget. Clip arrondi géré dans WidgetHostView.
        // Inset vertical fixe = aligne le widget sur les tuiles d'icônes voisines (haut/bas),
        // et réserve la zone basse pour le label. Constantes → aucune mesure par frame (60 fps).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = WIDGET_HORIZONTAL_INSET)
                .padding(top = topInsetDp, bottom = bottomReserveDp)
        ) {
            WidgetHostView(
                appWidgetId = placement.appWidgetId,
                cornerRadius = CORNER_RADIUS,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (editMode.isEditing)
                            Modifier.pointerInput(Unit) { detectTapGestures { /* absorbe les taps */ } }
                        else Modifier
                    )
            )
        }

        // Nom sous le widget (écran d'accueil uniquement), dans la zone réservée basse.
        // Masqué en mode édition (les handles de resize occupent le bas).
        if (showLabel && !label.isNullOrBlank() && !editMode.isEditing && bottomReserveDp > 0.dp) {
            androidx.compose.material3.Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp, start = 6.dp, end = 6.dp),
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(0f, 1f),
                        blurRadius = 4f
                    )
                )
            )
        }

        if (editMode.isEditing) {
            val cellWPx = with(density) { cellWidthDp.toPx() }
            val cellHPx = with(density) { cellHeightDp.toPx() }
            val origRect = WidgetGridRect(placement.col, placement.row, placement.colSpan, placement.rowSpan)

            // Bouton supprimer
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(HANDLE_SIZE)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
                    .pointerInput(placement.widgetId) {
                        detectTapGestures(onTap = { onRemove() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(width = 10.dp, height = 2.dp).background(Color.White))
            }

            // Coin haut-droit : colSpan+, rowSpan-
            ResizeHandle(
                alignment = Alignment.TopEnd,
                onDragStart = { resizeState.startResize(placement.widgetId, origRect, cellWPx, cellHPx,
                    resizeBounds.minColSpan, resizeBounds.maxColSpan, resizeBounds.minRowSpan, resizeBounds.maxRowSpan) },
                onDrag = { dx, dy -> resizeState.updateRawOffset(dx, -dy) },
                onDragEnd = { val r = resizeState.commit(); if (r != null) onResizeCommit(r) }
            )
            // Coin bas-gauche : colSpan-, rowSpan+
            ResizeHandle(
                alignment = Alignment.BottomStart,
                onDragStart = { resizeState.startResize(placement.widgetId, origRect, cellWPx, cellHPx,
                    resizeBounds.minColSpan, resizeBounds.maxColSpan, resizeBounds.minRowSpan, resizeBounds.maxRowSpan) },
                onDrag = { dx, dy -> resizeState.updateRawOffset(-dx, dy) },
                onDragEnd = { val r = resizeState.commit(); if (r != null) onResizeCommit(r) }
            )
            // Coin bas-droit : colSpan+, rowSpan+
            ResizeHandle(
                alignment = Alignment.BottomEnd,
                onDragStart = { resizeState.startResize(placement.widgetId, origRect, cellWPx, cellHPx,
                    resizeBounds.minColSpan, resizeBounds.maxColSpan, resizeBounds.minRowSpan, resizeBounds.maxRowSpan) },
                onDrag = { dx, dy -> resizeState.updateRawOffset(dx, dy) },
                onDragEnd = { val r = resizeState.commit(); if (r != null) onResizeCommit(r) }
            )
            // Bord bas centre : rowSpan+ uniquement
            ResizeHandle(
                alignment = Alignment.BottomCenter,
                onDragStart = { resizeState.startResize(placement.widgetId, origRect, cellWPx, cellHPx,
                    resizeBounds.minColSpan, resizeBounds.maxColSpan, resizeBounds.minRowSpan, resizeBounds.maxRowSpan) },
                onDrag = { _, dy -> resizeState.updateRawOffset(0f, dy) },
                onDragEnd = { val r = resizeState.commit(); if (r != null) onResizeCommit(r) }
            )
            // Bord droit centre : colSpan+ uniquement
            ResizeHandle(
                alignment = Alignment.CenterEnd,
                onDragStart = { resizeState.startResize(placement.widgetId, origRect, cellWPx, cellHPx,
                    resizeBounds.minColSpan, resizeBounds.maxColSpan, resizeBounds.minRowSpan, resizeBounds.maxRowSpan) },
                onDrag = { dx, _ -> resizeState.updateRawOffset(dx, 0f) },
                onDragEnd = { val r = resizeState.commit(); if (r != null) onResizeCommit(r) }
            )
        }
    }
}

@Composable
private fun BoxScope.ResizeHandle(
    alignment: Alignment,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    onDragStart: () -> Unit = {},
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    var accDx by remember { mutableFloatStateOf(0f) }
    var accDy by remember { mutableFloatStateOf(0f) }
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDrag by rememberUpdatedState(onDrag)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .size(HANDLE_SIZE)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { accDx = 0f; accDy = 0f; latestOnDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accDx += dragAmount.x; accDy += dragAmount.y
                        latestOnDrag(accDx, accDy)
                    },
                    onDragEnd = { latestOnDragEnd() },
                    onDragCancel = { latestOnDragEnd() }
                )
            }
    )
}
