package com.stanleycx.fruitos.data

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

data class BindWidgetInput(
    val appWidgetId: Int,
    val provider: ComponentName
)

data class ConfigureWidgetInput(
    val appWidgetId: Int,
    val configure: ComponentName
)

/** Lance la boîte de dialogue système "Autoriser l'ajout du widget". */
class BindWidgetContract : ActivityResultContract<BindWidgetInput, Boolean>() {
    override fun createIntent(context: Context, input: BindWidgetInput): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, input.appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, input.provider)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}

/** Lance l'activité de configuration du widget (si le provider en a une). */
class ConfigureWidgetContract : ActivityResultContract<ConfigureWidgetInput, Boolean>() {
    override fun createIntent(context: Context, input: ConfigureWidgetInput): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = input.configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, input.appWidgetId)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}
