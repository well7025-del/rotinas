package br.com.rotinas.etiquetas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Ditado contínuo em português, direto na tela do aplicativo (sem a janelinha
 * do Google). Os silêncios são propositalmente longos: ela dita o nome, pensa,
 * e continua listando as peças sem que o reconhecimento corte no meio.
 */
class VoiceInput(private val context: Context) {

    interface Listener {
        fun onReady()
        fun onPartial(text: String)
        fun onResult(text: String)
        fun onError(message: String)
        fun onVolume(level: Float)
    }

    private var recognizer: SpeechRecognizer? = null
    var isListening: Boolean = false
        private set

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(listener: Listener) {
        stop()

        if (!isAvailable()) {
            listener.onError(context.getString(R.string.erro_sem_reconhecimento))
            return
        }

        val speech = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speech
        isListening = true

        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = listener.onReady()

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) {
                // rms vem de -2 a 10 dB; normaliza para 0..1 para animar o botão.
                listener.onVolume(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                isListening = false
                listener.onError(messageFor(error))
                release()
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val text = firstResult(results)
                if (text.isNullOrBlank()) {
                    listener.onError(context.getString(R.string.erro_nao_entendi))
                } else {
                    listener.onResult(text)
                }
                release()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                firstResult(partialResults)?.takeIf { it.isNotBlank() }?.let(listener::onPartial)
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        speech.startListening(recognizerIntent())
    }

    /** Encerra a fala e força o reconhecedor a devolver o que já ouviu. */
    fun stopListening() {
        if (isListening) {
            recognizer?.stopListening()
            isListening = false
        }
    }

    fun stop() {
        isListening = false
        release()
    }

    private fun release() {
        recognizer?.let {
            it.setRecognitionListener(null)
            it.cancel()
            it.destroy()
        }
        recognizer = null
    }

    private fun recognizerIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, LANGUAGE)
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun messageFor(error: Int): String = context.getString(
        when (error) {
            SpeechRecognizer.ERROR_AUDIO -> R.string.erro_audio
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.erro_permissao
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> R.string.erro_rede
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.erro_nao_entendi
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> R.string.erro_ocupado
            else -> R.string.erro_reconhecimento
        },
    )

    private companion object {
        const val LANGUAGE = "pt-BR"
    }
}
