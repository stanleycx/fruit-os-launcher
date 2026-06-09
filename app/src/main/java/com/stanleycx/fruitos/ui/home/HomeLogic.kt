package com.stanleycx.fruitos.ui.home

import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.HomeItem
import com.stanleycx.fruitos.data.LauncherLayout
import com.stanleycx.fruitos.data.LauncherState
import com.stanleycx.fruitos.data.WidgetLayout
import com.stanleycx.fruitos.data.blockedSlots
import com.stanleycx.fruitos.data.newFolderId
import com.stanleycx.fruitos.data.suggestFolderName

internal const val SLOTS_PER_PAGE = LauncherLayout.APPS_PER_PAGE

/**
 * Place un dossier dans un slot précis avec décalage Fruit OS (placement libre + cascade).
 * Les dossiers ne fusionnent pas avec d'autres éléments.
 */
fun moveFolderToPageSlot(
    state: LauncherState,
    folder: HomeItem.Folder,
    targetPage: Int,
    targetSlot: Int
): LauncherState {
    val pages = state.pages.map { it.toMutableMap() }.toMutableList()
    val dockWithoutFolder = state.dock

    pages.forEach { page ->
        page.entries.removeAll { (it.value as? HomeItem.Folder)?.id == folder.id }
    }

    while (pages.size <= targetPage) pages.add(mutableMapOf())

    var currentPage = targetPage
    var insertSlot = targetSlot
    var itemToPlace: HomeItem? = folder

    while (itemToPlace != null) {
        while (pages.size <= currentPage) pages.add(mutableMapOf())
        val page = pages[currentPage]

        if (!page.containsKey(insertSlot)) {
            page[insertSlot] = itemToPlace
            itemToPlace = null
        } else {
            val overflow = shiftAndInsert(page, insertSlot, itemToPlace)
            itemToPlace = overflow
            if (overflow != null) { currentPage++; insertSlot = 0 }
        }
    }

    return LauncherState(pages = pages, dock = dockWithoutFolder, hidden = state.hidden)
}

/**
 * Place une app dans un slot précis avec décalage Fruit OS (placement libre + cascade).
 */
fun moveAppToPageSlot(
    state: LauncherState,
    app: AppInfo,
    targetPage: Int,
    targetSlot: Int
): LauncherState {
    val pages = state.pages.map { it.toMutableMap() }.toMutableList()
    val dockWithoutApp = state.dock.filter { it.packageName != app.packageName }

    pages.forEach { page ->
        page.entries.removeAll { (it.value as? HomeItem.App)?.app?.packageName == app.packageName }
    }

    while (pages.size <= targetPage) pages.add(mutableMapOf())

    var currentPage = targetPage
    var insertSlot = targetSlot
    var itemToPlace: HomeItem? = HomeItem.App(app)

    while (itemToPlace != null) {
        while (pages.size <= currentPage) pages.add(mutableMapOf())
        val page = pages[currentPage]

        if (!page.containsKey(insertSlot)) {
            page[insertSlot] = itemToPlace
            itemToPlace = null
        } else {
            val overflow = shiftAndInsert(page, insertSlot, itemToPlace)
            itemToPlace = overflow
            if (overflow != null) { currentPage++; insertSlot = 0 }
        }
    }

    return LauncherState(pages = pages, dock = dockWithoutApp, hidden = state.hidden)
}

/**
 * Fusionne l'app draguée dans le slot cible pour former (ou enrichir) un dossier.
 * - Cible = app  → crée un dossier [cible, draggée] avec un nom suggéré
 * - Cible = dossier → ajoute la draggée au dossier
 */
fun mergeIntoFolder(
    state: LauncherState,
    dragged: AppInfo,
    targetPage: Int,
    targetSlot: Int
): LauncherState {
    val pages = state.pages.map { it.toMutableMap() }.toMutableList()
    val dockWithoutApp = state.dock.filter { it.packageName != dragged.packageName }

    pages.forEach { page ->
        page.entries.removeAll { (it.value as? HomeItem.App)?.app?.packageName == dragged.packageName }
    }

    val target = pages.getOrNull(targetPage)?.get(targetSlot) ?: return state
    if (target is HomeItem.App && target.app.packageName == dragged.packageName) return state

    val merged: HomeItem.Folder = when (target) {
        is HomeItem.Folder ->
            target.copy(apps = (target.apps + dragged).distinctBy { it.packageName })
        is HomeItem.App ->
            HomeItem.Folder(
                id = newFolderId(),
                name = suggestFolderName(listOf(target.app, dragged)),
                apps = listOf(target.app, dragged)
            )
    }

    pages[targetPage][targetSlot] = merged
    return LauncherState(pages = pages, dock = dockWithoutApp, hidden = state.hidden)
}

