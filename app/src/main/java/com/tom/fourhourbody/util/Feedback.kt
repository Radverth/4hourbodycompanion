package com.tom.fourhourbody.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * A single completion tone, deliberately. Hold mode must feel calm — no per-second beeping.
 */
object Feedback {

    fun completionTone(context: Context) {
        runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).apply {
                startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                // Release after the tone has had time to play.
                android.os.Handler(context.mainLooper).postDelayed({ release() }, 400)
            }
        }
        vibrate(context, 120)
    }

    /**
     * The turn in the cadence guide, up: start lifting.
     *
     * Under a slow cadence you cannot watch a screen and keep form, so the cue has to be
     * audible — and it has to be distinguishable from [descendTone] without looking, so the
     * two are a different pitch and a different vibration pattern (one tap here, two on the
     * way down).
     */
    fun ascendTone(context: Context) {
        runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75).apply {
                startTone(ToneGenerator.TONE_PROP_ACK, 120)
                android.os.Handler(context.mainLooper).postDelayed({ release() }, 260)
            }
        }
        vibrate(context, 40)
    }

    /** The turn in the cadence guide, down: start lowering. See [ascendTone]. */
    fun descendTone(context: Context) {
        runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75).apply {
                startTone(ToneGenerator.TONE_PROP_PROMPT, 120)
                android.os.Handler(context.mainLooper).postDelayed({ release() }, 260)
            }
        }
        vibrate(context, longArrayOf(0, 40, 60, 40))
    }

    private fun vibrate(context: Context, ms: Long) {
        val vibrator = vibratorOf(context)
        runCatching {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** [pattern]: alternating off/on durations in ms, starting with an initial delay. */
    private fun vibrate(context: Context, pattern: LongArray) {
        val vibrator = vibratorOf(context)
        runCatching {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    private fun vibratorOf(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
