package com.stanleycx.fruitos.data

/**
 * Élément persisté dans un slot : référence d'app ou dossier (packages seulement).
 */
sealed interface LayoutItem {
    data class App(val packageName: String) : LayoutItem
    data class Folder(
        val id: String,
        val name: String,
        val packageNames: List<String>
    ) : LayoutItem
}

/**
 * Organisation choisie par l'utilisateur (version persistée).
 * Chaque page est une Map<slotIndex, LayoutItem> : placement libre, trous autorisés.
 */
data class LauncherLayout(
    val pages: List<Map<Int, LayoutItem>>,
    val dock: List<String>,
    val hidden: Set<String> = emptySet()
) {
    companion object {
        val Empty = LauncherLayout(
            pages = listOf(emptyMap()),
            dock = emptyList(),
            hidden = emptySet()
        )

        const val APPS_PER_PAGE = 24
        const val DOCK_MAX_SIZE = 4
        const val GRID_COLUMNS = 4
        const val GRID_ROWS = 6
    }

    /** Tous les packageNames du layout (pages + contenu des dossiers + dock + hidden). */
    fun allPackages(): Set<String> {
        val pagePkgs = pages.flatMap { page ->
            page.values.flatMap { item ->
                when (item) {
                    is LayoutItem.App -> listOf(item.packageName)
                    is LayoutItem.Folder -> item.packageNames
                }
            }
        }
        return pagePkgs.toSet() + dock.toSet() + hidden
    }
}

// ── Conversions LayoutItem ⇄ HomeItem ───────────────────────────────────────

/**
 * Résout un LayoutItem en HomeItem à partir des apps installées.
 * - App non installée → null (retirée)
 * - Dossier : on garde les apps installées ; 0 app → null, 1 app → app simple (dissolution Fruit OS)
 */
fun LayoutItem.toHomeItem(appsByPackage: Map<String, AppInfo>): HomeItem? = when (this) {
    is LayoutItem.App -> appsByPackage[packageName]?.let { HomeItem.App(it) }
    is LayoutItem.Folder -> {
        val apps = packageNames.mapNotNull { appsByPackage[it] }
        when {
            apps.isEmpty() -> null
            apps.size == 1 -> HomeItem.App(apps.first())
            else -> HomeItem.Folder(id, name, apps)
        }
    }
}

fun HomeItem.toLayoutItem(): LayoutItem = when (this) {
    is HomeItem.App -> LayoutItem.App(app.packageName)
    is HomeItem.Folder -> LayoutItem.Folder(id, name, apps.map { it.packageName })
}

sealed class LauncherSlot {
    data class Page(val pageIndex: Int, val position: Int) : LauncherSlot()
    data class Dock(val position: Int) : LauncherSlot()
}