fun shiftAndInsert(
    page: MutableMap<Int, HomeItem>,
    targetSlot: Int,
    item: HomeItem
): HomeItem? {
    var freeSlot = targetSlot
    while (freeSlot < SLOTS_PER_PAGE && page.containsKey(freeSlot)) freeSlot++

    if (freeSlot < SLOTS_PER_PAGE) {
        for (slot in freeSlot downTo targetSlot + 1) page[slot] = page[slot - 1]!!
        page[targetSlot] = item
        return null
    } else {
        val overflow = page[SLOTS_PER_PAGE - 1]
        for (slot in SLOTS_PER_PAGE - 1 downTo targetSlot + 1) page[slot] = page[slot - 1]!!
        page[targetSlot] = item
        return overflow
    }
}

fun moveAppToDock(
    state: LauncherState,
    app: AppInfo,
    targetSlot: Int
): LauncherState {
    val pages = state.pages.map { page ->
        page.filterValues { !(it is HomeItem.App && it.app.packageName == app.packageName) }
    }
    val dockWithoutApp = state.dock.filter { it.packageName != app.packageName }

    val wasInDock = state.dock.any { it.packageName == app.packageName }
    if (!wasInDock && dockWithoutApp.size >= 4) return state

    val newDock = dockWithoutApp.toMutableList()
    val safeSlot = targetSlot.coerceIn(0, newDock.size)
    newDock.add(safeSlot, app)

    return LauncherState(pages = pages, dock = newDock, hidden = state.hidden)
}

fun computeDragPreview(
    pageSlots: Map<Int, HomeItem>,
    pageIndex: Int,
    dragState: DragState
): Map<Int, HomeItem> {
    val draggedApp = dragState.draggedApp
    val draggedFolder = dragState.draggedFolder

    if (draggedFolder != null) {
        fun withoutDraggedFolder(m: Map<Int, HomeItem>) =
            m.filterValues { (it as? HomeItem.Folder)?.id != draggedFolder.id }

        if (dragState.hoverPageIndex != pageIndex || dragState.hoverSlotIndex < 0) {
            return withoutDraggedFolder(pageSlots)
        }

        val targetSlot = dragState.hoverSlotIndex.coerceIn(0, SLOTS_PER_PAGE - 1)
        val preview = withoutDraggedFolder(pageSlots).toMutableMap()

        var slot = targetSlot
        var toInsert: HomeItem? = draggedFolder
        while (toInsert != null && slot < SLOTS_PER_PAGE) {
            val existing = preview[slot]
            preview[slot] = toInsert
            toInsert = existing
            slot++
        }
        return preview
    }

    val dragged = draggedApp ?: return pageSlots

    fun withoutDragged(m: Map<Int, HomeItem>) =
        m.filterValues { !(it is HomeItem.App && it.app.packageName == dragged.packageName) }

    if (dragState.hoverPageIndex != pageIndex || dragState.hoverSlotIndex < 0) {
        return withoutDragged(pageSlots)
    }

    if (dragState.mergeTargetPage == pageIndex &&
        dragState.mergeTargetSlot == dragState.hoverSlotIndex
    ) {
        return withoutDragged(pageSlots)
    }

    val targetSlot = dragState.hoverSlotIndex.coerceIn(0, SLOTS_PER_PAGE - 1)
    val preview = withoutDragged(pageSlots).toMutableMap()

    var slot = targetSlot
    var toInsert: HomeItem? = HomeItem.App(dragged)
    while (toInsert != null && slot < SLOTS_PER_PAGE) {
        val existing = preview[slot]
        preview[slot] = toInsert
        toInsert = existing
        slot++
    }
    return preview
}

