package com.antigravity.virtualgamepad.repository

import android.content.Context
import android.content.SharedPreferences
import com.antigravity.virtualgamepad.model.ElementConfig
import com.antigravity.virtualgamepad.model.InputMode
import com.antigravity.virtualgamepad.model.LayoutSettings
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<LayoutSettings> = _settingsFlow.asStateFlow()

    private val _selectedPackagesFlow = MutableStateFlow(loadSelectedPackages())
    val selectedPackagesFlow: StateFlow<Set<String>> = _selectedPackagesFlow.asStateFlow()

    private val _isArmedFlow = MutableStateFlow(loadServiceArmed())
    val isArmedFlow: StateFlow<Boolean> = _isArmedFlow.asStateFlow()

    fun getSettings(): LayoutSettings = _settingsFlow.value

    fun getSelectedPackages(): Set<String> = _selectedPackagesFlow.value

    fun isServiceArmed(): Boolean = _isArmedFlow.value

    fun setServiceArmed(armed: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ARMED, armed).apply()
        _isArmedFlow.value = armed
    }

    private fun loadServiceArmed(): Boolean {
        return prefs.getBoolean(KEY_SERVICE_ARMED, false)
    }

    fun updateSettings(transform: (LayoutSettings) -> LayoutSettings) {
        val newSettings = transform(_settingsFlow.value)
        saveSettings(newSettings)
        _settingsFlow.value = newSettings
    }

    fun setInputMode(mode: InputMode) {
        updateSettings { it.copy(inputMode = mode) }
    }

    fun updateElement(elementId: String, transform: (ElementConfig) -> ElementConfig) {
        updateSettings { current ->
            val updatedList = current.elements.map { element ->
                if (element.id == elementId) transform(element) else element
            }
            current.copy(elements = updatedList)
        }
    }

    fun isGameSelected(packageName: String): Boolean {
        val key = packageName.trim().lowercase()
        return _selectedPackagesFlow.value.contains(key) ||
                key.contains("kotor") ||
                key.contains("swkotor")
    }

    fun setGameSelected(packageName: String, selected: Boolean) {
        val current = _selectedPackagesFlow.value.toMutableSet()
        val key = packageName.trim().lowercase()
        if (selected) {
            current.add(key)
        } else {
            current.remove(key)
        }
        prefs.edit().putStringSet(KEY_SELECTED_PACKAGES, current).apply()
        _selectedPackagesFlow.value = current
    }

    private fun loadSettings(): LayoutSettings {
        val json = prefs.getString(KEY_LAYOUT_SETTINGS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, LayoutSettings::class.java) ?: LayoutSettings.createDefault()
            } catch (e: Exception) {
                LayoutSettings.createDefault()
            }
        } else {
            LayoutSettings.createDefault()
        }
    }

    private fun saveSettings(settings: LayoutSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_LAYOUT_SETTINGS, json).apply()
    }

    private fun loadSelectedPackages(): Set<String> {
        val set = prefs.getStringSet(KEY_SELECTED_PACKAGES, null)
        return if (!set.isNullOrEmpty()) {
            set
        } else {
            setOf("com.aspyr.swkotor", "com.aspyr.swkotorii")
        }
    }

    companion object {
        private const val PREFS_NAME = "virtual_gamepad_prefs"
        private const val KEY_LAYOUT_SETTINGS = "key_layout_settings"
        private const val KEY_SELECTED_PACKAGES = "key_selected_packages_set"
        private const val KEY_SERVICE_ARMED = "key_service_armed_state"

        @Volatile
        private var instance: PreferencesRepository? = null

        fun getInstance(context: Context): PreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: PreferencesRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
