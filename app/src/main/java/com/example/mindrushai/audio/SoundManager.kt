package com.example.mindrushai.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.ToneGenerator
import android.media.AudioManager

/**
 * SoundManager
 *
 * Provides non-blocking sound feedback for gameplay events.
 * Uses [ToneGenerator] to generate tones programmatically —
 * no audio asset files required.
 *
 * All playback is fire-and-forget; no coroutines needed.
 * Call [release] when the owning component is destroyed.
 */
class SoundManager {

    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 60)

    // ── Public API ────────────────────────────────────────────────────────────

    /** Short high-pitched beep — player typed a correct word. */
    fun playCorrect() {
        runCatching { toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 80) }
    }

    /** Double high beep — round fully completed. */
    fun playRoundComplete() {
        runCatching {
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 100)
            Thread.sleep(120)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 100)
        }
    }

    /** Low buzz — wrong word entered, game over. */
    fun playWrong() {
        runCatching { toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 200) }
    }

    /** Soft click — invalid word (not a real English word). */
    fun playInvalid() {
        runCatching { toneGen.startTone(ToneGenerator.TONE_DTMF_0, 60) }
    }

    /** Ascending beep — word appears during memorise phase. */
    fun playWordAppear() {
        runCatching { toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 60) }
    }

    fun release() {
        runCatching { toneGen.release() }
    }
}