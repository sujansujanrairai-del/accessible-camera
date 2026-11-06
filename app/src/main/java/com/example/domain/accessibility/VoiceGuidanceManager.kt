package com.example.domain.accessibility

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.accessibility.AccessibilityManager
import java.util.Locale

class VoiceGuidanceManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

    private var speechRate: Float = 1.0f
    private var speechPitch: Float = 1.0f
    private var isVoiceGuidanceEnabled: Boolean = true

    private var lastSpokenText: String? = null
    private var lastSpokenTimestampMs: Long = 0L

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(speechPitch)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
            })
            isInitialized = true
        }
    }

    fun updateConfig(enabled: Boolean, rate: Float, pitch: Float) {
        isVoiceGuidanceEnabled = enabled
        speechRate = rate
        speechPitch = pitch
        if (isInitialized) {
            tts?.setSpeechRate(rate)
            tts?.setPitch(pitch)
        }
    }

    fun isTalkBackActive(): Boolean {
        return accessibilityManager?.isTouchExplorationEnabled == true
    }

    enum class Priority {
        CRITICAL,
        NORMAL,
        DEBOUNCED
    }

    fun announce(text: String, priority: Priority = Priority.NORMAL, debounceMs: Long = 1000L) {
        if (!isVoiceGuidanceEnabled || !isInitialized || text.isBlank()) return

        val now = System.currentTimeMillis()
        if (priority == Priority.DEBOUNCED) {
            if (text == lastSpokenText && now - lastSpokenTimestampMs < debounceMs) {
                return
            }
            if (now - lastSpokenTimestampMs < 600L) {
                return
            }
        }

        lastSpokenText = text
        lastSpokenTimestampMs = now

        val queueMode = when (priority) {
            Priority.CRITICAL -> TextToSpeech.QUEUE_FLUSH
            Priority.NORMAL -> TextToSpeech.QUEUE_ADD
            Priority.DEBOUNCED -> TextToSpeech.QUEUE_FLUSH
        }

        tts?.speak(text, queueMode, null, "utterance_${System.nanoTime()}")
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun release() {
        if (isInitialized) {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        }
    }
}
