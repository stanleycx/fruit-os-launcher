package com.stanleycx.fruitos.data

import android.content.pm.ApplicationInfo

/**
 * Catégories d'apps façon App Library Fruit OS.
 *
 * L'ordre de déclaration définit l'ordre d'affichage des dossiers dans l'App Library.
 * Les catégories spéciales (SUGGESTIONS, RECENTLY_ADDED) ne sont PAS dans cet enum :
 * elles sont calculées séparément car elles ne dépendent pas de la nature de l'app.
 *
 * @param label Le nom affiché du dossier (ex: "Réseaux sociaux")
 */
enum class AppCategory(val label: String) {
    SOCIAL("Réseaux sociaux"),
    ENTERTAINMENT("Divertissement"),
    CREATIVITY("Créativité"),
    PRODUCTIVITY("Productivité & finance"),
    INFO_READING("Infos & lecture"),
    GAMES("Jeux"),
    TRAVEL("Voyages"),
    SHOPPING("Shopping & alimentation"),
    HEALTH("Santé & forme"),
    EDUCATION("Éducation"),
    UTILITIES("Utilitaires"),
    OTHER("Autres");

    companion object {
        /**
         * Convertit la catégorie système Android (ApplicationInfo.category) vers
         * notre AppCategory. Renvoie null si la catégorie système est inconnue/absente,
         * ce qui déclenchera le fallback par mots-clés.
         */
        fun fromSystemCategory(systemCategory: Int): AppCategory? {
            return when (systemCategory) {
                ApplicationInfo.CATEGORY_GAME -> GAMES
                ApplicationInfo.CATEGORY_AUDIO -> ENTERTAINMENT
                ApplicationInfo.CATEGORY_VIDEO -> ENTERTAINMENT
                ApplicationInfo.CATEGORY_IMAGE -> CREATIVITY
                ApplicationInfo.CATEGORY_SOCIAL -> SOCIAL
                ApplicationInfo.CATEGORY_NEWS -> INFO_READING
                ApplicationInfo.CATEGORY_MAPS -> TRAVEL
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> PRODUCTIVITY
                // CATEGORY_ACCESSIBILITY existe depuis API 33
                else -> null
            }
        }
    }
}