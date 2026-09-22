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

    /** Used by the Tabata timer, where a work/rest transition genuinely needs a cue. */
    fun transitionTone(context: Context) {
        runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70).apply {
                startTone(ToneGenerator.TONE_PROP_ACK, 150)
                android.os.Handler(context.mainLooper).postDelayed({ release() }, 300)
            }
        }
        vibrate(context, 60)
    }

    /**
     * The turn at the top of a rep: stop lifting, start lowering.
     *
     * Under a 5s/5s tempo you cannot watch a screen and keep form, so the cue has to be
     * audible. It is pitched apart from [transitionTone] on purpose — one says "reverse",
     * the other says "that is a rep" — because hearing which is which is the whole point.
     */
    fun turnTone(context: Context) {
        runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75).apply {
                startTone(ToneGenerator.TONE_PROP_PROMPT, 120)
                android.os.Handler(context.mainLooper).postDelayed({ release() }, 260)
            }
        }
        vibrate(context, 40)
    }

    private fun vibrate(context: Context, ms: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        runCatching {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
