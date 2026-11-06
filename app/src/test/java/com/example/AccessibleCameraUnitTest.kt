package com.example

import com.example.data.model.CameraCapabilities
import com.example.data.model.CameraCaptureState
import com.example.data.model.CameraFacing
import com.example.data.model.CameraSettingsState
import com.example.data.model.CaptureTimer
import com.example.data.model.FaceAlignmentState
import com.example.data.model.FlashMode
import com.example.data.model.VoiceCommand
import com.example.data.preferences.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibleCameraUnitTest {

    @Test
    fun voiceCommand_takePhotoRecognized() {
        assertEquals(VoiceCommand.TAKE_PHOTO, VoiceCommand.fromSpokenText("take photo"))
        assertEquals(VoiceCommand.TAKE_PHOTO, VoiceCommand.fromSpokenText("shoot"))
        assertEquals(VoiceCommand.TAKE_PHOTO, VoiceCommand.fromSpokenText("capture image"))
        assertEquals(VoiceCommand.TAKE_PHOTO, VoiceCommand.fromSpokenText("say cheese"))
    }

    @Test
    fun voiceCommand_videoControlsRecognized() {
        assertEquals(VoiceCommand.START_VIDEO, VoiceCommand.fromSpokenText("start video"))
        assertEquals(VoiceCommand.START_VIDEO, VoiceCommand.fromSpokenText("record video"))
        assertEquals(VoiceCommand.STOP_VIDEO, VoiceCommand.fromSpokenText("stop"))
    }

    @Test
    fun voiceCommand_cameraSwitchAndZoomRecognized() {
        assertEquals(VoiceCommand.SWITCH_CAMERA, VoiceCommand.fromSpokenText("switch camera"))
        assertEquals(VoiceCommand.SWITCH_CAMERA, VoiceCommand.fromSpokenText("flip camera"))
        assertEquals(VoiceCommand.ZOOM_IN, VoiceCommand.fromSpokenText("zoom in"))
        assertEquals(VoiceCommand.ZOOM_OUT, VoiceCommand.fromSpokenText("zoom out"))
    }

    @Test
    fun voiceCommand_galleryAndNavigationRecognized() {
        assertEquals(VoiceCommand.OPEN_GALLERY, VoiceCommand.fromSpokenText("open gallery"))
        assertEquals(VoiceCommand.NEXT, VoiceCommand.fromSpokenText("next photo"))
        assertEquals(VoiceCommand.PREVIOUS, VoiceCommand.fromSpokenText("previous photo"))
        assertEquals(VoiceCommand.DELETE, VoiceCommand.fromSpokenText("delete this"))
        assertEquals(VoiceCommand.CONFIRM, VoiceCommand.fromSpokenText("confirm delete"))
        assertEquals(VoiceCommand.CANCEL, VoiceCommand.fromSpokenText("cancel"))
        assertEquals(VoiceCommand.HELP, VoiceCommand.fromSpokenText("help me"))
    }

    @Test
    fun cameraCapabilities_defaultValuesSensible() {
        val caps = CameraCapabilities(
            hasBackCamera = true,
            hasFrontCamera = true,
            hasFlashUnit = true,
            minZoomRatio = 1.0f,
            maxZoomRatio = 10.0f
        )
        assertTrue(caps.hasBackCamera)
        assertTrue(caps.hasFrontCamera)
        assertTrue(caps.hasFlashUnit)
        assertEquals(1.0f, caps.minZoomRatio, 0.001f)
        assertEquals(10.0f, caps.maxZoomRatio, 0.001f)
    }

    @Test
    fun cameraSettings_stateTransitions() {
        var state = CameraSettingsState()
        assertEquals(CameraFacing.BACK, state.cameraFacing)
        assertEquals(FlashMode.OFF, state.flashMode)
        assertEquals(CaptureTimer.OFF, state.timer)

        state = state.copy(
            cameraFacing = CameraFacing.FRONT,
            flashMode = FlashMode.AUTO,
            timer = CaptureTimer.SECONDS_3
        )
        assertEquals(CameraFacing.FRONT, state.cameraFacing)
        assertEquals(FlashMode.AUTO, state.flashMode)
        assertEquals(3, state.timer.seconds)
    }

    @Test
    fun faceAlignmentState_defaultNotAligned() {
        val face = FaceAlignmentState()
        assertFalse(face.hasFace)
        assertFalse(face.isWellAligned)
        assertEquals("No face detected", face.guidanceMessage)

        val aligned = FaceAlignmentState(
            hasFace = true,
            faceCount = 1,
            isWellAligned = true,
            guidanceMessage = "Face is centered and well aligned"
        )
        assertTrue(aligned.hasFace)
        assertTrue(aligned.isWellAligned)
    }

    @Test
    fun appPreferences_defaultsFollowAccessibilityPrinciples() {
        val prefs = AppPreferences()
        assertTrue("Voice guidance must be enabled by default", prefs.voiceGuidanceEnabled)
        assertTrue("Face guidance must be enabled by default", prefs.faceGuidanceEnabled)
        assertTrue("Haptics must be enabled by default", prefs.hapticEnabled)
        assertTrue("Audio cues must be enabled by default", prefs.audioCuesEnabled)
        assertFalse("Location tagging must be off by default for privacy", prefs.locationTaggingEnabled)
        assertFalse("Watermark should be off by default", prefs.watermarkEnabled)
    }

    @Test
    fun cameraCaptureState_hierarchy() {
        val ready: CameraCaptureState = CameraCaptureState.Ready
        val capturing: CameraCaptureState = CameraCaptureState.Capturing
        val recording: CameraCaptureState = CameraCaptureState.Recording
        val countdown: CameraCaptureState = CameraCaptureState.CountingDown(3)

        assertTrue(ready is CameraCaptureState.Ready)
        assertTrue(capturing is CameraCaptureState.Capturing)
        assertTrue(recording is CameraCaptureState.Recording)
        assertEquals(3, (countdown as CameraCaptureState.CountingDown).secondsRemaining)
    }
}
