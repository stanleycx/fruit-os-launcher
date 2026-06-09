package com.stanleycx.fruitos.data

/**
 * Représente une app installée sur le téléphone.
 * L'icône n'est PAS stockée ici (lourd + cher à charger) : chargement + rendu différé via IconCache
 * au moment du rendu de l'AppIcon / FillingAppIcon visible. Évite le blocage au cold start et rebuilds.
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val category: AppCategory = AppCategory.OTHER,
    val firstInstallTime: Long = 0L
)