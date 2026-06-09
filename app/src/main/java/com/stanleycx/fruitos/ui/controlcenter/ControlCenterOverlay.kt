package com.stanleycx.fruitos.ui.controlcenter

import android.app.NotificationManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.GlossLevel
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.backgroundBlurFor
import com.stanleycx.fruitos.ui.components.glass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PANEL_CORNER = 38.dp
private val GAP = 12.dp
private const val GRID_COLS = 4

// ─── Paramètres glass d'un panneau ───────────────────────────────────────────
@Composable
private fun Modifier.ccPanel(
    hazeState: HazeState,
    level: GlassLevel,
    tint: GlassTint,
    custom: Color?,
    loupe: LoupeLevel,
    gloss: GlossLevel,
    shape: RoundedCornerShape = RoundedCornerShape(PANEL_CORNER),
): Modifier = this
    .clip(shape)
    .glass(hazeState = hazeState, level = level, glassTint = tint, customTintColor = custom,
           shape = shape, loupeLevel = loupe, glossLevel = gloss)

// ─── Overlay principal ────────────────────────────────────────────────────────
@Composable
fun ControlCenterOverlay(
    state: ControlCenterState,
    hazeState: HazeState,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
) {
    val context = LocalContext.current
    val media = rememberActiveMedia(context)
    val torch = rememberTorchState(context)

    var expanded by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    // Grille unifiée : tuiles système + raccourcis + tuiles d'apps
    var tiles by remember { mutableStateOf(loadCcShortcuts(context)) }
    var showAddSheet by remember { mutableStateOf(false) }

    // À chaque ouverture du Control Center : on recharge la liste persistée ET on incrémente
    // refreshTick, qui force le re-chargement des icônes/labels des tuiles d'apps depuis le
    // PackageManager → reflète les changements faits dans les applications (icône, libellé,
    // tuile QS, app mise à jour/désinstallée).
    var refreshTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            tiles = loadCcShortcuts(context)
            refreshTick++
        }
    }

    if (!state.isOpen && expanded) expanded = false
    if (!state.isOpen && isEditMode) isEditMode = false
    if (!state.isOpen && showAddSheet) showAddSheet = false

    // À chaque entrée en mode édition : on relit la liste persistée et on force le re-chargement
    // des icônes/labels (certaines tuiles externes chargent leur icône en différé → sans ça elles
    // restent vides tant qu'on ne rouvre pas le Control Center).
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            tiles = loadCcShortcuts(context)
            refreshTick++
        }
    }

    // À chaque ouverture de la feuille « Ajouter » : on force la liste à se re-interroger
    // (apps + services de tuiles fraîchement installés/modifiés réapparaissent).
    LaunchedEffect(showAddSheet) { if (showAddSheet) refreshTick++ }

    val editJiggle = rememberInfiniteTransition("cc_edit_jiggle")
    val jiggleAngle by editJiggle.animateFloat(
        initialValue = -2f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(200), RepeatMode.Reverse),
        label = "cc_jiggle"
    )

    AnimatedVisibility(
        visible = state.isOpen,
        enter = fadeIn(tween(160)) + slideInVertically(
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        ) { it / 3 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 3 }
    ) {
        BackHandler(enabled = state.isOpen) {
            when {
                showAddSheet -> showAddSheet = false
                isEditMode -> isEditMode = false
                expanded -> expanded = false
                else -> state.close()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {

            // ── Scrim + blur ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(state = hazeState) { blurRadius = backgroundBlurFor(glassLevel) + 6.dp }
                    .background(Color.Black.copy(alpha = 0.30f))
                    .pointerInput(isEditMode, expanded, showAddSheet) {
                        detectTapGestures(
                            onTap = {
                                when {
                                    showAddSheet -> showAddSheet = false
                                    isEditMode -> isEditMode = false
                                    expanded -> expanded = false
                                    else -> state.close()
                                }
                            },
                            onLongPress = { if (!expanded && !showAddSheet) isEditMode = true }
                        )
                    }
            )

            // ── Tuiles CC ────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = !expanded,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(140)),
                exit = fadeOut(tween(120))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 26.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    verticalArrangement = Arrangement.spacedBy(GAP)
                ) {
                    // Bouton "Terminé" en mode édition
                    AnimatedVisibility(visible = isEditMode) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { isEditMode = false }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text("Terminé", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val totalWidth = maxWidth
                        val colWidth = (totalWidth - GAP * (GRID_COLS - 1)) / GRID_COLS

                        // ── Rangée fixe 1 : connectivité + média ─────────────
                        Row(
                            modifier = Modifier.fillMaxWidth().height(176.dp),
                            horizontalArrangement = Arrangement.spacedBy(GAP)
                        ) {
                            ConnectivityCluster(
                                modifier = Modifier.weight(1f).fillMaxHeight()
                                    .ccPanel(hazeState, glassLevel, glassTint, customTintColor, loupeLevel, glossLevel),
                                onAirplane = { openAirplaneSettings(context) },
                                onCellular = { openInternetPanel(context) },
                                onWifi = { openWifiPanel(context) },
                                onBluetooth = { openBluetoothSettings(context) }
                            )
                            MediaPanel(
                                media = media, onExpand = { expanded = true },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                                    .ccPanel(hazeState, glassLevel, glassTint, customTintColor, loupeLevel, glossLevel),
                            )
                        }

                        // ── Grille dynamique (tout le reste) ─────────────────
                        // Placée après la row fixe via padding top
                        Box(modifier = Modifier.padding(top = 176.dp + GAP)) {
                            CcTileGrid(
                                tiles = tiles,
                                colWidth = colWidth,
                                isEditMode = isEditMode,
                                refreshTick = refreshTick,
                                jiggleAngle = jiggleAngle,
                                hazeState = hazeState,
                                glassLevel = glassLevel,
                                glassTint = glassTint,
                                customTintColor = customTintColor,
                                loupeLevel = loupeLevel,
                                glossLevel = glossLevel,
                                torch = torch,
                                onUpdate = { newList -> tiles = newList; saveCcShortcuts(context, newList) },
                                onAddRequest = { showAddSheet = true },
                                onEnterEditMode = { isEditMode = true },
                                context = context
                            )
                        }
                    }
                }
            }

            // ── Now Playing ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f,
                    animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.92f, animationSpec = tween(180))
            ) {
                NowPlayingFull(media = media, onClose = { expanded = false }, onCast = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_CAST_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                })
            }

            // ── Sheet d'ajout ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showAddSheet,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it / 2 } + fadeIn(tween(200)),
                exit = slideOutVertically { it / 2 } + fadeOut(tween(180))
            ) {
                AddShortcutSheet(
                    existingShortcuts = tiles,
                    refreshTick = refreshTick,
                    onAdd = { s -> val n = tiles + s; tiles = n; saveCcShortcuts(context, n); refreshTick++; showAddSheet = false },
                    onDismiss = { showAddSheet = false }
                )
            }
        }
    }
}

