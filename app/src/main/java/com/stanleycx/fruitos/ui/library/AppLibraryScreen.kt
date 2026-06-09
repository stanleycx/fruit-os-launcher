package com.stanleycx.fruitos.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.AppLibrarySection
import com.stanleycx.fruitos.data.buildAlphabeticalList
import com.stanleycx.fruitos.data.buildAppLibrarySections
import com.stanleycx.fruitos.ui.components.AppIcon
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.backgroundBlurFor
import com.stanleycx.fruitos.ui.components.glass
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.GlossLevel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalConfiguration


/**
 * Page App Library, façon Fruit OS — plein écran.
 */
@Composable
fun AppLibraryScreen(
    allApps: List<AppInfo>,
    suggestedApps: List<AppInfo>,
    state: AppLibraryState,
    hazeState: HazeState,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
    onLaunchApp: (AppInfo) -> Unit,
    onDeleteApp: (AppInfo) -> Unit = {},
    modifier: Modifier = Modifier,
    onSwipeBack: () -> Unit = {},
    isEditing: Boolean = false,
    onLibraryAppDragStart: (AppInfo, Offset) -> Unit = { _, _ -> },
    onRequestEditMode: () -> Unit = {}
) {
    val onRequestDeleteApp: (AppInfo) -> Unit = onDeleteApp
    val sections = remember(allApps, suggestedApps) {
        buildAppLibrarySections(allApps, suggestedApps)
    }
    val alphabetical = remember(allApps) { buildAlphabeticalList(allApps) }

    val filtered = remember(state.query, alphabetical) {
        if (state.query.isBlank()) emptyList()
        else alphabetical.filter { it.label.contains(state.query, ignoreCase = true) }
    }

    val openedSection = remember(state.openedSectionId, sections) {
        sections.find { it.id == state.openedSectionId }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val searchBarHeight = 44.dp
    val searchBarTopGap = 16.dp           // ← MOLETTE "descendre le tout" (etait 6.dp)
    val searchBarBottom = statusBarTop + searchBarTopGap + searchBarHeight
    val contentTopPadding = searchBarBottom + 48.dp   // les dossiers demarrent ici (sous la barre)

    val sideMargin = 24.dp                // ← MOLETTE "marge bord d'ecran" (etait 16.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .hazeEffect(state = hazeState) {
                blurRadius = backgroundBlurFor(glassLevel)
                // Fond neutre (juste blur + assombrissement)
                tints = listOf(HazeTint(Color.Black.copy(alpha = 0.32f)))
                noiseFactor = 0f
            }
            .pointerInput(Unit) {
                var totalDx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDx = 0f },
                    onDragEnd = {
                        if (totalDx > 80f) onSwipeBack()
                        totalDx = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        totalDx += dragAmount
                        change.consume()
                    }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isSearching -> {
                    AlphabeticalList(
                        apps = filtered,
                        topPadding = contentTopPadding,
                        sideMargin = sideMargin,
                        onLaunchApp = onLaunchApp,
                        isEditing = isEditing,
                        onLibraryAppDragStart = onLibraryAppDragStart,
                        onRequestEditMode = onRequestEditMode,
                        onRequestDeleteApp = onRequestDeleteApp
                    )
                }
                else -> {
                    FoldersGrid(
                        sections = sections,
                        topPadding = contentTopPadding,
                        sideMargin = sideMargin,
                        hazeState = hazeState,
                        glassLevel = glassLevel,
                        glassTint = glassTint,
                        customTintColor = customTintColor,
                        loupeLevel = loupeLevel, glossLevel = glossLevel,
                        onLaunchApp = onLaunchApp,
                        onOpenFolder = { state.openSection(it) },
                        isEditing = isEditing,
                        onLibraryAppDragStart = onLibraryAppDragStart,
                        onRequestEditMode = onRequestEditMode,
                        onRequestDeleteApp = onRequestDeleteApp
                    )
                }
            }
        }

        // Gradient statique remplaçant le blur progressif sur le contenu scrollable.
        // Le hazeEffect + hazeSource sur contenu qui scroll recalcule le blur à chaque frame
        // → source directe du jank de scroll. Un gradient opaque→transparent est visuellement
        // similaire (fondu du contenu derrière la barre fixe) sans coût GPU par frame.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(searchBarBottom + 72.dp)
                .background(
                    Brush.verticalGradient(
                        // Scrim plus léger : fond le contenu sous la barre tout en le laissant
                        // visible quand il défile derrière la barre semi-transparente.
                        0f to Color.Black.copy(alpha = 0.28f),
                        1f to Color.Transparent
                    )
                )
        )

        // Barre de recherche fixe, sous la status bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = statusBarTop + searchBarTopGap,
                    start = sideMargin,        // ← marge laterale alignee sur les dossiers
                    end = sideMargin
                )
        ) {
            AppLibrarySearchBar(
                query = state.query,
                onQueryChange = { state.query = it },
                height = searchBarHeight,
                hazeState = hazeState,
                glassLevel = glassLevel,
                glassTint = glassTint,
                customTintColor = customTintColor,
                loupeLevel = LoupeLevel.None,
                glossLevel = glossLevel
            )
        }
    }

    AnimatedVisibility(
        visible = openedSection != null,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f)
    ) {
        openedSection?.let { section ->
            OpenedFolderOverlay(
                section = section,
                hazeState = hazeState,
                glassLevel = glassLevel,
                glassTint = glassTint,
                customTintColor = customTintColor,
                topPadding = contentTopPadding,
                onLaunchApp = onLaunchApp,
                onClose = { state.closeSection() },
                isEditing = isEditing,
                onLibraryAppDragStart = onLibraryAppDragStart,
                onRequestEditMode = onRequestEditMode,
                loupeLevel = loupeLevel, glossLevel = glossLevel,
                onRequestDeleteApp = onRequestDeleteApp
            )
        }
    }

}

