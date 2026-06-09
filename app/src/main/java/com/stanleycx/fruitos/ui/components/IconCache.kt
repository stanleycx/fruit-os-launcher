package com.stanleycx.fruitos.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Cache global des icônes rendues au format Fruit OS.
 *
 * Une icône est rendue une seule fois (création du bitmap + calcul de la couleur de fond)
 * puis stockée ici. Tous les AppIcon partagent ce cache.
 *
 * PERF CRITIQUE: plus de stockage d'icônes dans AppInfo ni de load à froid de toutes les apps.
 * Le raw icon (getApplicationIcon) + render est fait LAZY uniquement pour les icônes
 * qui deviennent visibles à l'écran (premier AppIcon composé pour ce pkg).
 */
object IconCache {
    // ConcurrentHashMap : getOrRender peut être appelé depuis un thread IO (pré-chauffe)
    // en parallèle du thread main (composition). HashMap plain n'est pas thread-safe.
    private val cache = java.util.concurrent.ConcurrentHashMap<String, RenderedIconCached>()

    data class RenderedIconCached(
        val imageBitmap: ImageBitmap,
        val backgroundColor: androidx.compose.ui.graphics.Color
    )

    /**
     * Récupère l'icône rendue depuis le cache, ou la charge + calcule si pas encore fait.
     * Thread-safe : peut être appelé depuis n'importe quel thread (IO pour le pré-chauffe,
     * main pour la composition des icônes visibles).
     * computeIfAbsent est atomique : deux threads appelant pour le même package ne
     * déclenchent le render qu'une seule fois.
     */
    fun getOrRender(packageName: String, context: Context): RenderedIconCached {
        return cache.computeIfAbsent(packageName) {
            val drawable: Drawable = try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) {
                context.packageManager.defaultActivityIcon
            }
            val rendered = renderFruitIcon(drawable, sizePx = 192)
            RenderedIconCached(
                imageBitmap = rendered.bitmap.asImageBitmap(),
                backgroundColor = rendered.backgroundColor
            )
        }
    }

    /** Invalide une seule entrée (utile après mise à jour d'une app). */
    fun invalidate(packageName: String) {
        cache.remove(packageName)
    }

    fun clear() {
        cache.clear()
    }
}