// ─── Grille unifiée (toutes les tuiles sauf row 1 fixe) ──────────────────────
@Composable
private fun CcTileGrid(
    tiles: List<CcShortcut>,
    colWidth: Dp,
    isEditMode: Boolean,
    refreshTick: Int,
    jiggleAngle: Float,
    hazeState: HazeState,
    glassLevel: GlassLevel,
    glassTint: GlassTint,
    customTintColor: Color?,
    loupeLevel: LoupeLevel,
    glossLevel: GlossLevel,
    torch: Pair<Boolean, (Boolean) -> Unit>,
    onUpdate: (List<CcShortcut>) -> Unit,
    onAddRequest: () -> Unit,
    onEnterEditMode: () -> Unit,
    context: android.content.Context,
) {
    val positions = remember(tiles) { packTiles(tiles) }
    val density = LocalDensity.current

    var dragging by remember { mutableStateOf<Int?>(null) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    // Index cible pendant le drag : tile la plus proche du centre draggué
    val dIdx = dragging
    val hoverIdx: Int = remember(dIdx, dragX, dragY, positions) {
        val safeIdx = dIdx ?: return@remember -1
        if (positions.isEmpty()) return@remember -1
        val src = positions.getOrNull(safeIdx) ?: return@remember -1
        val colWPx = with(density) { colWidth.toPx() }
        val gPx = with(density) { GAP.toPx() }
        val step = colWPx + gPx
        val cx = src.col * step + src.colSpan * colWPx * 0.5f + dragX
        val cy = src.row * step + src.rowSpan * colWPx * 0.5f + dragY
        var best: Int = safeIdx; var bestD = Float.MAX_VALUE
        positions.forEachIndexed { i, p ->
            if (i == safeIdx) return@forEachIndexed
            val pcx = p.col * step + p.colSpan * colWPx * 0.5f
            val pcy = p.row * step + p.rowSpan * colWPx * 0.5f
            val d = (cx - pcx) * (cx - pcx) + (cy - pcy) * (cy - pcy)
            if (d < bestD) { bestD = d; best = i }
        }
        best
    }

    // Liste avec le bouton "+" ajouté en mode édition
    val displayTiles = if (isEditMode) tiles + CcShortcut("_ADD") else tiles
    val displayPositions = remember(displayTiles) { packTiles(displayTiles) }

    val colWidthPx = with(density) { colWidth.roundToPx() }
    val gapPx = with(density) { GAP.roundToPx() }

    TileGridLayout(
        modifier = Modifier.fillMaxWidth(),
        colWidthPx = colWidthPx,
        gapPx = gapPx,
        gridCols = GRID_COLS,
        positions = displayPositions,
    ) {
        displayTiles.forEachIndexed { index, tile ->
            if (tile.type == "_ADD") {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier.size(58.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .clickable { onAddRequest() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
                return@forEachIndexed
            }
            val originalIndex = tiles.indexOf(tile).takeIf { it >= 0 } ?: index
            val isDragging = dIdx == originalIndex

            val tileShape = when (tile.type) {
                "ROTATION" -> RoundedCornerShape(34.dp)
                else -> RoundedCornerShape(PANEL_CORNER)
            }

            GridTile(
                tile = tile,
                index = originalIndex,
                isEditMode = isEditMode,
                refreshTick = refreshTick,
                jiggleAngle = jiggleAngle,
                isDragging = isDragging,
                dragOffsetX = if (isDragging) dragX else 0f,
                dragOffsetY = if (isDragging) dragY else 0f,
                modifier = Modifier.ccPanel(hazeState, glassLevel, glassTint, customTintColor, loupeLevel, glossLevel, tileShape),
                torch = torch,
                context = context,
                onRemove = {
                    val newList = tiles.toMutableList().apply { removeAt(originalIndex) }
                    onUpdate(newList)
                },
                onDragStart = { dragging = originalIndex; dragX = 0f; dragY = 0f },
                onDragDelta = { dx, dy -> dragX += dx; dragY += dy },
                onDragEnd = {
                    val dI = dragging
                    if (dI != null && hoverIdx in tiles.indices && hoverIdx != dI) {
                        val newList = tiles.toMutableList()
                        val item = newList.removeAt(dI)
                        newList.add(hoverIdx.coerceIn(0, newList.size), item)
                        onUpdate(newList)
                    }
                    dragging = null; dragX = 0f; dragY = 0f
                },
                onEnterEditMode = onEnterEditMode,
            )
        }
    }
}

// ─── Layout personnalisé pour la grille (Compose Layout API) ─────────────────
@Composable
private fun TileGridLayout(
    modifier: Modifier,
    colWidthPx: Int,
    gapPx: Int,
    gridCols: Int,
    positions: List<TilePosition>,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.mapIndexed { i, m ->
            val pos = positions.getOrNull(i)
            val w = if (pos != null) colWidthPx * pos.colSpan + gapPx * (pos.colSpan - 1) else colWidthPx
            val h = if (pos != null) colWidthPx * pos.rowSpan + gapPx * (pos.rowSpan - 1) else colWidthPx
            m.measure(Constraints.fixed(w.coerceAtLeast(1), h.coerceAtLeast(1)))
        }
        val maxRow = positions.maxOfOrNull { it.row + it.rowSpan } ?: 0
        val totalH = (colWidthPx * maxRow + gapPx * (maxRow - 1).coerceAtLeast(0)).coerceAtLeast(0)
        layout(width = constraints.maxWidth, height = totalH) {
            placeables.forEachIndexed { i, placeable ->
                val pos = positions.getOrNull(i) ?: return@forEachIndexed
                placeable.placeRelative(
                    x = pos.col * (colWidthPx + gapPx),
                    y = pos.row * (colWidthPx + gapPx)
                )
            }
        }
    }
}

// ─── Tuile unique dans la grille (wrapper drag + jiggle + remove) ─────────────
@Composable
private fun GridTile(
    tile: CcShortcut,
    index: Int,
    isEditMode: Boolean,
    refreshTick: Int,
    jiggleAngle: Float,
    isDragging: Boolean,
    dragOffsetX: Float,
    dragOffsetY: Float,
    modifier: Modifier,
    torch: Pair<Boolean, (Boolean) -> Unit>,
    context: android.content.Context,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onEnterEditMode: () -> Unit,
) {
    // Wiggle atténué sur les grandes tuiles : le déplacement d'un coin est ∝ à la taille,
    // donc on divise l'amplitude par la plus grande dimension (FOCUS=2, sliders=2 → ±1°).
    val sizeFactor = 1f / maxOf(tile.colSpan(), tile.rowSpan()).coerceAtLeast(1)
    val tileJiggle = if (isEditMode && !isDragging)
        jiggleAngle * (if (index % 2 == 0) 1f else -1f) * sizeFactor else 0f
    val animatedDX by animateFloatAsState(if (isDragging) dragOffsetX else 0f,
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow), label = "dx")
    val animatedDY by animateFloatAsState(if (isDragging) dragOffsetY else 0f,
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow), label = "dy")

    // Box EXTERNE : non clippée → le bouton ✕ peut déborder des coins (style Fruit OS).
    Box(
        modifier = Modifier
            .graphicsLayer {
                rotationZ = tileJiggle
                translationX = if (isDragging) dragOffsetX else animatedDX
                translationY = if (isDragging) dragOffsetY else animatedDY
                scaleX = if (isDragging) 1.08f else 1f
                scaleY = if (isDragging) 1.08f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { if (!isEditMode) onEnterEditMode(); onDragStart() },
                    onDrag = { _, d -> onDragDelta(d.x, d.y) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
    ) {
        // Tuile glass (clippée) — remplit la cellule
        Box(
            modifier = Modifier.fillMaxSize().then(modifier),
            contentAlignment = Alignment.Center
        ) {
            TileContent(tile = tile, torch = torch, context = context, refreshTick = refreshTick)
        }

        // Bouton de suppression en mode édition — débordant du coin haut-gauche
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-7).dp, y = (-7).dp)
                    .zIndex(2f)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = onRemove
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(11.dp))
            }
        }
    }
}

