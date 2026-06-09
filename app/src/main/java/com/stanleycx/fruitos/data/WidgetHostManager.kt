package com.stanleycx.fruitos.data

import android.appwidget.AppWidgetHost
import android.content.Context

object WidgetHostManager {
    const val HOST_ID = 1337
    private var host: AppWidgetHost? = null

    fun get(context: Context): AppWidgetHost =
        host ?: AppWidgetHost(context.applicationContext, HOST_ID).also { host = it }

    fun startListening(context: Context) = get(context).startListening()
    fun stopListening(context: Context) = get(context).stopListening()
    fun allocateId(context: Context): Int = get(context).allocateAppWidgetId()
    fun deleteId(context: Context, appWidgetId: Int) = get(context).deleteAppWidgetId(appWidgetId)
}
