package com.stanleycx.fruitos.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.AppLibrarySection
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.IconCache
import com.stanleycx.fruitos.ui.components.iconStyleBackground
import com.stanleycx.fruitos.ui.components.iconStyleGlyphFilter
import com.stanleycx.fruitos.ui.components.FruitIconShape
import com.stanleycx.fruitos.ui.components.glass
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.GlossLevel
import dev.chrisbanes.haze.HazeState
import com.stanleycx.fruitos.ui.home.useJiggleAngle

/**
 * Un dossier de catégorie dans l'App Library, façon Fruit OS.
 *
 * [iconCell] est calculé une seule fois dans FoldersGrid et transmis ici pour
 * éviter un BoxWithConstraints coûteux par dossier.
 */
@Composable
fun CategoryFolder(
    section: AppLibrarySection,
    iconCell: Dp,
    hazeState: HazeState,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
    onAppClick: (AppInfo) -> Unit,
    onOpenFolder: () -> Unit,
    isEditing: Boolean = false,
    onLibraryAppDragStart: (AppInfo, Offset) -> Unit = { _, _ -> },
    onRequestEditMode: () -> Unit = {},
    onRequestDeleteApp: (AppInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val apps = section.apps
    val hasOverflow = apps.size > 4
    val pad = 14.dp
    val gap = 8.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // graphicsLayer Offscreen uniquement sur la tuile dossier (pas sur le Column entier)
        // — une seule couche GPU par tuile, pas deux.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .glass(
                    hazeState = hazeState,
                    level = glassLevel,
                    glassTint = glassTint,
                    customTintColor = customTintColor,
                    shape = RoundedCornerShape(36.dp),
                    loupeLevel = loupeLevel,
                    glossLevel = glossLevel
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .pointerInput(isEditing) {
                        detectTapGestures(
                            onTap = { onOpenFolder() },
                            onLongPress = { if (!isEditing) onRequestEditMode() }
                        )
                    }
            ) {
                FolderGrid(
                    apps = apps,
                    hasOverflow = hasOverflow,
                    iconCell = iconCell,
                    gap = gap,
                    isEditing = isEditing,
                    onAppClick = onAppClick,
                    onOpenFolder = onOpenFolder,
                    onLibraryAppDragStart = onLibraryAppDragStart,
                    onRequestEditMode = onRequestEditMode,
                    onRequestDeleteApp = onRequestDeleteApp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = section.title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 4f
                )
            )
        )
    }
}