// ─── Contenu d'une tuile (routage par type) ───────────────────────────────────
@Composable
private fun TileContent(
    tile: CcShortcut,
    torch: Pair<Boolean, (Boolean) -> Unit>,
    context: android.content.Context,
    refreshTick: Int,
) {
    when (tile.type) {
        "ROTATION" -> RotationTileContent(context = context)
        "FOCUS" -> FocusTileContent(context = context)
        "BRIGHTNESS" -> BrightnessTileContent(context = context)
        "VOLUME" -> VolumeTileContent(context = context)
        "TORCH" -> ShortcutTileContent(Icons.Filled.FlashlightOn, active = torch.first,
            activeColor = Color.White, activeIconTint = Color(0xFFFFCC00)) { torch.second(!torch.first) }
        "TIMER" -> ShortcutTileContent(Icons.Filled.Timer) { launchTimer(context) }
        "CALCULATOR" -> ShortcutTileContent(Icons.Filled.Calculate) { launchCalculator(context) }
        "CAMERA" -> ShortcutTileContent(Icons.Filled.PhotoCamera) { launchCamera(context) }
        "APP" -> AppTileContent(tile = tile, context = context, refreshTick = refreshTick)
        "TILE" -> TileServiceContent(tile = tile, context = context, refreshTick = refreshTick)
    }
}

