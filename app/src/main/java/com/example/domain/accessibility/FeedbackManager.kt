package com.example.domain.accessibility

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

class FeedbackManager(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun playShutterTone(audioEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (audioEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
        }
        if (hapticEnabled) {
            vibrate(80, 200)
        }
    }

    fun playRecordStart(audioEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (audioEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)
        }
        if (hapticEnabled) {
            vibrate(100, 255)
        }
    }

    fun playRecordStop(audioEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (audioEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }
        if (hapticEnabled) {
            vibrate(120, 255)
        }
    }

    fun playFocusLocked(audioEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (audioEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
        }
        if (hapticEnabled) {
            vibrate(40, 150)
        }
    }

    fun playCountdownTick(audioEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (audioEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
        }
        if (hapticEnabled) {
            vibrate(30, 100)
        }
    }

    fun playError(audioEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (audioEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 250)
        }
        if (hapticEnabled) {
            vibratePattern(longArrayOf(0, 80, 50, 120))
        }
    }

    fun performClickHaptic(view: View? = null, hapticEnabled: Boolean = true) {
        if (!hapticEnabled) return
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) ?: vibrate(25, 120)
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        vibrator?.let { v ->
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    }

    private fun vibratePattern(pattern: LongArray) {
        vibrator?.let { v ->
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, -1)
            }
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
