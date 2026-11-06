package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "accessible_camera_settings")

data class AppPreferences(
    val voiceGuidanceEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val verbosity: String = "Normal",
    val faceGuidanceEnabled: Boolean = true,
    val sceneDescriptionIntervalSeconds: Int = 0, // 0 = off
    val hapticEnabled: Boolean = true,
    val audioCuesEnabled: Boolean = true,
    val themeMode: String = "SYSTEM",
    val horizonLevelEnabled: Boolean = true,
    val gridOverlay: String = "NONE",
    val locationTaggingEnabled: Boolean = false,
    val watermarkEnabled: Boolean = false,
    val watermarkText: String = "",
    val volumeKeyAction: String = "SHUTTER",
    val defaultAspectRatio: String = "4:3",
    val defaultVideoResolution: String = "1080p",
    val mirrorFrontCamera: Boolean = true,
    val hasCompletedWelcome: Boolean = false
)

class SettingsDataStore(private val context: Context) {
    companion object {
        private val KEY_VOICE_GUIDANCE = booleanPreferencesKey("voice_guidance")
        private val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        private val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        private val KEY_VERBOSITY = stringPreferencesKey("verbosity")
        private val KEY_FACE_GUIDANCE = booleanPreferencesKey("face_guidance")
        private val KEY_SCENE_DESC_INTERVAL = intPreferencesKey("scene_desc_interval")
        private val KEY_HAPTIC = booleanPreferencesKey("haptic")
        private val KEY_AUDIO_CUES = booleanPreferencesKey("audio_cues")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_HORIZON_LEVEL = booleanPreferencesKey("horizon_level")
        private val KEY_GRID_OVERLAY = stringPreferencesKey("grid_overlay")
        private val KEY_LOCATION_TAGGING = booleanPreferencesKey("location_tagging")
        private val KEY_WATERMARK = booleanPreferencesKey("watermark")
        private val KEY_WATERMARK_TEXT = stringPreferencesKey("watermark_text")
        private val KEY_VOLUME_ACTION = stringPreferencesKey("volume_action")
        private val KEY_DEFAULT_ASPECT = stringPreferencesKey("default_aspect")
        private val KEY_DEFAULT_VIDEO_RES = stringPreferencesKey("default_video_res")
        private val KEY_MIRROR_FRONT = booleanPreferencesKey("mirror_front")
        private val KEY_HAS_COMPLETED_WELCOME = booleanPreferencesKey("has_completed_welcome")
    }

    val preferencesFlow: Flow<AppPreferences> = context.settingsDataStore.data.map { prefs ->
        AppPreferences(
            voiceGuidanceEnabled = prefs[KEY_VOICE_GUIDANCE] ?: true,
            speechRate = prefs[KEY_SPEECH_RATE] ?: 1.0f,
            speechPitch = prefs[KEY_SPEECH_PITCH] ?: 1.0f,
            verbosity = prefs[KEY_VERBOSITY] ?: "Normal",
            faceGuidanceEnabled = prefs[KEY_FACE_GUIDANCE] ?: true,
            sceneDescriptionIntervalSeconds = prefs[KEY_SCENE_DESC_INTERVAL] ?: 0,
            hapticEnabled = prefs[KEY_HAPTIC] ?: true,
            audioCuesEnabled = prefs[KEY_AUDIO_CUES] ?: true,
            themeMode = prefs[KEY_THEME_MODE] ?: "SYSTEM",
            horizonLevelEnabled = prefs[KEY_HORIZON_LEVEL] ?: true,
            gridOverlay = prefs[KEY_GRID_OVERLAY] ?: "NONE",
            locationTaggingEnabled = prefs[KEY_LOCATION_TAGGING] ?: false,
            watermarkEnabled = prefs[KEY_WATERMARK] ?: false,
            watermarkText = prefs[KEY_WATERMARK_TEXT] ?: "",
            volumeKeyAction = prefs[KEY_VOLUME_ACTION] ?: "SHUTTER",
            defaultAspectRatio = prefs[KEY_DEFAULT_ASPECT] ?: "4:3",
            defaultVideoResolution = prefs[KEY_DEFAULT_VIDEO_RES] ?: "1080p",
            mirrorFrontCamera = prefs[KEY_MIRROR_FRONT] ?: true,
            hasCompletedWelcome = prefs[KEY_HAS_COMPLETED_WELCOME] ?: false
        )
    }

    suspend fun setVoiceGuidance(enabled: Boolean) = edit { it[KEY_VOICE_GUIDANCE] = enabled }
    suspend fun setSpeechRate(rate: Float) = edit { it[KEY_SPEECH_RATE] = rate }
    suspend fun setSpeechPitch(pitch: Float) = edit { it[KEY_SPEECH_PITCH] = pitch }
    suspend fun setVerbosity(verbosity: String) = edit { it[KEY_VERBOSITY] = verbosity }
    suspend fun setFaceGuidance(enabled: Boolean) = edit { it[KEY_FACE_GUIDANCE] = enabled }
    suspend fun setSceneDescriptionInterval(seconds: Int) = edit { it[KEY_SCENE_DESC_INTERVAL] = seconds }
    suspend fun setHaptic(enabled: Boolean) = edit { it[KEY_HAPTIC] = enabled }
    suspend fun setAudioCues(enabled: Boolean) = edit { it[KEY_AUDIO_CUES] = enabled }
    suspend fun setThemeMode(mode: String) = edit { it[KEY_THEME_MODE] = mode }
    suspend fun setHorizonLevel(enabled: Boolean) = edit { it[KEY_HORIZON_LEVEL] = enabled }
    suspend fun setGridOverlay(overlay: String) = edit { it[KEY_GRID_OVERLAY] = overlay }
    suspend fun setLocationTagging(enabled: Boolean) = edit { it[KEY_LOCATION_TAGGING] = enabled }
    suspend fun setWatermark(enabled: Boolean) = edit { it[KEY_WATERMARK] = enabled }
    suspend fun setWatermarkText(text: String) = edit { it[KEY_WATERMARK_TEXT] = text }
    suspend fun setVolumeKeyAction(action: String) = edit { it[KEY_VOLUME_ACTION] = action }
    suspend fun setDefaultAspectRatio(ratio: String) = edit { it[KEY_DEFAULT_ASPECT] = ratio }
    suspend fun setDefaultVideoResolution(res: String) = edit { it[KEY_DEFAULT_VIDEO_RES] = res }
    suspend fun setMirrorFrontCamera(mirror: Boolean) = edit { it[KEY_MIRROR_FRONT] = mirror }
    suspend fun setHasCompletedWelcome(completed: Boolean) = edit { it[KEY_HAS_COMPLETED_WELCOME] = completed }

    suspend fun resetDefaults() {
        context.settingsDataStore.edit { it.clear() }
    }

    private suspend fun edit(transform: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(transform)
    }
}
