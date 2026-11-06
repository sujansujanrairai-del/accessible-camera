package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CameraCapabilities
import com.example.data.model.CameraCaptureState
import com.example.data.model.CameraFacing
import com.example.data.model.CameraSettingsState
import com.example.data.model.CaptureTimer
import com.example.data.model.FaceAlignmentState
import com.example.data.model.FlashMode
import com.example.data.model.MediaItem
import com.example.data.model.VoiceCommand
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.CameraRepository
import com.example.data.repository.MediaRepository
import com.example.domain.accessibility.FeedbackManager
import com.example.domain.accessibility.VoiceCommandManager
import com.example.domain.accessibility.VoiceGuidanceManager
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import java.util.Locale

enum class AppScreen {
    WELCOME,
    MAIN,
    MEDIA_VIEWER,
    MEDIA_EDITOR
}

enum class MainTab {
    PHOTO,
    VIDEO,
    GALLERY,
    SETTINGS
}

enum class GalleryFilter {
    ALL,
    PHOTOS,
    VIDEOS,
    FAVORITES,
    TRASH
}

enum class GallerySortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    NAME,
    SIZE
}

class AppViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    val settingsDataStore = SettingsDataStore(application)
    val cameraRepository = CameraRepository(application)
    val mediaRepository = MediaRepository(application)
    val voiceGuidanceManager = VoiceGuidanceManager(application)
    val feedbackManager = FeedbackManager(application)

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val preferences: StateFlow<AppPreferences> = settingsDataStore.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppPreferences()
        )

    private val _currentScreen = MutableStateFlow(AppScreen.WELCOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedTab = MutableStateFlow(MainTab.PHOTO)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _cameraState = MutableStateFlow<CameraCaptureState>(CameraCaptureState.Ready)
    val cameraState: StateFlow<CameraCaptureState> = _cameraState.asStateFlow()

    private val _cameraSettings = MutableStateFlow(CameraSettingsState())
    val cameraSettings: StateFlow<CameraSettingsState> = _cameraSettings.asStateFlow()

    val cameraCapabilities: StateFlow<CameraCapabilities> = cameraRepository.capabilities

    private val _faceAlignmentState = MutableStateFlow(FaceAlignmentState())
    val faceAlignmentState: StateFlow<FaceAlignmentState> = _faceAlignmentState.asStateFlow()

    private val _sceneDescription = MutableStateFlow("")
    val sceneDescription: StateFlow<String> = _sceneDescription.asStateFlow()

    private val _deviceTiltAngle = MutableStateFlow(0f)
    val deviceTiltAngle: StateFlow<Float> = _deviceTiltAngle.asStateFlow()

    private val _isDeviceLevel = MutableStateFlow(false)
    val isDeviceLevel: StateFlow<Boolean> = _isDeviceLevel.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0L)
    val recordingDurationSeconds: StateFlow<Long> = _recordingDurationSeconds.asStateFlow()

    // Gallery state
    val allMediaItems: StateFlow<List<MediaItem>> = mediaRepository.getMediaItemsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _galleryFilter = MutableStateFlow(GalleryFilter.ALL)
    val galleryFilter: StateFlow<GalleryFilter> = _galleryFilter.asStateFlow()

    private val _gallerySearchQuery = MutableStateFlow("")
    val gallerySearchQuery: StateFlow<String> = _gallerySearchQuery.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _selectedMediaUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedMediaUris: StateFlow<Set<String>> = _selectedMediaUris.asStateFlow()

    private val _activeMediaItem = MutableStateFlow<MediaItem?>(null)
    val activeMediaItem: StateFlow<MediaItem?> = _activeMediaItem.asStateFlow()

    private val _slideshowActive = MutableStateFlow(false)
    val slideshowActive: StateFlow<Boolean> = _slideshowActive.asStateFlow()

    private var countdownJob: Job? = null
    private var recordingTimerJob: Job? = null
    private var slideshowJob: Job? = null
    private var wasLevelAnnounced = false

    // Voice command manager
    val voiceCommandManager = VoiceCommandManager(
        context = application,
        onCommandReceived = { command, spokenText ->
            handleVoiceCommand(command, spokenText)
        },
        onError = { error ->
            voiceGuidanceManager.announce(error, VoiceGuidanceManager.Priority.CRITICAL)
        }
    )

    init {
        // Observe preferences to sync TTS config
        viewModelScope.launch {
            preferences.collect { prefs ->
                voiceGuidanceManager.updateConfig(
                    enabled = prefs.voiceGuidanceEnabled,
                    rate = prefs.speechRate,
                    pitch = prefs.speechPitch
                )
                if (prefs.hasCompletedWelcome && _currentScreen.value == AppScreen.WELCOME) {
                    _currentScreen.value = AppScreen.MAIN
                }
            }
        }

        // Register tilt sensor
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // Detect capabilities on startup
        cameraRepository.detectCapabilities(_cameraSettings.value.cameraFacing)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val angle = Math.toDegrees(atan2(x.toDouble(), y.toDouble())).toFloat()
        // Convert to deviation from 0 or 90
        val tilt = if (abs(angle) <= 45) {
            angle
        } else if (angle > 45 && angle <= 135) {
            angle - 90
        } else if (angle < -45 && angle >= -135) {
            angle + 90
        } else {
            angle - 180
        }

        _deviceTiltAngle.value = tilt
        val isLevel = abs(tilt) < 1.5f
        _isDeviceLevel.value = isLevel

        if (isLevel && !wasLevelAnnounced) {
            wasLevelAnnounced = true
            if (preferences.value.horizonLevelEnabled) {
                feedbackManager.playFocusLocked(
                    audioEnabled = preferences.value.audioCuesEnabled,
                    hapticEnabled = preferences.value.hapticEnabled
                )
            }
        } else if (!isLevel) {
            wasLevelAnnounced = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun completeWelcome() {
        viewModelScope.launch {
            settingsDataStore.setHasCompletedWelcome(true)
            _currentScreen.value = AppScreen.MAIN
            voiceGuidanceManager.announce(
                "Welcome to Accessible Camera and Gallery. Photo camera tab opened. Camera ready.",
                VoiceGuidanceManager.Priority.CRITICAL
            )
        }
    }

    fun selectTab(tab: MainTab) {
        if (_selectedTab.value == tab) return
        _selectedTab.value = tab
        val announcement = when (tab) {
            MainTab.PHOTO -> "Photo tab. Press Shutter button to take photo."
            MainTab.VIDEO -> "Video tab. Press Record button to start recording."
            MainTab.GALLERY -> "Gallery tab. Showing your media items."
            MainTab.SETTINGS -> "Settings tab. Configure voice guidance, themes, and preferences."
        }
        voiceGuidanceManager.announce(announcement, VoiceGuidanceManager.Priority.CRITICAL)
    }

    fun switchCamera() {
        val nextFacing = if (_cameraSettings.value.cameraFacing == CameraFacing.BACK) {
            CameraFacing.FRONT
        } else {
            CameraFacing.BACK
        }
        _cameraSettings.value = _cameraSettings.value.copy(cameraFacing = nextFacing)
        cameraRepository.detectCapabilities(nextFacing)
        val text = if (nextFacing == CameraFacing.FRONT) "Front camera selected" else "Back camera selected"
        voiceGuidanceManager.announce(text, VoiceGuidanceManager.Priority.CRITICAL)
        feedbackManager.performClickHaptic(hapticEnabled = preferences.value.hapticEnabled)
    }

    fun cycleFlashMode() {
        val nextFlash = when (_cameraSettings.value.flashMode) {
            FlashMode.OFF -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.OFF
        }
        _cameraSettings.value = _cameraSettings.value.copy(flashMode = nextFlash)
        val text = "Flash set to ${nextFlash.name.lowercase()}"
        voiceGuidanceManager.announce(text, VoiceGuidanceManager.Priority.NORMAL)
        feedbackManager.performClickHaptic(hapticEnabled = preferences.value.hapticEnabled)
    }

    fun toggleTorch() {
        val newState = !_cameraSettings.value.isTorchOn
        _cameraSettings.value = _cameraSettings.value.copy(isTorchOn = newState)
        cameraRepository.enableTorch(newState)
        val text = if (newState) "Torch turned on" else "Torch turned off"
        voiceGuidanceManager.announce(text, VoiceGuidanceManager.Priority.NORMAL)
    }

    fun cycleTimer() {
        val nextTimer = when (_cameraSettings.value.timer) {
            CaptureTimer.OFF -> CaptureTimer.SECONDS_3
            CaptureTimer.SECONDS_3 -> CaptureTimer.SECONDS_5
            CaptureTimer.SECONDS_5 -> CaptureTimer.SECONDS_10
            CaptureTimer.SECONDS_10 -> CaptureTimer.OFF
        }
        _cameraSettings.value = _cameraSettings.value.copy(timer = nextTimer)
        val text = if (nextTimer.seconds == 0) "Timer off" else "Timer ${nextTimer.seconds} seconds"
        voiceGuidanceManager.announce(text, VoiceGuidanceManager.Priority.NORMAL)
    }

    fun setZoom(ratio: Float) {
        val clamped = ratio.coerceIn(
            cameraCapabilities.value.minZoomRatio,
            cameraCapabilities.value.maxZoomRatio
        )
        _cameraSettings.value = _cameraSettings.value.copy(zoomRatio = clamped)
        cameraRepository.setZoomRatio(clamped)
        val zoomFormatted = String.format(Locale.US, "%.1f", clamped)
        voiceGuidanceManager.announce("Zoom ${zoomFormatted}x", VoiceGuidanceManager.Priority.DEBOUNCED, 500)
    }

    fun setExposureIndex(index: Int) {
        val clamped = index.coerceIn(
            cameraCapabilities.value.minExposureIndex,
            cameraCapabilities.value.maxExposureIndex
        )
        _cameraSettings.value = _cameraSettings.value.copy(exposureCompensationIndex = clamped)
        cameraRepository.setExposureCompensation(clamped)
        val sign = if (clamped > 0) "+$clamped" else "$clamped"
        voiceGuidanceManager.announce("Exposure $sign", VoiceGuidanceManager.Priority.DEBOUNCED, 500)
    }

    fun toggleAudioRecording() {
        val newState = !_cameraSettings.value.isAudioEnabled
        _cameraSettings.value = _cameraSettings.value.copy(isAudioEnabled = newState)
        val text = if (newState) "Audio recording enabled" else "Audio recording muted"
        voiceGuidanceManager.announce(text, VoiceGuidanceManager.Priority.NORMAL)
    }

    fun updateFaceAlignment(state: FaceAlignmentState) {
        _faceAlignmentState.value = state
        if (preferences.value.faceGuidanceEnabled && state.guidanceMessage.isNotBlank()) {
            voiceGuidanceManager.announce(
                state.guidanceMessage,
                VoiceGuidanceManager.Priority.DEBOUNCED,
                2000L
            )
        }
    }

    fun updateSceneDescription(description: String) {
        _sceneDescription.value = description
        if (preferences.value.sceneDescriptionIntervalSeconds > 0) {
            voiceGuidanceManager.announce(
                description,
                VoiceGuidanceManager.Priority.DEBOUNCED,
                (preferences.value.sceneDescriptionIntervalSeconds * 1000).toLong()
            )
        }
    }

    fun takePhoto() {
        if (_cameraState.value is CameraCaptureState.Capturing ||
            _cameraState.value is CameraCaptureState.Saving ||
            countdownJob?.isActive == true
        ) {
            return
        }

        val timerSeconds = _cameraSettings.value.timer.seconds
        if (timerSeconds > 0) {
            countdownJob = viewModelScope.launch {
                for (s in timerSeconds downTo 1) {
                    _cameraState.value = CameraCaptureState.CountingDown(s)
                    voiceGuidanceManager.announce("$s", VoiceGuidanceManager.Priority.CRITICAL)
                    feedbackManager.playCountdownTick(
                        audioEnabled = preferences.value.audioCuesEnabled,
                        hapticEnabled = preferences.value.hapticEnabled
                    )
                    delay(1000)
                }
                executePhotoCapture()
            }
        } else {
            executePhotoCapture()
        }
    }

    private fun executePhotoCapture() {
        _cameraState.value = CameraCaptureState.Capturing
        feedbackManager.playShutterTone(
            audioEnabled = preferences.value.audioCuesEnabled,
            hapticEnabled = preferences.value.hapticEnabled
        )

        cameraRepository.takePhoto(
            flashMode = _cameraSettings.value.flashMode,
            onSuccess = { uri ->
                _cameraState.value = CameraCaptureState.Ready
                voiceGuidanceManager.announce("Photo captured and saved", VoiceGuidanceManager.Priority.CRITICAL)
            },
            onError = { error ->
                _cameraState.value = CameraCaptureState.Error(error)
                voiceGuidanceManager.announce("Capture failed: $error", VoiceGuidanceManager.Priority.CRITICAL)
                feedbackManager.playError(
                    audioEnabled = preferences.value.audioCuesEnabled,
                    hapticEnabled = preferences.value.hapticEnabled
                )
            }
        )
    }

    fun startVideoRecording() {
        if (_cameraState.value is CameraCaptureState.Recording) return
        _cameraState.value = CameraCaptureState.Recording
        _recordingDurationSeconds.value = 0L

        feedbackManager.playRecordStart(
            audioEnabled = preferences.value.audioCuesEnabled,
            hapticEnabled = preferences.value.hapticEnabled
        )
        voiceGuidanceManager.announce("Video recording started", VoiceGuidanceManager.Priority.CRITICAL)

        recordingTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _recordingDurationSeconds.value += 1
                if (_recordingDurationSeconds.value % 15 == 0L) {
                    val mins = _recordingDurationSeconds.value / 60
                    val secs = _recordingDurationSeconds.value % 60
                    val timeStr = if (mins > 0) "$mins minutes $secs seconds" else "$secs seconds"
                    voiceGuidanceManager.announce("Recording $timeStr", VoiceGuidanceManager.Priority.NORMAL)
                }
            }
        }

        cameraRepository.startRecording(
            audioEnabled = _cameraSettings.value.isAudioEnabled,
            onEvent = { /* VideoRecordEvent handled */ },
            onError = { error ->
                stopVideoRecording()
                _cameraState.value = CameraCaptureState.Error(error)
                voiceGuidanceManager.announce("Video recording failed: $error", VoiceGuidanceManager.Priority.CRITICAL)
            }
        )
    }

    fun pauseVideoRecording() {
        if (_cameraState.value is CameraCaptureState.Recording) {
            cameraRepository.pauseRecording()
            _cameraState.value = CameraCaptureState.RecordingPaused
            voiceGuidanceManager.announce("Recording paused", VoiceGuidanceManager.Priority.CRITICAL)
        }
    }

    fun resumeVideoRecording() {
        if (_cameraState.value is CameraCaptureState.RecordingPaused) {
            cameraRepository.resumeRecording()
            _cameraState.value = CameraCaptureState.Recording
            voiceGuidanceManager.announce("Recording resumed", VoiceGuidanceManager.Priority.CRITICAL)
        }
    }

    fun stopVideoRecording() {
        if (_cameraState.value is CameraCaptureState.Recording ||
            _cameraState.value is CameraCaptureState.RecordingPaused
        ) {
            recordingTimerJob?.cancel()
            cameraRepository.stopRecording()
            _cameraState.value = CameraCaptureState.Ready
            feedbackManager.playRecordStop(
                audioEnabled = preferences.value.audioCuesEnabled,
                hapticEnabled = preferences.value.hapticEnabled
            )
            val duration = _recordingDurationSeconds.value
            val mins = duration / 60
            val secs = duration % 60
            val durationText = if (mins > 0) "$mins minutes $secs seconds" else "$secs seconds"
            voiceGuidanceManager.announce("Video saved. Duration $durationText.", VoiceGuidanceManager.Priority.CRITICAL)
        }
    }

    // Gallery Actions
    fun setGalleryFilter(filter: GalleryFilter) {
        _galleryFilter.value = filter
        voiceGuidanceManager.announce("Showing ${filter.name.lowercase()} items", VoiceGuidanceManager.Priority.NORMAL)
    }

    fun setGallerySearchQuery(query: String) {
        _gallerySearchQuery.value = query
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
        val label = if (_isGridView.value) "Grid view" else "List view"
        voiceGuidanceManager.announce(label, VoiceGuidanceManager.Priority.NORMAL)
    }

    fun toggleMediaSelection(item: MediaItem) {
        val current = _selectedMediaUris.value.toMutableSet()
        if (current.contains(item.uri.toString())) {
            current.remove(item.uri.toString())
            voiceGuidanceManager.announce("${item.displayName} deselected", VoiceGuidanceManager.Priority.NORMAL)
        } else {
            current.add(item.uri.toString())
            voiceGuidanceManager.announce("${item.displayName} selected", VoiceGuidanceManager.Priority.NORMAL)
        }
        _selectedMediaUris.value = current
    }

    fun selectAllMedia(items: List<MediaItem>) {
        val allUris = items.map { it.uri.toString() }.toSet()
        _selectedMediaUris.value = allUris
        voiceGuidanceManager.announce("All ${allUris.size} items selected", VoiceGuidanceManager.Priority.NORMAL)
    }

    fun clearMediaSelection() {
        _selectedMediaUris.value = emptySet()
        voiceGuidanceManager.announce("Selection cleared", VoiceGuidanceManager.Priority.NORMAL)
    }

    fun openMediaViewer(item: MediaItem) {
        _activeMediaItem.value = item
        _currentScreen.value = AppScreen.MEDIA_VIEWER
        val type = if (item.isVideo) "Video" else "Photo"
        voiceGuidanceManager.announce("$type opened. ${item.displayName}", VoiceGuidanceManager.Priority.CRITICAL)
    }

    fun openMediaEditor(item: MediaItem) {
        _activeMediaItem.value = item
        _currentScreen.value = AppScreen.MEDIA_EDITOR
        voiceGuidanceManager.announce("Editor opened for ${item.displayName}", VoiceGuidanceManager.Priority.CRITICAL)
    }

    fun closeSubScreen() {
        _currentScreen.value = AppScreen.MAIN
        _slideshowActive.value = false
        slideshowJob?.cancel()
        voiceGuidanceManager.announce("Returned to gallery", VoiceGuidanceManager.Priority.NORMAL)
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            mediaRepository.toggleFavorite(item)
            val message = if (item.isFavorite) "Removed from favorites" else "Added to favorites"
            voiceGuidanceManager.announce(message, VoiceGuidanceManager.Priority.NORMAL)
        }
    }

    fun moveToTrash(items: List<MediaItem>) {
        viewModelScope.launch {
            for (item in items) {
                mediaRepository.moveToTrash(item)
            }
            clearMediaSelection()
            voiceGuidanceManager.announce("${items.size} items moved to trash", VoiceGuidanceManager.Priority.CRITICAL)
        }
    }

    fun restoreFromTrash(items: List<MediaItem>) {
        viewModelScope.launch {
            for (item in items) {
                mediaRepository.restoreFromTrash(item)
            }
            clearMediaSelection()
            voiceGuidanceManager.announce("${items.size} items restored", VoiceGuidanceManager.Priority.CRITICAL)
        }
    }

    fun deletePermanently(items: List<MediaItem>) {
        viewModelScope.launch {
            var count = 0
            for (item in items) {
                if (mediaRepository.deletePermanently(item)) count++
            }
            clearMediaSelection()
            voiceGuidanceManager.announce("$count items permanently deleted", VoiceGuidanceManager.Priority.CRITICAL)
        }
    }

    fun nextMediaItem(items: List<MediaItem>) {
        val current = _activeMediaItem.value ?: return
        val index = items.indexOfFirst { it.uri == current.uri }
        if (index != -1 && index < items.size - 1) {
            val next = items[index + 1]
            _activeMediaItem.value = next
            voiceGuidanceManager.announce("Next item. ${next.displayName}", VoiceGuidanceManager.Priority.NORMAL)
        }
    }

    fun previousMediaItem(items: List<MediaItem>) {
        val current = _activeMediaItem.value ?: return
        val index = items.indexOfFirst { it.uri == current.uri }
        if (index > 0) {
            val prev = items[index - 1]
            _activeMediaItem.value = prev
            voiceGuidanceManager.announce("Previous item. ${prev.displayName}", VoiceGuidanceManager.Priority.NORMAL)
        }
    }

    fun toggleSlideshow(items: List<MediaItem>) {
        if (_slideshowActive.value) {
            _slideshowActive.value = false
            slideshowJob?.cancel()
            voiceGuidanceManager.announce("Slideshow stopped", VoiceGuidanceManager.Priority.NORMAL)
        } else {
            _slideshowActive.value = true
            voiceGuidanceManager.announce("Slideshow started", VoiceGuidanceManager.Priority.NORMAL)
            slideshowJob = viewModelScope.launch {
                while (_slideshowActive.value && items.isNotEmpty()) {
                    delay(3500)
                    nextMediaItem(items)
                }
            }
        }
    }

    private fun handleVoiceCommand(command: VoiceCommand, rawText: String) {
        feedbackManager.performClickHaptic(hapticEnabled = preferences.value.hapticEnabled)
        when (command) {
            VoiceCommand.TAKE_PHOTO -> {
                if (_selectedTab.value != MainTab.PHOTO) selectTab(MainTab.PHOTO)
                takePhoto()
            }
            VoiceCommand.START_VIDEO -> {
                if (_selectedTab.value != MainTab.VIDEO) selectTab(MainTab.VIDEO)
                startVideoRecording()
            }
            VoiceCommand.STOP_VIDEO -> stopVideoRecording()
            VoiceCommand.SWITCH_CAMERA -> switchCamera()
            VoiceCommand.FLASH_ON -> {
                _cameraSettings.value = _cameraSettings.value.copy(flashMode = FlashMode.ON)
                voiceGuidanceManager.announce("Flash turned on", VoiceGuidanceManager.Priority.NORMAL)
            }
            VoiceCommand.FLASH_OFF -> {
                _cameraSettings.value = _cameraSettings.value.copy(flashMode = FlashMode.OFF)
                voiceGuidanceManager.announce("Flash turned off", VoiceGuidanceManager.Priority.NORMAL)
            }
            VoiceCommand.ZOOM_IN -> setZoom(_cameraSettings.value.zoomRatio + 0.5f)
            VoiceCommand.ZOOM_OUT -> setZoom(_cameraSettings.value.zoomRatio - 0.5f)
            VoiceCommand.OPEN_GALLERY -> selectTab(MainTab.GALLERY)
            VoiceCommand.NEXT -> nextMediaItem(allMediaItems.value)
            VoiceCommand.PREVIOUS -> previousMediaItem(allMediaItems.value)
            VoiceCommand.DELETE -> {
                val current = _activeMediaItem.value
                if (current != null) {
                    moveToTrash(listOf(current))
                    closeSubScreen()
                } else if (_selectedMediaUris.value.isNotEmpty()) {
                    val selected = allMediaItems.value.filter { _selectedMediaUris.value.contains(it.uri.toString()) }
                    moveToTrash(selected)
                }
            }
            VoiceCommand.HELP -> {
                voiceGuidanceManager.announce(
                    "Available voice commands: take photo, start video, stop video, switch camera, flash on, flash off, zoom in, zoom out, open gallery, next, previous, delete, cancel.",
                    VoiceGuidanceManager.Priority.CRITICAL
                )
            }
            VoiceCommand.CANCEL -> {
                voiceGuidanceManager.announce("Command cancelled", VoiceGuidanceManager.Priority.NORMAL)
            }
            else -> {
                voiceGuidanceManager.announce("Command not recognized: $rawText. Say help for available commands.", VoiceGuidanceManager.Priority.NORMAL)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager?.unregisterListener(this)
        cameraRepository.release()
        voiceGuidanceManager.release()
        feedbackManager.release()
        voiceCommandManager.release()
    }
}