/**
 * Supprime les pages d'apps TOTALEMENT vides (aucune icône/dossier ET aucun widget),
 * en réindexant à la fois les pages ET le `pageIndex` des widgets pour rester cohérent.
 * Ne supprime jamais la dernière page restante.
 *
 * Convention d'index : page d'apps i (0-based) ⇄ index pager i+1 (la page widget dédiée
 * est l'index pager 0, jamais touchée ici).
 *
 * Retourne le couple (state, widgetLayout) mis à jour ; renvoie les MÊMES instances si
 * rien n'a changé (permet un court-circuit `===` côté appelant, zéro recomposition inutile).
 */
fun removeEmptyPages(
    state: LauncherState,
    widgetLayout: WidgetLayout
): Pair<LauncherState, WidgetLayout> {
    if (state.pages.size <= 1) return state to widgetLayout

    // Index pager des pages d'apps qui portent au moins un widget.
    val pagerIndicesWithWidget = widgetLayout.placements
        .filter { it.pageIndex >= 1 }
        .map { it.pageIndex }
        .toSet()

    // On garde une page d'apps i si elle a un item OU un widget (index pager i+1).
    val keptOldIndices = state.pages.indices.filter { i ->
        state.pages[i].isNotEmpty() || (i + 1) in pagerIndicesWithWidget
    }

    // Rien à retirer.
    if (keptOldIndices.size == state.pages.size) return state to widgetLayout

    // Toujours conserver au moins une page.
    val finalOldIndices = keptOldIndices.ifEmpty { listOf(0) }

    // old appIndex → new appIndex
    val remap = HashMap<Int, Int>()
    finalOldIndices.forEachIndexed { newIdx, oldIdx -> remap[oldIdx] = newIdx }

    val newPages = finalOldIndices.map { state.pages[it] }

    // Réindexe les widgets des pages d'apps ; la page widget dédiée (pageIndex 0) reste telle quelle.
    val newPlacements = widgetLayout.placements.map { pl ->
        if (pl.pageIndex == 0) return@map pl
        val newApp = remap[pl.pageIndex - 1]
        if (newApp != null) pl.copy(pageIndex = newApp + 1) else pl
    }

    val newState = state.copy(pages = newPages)
    val newLayout = if (newPlacements == widgetLayout.placements) widgetLayout
                    else widgetLayout.copy(placements = newPlacements)
    return newState to newLayout
}

/**
 * Repack complet d'une page autour d'un widget.
 * Les slots couverts par le widget sont traités comme des "murs" :
 * tous les items de la page sont collectés dans l'ordre des slots,
 * puis replacés dans les slots disponibles (hors widget) dans ce même ordre.
 * Les items qui débordent sont insérés en cascade au début de la page suivante.
 */
fun repackPageAroundWidget(
    state: LauncherState,
    appPageIndex: Int,
    widgetBlockedSlots: Set<Int>
): LauncherState {
    val pages = state.pages.map { it.toMutableMap() }.toMutableList()
    while (pages.size <= appPageIndex) pages.add(mutableMapOf())
    val page = pages[appPageIndex]

    val availableSlots = (0 until SLOTS_PER_PAGE).filter { it !in widgetBlockedSlots }
    val allItems = (0 until SLOTS_PER_PAGE).mapNotNull { page[it] }

    (0 until SLOTS_PER_PAGE).forEach { page.remove(it) }

    val fitting = allItems.take(availableSlots.size)
    val overflow = allItems.drop(availableSlots.size)
    fitting.forEachIndexed { i, item -> page[availableSlots[i]] = item }

    var current = state.copy(pages = pages.map { it.toMap() })

    for (item in overflow.reversed()) {
        current = insertHomeItemAtSlot(current, item, appPageIndex + 1, 0)
    }

    return current
}

/**
 * Insère un item à [targetSlot] de [targetPage] avec cascade style Fruit OS
 * (shiftAndInsert + débordement automatique sur les pages suivantes).
 */