// ─── Contenu : Rotation ───────────────────────────────────────────────────────
@Composable
private fun RotationTileContent(context: android.content.Context) {
    var locked by remember { mutableFloatStateOf(if (isRotationLocked(context)) 1f else 0f) }
    Box(
        modifier = Modifier.fillMaxSize().clickable {
            if (!canWriteSettings(context)) { requestWriteSettings(context); return@clickable }
            val nl = locked < 0.5f; setRotationLocked(context, nl)
            locked = if (nl) 1f else 0f
        },
        contentAlignment = Alignment.Center
    ) {
        val on = locked > 0.5f
        Box(
            modifier = Modifier.size(54.dp).clip(CircleShape)
                .background(if (on) Color.White else Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (on) Icons.Filled.ScreenLockRotation else Icons.Filled.ScreenRotation,
                contentDescription = null,
                tint = if (on) Color(0xFFFF3B30) else Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

// ─── Contenu : Focus (centré + flèches haut/bas pour changer de mode) ─────────
@Composable
private fun FocusTileContent(context: android.content.Context) {
    val (dndState, setDndState) = rememberDndState(context)
    val dndActive = dndState.active
    val dndMode = dndState.mode

    val dndModes = listOf(
        NotificationManager.INTERRUPTION_FILTER_PRIORITY to "Priorité",
        NotificationManager.INTERRUPTION_FILTER_ALARMS to "Alarmes",
        NotificationManager.INTERRUPTION_FILTER_NONE to "Silence",
    )
    val modeLabel = if (dndActive) {
        dndModes.firstOrNull { it.first == dndMode }?.second ?: "Focus"
    } else "Focus"

    fun prevMode() {
        if (!dndActive) {
            setDndState(DndUiState(active = true, mode = NotificationManager.INTERRUPTION_FILTER_PRIORITY))
            return
        }
        val cur = dndModes.indexOfFirst { it.first == dndMode }
        val prev = ((cur - 1 + dndModes.size) % dndModes.size)
        setDndState(dndState.copy(mode = dndModes[prev].first))
    }
    fun nextMode() {
        if (!dndActive) {
            setDndState(DndUiState(active = true, mode = NotificationManager.INTERRUPTION_FILTER_PRIORITY))
            return
        }
        val cur = dndModes.indexOfFirst { it.first == dndMode }
        val next = (cur + 1) % dndModes.size
        if (next == 0) { setDndState(dndState.copy(active = false)); return }
        setDndState(dndState.copy(mode = dndModes[next].first))
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Groupe centré : [flèches ▲▼] [logo] [label]. L'espace des flèches est réservé
        // même quand DND est inactif → le bloc logo+texte ne se décale pas et reste centré.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(22.dp), contentAlignment = Alignment.Center) {
                if (dndActive) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp, null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp).clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { prevMode() }
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowDown, null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp).clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { nextMode() }
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            // Logo + label (tap = toggle DND)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (dndActive) setDndState(dndState.copy(active = false))
                    else setDndState(DndUiState(active = true, mode = NotificationManager.INTERRUPTION_FILTER_PRIORITY))
                }
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(if (dndActive) Color(0xFF5E5CE6) else Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.DoNotDisturbOn, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(modeLabel, color = Color.White, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

// ─── Contenu : Luminosité ────────────────────────────────────────────────────
@Composable
private fun BrightnessTileContent(context: android.content.Context) {
    var value by remember { mutableFloatStateOf(getBrightness(context)) }
    VerticalSlider(
        modifier = Modifier.fillMaxSize(),
        value = value,
        icon = Icons.Filled.LightMode,
        onChange = { v ->
            if (!canWriteSettings(context)) { requestWriteSettings(context); return@VerticalSlider }
            value = v; setBrightness(context, v)
        }
    )
}

// ─── Contenu : Volume ────────────────────────────────────────────────────────
@Composable
private fun VolumeTileContent(context: android.content.Context) {
    val (value, setVol) = rememberLiveVolume(context)
    VerticalSlider(
        modifier = Modifier.fillMaxSize(),
        value = value,
        icon = if (value < 0.01f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
        onChange = setVol
    )
}

@Composable
private fun VerticalSlider(modifier: Modifier, value: Float, icon: ImageVector, onChange: (Float) -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PANEL_CORNER))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { off -> onChange((1f - off.y / size.height).coerceIn(0f, 1f)) },
                    onVerticalDrag = { ch, _ -> onChange((1f - ch.position.y / size.height).coerceIn(0f, 1f)) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { off -> onChange((1f - off.y / size.height).coerceIn(0f, 1f)) }
            }
    ) {
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .fillMaxHeight(value.coerceIn(0.001f, 1f))
                .background(Color.White.copy(alpha = 0.92f))
        )
        Icon(imageVector = icon, contentDescription = null,
            tint = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).size(26.dp))
    }
}

// ─── Contenu : Raccourci système (icône dans cercle) ─────────────────────────
@Composable
private fun ShortcutTileContent(
    icon: ImageVector,
    active: Boolean = false,
    activeColor: Color = Color.White,
    activeIconTint: Color = Color(0xFFFFCC00),
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(58.dp).clip(CircleShape)
                .background(if (active) activeColor else Color.White.copy(alpha = 0.16f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (active) activeIconTint else Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

// ─── Contenu : App shortcut ──────────────────────────────────────────────────
@Composable
private fun AppTileContent(tile: CcShortcut, context: android.content.Context, refreshTick: Int) {
    var bmp by remember(tile.pkg, refreshTick) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(tile.pkg, refreshTick) {
        bmp = withContext(Dispatchers.IO) { tile.pkg?.let { loadAppIcon(context, it) } }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(58.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .clickable { tile.pkg?.let { launchApp(context, it) } },
            contentAlignment = Alignment.Center
        ) {
            val b = bmp
            if (b != null) {
                androidx.compose.foundation.Image(
                    bitmap = b.asImageBitmap(), null,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }
}

// ─── Contenu : Tile service ──────────────────────────────────────────────────
@Composable
private fun TileServiceContent(tile: CcShortcut, context: android.content.Context, refreshTick: Int) {
    var bmp by remember(tile.extra ?: tile.pkg, refreshTick) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(tile.extra ?: tile.pkg, refreshTick) {
        bmp = withContext(Dispatchers.IO) {
            tile.pkg?.let { loadTileIcon(context, it, tile.extra) }
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(58.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .clickable { tile.pkg?.let { launchTileOrApp(context, it) } },
            contentAlignment = Alignment.Center
        ) {
            val b = bmp
            if (b != null) {
                androidx.compose.foundation.Image(
                    bitmap = b.asImageBitmap(), null,
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }
}

// ─── Fonctions utilitaires d'icônes ──────────────────────────────────────────
private fun loadAppIcon(context: android.content.Context, pkg: String): android.graphics.Bitmap? =
    runCatching {
        val drawable = context.packageManager.getApplicationIcon(pkg)
        val bmp = android.graphics.Bitmap.createBitmap(56, 56, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp); drawable.setBounds(0, 0, 56, 56); drawable.draw(canvas); bmp
    }.getOrNull()

private fun loadTileIcon(context: android.content.Context, pkg: String, component: String?): android.graphics.Bitmap? =
    runCatching {
        val pm = context.packageManager
        val drawable = if (component != null)
            pm.getServiceInfo(android.content.ComponentName(pkg, component), 0).loadIcon(pm)
        else pm.getApplicationIcon(pkg)
        val bmp = android.graphics.Bitmap.createBitmap(56, 56, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp); drawable.setBounds(0, 0, 56, 56); drawable.draw(canvas); bmp
    }.getOrNull() ?: loadAppIcon(context, pkg)

// ─── Connectivité ─────────────────────────────────────────────────────────────
@Composable
private fun ConnectivityCluster(modifier: Modifier, onAirplane: () -> Unit, onCellular: () -> Unit, onWifi: () -> Unit, onBluetooth: () -> Unit) {
    val context = LocalContext.current
    val states = rememberConnStates(context)
    Box(modifier = modifier.padding(14.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ConnToggle(Icons.Filled.AirplanemodeActive, Color(0xFFFF9500), states.airplane, onAirplane)
                ConnToggle(Icons.Filled.SignalCellularAlt, Color(0xFF34C759), states.cellular, onCellular)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ConnToggle(Icons.Filled.Wifi, Color(0xFF0A84FF), states.wifi, onWifi)
                ConnToggle(Icons.Filled.Bluetooth, Color(0xFF0A84FF), states.bluetooth, onBluetooth)
            }
        }
    }
}

@Composable
private fun ConnToggle(icon: ImageVector, color: Color, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(52.dp).clip(CircleShape)
            .background(if (active) color else Color.White.copy(alpha = 0.16f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (active) Color.White else Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
    }
}

// ─── Media panel ─────────────────────────────────────────────────────────────
@Composable
private fun MediaPanel(media: MediaUiState, onExpand: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier.clickable(enabled = media.active, onClick = onExpand).padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Contenu centré verticalement ET horizontalement dans la tuile.
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                val art = media.art
                if (art != null) {
                    androidx.compose.foundation.Image(bitmap = art.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(if (media.active) media.title else "Aucun média", color = Color.White, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().basicMarquee())
            if (media.artist.isNotBlank()) Text(media.artist, color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                MediaBtn(Icons.Filled.SkipPrevious, media.active) { media.prev() }
                MediaBtn(if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, media.active, big = true) { media.playPause() }
                MediaBtn(Icons.Filled.SkipNext, media.active) { media.next() }
            }
        }
    }
}

@Composable
private fun MediaBtn(icon: ImageVector, enabled: Boolean, big: Boolean = false, onClick: () -> Unit) {
    Icon(imageVector = icon, null, tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f),
        modifier = Modifier.size(if (big) 38.dp else 30.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick))
}

// ─── Now Playing plein écran ──────────────────────────────────────────────────
private fun fmtTime(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }

@Composable
private fun NowPlayingFull(media: MediaUiState, onClose: () -> Unit, onCast: () -> Unit) {
    val topPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val dominant = remember(media.art) { dominantColorOf(media.art) } ?: Color(0xFF2E7D5B)
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(dominant, Color.White, 0.12f), lerp(dominant, Color.Black, 0.25f), lerp(dominant, Color.Black, 0.62f))))
            .pointerInput(Unit) {
                var acc = 0f
                detectVerticalDragGestures(onDragStart = { acc = 0f }, onDragEnd = { if (acc > 140f) onClose() }, onVerticalDrag = { _, dy -> acc += dy })
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = topPad + 10.dp, start = 26.dp, end = 26.dp, bottom = 18.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.padding(bottom = 14.dp).size(width = 38.dp, height = 5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() })
            Spacer(Modifier.weight(0.5f))
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                val art = media.art
                if (art != null) androidx.compose.foundation.Image(bitmap = art.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(72.dp))
            }
            Spacer(Modifier.height(22.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Titre centré ; défile horizontalement (marquee) s'il est trop long.
                Text(media.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().basicMarquee())
                if (media.artist.isNotBlank()) Text(media.artist, color = Color.White.copy(alpha = 0.72f),
                    fontSize = 18.sp, maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            NowPlayingProgress(media)
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(26.dp))
                MediaBtn(Icons.Filled.SkipPrevious, media.active, big = true) { media.prev() }
                MediaBtn(if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, media.active, big = true) { media.playPause() }
                MediaBtn(Icons.Filled.SkipNext, media.active, big = true) { media.next() }
                Icon(Icons.Filled.Cast, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(20.dp))
            NowPlayingVolume()
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCast() }) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Speaker, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text("Control Other\nSpeakers & TVs", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun NowPlayingProgress(media: MediaUiState) {
    val dur = media.duration.coerceAtLeast(0L)
    var dragFrac by remember { mutableStateOf<Float?>(null) }
    val livePos by produceState(initialValue = media.livePosition(), media.controller, media.isPlaying) {
        while (true) { value = media.livePosition(); kotlinx.coroutines.delay(500) }
    }
    val frac = dragFrac ?: if (dur > 0) (livePos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val shownPos = if (dur > 0) (frac * dur).toLong() else livePos
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(20.dp)
                .pointerInput(dur) {
                    if (dur <= 0) return@pointerInput
                    fun seekAt(x: Float) { dragFrac = (x / size.width).coerceIn(0f, 1f) }
                    detectHorizontalDragGestures(onDragStart = { off -> seekAt(off.x) }, onHorizontalDrag = { ch, _ -> seekAt(ch.position.x) },
                        onDragEnd = { dragFrac?.let { media.seekTo((it * dur).toLong()) }; dragFrac = null }, onDragCancel = { dragFrac = null })
                }
                .pointerInput(dur) { if (dur <= 0) return@pointerInput; detectTapGestures { off -> val f = (off.x / size.width).coerceIn(0f, 1f); media.seekTo((f * dur).toLong()) } },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)))
            Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)))
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmtTime(shownPos), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Text(if (dur > 0) "-" + fmtTime((dur - shownPos).coerceAtLeast(0)) else "", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun NowPlayingVolume() {
    val context = LocalContext.current
    val (vol, setVol) = rememberLiveVolume(context)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (vol < 0.01f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeDown, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.weight(1f).height(20.dp)
                .pointerInput(Unit) { fun setAt(x: Float) { setVol((x / size.width).coerceIn(0f, 1f)) }; detectHorizontalDragGestures(onDragStart = { off -> setAt(off.x) }, onHorizontalDrag = { ch, _ -> setAt(ch.position.x) }) }
                .pointerInput(Unit) { detectTapGestures { off -> setVol((off.x / size.width).coerceIn(0f, 1f)) } },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)))
            Box(Modifier.fillMaxWidth(vol.coerceIn(0f, 1f)).height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)))
        }
        Spacer(Modifier.width(12.dp))
        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
    }
}

