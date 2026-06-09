package com.stanleycx.fruitos.data

/**
 * Un élément posé dans un slot de la grille du home :
 * soit une app seule, soit un dossier (nommable) contenant plusieurs apps.
 */
sealed interface HomeItem {
    /** Clé stable (Compose / recherche). */
    val key: String

    data class App(val app: AppInfo) : HomeItem {
        override val key: String get() = app.packageName
    }

    data class Folder(
        val id: String,
        val name: String,
        val apps: List<AppInfo>
    ) : HomeItem {
        override val key: String get() = id
    }
}