package com.stanleycx.fruitos.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * État de l'App Library : texte de recherche + dossier de catégorie ouvert.
 *
 * - query vide  → vue par dossiers de catégories
 * - query rempli → liste alphabétique filtrée (la recherche masque les dossiers)
 * - openedSectionId non-null → un dossier est ouvert en plein écran (vue "toutes les apps de la catégorie")
 */
class AppLibraryState {
    var query: String by mutableStateOf("")

    var openedSectionId: String? by mutableStateOf(null)
        private set

    val isSearching: Boolean
        get() = query.isNotBlank()

    fun openSection(id: String) {
        openedSectionId = id
    }

    fun closeSection() {
        openedSectionId = null
    }

    /** Réinitialise tout (appelé quand on quitte la page App Library). */
    fun reset() {
        query = ""
        openedSectionId = null
    }
}

@Composable
fun rememberAppLibraryState(): AppLibraryState {
    return remember { AppLibraryState() }
}