fun insertHomeItemAtSlot(
    state: LauncherState,
    item: HomeItem,
    targetPage: Int,
    targetSlot: Int
): LauncherState {
    val pages = state.pages.map { it.toMutableMap() }.toMutableList()
    var currentPage = targetPage
    var insertSlot = targetSlot
    var itemToPlace: HomeItem? = item

    while (itemToPlace != null) {
        while (pages.size <= currentPage) pages.add(mutableMapOf())
        val overflow = shiftAndInsert(pages[currentPage], insertSlot, itemToPlace)
        itemToPlace = overflow
        if (overflow != null) {
            currentPage++
            insertSlot = 0
        }
    }

    return state.copy(pages = pages.map { it.toMap() })
}

/** Place une app dans le premier slot libre (cree une page si tout est plein). */
fun placeAppFirstFree(
    state: LauncherState,
    app: AppInfo,
    widgetLayout: WidgetLayout = WidgetLayout.Empty
): LauncherState {
    val pages = state.pages.map { it.toMutableMap() }.toMutableList()
    if (pages.isEmpty()) pages.add(mutableMapOf())

    pages.forEach { pg ->
        pg.entries.removeAll { (it.value as? HomeItem.App)?.app?.packageName == app.packageName }
    }

    for ((pageIdx, page) in pages.withIndex()) {
        // Slots couverts par un widget sur cette page (index pager = pageIdx + 1) : à ÉVITER,
        // sinon l'app atterrit "sous" le widget (elle paraît derrière). On la place dans la
        // première cellule réellement libre ET non recouverte par un widget.
        val blocked = widgetLayout.placements
            .filter { it.pageIndex == pageIdx + 1 }
            .flatMap { it.blockedSlots() }
            .toSet()
        for (slot in 0 until SLOTS_PER_PAGE) {
            if (slot !in blocked && !page.containsKey(slot)) {
                page[slot] = HomeItem.App(app)
                return state.copy(pages = pages)
            }
        }
    }
    pages.add(mutableMapOf(0 to HomeItem.App(app)))
    return state.copy(pages = pages)
}

/**
 * Réarrangement à l'agrandissement d'un widget. Déplace les icônes des cellules NOUVELLEMENT
 * condamnées (`newBlocked − oldBlocked`) selon deux régimes :
 *
 *  • PAGE PAS PLEINE (assez de cellules libres pour toutes les recouvertes) → CHIRURGICAL :
 *    seules les icônes recouvertes bougent, chacune vers la 1ʳᵉ cellule libre (scan depuis 0,
 *    hors widget). Toutes les autres icônes restent STRICTEMENT à leur place.
 *    Ex : widget {0,1}→{0,1,4,5}, page aérée → seules les cellules 4,5 bougent ; la 20 ne bouge pas.
 *
 *  • PAGE PLEINE (pas assez de trous) → CASCADE : les icônes recouvertes prennent les premières
 *    cellules disponibles (juste après le widget), ce qui DÉCALE les autres ; les icônes en trop
 *    (la queue de page) débordent sur la page suivante.
 *    Ex : widget {0,1,2,3}→{0..7} page pleine → ex-4,5,6,7 vont en 8,9,10,11 ; les ex-8.. se
 *    décalent ; les dernières débordent sur la page suivante.
 *
 * Invariant : aucune icône n'est jamais placée dans une cellule du widget ([newBlocked]).
 * Si le widget rétrécit (aucune cellule nouvellement bloquée), rien ne bouge.
 */
