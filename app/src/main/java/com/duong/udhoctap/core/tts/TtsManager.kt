package com.duong.udhoctap.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("vi", "VN").let { vi ->
                    val result = tts?.isLanguageAvailable(vi) ?: TextToSpeech.LANG_NOT_SUPPORTED
                    if (result >= TextToSpeech.LANG_AVAILABLE) vi else Locale.ENGLISH
                }
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (!isReady) return
        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ud_hoc_tap_tts")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
