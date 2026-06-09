package com.stanleycx.fruitos.data


import java.util.UUID

/**
 * État complet du launcher prêt à afficher.
 * Chaque page est une Map<slotIndex, HomeItem> : app ou dossier, trous autorisés.
 */
data class LauncherState(
    val pages: List<Map<Int, HomeItem>>,
    val dock: List<AppInfo>,
    val hidden: Set<String> = emptySet()
)

/**
 * Fusionne les apps installées avec le layout sauvegardé.
 */
fun buildLauncherState(
    installedApps: List<AppInfo>,
    layout: LauncherLayout
): LauncherState {
    val appsByPackage = installedApps.associateBy { it.packageName }

    val dock = layout.dock.mapNotNull { appsByPackage[it] }

    // Chaque page : on résout les LayoutItem en HomeItem (apps non installées retirées)
    val laidOutPages = layout.pages.map { page ->
        page.mapNotNull { (slot, item) ->
            item.toHomeItem(appsByPackage)?.let { slot to it }
        }.toMap()
    }

    // Apps fraîchement installées : pas encore placées (ni en dossier) ET pas cachées
    val placedPackages = layout.allPackages()
    val newApps = installedApps.filter { it.packageName !in placedPackages }

    val finalPages = distributeNewApps(laidOutPages, newApps)

    return LauncherState(pages = finalPages, dock = dock, hidden = layout.hidden)
}

/**
 * Place les nouvelles apps dans les premiers slots libres, page par page.
 */
private fun distributeNewApps(
    existingPages: List<Map<Int, HomeItem>>,
    newApps: List<AppInfo>
): List<Map<Int, HomeItem>> {
    if (newApps.isEmpty()) {
        return existingPages.ifEmpty { listOf(emptyMap()) }
    }

    val result = existingPages.map { it.toMutableMap() }.toMutableList()
    if (result.isEmpty()) result.add(mutableMapOf())

    val maxPerPage = LauncherLayout.APPS_PER_PAGE
    val queue = newApps.toMutableList()

    for (page in result) {
        var slot = 0
        while (slot < maxPerPage && queue.isNotEmpty()) {
            if (!page.containsKey(slot)) {
                page[slot] = HomeItem.App(queue.removeAt(0))
            }
            slot++
        }
    }

    while (queue.isNotEmpty()) {
        val newPage = mutableMapOf<Int, HomeItem>()
        var slot = 0
        while (slot < maxPerPage && queue.isNotEmpty()) {
            newPage[slot] = HomeItem.App(queue.removeAt(0))
            slot++
        }
        result.add(newPage)
    }

    return result
}

/**
 * Convertit un LauncherState en LauncherLayout pour la sauvegarde.
 */
fun LauncherState.toLayout(): LauncherLayout {
    return LauncherLayout(
        pages = pages.map { page -> page.mapValues { (_, item) -> item.toLayoutItem() } },
        dock = dock.map { it.packageName },
        hidden = hidden
    )
}


/** Identifiant unique pour un nouveau dossier. */
fun newFolderId(): String = "folder_" + UUID.randomUUID().toString()

/**
 * Nom suggéré pour un dossier façon Fruit OS : la catégorie la plus représentée
 * parmi ses apps (sinon "Dossier").
 */
fun suggestFolderName(apps: List<AppInfo>): String {
    if (apps.isEmpty()) return "Dossier"
    val topCategory = apps.groupingBy { it.category }.eachCount()
        .maxByOrNull { it.value }?.key ?: AppCategory.OTHER
    return if (topCategory == AppCategory.OTHER) "Dossier" else topCategory.label
}