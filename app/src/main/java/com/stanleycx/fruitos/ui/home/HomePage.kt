package com.stanleycx.fruitos.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.HomeItem
import com.stanleycx.fruitos.data.AppRepository
import com.stanleycx.fruitos.ui.components.AppIcon
import com.stanleycx.fruitos.ui.components.FolderIcon
import com.stanleycx.fruitos.ui.components.FruitIconShape
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.GlossLevel
import com.stanleycx.fruitos.data.LauncherLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val GRID_COLUMNS = LauncherLayout.GRID_COLUMNS
private const val GRID_ROWS = LauncherLayout.GRID_ROWS

@Composable
fun HomePage(
    slots: Map<Int, HomeItem>,
    // Slots RÉELS de la page (état courant, NON décalé par le preview de drag).
    // Indispensable pour la détection de cible de fusion : `slots` (preview) insère
    // l'app draguée dans le slot survolé, ce qui masque l'occupant réel et empêchait
    // d'armer la fusion (on restait coincé en "écart automatique").
    realSlots: Map<Int, HomeItem> = emptyMap(),
    // Slots couverts par un widget sur cette page → interdits comme cible de drop d'app.
    blockedSlots: Set<Int> = emptySet(),
    appRepository: AppRepository,
    editMode: EditModeState,
    dragState: DragState,
    pageIndex: Int,
    onLongPress: () -> Unit,
    onRemoveApp: (AppInfo) -> Unit,
    onDragStart: (AppInfo, Offset) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    // Nouveaux callbacks pour le drag des dossiers
    onFolderDragStart: (HomeItem.Folder, Offset) -> Unit = { _, _ -> },
    onFolderDrag: (Offset) -> Unit = {},
    onFolderDragEnd: () -> Unit = {},
    onOpenFolder: (Int) -> Unit = {},
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
    onRequestContextMenu: (HomeItem, Rect) -> Unit = { _, _ -> },
    menuTarget: HomeItem? = null,           // l'élément pour lequel le menu contextuel est ouvert (pour grossir + cacher l'original)
    notificationCounts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pageScope = rememberCoroutineScope()
    // (plus de state local pour le menu contextuel : géré au niveau HomeScreen pour overlay plein écran)

    // Arbitrage par POSITION + petit délai d'écart sur les bords (REFACTORED FOR FLUIDITY):
    //  - Plus de polling 60fps while+delay par page. Sampling est fait dans le 1 global pointer loop.
    //  - Ici on utilise derivedStateOf pour "is finger over my grid + which slot" (reacts at recompose rate).
    //  - Dwell timers (180ms spread) sont des one-shot delay jobs lancés/cancelés sur changement de slot.
    //  - Résultat : 0 CPU pendant que le doigt bouge (sauf les jobs suspendus), updates à taux natif via global.
    val centerZone = 0.55f
    val spreadDelayMs = 180

    val fingerOverMyGrid by remember(dragState, pageIndex) {
        derivedStateOf {
            val b = dragState.pageGridBounds[pageIndex] ?: return@derivedStateOf null
            val f = dragState.position
            if (f.x !in b.left..b.right || f.y !in b.top..b.bottom) return@derivedStateOf null
            val relX = f.x - b.left
            val relY = (f.y - b.top).coerceAtLeast(0f)
            val cw = b.width / GRID_COLUMNS
            val rh = b.height / GRID_ROWS
            val c = (relX / cw).toInt().coerceIn(0, GRID_COLUMNS - 1)
            val r = (relY / rh).toInt().coerceIn(0, GRID_ROWS - 1)
            r * GRID_COLUMNS + c
        }
    }

    // FIX dossiers : la candidature à la fusion (doigt au CENTRE d'une cellule) doit être
    // ré-évaluée pendant que le doigt bouge À L'INTÉRIEUR du même slot. Sans ça, l'effet
    // ci-dessous (keyé sur le seul index de slot) ne se relançait jamais → on entrait
    // toujours par le bord (inCenter=false) et la branche fusion n'était jamais atteinte.
    // derivedStateOf n'émet que lorsque le booléen BASCULE → aucun churn par frame.
    val fingerInCenter by remember(dragState, pageIndex) {
        derivedStateOf {
            val b = dragState.pageGridBounds[pageIndex] ?: return@derivedStateOf false
            val f = dragState.position
            if (f.x !in b.left..b.right || f.y !in b.top..b.bottom) return@derivedStateOf false
            val relX = f.x - b.left
            val relY = (f.y - b.top).coerceAtLeast(0f)
            val cw = b.width / GRID_COLUMNS
            val rh = b.height / GRID_ROWS
            val col = (relX / cw).toInt().coerceIn(0, GRID_COLUMNS - 1)
            val row = (relY / rh).toInt().coerceIn(0, GRID_ROWS - 1)
            val fracX = ((relX - col * cw) / cw).coerceIn(0f, 1f)
            val fracY = ((relY - row * rh) / rh).coerceIn(0f, 1f)
            val lo = (1f - centerZone) / 2f
            val hi = 1f - lo
            fracX in lo..hi && fracY in lo..hi
        }
    }

    // Non-observable ref : écrire dans mutableStateOf<Job?> déclencherait une recomposition
    // de HomePage à chaque cancel/restart du job (plusieurs fois par frame pendant le drag).
    val spreadJobRef = remember { object { var job: Job? = null } }

    LaunchedEffect(dragState.isDragging, fingerOverMyGrid, fingerInCenter, pageIndex, blockedSlots) {
        spreadJobRef.job?.cancel()
        spreadJobRef.job = null

        if (!dragState.isDragging) {
            if (dragState.hoverPageIndex == pageIndex) { dragState.hoverPageIndex = -1; dragState.hoverSlotIndex = -1 }
            if (dragState.rawHoverPageIndex == pageIndex) { dragState.rawHoverPageIndex = -1; dragState.rawHoverSlotIndex = -1 }
            return@LaunchedEffect
        }

        val slotIndex = fingerOverMyGrid
        // Slot recouvert par un widget → cible interdite (sinon une app pouvait se déposer
        // SOUS le widget, ex. derrière le cadre photo). Traité comme "doigt hors grille".
        if (slotIndex == null || slotIndex in blockedSlots) {
            // Finger left this page's grid (ou sur un slot widget)
            if (dragState.hoverPageIndex == pageIndex) { dragState.hoverPageIndex = -1; dragState.hoverSlotIndex = -1 }
            if (dragState.rawHoverPageIndex == pageIndex) { dragState.rawHoverPageIndex = -1; dragState.rawHoverSlotIndex = -1 }
            return@LaunchedEffect
        }
        val currentSlot = slotIndex  // non-null here

        if (dragState.hoveringDock) {
            dragState.hoveringDock = false
            dragState.hoverDockSlot = -1
        }

        val dragged = dragState.draggedApp
        // Occupant lu sur l'état RÉEL (pas le preview) → stable quel que soit l'écart visuel.
        val occupant = realSlots[currentSlot]
        val isOccupiedByOther = occupant != null && dragged != null &&
                !(occupant is HomeItem.App && occupant.app.packageName == dragged.packageName)
        // Un dossier est une cible de drop directe : pas de phase "écart", on arme la
        // fusion dès le survol (façon Fruit OS), sans exiger d'être pile au centre.
        val occupantIsFolder = occupant is HomeItem.Folder

        // inCenter est un derivedStateOf (clé de cet effet) : il bascule quand le doigt
        // entre/sort de la zone centrale du slot, ce qui relance l'effet et permet d'armer
        // la fusion même quand on est entré dans la cellule par le bord.
        val inCenter = fingerInCenter

        when {
            isOccupiedByOther && (occupantIsFolder || inCenter) -> {
                // Candidat à la fusion (dossier survolé, ou app survolée au centre).
                // On ANNULE tout écart en cours et on efface hoverSlot : le preview
                // revient à l'état réel (l'occupant ne se pousse plus) → feedback propre.
                dragState.rawHoverPageIndex = pageIndex
                dragState.rawHoverSlotIndex = currentSlot
                if (dragState.hoverPageIndex == pageIndex) {
                    dragState.hoverPageIndex = -1
                    dragState.hoverSlotIndex = -1
                }
            }
            isOccupiedByOther -> {
                // Border → arm one-shot delay for spread (no tight loop, sleeps)
                dragState.rawHoverPageIndex = -1
                dragState.rawHoverSlotIndex = -1

                spreadJobRef.job = pageScope.launch {
                    kotlinx.coroutines.delay(spreadDelayMs.toLong())
                    if (dragState.isDragging && fingerOverMyGrid == currentSlot) {
                        dragState.hoverPageIndex = pageIndex
                        dragState.hoverSlotIndex = currentSlot
                    }
                }
            }
            else -> {
                dragState.hoverPageIndex = pageIndex
                dragState.hoverSlotIndex = currentSlot
                dragState.rawHoverPageIndex = -1
                dragState.rawHoverSlotIndex = -1
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            // Plus de gros bottom padding ici : la réservation se fait au niveau du Pager
            // via le Spacer(184.dp) dans HomeScreen (synchronisé avec la vraie hauteur du dock).
            // On garde juste un tout petit padding bas pour que le label de la dernière ligne
            // ne soit pas collé au bord de la zone.
            .padding(top = 8.dp, bottom = 8.dp)
            .onGloballyPositioned { coordinates ->
                dragState.pageGridBounds[pageIndex] = Rect(
                    offset = coordinates.positionInRoot(),
                    size = coordinates.size.toSize()
                )
            }
            .background(Color.Transparent),
        // Pas de spacedBy : les 6 .weight(1f) divisent l'espace en bandes parfaitement égales.
        // L'espacement régulier vient de la hauteur disponible + le petit padding vertical dans chaque Row.
    ) {
        for (row in 0 until GRID_ROWS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 2.dp)   // petite marge uniforme dans chaque bande → espacement visuel régulier et aéré
                    .background(Color.Transparent),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 0 until GRID_COLUMNS) {
                    val slotIndex = row * GRID_COLUMNS + col
                    val item = slots[slotIndex]

                    // derivedStateOf gate : quand mergeTargetPage/Slot changent, les 24 slots
                    // évaluent leur derivedStateOf mais seuls les 1-2 dont la valeur change
                    // déclenchent une recomposition. Sans ça, les 24 slots recomposent ensemble.
                    val isMergeTarget by remember(pageIndex, slotIndex) {
                        derivedStateOf {
                            dragState.mergeTargetPage == pageIndex &&
                                    dragState.mergeTargetSlot == slotIndex
                        }
                    }
                    val mergeScale by animateFloatAsState(
                        targetValue = if (isMergeTarget) 0.82f else 1f,
                        // Spring Fruit OS : rebond léger quand l'icône cible "aspire" la draguée.
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        ),
                        label = "merge_scale_$slotIndex"
                    )

                    // Survol "cible de drop" IMMÉDIAT (rawHover, avant le dwell de 450ms).
                    // Sert au feedback Fruity Glass instantané sur un dossier survolé.
                    val isDropHover by remember(pageIndex, slotIndex) {
                        derivedStateOf {
                            dragState.rawHoverPageIndex == pageIndex &&
                                    dragState.rawHoverSlotIndex == slotIndex
                        }
                    }
                    val itemIsFolder = item is HomeItem.Folder
                    // Un dossier "s'ouvre pour accueillir" dès le survol (grandit légèrement),
                    // au lieu de l'effet d'aspiration (rétrécissement) réservé aux apps.
                    val folderInviteScale by animateFloatAsState(
                        targetValue = if (itemIsFolder && (isDropHover || isMergeTarget)) 1.12f else 1f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        ),
                        label = "folder_invite_$slotIndex"
                    )
                    // Glow Fruity Glass du dossier : monte dès rawHover, plein au merge.
                    val folderGlow by animateFloatAsState(
                        targetValue = if (itemIsFolder && (isDropHover || isMergeTarget)) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 140),
                        label = "folder_glow_$slotIndex"
                    )
                    // Échelle finale : app cible = aspiration (mergeScale), dossier = accueil.
                    val finalSlotScale = if (itemIsFolder) folderInviteScale else mergeScale

                    key("slot_$slotIndex") {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            // Halo "aspiration" pour une APP cible (après dwell).
                            if (isMergeTarget && !itemIsFolder) {
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)   // halo de fusion un peu plus grand que l'icône (64dp)
                                        .clip(FruitIconShape)
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .border(
                                            width = 3.dp,
                                            color = Color.White.copy(alpha = 0.7f),
                                            shape = FruitIconShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) { }
                            }

                            // Glow Fruity Glass d'accueil pour un DOSSIER cible (immédiat).
                            if (folderGlow > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .scale(folderInviteScale)
                                        .clip(FruitIconShape)
                                        .background(Color.White.copy(alpha = 0.18f * folderGlow))
                                        .border(
                                            width = 2.5.dp,
                                            color = Color.White.copy(alpha = 0.75f * folderGlow),
                                            shape = FruitIconShape
                                        )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .scale(finalSlotScale)
                            ) {
                                when (item) {
                                    is HomeItem.App -> {
                                        val isAppMenuTarget = menuTarget is HomeItem.App &&
                                                menuTarget.app.packageName == item.app.packageName

                                        AppIcon(
                                            app = item.app,
                                            iconSize = 68.dp,
                                            onClick = {
                                                if (!editMode.isEditing) {
                                                    appRepository.launchApp(item.app.packageName)
                                                }
                                            },
                                            onLongClick = {
                                                if (editMode.isEditing) {
                                                    onLongPress()
                                                }
                                            },
                                            onLongClickForMenu = { rect ->
                                                if (!editMode.isEditing) {
                                                    onRequestContextMenu(item, rect)
                                                }
                                            },
                                            onRemove = { onRemoveApp(item.app) },
                                            onDragStart = { position -> onDragStart(item.app, position) },
                                            onDrag = { delta -> onDragDelta(delta) },
                                            onDragEnd = onDragEnd,
                                            isEditing = editMode.isEditing,
                                            isBeingDragged = dragState.draggedApp?.packageName == item.app.packageName,
                                            isContextMenuTarget = isAppMenuTarget,
                                            badgeCount = notificationCounts[item.app.packageName] ?: 0
                                        )
                                    }
                                    is HomeItem.Folder -> {
                                        val isBeingDragged = dragState.draggedFolder?.id == item.id
                                        val isFolderMenuTarget = menuTarget is HomeItem.Folder &&
                                                menuTarget.id == item.id
                                        val folderBadgeCount = item.apps
                                            .sumOf { app -> notificationCounts[app.packageName] ?: 0 }

                                        FolderIcon(
                                            folder = item,
                                            iconSize = 68.dp,

                                            onOpen = { onOpenFolder(slotIndex) },
                                            onLongClickForMenu = { rect ->
                                                if (!editMode.isEditing) {
                                                    onRequestContextMenu(item, rect)
                                                }
                                            },
                                            isEditing = editMode.isEditing,
                                            isBeingDragged = isBeingDragged,
                                            onDragStart = { position -> onFolderDragStart(item, position) },
                                            onDrag = onFolderDrag,
                                            onDragEnd = onFolderDragEnd,
                                            hazeState = hazeState,
                                            glassLevel = glassLevel,
                                            glassTint = glassTint,
                                            customTintColor = customTintColor,
                                            loupeLevel = loupeLevel, glossLevel = glossLevel,
                                            isContextMenuTarget = isFolderMenuTarget,
                                            badgeCount = folderBadgeCount
                                        )
                                    }
                                    null -> {
                                        /* slot vide - le long press sur zone vide est géré au niveau HomeScreen */
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}