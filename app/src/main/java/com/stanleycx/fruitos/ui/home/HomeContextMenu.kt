package com.stanleycx.fruitos.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RemoveCircle
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.HomeItem
import com.stanleycx.fruitos.ui.components.ContextMenuAction

/**
 * Construit la liste d'actions du menu contextuel (long press) pour un élément
 * de l'écran d'accueil — app ou dossier. Fonction pure, sans état.
 */
internal fun buildContextMenuActions(
    item: HomeItem,
    context: android.content.Context,
    onEditHome: () -> Unit,
    onRemove: () -> Unit,
    onRenameFolder: (HomeItem.Folder) -> Unit = {},
    onUninstall: (AppInfo) -> Unit = {},
    onCustomizeIcon: (AppInfo) -> Unit = {},
    closeCurrentFolder: () -> Unit = {}
): List<ContextMenuAction> {
    val actions = mutableListOf<ContextMenuAction>()

    actions.add(
        ContextMenuAction(
            title = "Éditer l'écran d'accueil",
            icon = Icons.Default.Edit,
            onClick = onEditHome
        )
    )

    when (item) {
        is HomeItem.App -> {
            actions.add(
                ContextMenuAction(
                    title = "Personnaliser l'icône",
                    icon = Icons.Default.Palette,
                    onClick = { onCustomizeIcon(item.app) }
                )
            )
            actions.add(
                ContextMenuAction(
                    title = "Infos sur l'application",
                    icon = Icons.Default.Info,
                    onClick = {
                        closeCurrentFolder()  // Ferme le dossier avant d'ouvrir les infos système
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${item.app.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
            )
            actions.add(
                ContextMenuAction(
                    title = "Supprimer l'app",
                    icon = Icons.Default.RemoveCircle,
                    isDestructive = true,
                    onClick = onRemove
                )
            )
            actions.add(
                ContextMenuAction(
                    title = "Désinstaller",
                    icon = Icons.Default.Delete,
                    isDestructive = true,
                    onClick = { onUninstall(item.app) }
                )
            )
        }
        is HomeItem.Folder -> {
            actions.add(
                ContextMenuAction(
                    title = "Renommer le dossier",
                    icon = Icons.Default.Edit,
                    onClick = { onRenameFolder(item) }
                )
            )
            actions.add(
                ContextMenuAction(
                    title = "Supprimer le dossier",
                    icon = Icons.Default.RemoveCircle,
                    isDestructive = true,
                    onClick = onRemove
                )
            )
        }
    }

    return actions
}
