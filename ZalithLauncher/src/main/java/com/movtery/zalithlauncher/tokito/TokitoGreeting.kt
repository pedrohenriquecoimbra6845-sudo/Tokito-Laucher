/*
 * Tokito Launcher
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com>, the Tokito Launcher creators and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.tokito

import android.content.Context
import android.speech.tts.TextToSpeech
import com.movtery.zalithlauncher.R
import java.time.LocalTime

/** The parts of the day the greeting of the Mini Wallpaper tells apart */
enum class DayPeriod(val wordRes: Int) {
    /** 05:00 to 11:59 */
    MORNING(R.string.home_greet_morning),

    /** 12:00 to 17:59 */
    AFTERNOON(R.string.home_greet_afternoon),

    /** 18:00 to 04:59 */
    NIGHT(R.string.home_greet_night)
}

/**
 * Tokito Launcher: what the Mini Wallpaper says to the player. "Welcome" when the launcher opens, the greeting of the
 * time of the day (good morning, good afternoon, good night) and a warrior phrase. The greeting is said again when
 * the part of the day changes while the launcher is open.
 */
object TokitoGreeting {
    fun periodAt(hour: Int): DayPeriod = when (hour) {
        in 5..11 -> DayPeriod.MORNING
        in 12..17 -> DayPeriod.AFTERNOON
        else -> DayPeriod.NIGHT
    }

    fun periodNow(): DayPeriod = periodAt(LocalTime.now().hour)

    /** The warrior phrases (original, short, in the language of the app) */
    val phrases = listOf(
        R.string.home_greet_p0,
        R.string.home_greet_p1,
        R.string.home_greet_p2,
        R.string.home_greet_p3,
        R.string.home_greet_p4,
        R.string.home_greet_p5,
        R.string.home_greet_p6,
        R.string.home_greet_p7,
        R.string.home_greet_p8,
        R.string.home_greet_p9,
        R.string.home_greet_p10,
        R.string.home_greet_p11,
        R.string.home_greet_p12,
        R.string.home_greet_p13,
        R.string.home_greet_p14,
        R.string.home_greet_p15,
        R.string.home_greet_p16,
        R.string.home_greet_p17,
        R.string.home_greet_p18,
        R.string.home_greet_p19
    )

    /** A phrase for now; the one that was shown last is skipped, so the same phrase never comes twice in a row */
    @Volatile
    private var lastPhrase = -1

    @Synchronized
    fun randomPhrase(): Int {
        var next = phrases.indices.random()
        if (next == lastPhrase) next = (next + 1) % phrases.size
        lastPhrase = next
        return phrases[next]
    }
}

/**
 * The voice of the phone (the text to speech engine that the phone has) reading the greeting out loud.
 * Create one only while the player wants the voice and [release] it after, so the engine is not kept alive.
 * A phone without a voice engine (or without the language) just stays quiet.
 */
class GreetingVoice(context: Context) {
    @Volatile
    private var engine: TextToSpeech? = null

    @Volatile
    private var ready = false

    @Volatile
    private var pending: String? = null

    init {
        val app = context.applicationContext
        engine = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val language = engine?.setLanguage(app.resources.configuration.locales[0])
                ready = language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED
                if (ready) pending?.let { speak(it) }
            }
        }
    }

    /** Reads [text] now (it waits for the engine when it is still starting) */
    fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        pending = null
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tokito-greeting")
    }

    fun release() {
        ready = false
        pending = null
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}
