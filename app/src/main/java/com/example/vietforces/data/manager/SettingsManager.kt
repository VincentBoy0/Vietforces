package com.example.vietforces.data.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import com.example.vietforces.data.storage.PreferencesManager

/**
 * Manager for app settings - mascot size, font size, etc.
 * Now persists settings to SharedPreferences.
 */
object SettingsManager {
    // Mascot size multiplier (0.5 = 50%, 1.0 = 100%, 1.5 = 150%, 2.0 = 200%)
    var mascotSizeMultiplier by mutableFloatStateOf(1.0f)
        private set

    // Mascot text size multiplier (0.5 = 50%, 1.0 = 100%, 1.5 = 150%, 2.0 = 200%)
    var mascotTextSizeMultiplier by mutableFloatStateOf(1.0f)
        private set

    // Base sizes
    const val BASE_MASCOT_SIZE = 70f  // dp
    const val BASE_MASCOT_TEXT_SIZE = 12f  // sp
    const val BASE_MASCOT_EMOJI_SIZE = 32f  // sp

    /**
     * Load settings from SharedPreferences
     * Call this after PreferencesManager.init()
     */
    fun loadFromPreferences() {
        try {
            mascotSizeMultiplier = PreferencesManager.getMascotSizeMultiplier()
            mascotTextSizeMultiplier = PreferencesManager.getMascotTextSizeMultiplier()
        } catch (e: Exception) {
            // PreferencesManager not initialized yet, use defaults
        }
    }

    /**
     * Set mascot size multiplier
     * @param multiplier Value between 0.5 and 2.0
     */
    fun setMascotSize(multiplier: Float) {
        mascotSizeMultiplier = multiplier.coerceIn(0.5f, 2.0f)
        try {
            PreferencesManager.saveMascotSizeMultiplier(mascotSizeMultiplier)
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }

    /**
     * Set mascot text size multiplier
     * @param multiplier Value between 0.5 and 2.0
     */
    fun setMascotTextSize(multiplier: Float) {
        mascotTextSizeMultiplier = multiplier.coerceIn(0.5f, 2.0f)
        try {
            PreferencesManager.saveMascotTextSizeMultiplier(mascotTextSizeMultiplier)
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }

    /**
     * Get current mascot size in dp
     */
    fun getMascotSize(): Float = BASE_MASCOT_SIZE * mascotSizeMultiplier

    /**
     * Get current mascot text size in sp
     */
    fun getMascotTextSize(): Float = BASE_MASCOT_TEXT_SIZE * mascotTextSizeMultiplier

    /**
     * Get current mascot emoji size in sp
     */
    fun getMascotEmojiSize(): Float = BASE_MASCOT_EMOJI_SIZE * mascotSizeMultiplier

    /**
     * Reset to default settings
     */
    fun resetToDefaults() {
        mascotSizeMultiplier = 1.0f
        mascotTextSizeMultiplier = 1.0f
        try {
            PreferencesManager.saveMascotSizeMultiplier(1.0f)
            PreferencesManager.saveMascotTextSizeMultiplier(1.0f)
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }
}