// ─── Sheet d'ajout de tuile ────────────────────────────────────────────────────
@Composable
private fun AddShortcutSheet(
    existingShortcuts: List<CcShortcut>,
    refreshTick: Int,
    onAdd: (CcShortcut) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val apps = rememberInstalledApps(refreshTick)
    val tileServices = rememberTileServices(refreshTick)

    val existingTypes = existingShortcuts.map { it.type }.toSet()
    val existingPkgs = existingShortcuts.filter { it.type == "APP" }.mapNotNull { it.pkg }.toSet()
    val existingTilePairs = existingShortcuts.filter { it.type == "TILE" }
        .mapNotNull { s -> s.pkg?.let { Pair(it, s.extra) } }.toSet()

    val builtinInfo = mapOf(
        "TORCH" to Pair("Lampe", Icons.Filled.FlashlightOn),
        "TIMER" to Pair("Minuteur", Icons.Filled.Timer),
        "CALCULATOR" to Pair("Calculatrice", Icons.Filled.Calculate),
        "CAMERA" to Pair("Photo", Icons.Filled.PhotoCamera),
    )
    val availableBuiltins = builtinInfo.entries.filter { it.key !in existingTypes }

    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Ajouter une tuile", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (availableBuiltins.isNotEmpty()) {
                item { Text("Contrôles", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 10.dp)) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                        items(availableBuiltins) { (type, info) ->
                            val (label, icon) = info
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAdd(CcShortcut(type, label = label)) }) {
                                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                            }
                        }
                    }
                }
            }
            if (tileServices.isNotEmpty()) {
                item { Text("Tuiles d'apps", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 10.dp)) }
                items(tileServices.filter { Pair(it.pkg, it.componentName) !in existingTilePairs }) { tile ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onAdd(CcShortcut("TILE", pkg = tile.pkg, label = tile.label, extra = tile.componentName)) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val tileIcon = rememberTileIconBitmap(tile.pkg, tile.componentName, refreshTick)
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            if (tileIcon != null) androidx.compose.foundation.Image(bitmap = tileIcon.asImageBitmap(), null, modifier = Modifier.size(28.dp), contentScale = ContentScale.Fit)
                        }
                        Text(tile.label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Filled.Add, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            item { Text("Applications", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 10.dp)) }
            items(apps.filter { it.first !in existingPkgs }) { (pkg, label) ->
                Row(modifier = Modifier.fillMaxWidth().clickable { onAdd(CcShortcut("APP", pkg = pkg, label = label)) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val iconBmp = rememberAppIconBitmap(pkg, refreshTick)
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        if (iconBmp != null) androidx.compose.foundation.Image(bitmap = iconBmp.asImageBitmap(), null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                    }
                    Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Filled.Add, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ─── Helpers composable ────────────────────────────────────────────────────────
@Composable
private fun rememberTileServices(refreshTick: Int): List<TileServiceInfo> {
    val context = LocalContext.current
    var tiles by remember { mutableStateOf<List<TileServiceInfo>>(emptyList()) }
    LaunchedEffect(refreshTick) { tiles = withContext(Dispatchers.IO) { loadTileServices(context) } }
    return tiles
}

@Composable
private fun rememberTileIconBitmap(pkg: String, component: String, refreshTick: Int): android.graphics.Bitmap? {
    val context = LocalContext.current
    var bmp by remember(component, refreshTick) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(component, refreshTick) { bmp = withContext(Dispatchers.IO) { loadTileIcon(context, pkg, component) } }
    return bmp
}

@Composable
private fun rememberInstalledApps(refreshTick: Int): List<Pair<String, String>> {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(refreshTick) {
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { it.packageName to pm.getApplicationLabel(it).toString() }
                .sortedBy { it.second }
        }
    }
    return apps
}

@Composable
private fun rememberAppIconBitmap(pkg: String, refreshTick: Int): android.graphics.Bitmap? {
    val context = LocalContext.current
    var bmp by remember(pkg, refreshTick) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(pkg, refreshTick) { bmp = withContext(Dispatchers.IO) { loadAppIcon(context, pkg) } }
    return bmp
}
