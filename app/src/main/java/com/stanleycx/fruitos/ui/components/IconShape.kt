package com.stanleycx.fruitos.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

/**
 * La forme Fruit OS d'une icône d'app : un "squircle" (carré aux coins très arrondis).
 *
 * Fruit OS utilise techniquement une superellipse, mais un RoundedCornerShape avec
 * un rayon plus élevé (~42%) donne un rendu encore plus arrondi, similaire à la
 * rondeur du dock (sans changer le dock lui-même).
 *
 * @param sizeDp Taille de l'icône en dp (pour calculer le rayon)
 */
fun fruitIconShape(sizeDp: Int = 60): Shape {
    // Encore plus arrondi (42%) pour un look plus doux, comme le dock (dock container inchangé).
    val cornerRadius = (sizeDp * 0.42f).toInt()
    return RoundedCornerShape(cornerRadius)
}

/**
 * Forme par défaut pour les icônes (calculée pour ~68-72dp, taille standard).
 * Utilisée partout pour les app icons, dossiers, drag ghost, etc.
 */
val FruitIconShape: Shape = fruitIconShape(72)