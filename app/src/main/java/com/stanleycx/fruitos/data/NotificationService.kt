package com.stanleycx.fruitos.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationService : NotificationListenerService() {

    companion object {
        private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()

        fun isEnabled(context: Context): Boolean {
            val cn = ComponentName(context, NotificationService::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(cn.flattenToString())
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshCounts()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshCounts()
    }

    private fun refreshCounts() {
        val active = try { activeNotifications } catch (_: Exception) { null } ?: return
        val counts = active
            .filter { sbn ->
                val flags = sbn.notification?.flags ?: 0
                (flags and Notification.FLAG_GROUP_SUMMARY) == 0
            }
            .groupBy { it.packageName }
            .mapValues { (_, list) -> list.size }
        _notificationCounts.value = counts
    }
}
