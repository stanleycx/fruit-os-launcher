package com.stanleycx.fruitos.ui.controlcenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * État du Control Center Fruit OS-like : visible / caché.
 * Mêmes conventions que SpotlightState pour rester cohérent avec les autres overlays.
 */
class ControlCenterState {
    var isOpen: Boolean by mutableStateOf(false)
        private set

    fun open() { isOpen = true }
    fun close() { isOpen = false }
    fun toggle() { isOpen = !isOpen }
}

@Composable
fun rememberControlCenterState(): ControlCenterState = remember { ControlCenterState() }
