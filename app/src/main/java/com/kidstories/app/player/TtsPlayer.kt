package com.kidstories.app.player

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsPlayer(context: Context) {

    var isSpeaking: Boolean = false
        private set
    var speed: Float = 1.0f
        private set

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("fa")
            }
        }
    }

    fun speak(text: String) {
        tts.setSpeechRate(speed)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        isSpeaking = true
    }

    fun stop() {
        tts.stop()
        isSpeaking = false
    }

    fun setSpeed(newSpeed: Float) {
        speed = newSpeed
    }

    fun shutdown() {
        tts.shutdown()
    }
}
