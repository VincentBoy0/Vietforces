package com.example.vietforces.data.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.vietforces.data.storage.PreferencesManager

/**
 * Manager for user profile information.
 * Persists data to SharedPreferences.
 */
object ProfileManager {

    var name by mutableStateOf("")
        private set

    var phone by mutableStateOf("")
        private set

    var address by mutableStateOf("")
        private set

    private var isInitialized = false

    /**
     * Load profile from SharedPreferences
     * Call this after PreferencesManager.init()
     */
    fun loadFromPreferences() {
        if (isInitialized) return
        try {
            name = PreferencesManager.getProfileName()
            phone = PreferencesManager.getProfilePhone()
            address = PreferencesManager.getProfileAddress()
            isInitialized = true
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }

    /**
     * Update user name
     */
    fun updateName(value: String) {
        name = value
        try {
            PreferencesManager.saveProfileName(value)
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }

    /**
     * Update user phone
     */
    fun updatePhone(value: String) {
        phone = value
        try {
            PreferencesManager.saveProfilePhone(value)
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }

    /**
     * Update user address
     */
    fun updateAddress(value: String) {
        address = value
        try {
            PreferencesManager.saveProfileAddress(value)
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }

    /**
     * Clear all profile data
     */
    fun clearProfile() {
        name = ""
        phone = ""
        address = ""
        try {
            PreferencesManager.saveProfileName("")
            PreferencesManager.saveProfilePhone("")
            PreferencesManager.saveProfileAddress("")
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }
}