fun displaceNewlyBlockedOnly(
    state: LauncherState,
    appPageIndex: Int,
    newBlocked: Set<Int>,
    oldBlocked: Set<Int>
): LauncherState {
    val newlyBlocked = (newBlocked - oldBlocked).sorted()
    if (newlyBlocked.isEmpty()) return state

    val pages = state.pages.map { it.toMutableMap() }.toMutableList()
    while (pages.size <= appPageIndex) pages.add(mutableMapOf())
    val page = pages[appPageIndex]

    // Icônes directement recouvertes par l'agrandissement (ordre de lecture).
    val displaced = newlyBlocked.mapNotNull { slot -> page.remove(slot) }
    if (displaced.isEmpty()) return state.copy(pages = pages.map { it.toMap() })

    // Cellules utilisables par les icônes = toutes sauf celles du widget.
    val available = (0 until SLOTS_PER_PAGE).filter { it !in newBlocked }
    val emptyCount = available.count { !page.containsKey(it) }

    // ── Régime CHIRURGICAL : il reste assez de trous pour caser les recouvertes ──────────
    if (displaced.size <= emptyCount) {
        for (item in displaced) {
            val freeSlot = available.first { !page.containsKey(it) }
            page[freeSlot] = item
        }
        return state.copy(pages = pages.map { it.toMap() })
    }

    // ── Régime CASCADE (page pleine) : recouvertes d'abord, puis le reste, dans les cellules
    //    disponibles ; la queue déborde sur la page suivante ──────────────────────────────
    val remaining = available.mapNotNull { page[it] }      // icônes restantes, en ordre de lecture
    val ordered = displaced + remaining                    // recouvertes EN TÊTE des disponibilités
    available.forEach { page.remove(it) }                  // vide les cellules non bloquées

    val fitting = ordered.take(available.size)
    val overflow = ordered.drop(available.size)            // = la queue de page
    fitting.forEachIndexed { i, item -> page[available[i]] = item }

    var current = state.copy(pages = pages.map { it.toMap() })
    for (item in overflow.reversed()) {                    // reversed → ordre préservé page suivante
        current = insertHomeItemAtSlot(current, item, appPageIndex + 1, 0)
    }
    return current
}

fun removeAppFromHome(state: LauncherState, packageName: String): LauncherState {
    var removedPageIndex = -1
    var removedSlot = -1
    for ((pageIdx, page) in state.pages.withIndex()) {
        val entry = page.entries.find {
            it.value is HomeItem.App && (it.value as HomeItem.App).app.packageName == packageName
        }
        if (entry != null) {
            removedPageIndex = pageIdx
            removedSlot = entry.key
            break
        }
    }

    val newPages = state.pages.map { page ->
        page.filterValues { !(it is HomeItem.App && it.app.packageName == packageName) }
    }
    val newDock = state.dock.filter { it.packageName != packageName }
    var newState = LauncherState(pages = newPages, dock = newDock, hidden = state.hidden + packageName)

    if (removedPageIndex >= 0 && removedSlot >= 0) {
        newState = compactGroupAfterRemoval(newState, removedPageIndex, removedSlot)
    }
    return newState
}

/**
 * Décale vers la gauche les icones consécutives qui suivent [removedSlot]
 * pour combler le trou laissé par la suppression.
 * S'arrête dès qu'un slot vide interrompt la séquence (gap = frontière de groupe).
 */
fun compactGroupAfterRemoval(
    state: LauncherState,
    appPageIndex: Int,
    removedSlot: Int
): LauncherState {
    if (appPageIndex !in state.pages.indices) return state
    val pages = state.pages.map { it.toMutableMap() }.toMutableList()
    val page = pages[appPageIndex]
    var current = removedSlot
    while (current + 1 < SLOTS_PER_PAGE && page.containsKey(current + 1)) {
        page[current] = page[current + 1]!!
        current++
    }
    if (current > removedSlot) page.remove(current)
    return state.copy(pages = pages.map { it.toMap() })
}

/**
 * Supprime un dossier du home (par son id stable).
 * - Retire l'entrée dossier de sa page.
 * - Ajoute tous les packages d'apps du dossier dans hidden (ils réapparaissent dans l'App Library).
 * - Compacte la page (comme pour removeAppFromHome) pour combler le trou.
 * - Les dossiers ne sont jamais dans le dock, donc pas de traitement dock.
 */
fun removeFolderFromHome(state: LauncherState, folder: HomeItem.Folder): LauncherState {
    var removedPageIndex = -1
    var removedSlot = -1
    for ((pageIdx, page) in state.pages.withIndex()) {
        val entry = page.entries.find { (it.value as? HomeItem.Folder)?.id == folder.id }
        if (entry != null) {
            removedPageIndex = pageIdx
            removedSlot = entry.key
            break
        }
    }

    val newPages = state.pages.map { page ->
        page.filterValues { (it as? HomeItem.Folder)?.id != folder.id }
    }

    val hiddenApps = folder.apps.map { it.packageName }.toSet()
    var newState = LauncherState(
        pages = newPages,
        dock = state.dock,
        hidden = state.hidden + hiddenApps
    )

    if (removedPageIndex >= 0 && removedSlot >= 0) {
        newState = compactGroupAfterRemoval(newState, removedPageIndex, removedSlot)
    }
    return newState
}