@Composable
private fun FoldersGrid(
    sections: List<AppLibrarySection>,
    topPadding: androidx.compose.ui.unit.Dp,
    sideMargin: androidx.compose.ui.unit.Dp,
    hazeState: HazeState,
    glassLevel: GlassLevel,
    glassTint: GlassTint,
    customTintColor: Color? = null,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenFolder: (String) -> Unit,
    isEditing: Boolean = false,
    onLibraryAppDragStart: (AppInfo, Offset) -> Unit = { _, _ -> },
    onRequestEditMode: () -> Unit = {},
    onRequestDeleteApp: (AppInfo) -> Unit = {},
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Calcul unique de iconCell pour tous les dossiers — évite un BoxWithConstraints
    // par dossier (coûteux en sub-composition lors du scroll).
    val columnGap = 16.dp
    val pad = 14.dp
    val gap = 8.dp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val cellWidth = (screenWidthDp - sideMargin * 2 - columnGap) / 2
    val iconCell = (cellWidth - pad * 2 - gap) / 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(columnGap),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(
            start = sideMargin,
            end = sideMargin,
            top = topPadding,
            bottom = navBottom + 24.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        items(sections, key = { it.id }) { section ->
            CategoryFolder(
                section = section,
                iconCell = iconCell,
                hazeState = hazeState,
                glassLevel = glassLevel,
                glassTint = glassTint,
                customTintColor = customTintColor,
                loupeLevel = loupeLevel,
                glossLevel = glossLevel,
                onAppClick = onLaunchApp,
                onOpenFolder = { onOpenFolder(section.id) },
                isEditing = isEditing,
                onLibraryAppDragStart = onLibraryAppDragStart,
                onRequestEditMode = onRequestEditMode,
                onRequestDeleteApp = onRequestDeleteApp
            )
        }
    }
}

@Composable
private fun AlphabeticalList(
    apps: List<AppInfo>,
    topPadding: androidx.compose.ui.unit.Dp,
    sideMargin: androidx.compose.ui.unit.Dp,
    onLaunchApp: (AppInfo) -> Unit,
    isEditing: Boolean = false,
    onLibraryAppDragStart: (AppInfo, Offset) -> Unit = { _, _ -> },
    onRequestEditMode: () -> Unit = {},
    onRequestDeleteApp: (AppInfo) -> Unit = {}
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        contentPadding = PaddingValues(
            start = sideMargin,
            end = sideMargin,
            top = topPadding,
            bottom = navBottom + 24.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppListRow(
                app = app,
                onClick = { onLaunchApp(app) },
                isEditing = isEditing,
                onLibraryAppDragStart = onLibraryAppDragStart,
                onRequestEditMode = onRequestEditMode,
                onRequestDeleteApp = onRequestDeleteApp
            )
        }
    }
}

