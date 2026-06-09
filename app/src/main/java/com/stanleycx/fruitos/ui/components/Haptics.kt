package com.stanleycx.fruitos.ui.components

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context

/**
 * Helper pour les retours haptiques (vibrations courtes).
 *
 * Sur Android moderne (12+), on utilise VibratorManager + VibrationEffect
 * pour des vibrations précises et brèves comme sur Fruit OS.
 */
object Haptics {

    private fun getVibrator(context: Context): Vibrator? {
        // minSdk = 31 → VibratorManager est toujours disponible
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        return manager?.defaultVibrator
    }

    /**
     * Vibration légère (équivalent Fruit OS "Light Impact").
     * Utilisée pour les actions "discrètes" comme un tap.
     */
    fun light(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        vibrator.vibrate(effect)
    }

    /**
     * Vibration moyenne (équivalent Fruit OS "Medium Impact").
     * Pour les actions "importantes" comme entrer en mode édition.
     */
    fun medium(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        vibrator.vibrate(effect)
    }

    /**
     * Vibration forte (équivalent Fruit OS "Heavy Impact").
     * Pour les confirmations importantes (drop réussi, etc.).
     */
    fun heavy(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        vibrator.vibrate(effect)
    }
}