package com.stanleycx.fruitos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.material3.Text
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.HomeItem
import com.stanleycx.fruitos.ui.components.Haptics
import com.stanleycx.fruitos.ui.components.IconCache
import com.stanleycx.fruitos.ui.components.PageIndicator
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.backgroundBlurFor
import com.stanleycx.fruitos.ui.components.glass
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.GlossLevel
import com.stanleycx.fruitos.ui.components.NotificationBadge
import kotlin.math.roundToInt

/**
 * Ouverture d'un dossier du HOME facon Fruit OS : carte carree arrondie centree.
 * Mode edition PARTAGE avec le home.
 *
 * DRAG : un SEUL tracking global (au niveau du Box racine, en passe Initial),
 * exactement comme le home. Les icones ne font qu'ARMER le drag au long-press ;
 * tout le suivi se passe au-dessus => plus de transfert de pointeur entre
 * detecteurs voisins (fini le drop sauvage au survol d'une autre app), et le
 * geste survit aussi au flip de page.
 */
@Composable
fun FolderOverlay(
    folder: HomeItem.Folder,
    editMode: EditModeState,
    hazeState: HazeState,
    glassLevel: GlassLevel = GlassLevel.Thick,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
    onLaunchApp: (AppInfo) -> Unit,
    onRename: (String) -> Unit,
    onReorder: (List<AppInfo>) -> Unit,
    onPullOut: (AppInfo) -> Unit,
    onRemove: (AppInfo) -> Unit,
    onClose: () -> Unit,
    onRequestContextMenu: (AppInfo, Rect) -> Unit = { _, _ -> },
    notificationCounts: Map<String, Int> = emptyMap()
) {
    var name by remember(folder.id) { mutableStateOf(folder.name) }
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val context = LocalContext.current

    val editing = editMode.isEditing

    var workingApps by remember(folder.id) { mutableStateOf(folder.apps) }
    LaunchedEffect(folder.apps) { workingApps = folder.apps }

    val pages = workingApps.chunked(9)
    val pageCount = pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    val iconSize = 64.dp

    // --- Etat du drag (global) ---
    var draggedPkg by remember { mutableStateOf<String?>(null) }
    var fingerRoot by remember { mutableStateOf(Offset.Zero) }
    var overlayRoot by remember { mutableStateOf(Offset.Zero) }
    var cardBounds by remember { mutableStateOf<Rect?>(null) }
    var gridBounds by remember { mutableStateOf<Rect?>(null) }
    val iconRootPos = remember { mutableStateMapOf<String, Offset>() }

    // Drapeau : une icone a arme le drag, le tracking global doit prendre le relais.
    var armDragPkg by remember { mutableStateOf<String?>(null) }
    var flipping by remember { mutableStateOf(false) }

    val iconHalfPx = with(density) { 30.dp.toPx() }

    fun commitName() = onRename(name)
    val commitAndClose = { commitName(); onClose() }

    // Reorder en direct selon la position absolue du doigt (spread immediat).
    fun reorderAt(pkg: String, pos: Offset) {
        if (flipping) return
        val gb = gridBounds ?: return
        if (pos.x !in gb.left..gb.right || pos.y !in gb.top..gb.bottom) return
        val cellW = gb.width / 3f
        val cellH = gb.height / 3f
        val col = ((pos.x - gb.left) / cellW).toInt().coerceIn(0, 2)
        val row = ((pos.y - gb.top) / cellH).toInt().coerceIn(0, 2)
        val slot = row * 3 + col
        val target = (pagerState.currentPage * 9 + slot).coerceAtMost(workingApps.lastIndex)
        val from = workingApps.indexOfFirst { it.packageName == pkg }
        if (from >= 0 && target >= 0 && target != from) {
            val list = workingApps.toMutableList()
            val item = list.removeAt(from)
            list.add(target.coerceIn(0, list.size), item)
            workingApps = list
        }
    }

    fun finishDrag(pkg: String) {
        val cb = cardBounds
        val outside = cb != null &&
                (fingerRoot.x !in cb.left..cb.right || fingerRoot.y !in cb.top..cb.bottom)
        val app = workingApps.firstOrNull { it.packageName == pkg }
        draggedPkg = null
        armDragPkg = null
        if (outside && app != null) {
            commitName()
            onPullOut(app)
        } else {
            onReorder(workingApps)
        }
    }

    // Auto-flip : si le doigt depasse la grille a gauche/droite pendant un drag,
    // on tourne la page tout seul (saut instantane = ne vole pas le geste global).
    LaunchedEffect(draggedPkg) {
        if (draggedPkg == null) return@LaunchedEffect
        while (draggedPkg != null) {
            val gb = gridBounds
            val cb = cardBounds
            if (gb != null && cb != null && !flipping &&
                fingerRoot.y in cb.top..cb.bottom
            ) {
                val cur = pagerState.currentPage
                when {
                    fingerRoot.x > gb.right && cur < pageCount - 1 -> {
                        flipping = true
                        pagerState.scrollToPage(cur + 1)
                        kotlinx.coroutines.delay(350)
                        flipping = false
                    }
                    fingerRoot.x < gb.left && cur > 0 -> {
                        flipping = true
                        pagerState.scrollToPage(cur - 1)
                        kotlinx.coroutines.delay(350)
                        flipping = false
                    }
                }
            }
            kotlinx.coroutines.delay(80)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayRoot = it.positionInRoot() }
            // Fond derrière le dossier ouvert (depuis l'écran d'accueil) = blur neutre simple
            // On ne veut PAS le verre + teinte sur tout le fond ici.
            .hazeEffect(state = hazeState) {
                blurRadius = backgroundBlurFor(glassLevel)
                tints = emptyList()
                noiseFactor = 0.015f
            }
            // Tap dans le vide => commit + fermeture.
            .pointerInput(Unit) { detectTapGestures(onTap = { commitAndClose() }) }
            // TRACKING GLOBAL DU DRAG (passe Initial : on voit le geste avant les
            // icones, et on ne le lache jamais => plus de transfert de pointeur).
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // On attend qu'une icone arme le drag (long-press).
                        awaitPointerEvent(pass = PointerEventPass.Initial)
                        val armed = armDragPkg
                        if (armed != null) {
                            armDragPkg = null
                            draggedPkg = armed
                            // Position de depart = centre connu de l'icone armee.
                            fingerRoot = (iconRootPos[armed] ?: Offset.Zero) +
                                    Offset(iconHalfPx, iconHalfPx)

                            var dragging = true
                            while (dragging) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val change = event.changes.firstOrNull()
                                if (change == null) {
                                    dragging = false
                                    finishDrag(armed)
                                } else if (change.pressed) {
                                    fingerRoot = change.position
                                    reorderAt(armed, change.position)
                                    change.consume()
                                } else {
                                    dragging = false
                                    finishDrag(armed)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                ),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitName(); keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onGloballyPositioned { cardBounds = Rect(it.positionInRoot(), it.size.toSize()) }
                    .clip(RoundedCornerShape(44.dp))
                    .glass(
                        hazeState = hazeState,
                        level = glassLevel,
                        glassTint = glassTint,
                        customTintColor = customTintColor,
                        shape = RoundedCornerShape(44.dp),
                        loupeLevel = loupeLevel, glossLevel = glossLevel
                    )
                    .pointerInput(editing) {
                        detectTapGestures(onTap = { if (editing) editMode.exit() })
                    }
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = draggedPkg == null,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) { pageIdx ->
                        FolderPageGrid(
                            apps = pages.getOrElse(pageIdx) { emptyList() },
                            isEditing = editing,
                            draggedPkg = draggedPkg,
                            iconSize = iconSize,
                            notificationCounts = notificationCounts,
                            onCaptureGrid = { rect ->
                                if (pageIdx == pagerState.currentPage) gridBounds = rect
                            },
                            onCaptureIcon = { pkg, pos -> iconRootPos[pkg] = pos },
                            onLaunch = { app -> commitName(); onLaunchApp(app); onClose() },
                            onEnterEdit = {
                                if (!editMode.isEditing) { editMode.enter(); Haptics.medium(context) }
                            },
                            onArmDrag = { pkg -> armDragPkg = pkg },
                            onRemove = onRemove,
                            onRequestContextMenu = onRequestContextMenu
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().height(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pageCount > 1) {
                            PageIndicator(
                                pageCount = pageCount,
                                currentPage = pagerState.currentPage,
                                pageOffsetFraction = { pagerState.currentPageOffsetFraction }
                            )
                        }
                    }
                }
            }
        }

        // Fantome qui suit le doigt
        draggedPkg?.let { pkg ->
            workingApps.firstOrNull { it.packageName == pkg }?.let { app ->
                val ctx = LocalContext.current
                val cached = remember(pkg) { IconCache.getOrRender(pkg, ctx) }
                com.stanleycx.fruitos.ui.components.StyledIconTile(
                    imageBitmap = cached.imageBitmap,
                    backgroundColor = cached.backgroundColor,
                    size = iconSize,
                    contentDescription = app.label,
                    shadowElevation = 16.dp,
                    packageName = app.packageName,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (fingerRoot.x - overlayRoot.x - iconHalfPx).roundToInt(),
                                (fingerRoot.y - overlayRoot.y - iconHalfPx).roundToInt()
                            )
                        }
                        .scale(1.15f)
                )
            }
        }
    }
}

