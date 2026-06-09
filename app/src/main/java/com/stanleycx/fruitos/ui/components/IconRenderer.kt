package com.stanleycx.fruitos.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap

/**
 * Résultat de l'extraction d'une icône : un bitmap (toujours présent)
 * et une couleur de fond suggérée pour la mettre derrière.
 */
data class RenderedIcon(
    val bitmap: Bitmap,
    val backgroundColor: Color
)

/**
 * Transforme une icône Android brute en bitmap "Fruit OS-ready" :
 * - Si c'est une AdaptiveIcon : on prend le foreground et le background séparément
 * - Sinon : on prend l'icône telle quelle et on calcule une couleur de fond moyenne
 */
fun renderFruitIcon(drawable: Drawable, sizePx: Int = 192): RenderedIcon {
    return if (drawable is AdaptiveIconDrawable) {
        // Les icônes adaptatives ont déjà leur agrandissement (safe-zone) calé : on n'y touche pas.
        renderAdaptiveIcon(drawable, sizePx)
    } else {
        renderRegularIcon(drawable, sizePx)
    }
}

/**
 * Pour les AdaptiveIcons : on dessine seulement le foreground sur un fond
 * dérivé du background. Comme ça, l'icône Fruit OS aura le logo "qui flotte"
 * sur une couleur, comme sur Fruit OS.
 */
private fun renderAdaptiveIcon(adaptive: AdaptiveIconDrawable, sizePx: Int): RenderedIcon {
    // Le background de l'AdaptiveIcon nous donne la couleur dominante
    val bgDrawable = adaptive.background
    val bgBitmap = bgDrawable?.toBitmap(sizePx, sizePx)
    val bgColor = bgBitmap?.let { averageColor(it) } ?: Color.White

    // On dessine le foreground sur fond transparent
    val foreground = adaptive.foreground
    val fgBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(fgBitmap)

    // L'AdaptiveIcon a un "safe zone" : le foreground doit être agrandi
    // pour utiliser tout l'espace visuel (sinon il paraît trop petit)
    val padding = (sizePx * -0.18f).toInt()  // négatif = on agrandit
    foreground?.setBounds(padding, padding, sizePx - padding, sizePx - padding)
    foreground?.draw(canvas)

    return RenderedIcon(bitmap = fgBitmap, backgroundColor = bgColor)
}

/**
 * Pour les icônes classiques : on garde l'icône telle quelle
 * et on devine une couleur de fond depuis ses pixels.
 */
private fun renderRegularIcon(drawable: Drawable, sizePx: Int): RenderedIcon {
    val bitmap = drawable.toBitmap(sizePx, sizePx)
    val avgColor = averageColor(bitmap)
    return RenderedIcon(bitmap = bitmap, backgroundColor = avgColor)
}

/**
 * Calcule la couleur moyenne d'un bitmap (utile pour deviner un fond).
 * Pour éviter d'analyser tous les pixels (lent), on échantillonne 1 pixel sur 8.
 */
private fun averageColor(bitmap: Bitmap): Color {
    var r = 0L; var g = 0L; var b = 0L; var count = 0L
    val step = 8

    for (x in 0 until bitmap.width step step) {
        for (y in 0 until bitmap.height step step) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = (pixel shr 24) and 0xFF
            if (alpha < 50) continue  // ignore les pixels transparents

            r += (pixel shr 16) and 0xFF
            g += (pixel shr 8) and 0xFF
            b += pixel and 0xFF
            count++
        }
    }

    if (count == 0L) return Color.White
    return Color(
        red = (r / count).toInt() / 255f,
        green = (g / count).toInt() / 255f,
        blue = (b / count).toInt() / 255f
    )
}