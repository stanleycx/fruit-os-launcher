package com.stanleycx.fruitos.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

/**
 * Helper pour accéder aux statistiques d'usage des apps.
 *
 * Nécessite la permission PACKAGE_USAGE_STATS, qui se vérifie via AppOpsManager
 * et se demande en ouvrant la page "Accès aux données d'utilisation" des paramètres.
 */
object UsageStatsHelper {

    /**
     * Vérifie si la permission d'accès aux stats d'usage est accordée.
     * unsafeCheckOpNoThrow est déprécié mais reste la voie standard pour interroger
     * AppOpsManager sans lever d'exception ; pas de remplacement multi-version équivalent.
     */
    @Suppress("DEPRECATION")
    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Retourne les N packages les plus utilisés sur les 7 derniers jours,
     * triés du plus utilisé au moins utilisé.
     *
     * @param limit Nombre maximum de packages à retourner
     * @return Liste de packageNames, ou liste vide si pas de permission/données
     */
    fun getMostUsedPackages(context: Context, limit: Int = 8): List<String> {
        if (!hasPermission(context)) return emptyList()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val now = System.currentTimeMillis()
        val weekAgo = now - 1000L * 60 * 60 * 24 * 7  // 7 jours en millisecondes

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            weekAgo,
            now
        ) ?: return emptyList()

        // On agrège le temps total au premier plan par package
        // (queryUsageStats peut retourner plusieurs entrées pour un même package)
        val totalTimeByPackage = mutableMapOf<String, Long>()
        for (usageStat in stats) {
            val current = totalTimeByPackage[usageStat.packageName] ?: 0L
            totalTimeByPackage[usageStat.packageName] = current + usageStat.totalTimeInForeground
        }

        // On trie par temps décroissant, on filtre les apps sans usage, et on limite
        return totalTimeByPackage.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { it.key }
            .take(limit)
    }
}