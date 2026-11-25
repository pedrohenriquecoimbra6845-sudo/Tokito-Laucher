/*
 * Tokito Launcher
 * Based on Zalith Launcher 2, Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.tokito

import android.content.Context
import android.os.Process
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

private const val TAG = "TokitoBoot"

/**
 * Tokito Launcher: the opening of the game.
 *
 * - **Time:** it is counted from the moment the player taps PLAY (the launcher process writes it in the intent of
 *   the game) until the game reaches its title screen. The goal is [TARGET_SECONDS] seconds at most.
 *   The last time is kept in a file so the launcher can show it.
 * - **Loading screen:** while the game loads (the time of the progress bar of Minecraft), the loading screen of the
 *   launcher covers it. The progress comes from the log of the game: the start of Java, the resources, every
 *   texture atlas that is built, and the sound engine (the last thing that starts before the title screen).
 */
object TokitoBoot {
    const val INTENT_PRESS_MS = "tokito_press_ms"
    const val TARGET_SECONDS = 35

    /** The loading screen goes away by itself after this, even if the game never says that it is ready */
    const val MAX_LOADING_SECONDS = 150

    /** The player may skip the loading screen after this */
    const val SKIP_AFTER_SECONDS = 15

    private const val HUB_KEY = "tokito-boot"
    private const val LAST_FILE = "tokito_boot_last.txt"

    enum class Stage { START, JAVA, RESOURCES, TEXTURES, SOUND }

    // ------------------------------------------------------------------ launcher process

    /** When the player tapped PLAY (the launcher process) */
    @Volatile
    var pressMs: Long = 0L
        private set

    fun markPress() {
        pressMs = System.currentTimeMillis()
    }

    /** Seconds that the last game took to open, or -1 */
    fun lastSeconds(context: Context): Int = runCatching {
        File(context.cacheDir, LAST_FILE).readText().trim().toInt()
    }.getOrDefault(-1)

    // ------------------------------------------------------------------ game process

    /** The loading screen covers the game */
    var loading by mutableStateOf(false)
        private set

    /** The game said that it is ready (the loading screen is about to go away) */
    var ready by mutableStateOf(false)
        private set

    /** 0 to 1 */
    var progress by mutableFloatStateOf(0f)
        private set

    var stage by mutableStateOf(Stage.START)
        private set

    /** Seconds that the game took to open (-1 until it opened) */
    var openedSeconds by mutableIntStateOf(-1)
        private set

    private var beginMs = 0L
    private var atlases = 0

    /** When the last line of the log of the game arrived (elapsedRealtime), 0 = none yet */
    @Volatile
    private var lastLineMs = 0L

    /** Milliseconds without any new line in the log of the game (0 while no line arrived yet) */
    fun quietMillis(): Long = if (lastLineMs == 0L) 0L else SystemClock.elapsedRealtime() - lastLineMs

    /**
     * The game stopped writing in the log after it built its textures: it is at the title screen (some setups never
     * print the sound engine line). The loading screen can go away.
     */
    fun markReadyByQuiet() {
        if (!loading || ready) return
        stage = Stage.SOUND
        progress = 0.96f
        ready = true
        TokitoDriverGuard.noteFirstFrame()
    }

    /**
     * The game process starts: the loading screen comes up and starts to listen to the log of the game.
     * @param pressMsFromIntent the time that the player tapped PLAY, 0 if unknown
     */
    fun begin(pressMsFromIntent: Long) {
        val now = System.currentTimeMillis()
        beginMs = if (pressMsFromIntent > 0L && now - pressMsFromIntent in 0L..(10 * 60_000L)) {
            pressMsFromIntent
        } else {
            now - (SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime())
        }
        atlases = 0
        lastLineMs = 0L
        progress = 0.05f
        stage = Stage.START
        ready = false
        openedSeconds = -1
        loading = true
        TokitoLogHub.set(HUB_KEY) { onLogLine(it) }
    }

    fun elapsedSeconds(): Int = ((System.currentTimeMillis() - beginMs) / 1000L).toInt().coerceAtLeast(0)

    private fun onLogLine(line: String) {
        lastLineMs = SystemClock.elapsedRealtime()
        if (!loading || ready) return
        when {
            //the sound engine is the last thing that starts before the title screen
            line.contains("Sound engine started") || line.contains("SoundSystem started") -> {
                stage = Stage.SOUND
                progress = 0.96f
                ready = true
                TokitoDriverGuard.noteFirstFrame()
            }
            line.contains("-atlas") || line.contains("textures/atlas") -> {
                atlases++
                stage = Stage.TEXTURES
                progress = maxOf(progress, (0.40f + atlases * 0.08f).coerceAtMost(0.88f))
            }
            line.contains("Reloading ResourceManager") || line.contains("Loading resource") ||
                line.contains("Datafixer optimization") -> {
                if (stage < Stage.RESOURCES) stage = Stage.RESOURCES
                progress = maxOf(progress, 0.28f)
            }
            line.contains("LWJGL") || line.contains("Setting user") || line.contains("Backend library") ||
                line.contains("Minecraft Version") -> {
                if (stage < Stage.JAVA) stage = Stage.JAVA
                progress = maxOf(progress, 0.14f)
            }
        }
    }

    /** The loading screen goes away; the time is written and logged */
    fun finish(context: Context) {
        if (!loading) return
        TokitoLogHub.remove(HUB_KEY)
        val seconds = elapsedSeconds()
        openedSeconds = seconds
        progress = 1f
        loading = false
        Logger.info(TAG, "The game opened in $seconds s (goal: $TARGET_SECONDS s)")
        runCatching { File(context.cacheDir, LAST_FILE).writeText(seconds.toString()) }
    }
}
