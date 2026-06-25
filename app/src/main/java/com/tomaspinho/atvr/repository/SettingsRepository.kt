package com.tomaspinho.atvr.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.tomaspinho.atvr.ui.theme.ThemeType

private val Context.settingsStore by preferencesDataStore(name = "atv_settings")

/**
 * Local UI preferences: theme, display toggles, volume capture, onboarding
 * completion. No pyatv interaction. Used by ViewModels and the activity to
 * drive Compose state.
 */
class SettingsRepository(private val context: Context) {

    val themeFlow: Flow<ThemeType> =
        context.settingsStore.data.map { it[THEME].toThemeType() }

    val showMediaPlayerFlow: Flow<Boolean> =
        context.settingsStore.data.map { it[SHOW_MEDIA_PLAYER] ?: true }

    val mediaNotificationEnabledFlow: Flow<Boolean> =
        context.settingsStore.data.map { it[MEDIA_NOTIFICATION] ?: false }

    val touchpadAtBottomFlow: Flow<Boolean> =
        context.settingsStore.data.map { it[TOUCHPAD_AT_BOTTOM] ?: false }

    val volumeCaptureEnabledFlow: Flow<Boolean> =
        context.settingsStore.data.map { it[VOLUME_CAPTURE] ?: false }

    val showUnknownDevicesFlow: Flow<Boolean> =
        context.settingsStore.data.map { it[SHOW_UNKNOWN] ?: false }

    val onboardingCompleteFlow: Flow<Boolean> =
        context.settingsStore.data.map { it[ONBOARDING_COMPLETE] ?: false }

    suspend fun setTheme(type: ThemeType) {
        context.settingsStore.edit { it[THEME] = type.name }
    }

    suspend fun setShowMediaPlayer(value: Boolean) {
        context.settingsStore.edit { it[SHOW_MEDIA_PLAYER] = value }
    }

    suspend fun setMediaNotificationEnabled(value: Boolean) {
        context.settingsStore.edit { it[MEDIA_NOTIFICATION] = value }
    }

    suspend fun setTouchpadAtBottom(value: Boolean) {
        context.settingsStore.edit { it[TOUCHPAD_AT_BOTTOM] = value }
    }

    suspend fun setVolumeCaptureEnabled(value: Boolean) {
        context.settingsStore.edit { it[VOLUME_CAPTURE] = value }
    }

    suspend fun setShowUnknownDevices(value: Boolean) {
        context.settingsStore.edit { it[SHOW_UNKNOWN] = value }
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.settingsStore.edit { it[ONBOARDING_COMPLETE] = value }
    }

    private fun String?.toThemeType(): ThemeType =
        this?.let { runCatching { ThemeType.valueOf(it) }.getOrNull() } ?: ThemeType.SYSTEM

    companion object {
        private val THEME = stringPreferencesKey("theme")
        private val SHOW_MEDIA_PLAYER = booleanPreferencesKey("show_media_player")
        private val MEDIA_NOTIFICATION = booleanPreferencesKey("media_notification")
        private val TOUCHPAD_AT_BOTTOM = booleanPreferencesKey("touchpad_at_bottom")
        private val VOLUME_CAPTURE = booleanPreferencesKey("volume_capture")
        private val SHOW_UNKNOWN = booleanPreferencesKey("show_unknown_devices")
        private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }
}