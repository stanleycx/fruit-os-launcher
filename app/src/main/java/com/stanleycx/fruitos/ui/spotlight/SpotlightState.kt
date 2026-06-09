package com.stanleycx.fruitos.ui.spotlight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * État du Spotlight : visible/caché + texte recherché.
 */
class SpotlightState {
    var isOpen: Boolean by mutableStateOf(false)
        private set

    var query: String by mutableStateOf("")

    fun open() {
        isOpen = true
        query = ""
    }

    fun close() {
        isOpen = false
        query = ""
    }
}

@Composable
fun rememberSpotlightState(): SpotlightState {
    return remember { SpotlightState() }
}