package com.stanleycx.fruitos.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.sin

/**
 * CompositionLocal partagé par toutes les icônes.
 * Une seule InfiniteTransition tourne au niveau HomeScreen,
 * chaque icône en dérive son angle via une phase unique (seed).
 * Remplace 28 InfiniteTransition simultanées → gain majeur en mode édition.
 */
val LocalJiggleBase = compositionLocalOf { 0f }

/**
 * État global du mode édition (équivalent du "jiggle mode" d'Fruit OS).
 */
class EditModeState {
    var isEditing: Boolean by mutableStateOf(false)
        private set

    fun enter() { isEditing = true }
    fun exit() { isEditing = false }
}

@Composable
fun rememberEditModeState(): EditModeState = remember { EditModeState() }

/**
 * Fournit la valeur de rotation pour le "tremblement" Fruit OS.
 * Lit la valeur partagée depuis LocalJiggleBase (fournie par HomeScreen).
 * Court-circuite immédiatement si isEditing = false → zéro overhead hors édition.
 *
 * @param amplitudeDeg amplitude angulaire en degrés. 2° convient aux icônes (68 dp) ;
 *   un widget large doit recevoir une amplitude bien plus faible (≈0.3–1°) sinon le
 *   déplacement de ses coins (proportionnel à sa taille) paraît énorme.
 */
@Composable
fun useJiggleAngle(isEditing: Boolean, seed: Int = 0, amplitudeDeg: Float = 2f): Float {
    if (!isEditing) return 0f
    val base = LocalJiggleBase.current
    val direction = if (seed % 2 == 0) 1f else -1f
    val phase = (seed and 0x7FFFFFFF) % 100 / 100f
    return sin((base + phase) * 2f * PI.toFloat()) * amplitudeDeg * direction
}
