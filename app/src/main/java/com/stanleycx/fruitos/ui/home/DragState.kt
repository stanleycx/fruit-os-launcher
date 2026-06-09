package com.stanleycx.fruitos.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.HomeItem

/**
 * État global du drag and drop – Version ultra-smooth Fruit OS
 * Supporte maintenant les Apps ET les Dossiers.
 */
class DragState {
    /**
     * L'élément en cours de drag (App ou Folder).
     * C'est la source de vérité.
     */
    var draggedItem: HomeItem? by mutableStateOf(null)
        private set

    /**
     * Compatibilité rétro pour le code existant qui utilise encore draggedApp.
     * Sera supprimé une fois la migration complète.
     */
    val draggedApp: AppInfo?
        get() = (draggedItem as? HomeItem.App)?.app

    val isDraggingFolder: Boolean
        get() = draggedItem is HomeItem.Folder

    val draggedFolder: HomeItem.Folder?
        get() = draggedItem as? HomeItem.Folder

    var position: Offset by mutableStateOf(Offset.Zero)
        private set

    var hoverPageIndex: Int by mutableStateOf(-1)
    var hoverSlotIndex: Int by mutableStateOf(-1)
    // Slot réellement survolé, publié SANS délai (pour la détection de fusion).
    // hoverSlotIndex, lui, est temporisé (pour l'écart) → les deux sont distincts.
    var rawHoverPageIndex: Int by mutableStateOf(-1)
    var rawHoverSlotIndex: Int by mutableStateOf(-1)

    var hoveringDock: Boolean by mutableStateOf(false)
    var hoverDockSlot: Int by mutableStateOf(-1)

    var dockBounds: Rect? by mutableStateOf(null)

    var isChangingPage: Boolean by mutableStateOf(false)
    var pendingNewPageIndex: Int by mutableStateOf(-1)

    // Slot survolé assez longtemps pour former un dossier (fusion). -1 = aucune.
    var mergeTargetPage: Int by mutableStateOf(-1)
    var mergeTargetSlot: Int by mutableStateOf(-1)
    val hasMergeTarget: Boolean
        get() = mergeTargetPage >= 0 && mergeTargetSlot >= 0

    var awaitingGlobalTracking: Boolean by mutableStateOf(false)

    val isDragging: Boolean
        get() = draggedItem != null

    val pageGridBounds = mutableStateMapOf<Int, Rect>()

    /**
     * Démarre le drag d'un élément du home (App ou Folder).
     */
    fun start(item: HomeItem, startPosition: Offset) {
        draggedItem = item
        position = startPosition
        awaitingGlobalTracking = true
    }

    fun update(newPosition: Offset) {
        position = newPosition
        // Note: full hover recompute (page/dock slot + merge candidate) is driven
        // from the global pointerInput tracking loop + one-shot dwell timers
        // (see HomeScreen). This keeps sampling at native touch rate with zero
        // extra polling coroutines per page/dock.
    }

    /**
     * Sync dock hover computation. Called from the single global drag tracking loop
     * at pointer event rate (no polling LaunchedEffect).
     */
    fun updateDockHover() {
        val bounds = dockBounds ?: return
        val finger = position
        val isOver = finger.x in bounds.left..bounds.right &&
                finger.y in bounds.top..bounds.bottom
        if (isOver) {
            val relX = finger.x - bounds.left
            val slotWidth = bounds.width / 4f
            val slot = (relX / slotWidth).toInt().coerceIn(0, 3)
            hoveringDock = true
            hoverDockSlot = slot
            // Clear page hover when over dock (Fruit OS behavior)
            if (hoverPageIndex >= 0) {
                hoverPageIndex = -1
                hoverSlotIndex = -1
            }
        } else if (hoveringDock) {
            hoveringDock = false
            hoverDockSlot = -1
        }
    }

    fun isOverDock(): Boolean {
        val bounds = dockBounds ?: return false
        return position.x in bounds.left..bounds.right &&
                position.y in bounds.top..bounds.bottom
    }

    fun computeDockSlot(): Int {
        val bounds = dockBounds ?: return -1
        val relX = position.x - bounds.left
        val slotWidth = bounds.width / 4f
        return (relX / slotWidth).toInt().coerceIn(0, 3)
    }

    fun end() {
        draggedItem = null
        position = Offset.Zero
        hoverPageIndex = -1
        hoverSlotIndex = -1
        hoveringDock = false
        hoverDockSlot = -1
        awaitingGlobalTracking = false
        isChangingPage = false
        pendingNewPageIndex = -1
        mergeTargetPage = -1
        mergeTargetSlot = -1
        rawHoverPageIndex = -1
        rawHoverSlotIndex = -1
    }
}

@Composable
fun rememberDragState(): DragState {
    return remember { DragState() }
}