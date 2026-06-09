package com.stanleycx.fruitos.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.documentfile.provider.DocumentFile
import com.stanleycx.fruitos.R

/**
 * Cadre Photo : photo unique OU diaporama (dossier/plusieurs photos) avec défilement automatique
 * piloté par AlarmManager (intervalle ≥ 1 min, limite OS pour les widgets).
 */
class PhotoFrameWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updatePhotoWidget(context, appWidgetManager, id)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefs = getWidgetPrefs(context)
        for (id in appWidgetIds) {
            cancelCycle(context, id)
            prefs.edit()
                .remove("photo_uri_$id").remove("photo_uris_$id")
                .remove("photo_folder_uri_$id").remove("photo_index_$id")
                .remove("photo_cycle_ms_$id").apply()
        }
    }

    private fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_photo_frame)
        val prefs = getWidgetPrefs(context)
        val uriList = resolveUris(context, appWidgetId)

        if (uriList.isNotEmpty()) {
            var index = prefs.getInt("photo_index_$appWidgetId", 0)
            if (index >= uriList.size) index = 0
            // setImageViewUri : l'HÔTE (notre launcher) charge l'URI lui-même via la permission
            // persistante de l'app → pas de bitmap à transférer (plus de limite mémoire RemoteViews
            // qui faisait disparaître les photos), pas de décodage côté provider.
            views.setImageViewUri(R.id.photo, Uri.parse(uriList[index]))
            views.setViewVisibility(R.id.empty_container, View.GONE)
            views.setOnClickPendingIntent(R.id.empty_container, null)
            // Prochain cycle.
            prefs.edit().putInt("photo_index_$appWidgetId", (index + 1) % uriList.size).apply()
        } else {
            showEmpty(context, views, appWidgetId)
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun showEmpty(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setImageViewResource(R.id.photo, android.R.color.transparent)
        views.setViewVisibility(R.id.empty_container, View.VISIBLE)
        // « Appuyez pour configurer » → ouvre l'écran de config de CE widget.
        val configIntent = Intent(context, PhotoFrameWidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.parse("photoframe://config/$appWidgetId")
        }
        val pi = PendingIntent.getActivity(
            context, appWidgetId, configIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.empty_container, pi)
    }

    companion object {
        private fun getWidgetPrefs(context: Context): SharedPreferences =
            context.getSharedPreferences("launcher_widgets", Context.MODE_PRIVATE)

        /** Liste des URIs (priorité au dossier, sinon liste, sinon photo unique legacy). */
        fun resolveUris(context: Context, appWidgetId: Int): List<String> {
            val prefs = getWidgetPrefs(context)
            prefs.getString("photo_folder_uri_$appWidgetId", null)?.let { folder ->
                try {
                    val tree = DocumentFile.fromTreeUri(context, Uri.parse(folder))
                    val images = tree?.listFiles()?.filter { it.isFile && it.type?.startsWith("image/") == true }
                    if (!images.isNullOrEmpty()) return images.map { it.uri.toString() }
                } catch (e: Exception) { /* fallthrough */ }
            }
            prefs.getString("photo_uris_$appWidgetId", null)?.let { joined ->
                val list = joined.split("|||").filter { it.isNotBlank() }
                if (list.isNotEmpty()) return list
            }
            return prefs.getString("photo_uri_$appWidgetId", null)?.let { listOf(it) } ?: emptyList()
        }

        fun savePhotoUris(context: Context, appWidgetId: Int, uriStrings: List<String>) {
            getWidgetPrefs(context).edit()
                .putString("photo_uris_$appWidgetId", uriStrings.joinToString("|||"))
                .putInt("photo_index_$appWidgetId", 0).apply()
        }

        fun savePhotoFolderUri(context: Context, appWidgetId: Int, folderUri: String) {
            getWidgetPrefs(context).edit()
                .putString("photo_folder_uri_$appWidgetId", folderUri)
                .putInt("photo_index_$appWidgetId", 0).apply()
        }

        fun clearPhotoFolderUri(context: Context, appWidgetId: Int) {
            getWidgetPrefs(context).edit().remove("photo_folder_uri_$appWidgetId").apply()
        }

        fun clearPhotoUris(context: Context, appWidgetId: Int) {
            getWidgetPrefs(context).edit().remove("photo_uris_$appWidgetId").remove("photo_uri_$appWidgetId").apply()
        }

        fun savePhotoCycleDuration(context: Context, appWidgetId: Int, durationMs: Long) {
            getWidgetPrefs(context).edit().putLong("photo_cycle_ms_$appWidgetId", durationMs).apply()
        }

        private fun cyclePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, PhotoFrameWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                data = Uri.parse("photoframe://cycle/$appWidgetId")
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            return PendingIntent.getBroadcast(
                context, appWidgetId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        /** (Re)programme ou annule le défilement selon l'intervalle + nb de photos. */
        fun applyCycle(context: Context, appWidgetId: Int) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = cyclePendingIntent(context, appWidgetId)
            am.cancel(pi)
            val ms = getWidgetPrefs(context).getLong("photo_cycle_ms_$appWidgetId", 0L)
            val count = resolveUris(context, appWidgetId).size
            // AlarmManager pour widgets : intervalle effectif min ~1 min.
            if (ms >= 60_000L && count > 1) {
                am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + ms, ms, pi)
            }
        }

        fun cancelCycle(context: Context, appWidgetId: Int) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(cyclePendingIntent(context, appWidgetId))
        }

        fun updateOne(context: Context, appWidgetId: Int) {
            val intent = Intent(context, PhotoFrameWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            context.sendBroadcast(intent)
        }

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, PhotoFrameWidgetProvider::class.java))
            val intent = Intent(context, PhotoFrameWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
