package com.stanleycx.fruitos.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.stanleycx.fruitos.data.HomeItem
import com.stanleycx.fruitos.ui.components.FolderMiniGrid
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.IconCache
import com.stanleycx.fruitos.ui.components.FruitIconShape
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.GlossLevel
import com.stanleycx.fruitos.ui.components.glass
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

/**
 * Affiche l'icône "fantôme" qui suit le doigt pendant un drag.
 * Supporte les Apps et les Dossiers (style Fruit OS).
 */
@Composable
fun DragGhost(
    dragState: DragState,
    hazeState: dev.chrisbanes.haze.HazeState,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None
) {
    when (val item = dragState.draggedItem) {
        is HomeItem.App -> AppDragGhost(item, dragState)
        is HomeItem.Folder -> FolderDragGhost(item, dragState, hazeState, glassLevel, glassTint, customTintColor, loupeLevel, glossLevel)
        null -> return
    }
}

@Composable
private fun AppDragGhost(appItem: HomeItem.App, dragState: DragState) {
    val app = appItem.app
    val context = LocalContext.current
    val cached = remember(app.packageName) {
        IconCache.getOrRender(app.packageName, context)
    }

    com.stanleycx.fruitos.ui.components.StyledIconTile(
        imageBitmap = cached.imageBitmap,
        backgroundColor = cached.backgroundColor,
        size = 68.dp,
        contentDescription = app.label,
        shadowElevation = 16.dp,
        packageName = app.packageName,
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (dragState.position.x - 102).roundToInt(),
                    y = (dragState.position.y - 102).roundToInt()
                )
            }
            .scale(1.20f)
    )
}

@Composable
private fun FolderDragGhost(
    folder: HomeItem.Folder,
    dragState: DragState,
    hazeState: dev.chrisbanes.haze.HazeState,
    glassLevel: GlassLevel,
    glassTint: GlassTint,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (dragState.position.x - 102).roundToInt(),
                    y = (dragState.position.y - 102).roundToInt()
                )
            }
            .size(68.dp)
            .scale(1.22f)
            .clip(FruitIconShape)
            .glass(
                hazeState = hazeState,
                level = glassLevel,
                glassTint = glassTint,
                customTintColor = customTintColor,
                shape = FruitIconShape,
                loupeLevel = loupeLevel,
                glossLevel = glossLevel
            ),
        contentAlignment = Alignment.Center
    ) {
        FolderMiniGrid(folder.apps)
    }
}
