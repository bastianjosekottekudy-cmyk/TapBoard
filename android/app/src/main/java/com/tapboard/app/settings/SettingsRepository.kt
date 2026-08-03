package com.tapboard.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("tapboard_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val sensitivity = floatPreferencesKey("sensitivity")
        val invertScroll = booleanPreferencesKey("invert_scroll")
        val darkTheme = booleanPreferencesKey("dark_theme")
        val haptics = booleanPreferencesKey("haptics")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val wifiPin = stringPreferencesKey("wifi_pin")
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboardingDone] ?: false }
    val sensitivity: Flow<Float> = context.dataStore.data.map { it[Keys.sensitivity] ?: 1.2f }
    val invertScroll: Flow<Boolean> = context.dataStore.data.map { it[Keys.invertScroll] ?: false }
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.darkTheme] ?: true }
    val haptics: Flow<Boolean> = context.dataStore.data.map { it[Keys.haptics] ?: true }
    val keepScreenOn: Flow<Boolean> = context.dataStore.data.map { it[Keys.keepScreenOn] ?: true }
    val wifiPin: Flow<String> = context.dataStore.data.map { it[Keys.wifiPin] ?: "" }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.onboardingDone] = done }
    }

    suspend fun setSensitivity(value: Float) {
        context.dataStore.edit { it[Keys.sensitivity] = value.coerceIn(0.3f, 3f) }
    }

    suspend fun setInvertScroll(value: Boolean) {
        context.dataStore.edit { it[Keys.invertScroll] = value }
    }

    suspend fun setDarkTheme(value: Boolean) {
        context.dataStore.edit { it[Keys.darkTheme] = value }
    }

    suspend fun setHaptics(value: Boolean) {
        context.dataStore.edit { it[Keys.haptics] = value }
    }

    suspend fun setKeepScreenOn(value: Boolean) {
        context.dataStore.edit { it[Keys.keepScreenOn] = value }
    }

    suspend fun setWifiPin(value: String) {
        context.dataStore.edit { it[Keys.wifiPin] = value.filter { ch -> ch.isDigit() }.take(6) }
    }
}