@Composable
private fun FolderPageGrid(
    apps: List<AppInfo>,
    isEditing: Boolean,
    draggedPkg: String?,
    iconSize: Dp,
    notificationCounts: Map<String, Int> = emptyMap(),
    onCaptureGrid: (Rect) -> Unit,
    onCaptureIcon: (String, Offset) -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onEnterEdit: () -> Unit,
    onArmDrag: (String) -> Unit,
    onRemove: (AppInfo) -> Unit,
    onRequestContextMenu: (AppInfo, Rect) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
            .onGloballyPositioned { onCaptureGrid(Rect(it.positionInRoot(), it.size.toSize())) },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (r in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (c in 0 until 3) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        apps.getOrNull(r * 3 + c)?.let { app ->
                            FolderDraggableIcon(
                                app = app,
                                isEditing = isEditing,
                                isDragged = draggedPkg == app.packageName,
                                iconSize = iconSize,
                                badgeCount = notificationCounts[app.packageName] ?: 0,
                                onCaptureIcon = onCaptureIcon,
                                onLaunch = onLaunch,
                                onEnterEdit = onEnterEdit,
                                onArmDrag = onArmDrag,
                                onRemove = onRemove,
                                onLongClickForMenu = { rect -> onRequestContextMenu(app, rect) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderDraggableIcon(
    app: AppInfo,
    isEditing: Boolean,
    isDragged: Boolean,
    iconSize: Dp,
    badgeCount: Int = 0,
    onCaptureIcon: (String, Offset) -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onEnterEdit: () -> Unit,
    onArmDrag: (String) -> Unit,
    onRemove: (AppInfo) -> Unit,
    onLongClickForMenu: (Rect) -> Unit = {}
) {
    val ctx = LocalContext.current
    val cached = remember(app.packageName) { IconCache.getOrRender(app.packageName, ctx) }
    val jiggle = useJiggleAngle(isEditing = isEditing, seed = app.packageName.hashCode())
    val density = LocalDensity.current

    // PERF: updated callbacks for the pointerInput inside open folder (homepage folder view).
    val onLaunchUpdated by rememberUpdatedState(onLaunch)
    val onLongClickForMenuUpdated by rememberUpdatedState(onLongClickForMenu)
    val onRemoveUpdated by rememberUpdatedState(onRemove)
    val onArmDragUpdated by rememberUpdatedState(onArmDrag)  // for edit drag

    // Position de la tuile visuelle de l'icône (pour le menu contextuel + highlight flottant)
    val tilePositionRef = remember { mutableStateOf(Offset.Zero) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .onGloballyPositioned { onCaptureIcon(app.packageName, it.positionInRoot()) }
            .alpha(if (isDragged) 0f else 1f)
            // L'icone ne fait QUE detecter tap / long-press. AUCUN detecteur de drag
            // ici => plus de conflit de pointeur. Le suivi est global (Box racine).
            .pointerInput(app.packageName, isEditing) {
                detectTapGestures(
                    onTap = { if (!isEditing) onLaunchUpdated(app) },
                    onLongPress = {
                        Haptics.light(ctx)
                        if (!isEditing) {
                            // Ouvre le menu contextuel (Personnaliser l'icône etc.) comme sur la grille principale.
                            // On ne force plus l'édition automatiquement : le menu propose "Éditer l'écran d'accueil".
                            val iconSizePx = with(density) { iconSize.toPx() }
                            val tilePos = tilePositionRef.value
                            val iconRect = Rect(
                                offset = tilePos,
                                size = Size(width = iconSizePx, height = iconSizePx)
                            )
                            onLongClickForMenuUpdated(iconRect)
                            // Pas d'onArmDrag ici (le menu gère l'édition si choisie ; long-press à nouveau une fois en édition pour drag).
                        } else {
                            // En mode édition : long press arme le drag (comportement existant pour réordonner dans le dossier)
                            onArmDragUpdated(app.packageName)
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(iconSize + 10.dp)
                .rotate(jiggle),
            contentAlignment = Alignment.Center
        ) {
            // Capture la position exacte de la tuile icône (squircle) pour calculer le Rect du menu contextuel
            // et positionner correctement le highlight flottant quand le menu s'ouvre.
            Box(
                modifier = Modifier
                    .onGloballyPositioned { tilePositionRef.value = it.positionInRoot() }
                    .size(iconSize),
                contentAlignment = Alignment.Center
            ) {
                com.stanleycx.fruitos.ui.components.StyledIconTile(
                    imageBitmap = cached.imageBitmap,
                    backgroundColor = cached.backgroundColor,
                    size = iconSize,
                    contentDescription = app.label,
                    shadowElevation = 4.dp,
                    packageName = app.packageName
                )
            }

            if (isEditing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(24.dp)
                        .offset(x = (-3).dp, y = (-2).dp)
                        .shadow(elevation = 2.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .pointerInput(app.packageName) {
                            detectTapGestures(onTap = { onRemoveUpdated(app) })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 10.dp, height = 2.dp)
                            .background(Color.Black)
                    )
                }
            }

            if (badgeCount > 0 && !isEditing) {
                NotificationBadge(
                    count = badgeCount,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-2).dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f
                )
            )
        )
    }
}