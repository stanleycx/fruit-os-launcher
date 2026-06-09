package com.stanleycx.fruitos.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.ui.components.IconCache
import com.stanleycx.fruitos.ui.components.iconStyleBackground
import com.stanleycx.fruitos.ui.components.iconStyleGlyphFilter
import com.stanleycx.fruitos.ui.components.iconStyleLogoScale
import com.stanleycx.fruitos.ui.components.fruitIconShape

/**
 * Icône d'app "nue" qui remplit tout son conteneur (pas de taille fixe, pas de label).
 *
 * Utilisée dans les dossiers de l'App Library, où la taille de l'icône dépend
 * de la cellule de la grille (variable selon la largeur d'écran).
 *
 * Réutilise IconCache pour le rendu Fruit OS (bitmap + couleur de fond) déjà calculé.
 *
 * @param cornerSizeDp taille de référence pour calculer l'arrondi (squircle).
 *   Pour de petites icônes de dossier, ~28dp+ donne un arrondi proportionné (encore plus arrondi).
 */
@Composable
fun FillingAppIcon(
    app: AppInfo,
    cornerSizeDp: Int = 28,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cached = remember(app.packageName) {
        IconCache.getOrRender(app.packageName, context)
    }

    val zoom = iconStyleLogoScale(app.packageName)
    val glyphFilter = iconStyleGlyphFilter(app.packageName)
    Box(
        modifier = modifier
            .fillMaxSize()
            .iconStyleBackground(cached.backgroundColor, fruitIconShape(cornerSizeDp), app.packageName)
    ) {
        Image(
            bitmap = cached.imageBitmap,
            contentDescription = app.label,
            colorFilter = glyphFilter,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Offscreen seulement si filtre couleur appliqué (cf. StyledIconTile) — null en mode Défaut.
                    compositingStrategy =
                        if (glyphFilter != null) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                    if (zoom != 1f) { scaleX = zoom; scaleY = zoom }
                }
        )
    }
}