@Composable
private fun AppListRow(
    app: AppInfo,
    onClick: () -> Unit,
    isEditing: Boolean = false,
    onLibraryAppDragStart: (AppInfo, Offset) -> Unit = { _, _ -> },
    onRequestEditMode: () -> Unit = {},
    onRequestDeleteApp: (AppInfo) -> Unit = {}
) {
    val rowRootPosition = remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val newPos = coordinates.positionInRoot()
                if (newPos != rowRootPosition.value) rowRootPosition.value = newPos
            }
            .pointerInput(app.packageName, isEditing) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { offset ->
                        if (!isEditing) {
                            onRequestEditMode()
                        } else {
                            onLibraryAppDragStart(app, rowRootPosition.value + offset)
                        }
                    }
                )
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            AppIcon(
                app = app,
                onClick = onClick,
                onRemove = { onRequestDeleteApp(app) },
                onDragStart = { pos -> onLibraryAppDragStart(app, pos) },
                isEditing = isEditing,
                isLibraryContext = true,
                showLabel = false,
                iconSize = 38.dp,
                onLongClickForMenu = if (!isEditing) {
                    { onRequestEditMode() }
                } else {
                    {}
                }
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * Barre de recherche pilule avec effet "magnifying glass" poussé (Fruity Glass).
 */
@Composable
private fun AppLibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    height: androidx.compose.ui.unit.Dp,
    hazeState: HazeState,
    glassLevel: GlassLevel,
    glassTint: GlassTint,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            // Glassmorphisme LÉGER : fond semi-transparent (pas de blur du contenu, qui
            // recalculé par frame faisait ramer le scroll). Les icônes / dossiers restent
            // visibles en défilant DERRIÈRE la barre fixe. Fine bordure pour le relief verre.
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Champ de saisie pleine largeur (invisible quand vide ; le placeholder
        // centré ci-dessous prend le relais visuel).
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Placeholder centré (loupe + texte) visible uniquement quand le champ est vide.
        if (query.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Bibliothèque d'apps",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Overlay plein écran montrant TOUTES les apps d'une catégorie.
 */
@Composable
private fun OpenedFolderOverlay(
    section: AppLibrarySection,
    hazeState: HazeState,
    glassLevel: GlassLevel,
    glassTint: GlassTint,
    customTintColor: Color? = null,
    topPadding: androidx.compose.ui.unit.Dp,
    onLaunchApp: (AppInfo) -> Unit,
    onClose: () -> Unit,
    isEditing: Boolean = false,
    onLibraryAppDragStart: (AppInfo, Offset) -> Unit = { _, _ -> },
    onRequestEditMode: () -> Unit = {},
    onRequestDeleteApp: (AppInfo) -> Unit = {},
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Un seul gestionnaire : tap n'importe où = fermer l'overlay (lance l'app si sur icône via son propre handler)
            // Long press n'importe où (hors édition) = mode édition. Évite les conflits entre 2 pointerInput.
            .pointerInput(isEditing) {
                detectTapGestures(
                    onTap = { onClose() },
                    onLongPress = { if (!isEditing) onRequestEditMode() }
                )
            }
            // Utilise le glass + tint global pour que tout le background du dossier ouvert
            // dans l'App Library respecte les réglages de l'utilisateur
            .glass(
                hazeState = hazeState,
                level = glassLevel,
                glassTint = glassTint,
                customTintColor = customTintColor,
                shape = androidx.compose.ui.graphics.RectangleShape,
                showBorder = false,
                loupeLevel = loupeLevel, glossLevel = glossLevel
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            Text(
                text = section.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = navBottom + 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(section.apps, key = { it.packageName }) { app ->
                    AppIcon(
                        app = app,
                        onClick = { onLaunchApp(app) },
                        onRemove = { onRequestDeleteApp(app) },
                        onDragStart = { pos -> onLibraryAppDragStart(app, pos) },
                        isEditing = isEditing,
                        isLibraryContext = true,
                        showLabel = true,
                        iconSize = 60.dp,
                        onLongClickForMenu = if (!isEditing) {
                            { onRequestEditMode() }
                        } else {
                            {}
                        }
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(topPadding))
                }
            }
        }
    }
}
