package com.stanleycx.fruitos.ui.dock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.AppRepository
import com.stanleycx.fruitos.data.LauncherLayout
import com.stanleycx.fruitos.ui.components.AppIcon
import com.stanleycx.fruitos.ui.home.DragState
import com.stanleycx.fruitos.ui.home.EditModeState
import dev.chrisbanes.haze.HazeState
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.glass
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.GlossLevel

/**
 * Le dock Fruit OS : barre en bas avec jusqu'à 4 apps, fond semi-transparent.
 *
 * Supporte le drag and drop : on peut faire glisser des apps depuis et vers le dock.
 */
@Composable
fun Dock(
    apps: List<AppInfo>,
    appRepository: AppRepository,
    editMode: EditModeState,
    dragState: DragState,
    hazeState: dev.chrisbanes.haze.HazeState,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
    onLongPress: () -> Unit,
    onRemoveApp: (AppInfo) -> Unit,
    onDragStart: (AppInfo, Offset) -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    notificationCounts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    // Calcul de l'ordre d'affichage : si on drag vers le dock, l'aperçu apparaît
    val dragged = dragState.draggedApp
    val displayApps = when {
        // App du dock en cours de drag : on la garde, isBeingDragged la masquera
        dragged != null && apps.any { it.packageName == dragged.packageName } -> {
            apps
        }
        // App d'ailleurs qui survole le dock MAIS dock plein : pas d'aperçu
        dragged != null && dragState.hoveringDock && apps.size >= LauncherLayout.DOCK_MAX_SIZE -> {
            apps  // Le dock reste tel quel = signal visuel "pas de place"
        }
        // App d'ailleurs qui survole le dock et dock non plein : aperçu
        dragged != null && dragState.hoveringDock -> {
            val safeSlot = dragState.hoverDockSlot.coerceIn(0, apps.size)
            buildList {
                addAll(apps.subList(0, safeSlot))
                add(dragged)
                addAll(apps.subList(safeSlot, apps.size))
            }
        }
        else -> apps
    }

    // Dock hover is now driven synchronously from the global pointer tracking loop in HomeScreen
    // (dragState.updateDockHover() called at native touch rate). No polling coroutine here.
    // Cleanup on drag end is handled in dragState.end().

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(96.dp)  // 🆕 plus haut
            .clip(RoundedCornerShape(36.dp))
            // Utilisation du nouveau système de verre multi-couches (Fruit OS style)
            .glass(
                hazeState = hazeState,
                level = glassLevel,
                glassTint = glassTint,
                customTintColor = customTintColor,
                shape = RoundedCornerShape(36.dp),
                loupeLevel = loupeLevel, glossLevel = glossLevel
            )
            .padding(horizontal = 12.dp)
            // ...
            .onGloballyPositioned { coordinates ->
                dragState.dockBounds = Rect(
                    offset = coordinates.positionInRoot(),
                    size = coordinates.size.toSize()
                )
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (app in displayApps.take(LauncherLayout.DOCK_MAX_SIZE)) {
            AppIcon(
                app = app,
                onClick = {
                    if (!editMode.isEditing) {
                        appRepository.launchApp(app.packageName)
                    }
                },
                onLongClick = onLongPress,
                onRemove = { onRemoveApp(app) },
                onDragStart = { position -> onDragStart(app, position) },
                onDrag = { delta -> onDragDelta(delta) },
                onDragEnd = onDragEnd,
                isEditing = editMode.isEditing,
                isBeingDragged = dragState.draggedApp?.packageName == app.packageName,
                showLabel = false,
                iconSize = 72.dp,
                badgeCount = notificationCounts[app.packageName] ?: 0
            )
        }
    }
}