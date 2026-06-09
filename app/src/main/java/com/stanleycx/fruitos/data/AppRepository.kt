package com.stanleycx.fruitos.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * S'occupe de récupérer la liste des apps installées sur le téléphone.
 */
class AppRepository(private val context: Context) {

    /**
     * Renvoie toutes les apps "lançables" (celles qui apparaissent dans le tiroir).
     * On exclut notre propre launcher pour éviter de l'afficher dans sa propre liste.
     *
     * IMPORTANT PERF: plus de loadIcon() synchrone ici. Les icônes (lourdes) sont chargées
     * à la demande + rendues en cache uniquement pour les icônes VISIBLES (AppIcon).
     * loadLabel + catégorisation reste rapide (~10-30ms pour 150 apps).
     */
    fun loadInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(intent, 0)

        return resolveInfos
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(packageManager).toString()

                AppInfo(
                    label = label,
                    packageName = pkg,
                    category = AppCategorizer.categorize(
                        packageName = pkg,
                        label = label,
                        systemCategory = systemCategoryOf(packageManager, pkg)
                    ),
                    firstInstallTime = firstInstallTimeOf(packageManager, pkg)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Charge l'icône brute pour un package donné (utilisé uniquement par IconCache au premier rendu visible).
     * Rapide car le système met en cache les icônes ; on ne l'appelle que pour les apps rendues.
     */
    fun loadIcon(packageName: String): android.graphics.drawable.Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Récupère ApplicationInfo.category (ou CATEGORY_UNDEFINED si introuvable).
     */
    private fun systemCategoryOf(pm: PackageManager, packageName: String): Int {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appInfo.category
        } catch (e: PackageManager.NameNotFoundException) {
            ApplicationInfo.CATEGORY_UNDEFINED
        }
    }

    /**
     * Récupère la date de première installation (ms). 0 si introuvable.
     */
    private fun firstInstallTimeOf(pm: PackageManager, packageName: String): Long {
        return try {
            pm.getPackageInfo(packageName, 0).firstInstallTime
        } catch (e: PackageManager.NameNotFoundException) {
            0L
        }
    }

    /**
     * Lance une app par son packageName.
     */
    fun launchApp(packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            // FLAG_ACTIVITY_NEW_TASK est requis quand on lance depuis un launcher
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }
}
