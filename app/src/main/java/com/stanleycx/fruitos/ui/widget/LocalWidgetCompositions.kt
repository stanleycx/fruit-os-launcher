package com.stanleycx.fruitos.ui.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppWidgetHost = staticCompositionLocalOf<AppWidgetHost> {
    error("LocalAppWidgetHost not provided")
}

val LocalAppWidgetManager = staticCompositionLocalOf<AppWidgetManager> {
    error("LocalAppWidgetManager not provided")
}
