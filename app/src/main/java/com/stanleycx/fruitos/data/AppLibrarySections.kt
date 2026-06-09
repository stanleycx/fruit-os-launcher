package com.stanleycx.fruitos.data

/**
 * Une section affichée dans l'App Library (= un "dossier" de catégorie,
 * ou une section spéciale comme Suggestions / Ajoutés récemment).
 *
 * @param id Identifiant stable (pour les clés Compose et l'ouverture de dossier)
 * @param title Titre affiché
 * @param apps Apps de la section, déjà triées pour l'affichage
 * @param isSpecial true pour Suggestions / Ajoutés récemment (rendu/comportement à part)
 */
data class AppLibrarySection(
    val id: String,
    val title: String,
    val apps: List<AppInfo>,
    val isSpecial: Boolean = false
)

/**
 * Construit les sections de l'App Library à partir de toutes les apps installées.
 *
 * Règles façon Fruit OS :
 * - "Suggestions" en premier (apps les plus utilisées), si dispo
 * - "Ajoutés récemment" ensuite (installées il y a moins de N jours)
 * - Puis un dossier par catégorie, dans l'ordre de l'enum AppCategory
 * - Une catégorie sans app n'apparaît pas
 * - À l'intérieur d'un dossier, tri alphabétique
 *
 * @param allApps toutes les apps installées
 * @param suggestedApps apps suggérées (issues de UsageStatsHelper), peut être vide
 * @param recentDays nombre de jours pour considérer une app comme "récemment ajoutée"
 */
fun buildAppLibrarySections(
    allApps: List<AppInfo>,
    suggestedApps: List<AppInfo>,
    recentDays: Int = 14
): List<AppLibrarySection> {
    val sections = mutableListOf<AppLibrarySection>()

    // --- Section spéciale : Suggestions ---
    if (suggestedApps.isNotEmpty()) {
        sections.add(
            AppLibrarySection(
                id = "special_suggestions",
                title = "Suggestions",
                apps = suggestedApps,
                isSpecial = true
            )
        )
    }

    // --- Section spéciale : Ajoutés récemment ---
    val now = System.currentTimeMillis()
    val recentThreshold = now - recentDays.toLong() * 24 * 60 * 60 * 1000
    val recentApps = allApps
        .filter { it.firstInstallTime > recentThreshold && it.firstInstallTime > 0 }
        .sortedByDescending { it.firstInstallTime }

    if (recentApps.isNotEmpty()) {
        sections.add(
            AppLibrarySection(
                id = "special_recent",
                title = "Ajoutés récemment",
                apps = recentApps,
                isSpecial = true
            )
        )
    }

    // --- Dossiers par catégorie ---
    val byCategory = allApps.groupBy { it.category }

    for (category in AppCategory.entries) {
        val appsInCategory = byCategory[category]
            ?.sortedBy { it.label.lowercase() }
            ?: continue

        if (appsInCategory.isEmpty()) continue

        sections.add(
            AppLibrarySection(
                id = "cat_${category.name}",
                title = category.label,
                apps = appsInCategory,
                isSpecial = false
            )
        )
    }

    return sections
}

/**
 * Liste alphabétique à plat de toutes les apps (pour la vue recherche/liste A→Z).
 */
fun buildAlphabeticalList(allApps: List<AppInfo>): List<AppInfo> {
    return allApps.sortedBy { it.label.lowercase() }
}