@Composable
private fun FolderGrid(
    apps: List<AppInfo>,
    hasOverflow: Boolean,
    iconCell: Dp,
    gap: Dp,
    isEditing: Boolean = false,
    onAppClick: (AppInfo) -> Unit,
    onOpenFolder: () -> Unit,
    onLibraryAppDragStart: (AppInfo, Offset) -> Unit = { _, _ -> },
    onRequestEditMode: () -> Unit = {},
    onRequestDeleteApp: (AppInfo) -> Unit = {}
) {
    val firstThree = apps.take(3)
    val fourthApp = if (!hasOverflow) apps.getOrNull(3) else null
    val overflowApps = if (hasOverflow) apps.drop(3).take(4) else emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            CellSlot(firstThree.getOrNull(0), iconCell, isEditing, showRemoveBadge = isEditing, onAppClick = onAppClick, onLibraryAppDragStart = onLibraryAppDragStart, onRequestEditMode = onRequestEditMode, onRequestDeleteApp = onRequestDeleteApp)
            CellSlot(firstThree.getOrNull(1), iconCell, isEditing, showRemoveBadge = isEditing, onAppClick = onAppClick, onLibraryAppDragStart = onLibraryAppDragStart, onRequestEditMode = onRequestEditMode, onRequestDeleteApp = onRequestDeleteApp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            CellSlot(firstThree.getOrNull(2), iconCell, isEditing, showRemoveBadge = isEditing, onAppClick = onAppClick, onLibraryAppDragStart = onLibraryAppDragStart, onRequestEditMode = onRequestEditMode, onRequestDeleteApp = onRequestDeleteApp)
            Box(
                modifier = Modifier
                    .size(iconCell)
                    .pointerInput(isEditing) {
                        detectTapGestures(
                            onTap = { onOpenFolder() },
                            onLongPress = { if (!isEditing) onRequestEditMode() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    hasOverflow -> OverflowCluster(overflowApps, iconCell, isEditing)
                    fourthApp != null -> CellSlot(fourthApp, iconCell, isEditing, showRemoveBadge = isEditing, onAppClick = onAppClick, onLibraryAppDragStart = onLibraryAppDragStart, onRequestEditMode = onRequestEditMode, onRequestDeleteApp = onRequestDeleteApp)
                }
            }
        }
    }
}

@Composable
private fun CellSlot(
    app: AppInfo?,
    size: Dp,
    isEditing: Boolean = false,
    showRemoveBadge: Boolean = false,
    onAppClick: (AppInfo) -> Unit,
    onLibraryAppDragStart: (AppInfo, Offset) -> Unit = { _, _ -> },
    onRequestEditMode: () -> Unit = {},
    onRequestDeleteApp: (AppInfo) -> Unit = {}
) {
    if (app == null) {
        Box(modifier = Modifier.size(size))
        return
    }

    val cellRootPosition = remember { mutableStateOf(Offset.Zero) }
    val xTouchZonePx = with(androidx.compose.ui.platform.LocalDensity.current) { 22.dp.toPx() }

    Box(
        modifier = Modifier
            .size(size)
            .onGloballyPositioned { coordinates ->
                // Guard : évite les writes d'état inutiles quand la position n'a pas changé
                val newPos = coordinates.positionInRoot()
                if (newPos != cellRootPosition.value) cellRootPosition.value = newPos
            }
            .pointerInput(app.packageName, isEditing) {
                detectTapGestures(
                    onTap = { offset ->
                        if (isEditing && showRemoveBadge &&
                            offset.x < xTouchZonePx && offset.y < xTouchZonePx
                        ) {
                            onRequestDeleteApp(app)
                        } else {
                            onAppClick(app)
                        }
                    },
                    onLongPress = { offset ->
                        if (!isEditing) {
                            onRequestEditMode()
                        } else {
                            onLibraryAppDragStart(app, cellRootPosition.value + offset)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        StaticIcon(app, size, isEditing)

        if (showRemoveBadge && isEditing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = -4.dp, y = -4.dp)
                    .size(19.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(0.5.dp, Color.Black.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 14.dp, height = 2.dp)
                            .graphicsLayer { rotationZ = 45f }
                            .background(Color.Black)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 14.dp, height = 2.dp)
                            .graphicsLayer { rotationZ = -45f }
                            .background(Color.Black)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverflowCluster(apps: List<AppInfo>, size: Dp, isEditing: Boolean = false) {
    val miniGap = 4.dp
    val miniCell = (size - miniGap) / 2

    Column(verticalArrangement = Arrangement.spacedBy(miniGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(miniGap)) {
            StaticIcon(apps.getOrNull(0), miniCell, isEditing)
            StaticIcon(apps.getOrNull(1), miniCell, isEditing)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(miniGap)) {
            StaticIcon(apps.getOrNull(2), miniCell, isEditing)
            StaticIcon(apps.getOrNull(3), miniCell, isEditing)
        }
    }
}

@Composable
private fun StaticIcon(
    app: AppInfo?,
    size: Dp,
    isEditing: Boolean = false
) {
    if (app == null) {
        Box(modifier = Modifier.size(size))
        return
    }
    val ctx = LocalContext.current
    val cached = remember(app.packageName) {
        IconCache.getOrRender(app.packageName, ctx)
    }
    val jiggle = if (isEditing) useJiggleAngle(isEditing = true, seed = app.packageName.hashCode()) else 0f
    val zoom = com.stanleycx.fruitos.ui.components.iconStyleLogoScale(app.packageName)
    val glyphFilter = iconStyleGlyphFilter(app.packageName)

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = jiggle }
            .iconStyleBackground(cached.backgroundColor, com.stanleycx.fruitos.ui.components.FruitIconShape, app.packageName)
    ) {
        Image(
            bitmap = cached.imageBitmap,
            contentDescription = app.label,
            colorFilter = glyphFilter,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Offscreen seulement si filtre couleur appliqué (cf. StyledIconTile) — null en mode Défaut.
                    compositingStrategy =
                        if (glyphFilter != null) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                    if (zoom != 1f) { scaleX = zoom; scaleY = zoom }
                }
        )
    }
}
