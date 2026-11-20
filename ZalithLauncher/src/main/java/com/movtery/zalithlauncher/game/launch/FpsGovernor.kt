/*
 * Tokito Launcher - unofficial modified version of Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors (Zalith Launcher 2)
 * Modifications Copyright (C) 2026 Tokito Launcher contributors
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

package com.movtery.zalithlauncher.game.launch

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.bridge.ZLBridge
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.OptimizationMode
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lwjgl.glfw.CallbackBridge

private const val TAG = "FpsGovernor"

/**
 * Tokito Launcher: what the FPS governor is doing now (read by the game menu).
 */
object FpsGovernorState {
    /** FPS limit in use (0 = no limit) */
    var targetFps by mutableIntStateOf(0)

    /** FPS the game really draws */
    var currentFps by mutableIntStateOf(0)

    /** Refresh rate of the screen, in Hz */
    var displayHz by mutableIntStateOf(0)

    /** Heat of the phone: 0 = cool ... 4 = critical */
    var heat by mutableIntStateOf(0)

    fun reset() {
        targetFps = 0
        currentFps = 0
        heat = 0
    }
}

/**
 * Tokito Launcher: the FPS governor of the optimization modes. It runs in the game process and does what the
 * old modes did by changing the options of the game, but on the side of the launcher:
 *
 * - The FPS limit is set in the native frame limiter ([ZLBridge.setFpsLimit]) and moves **in steps of 10**.
 * - **Heat:** hot phone, lower limit (about 80, then 70, then less; see [heatCap]).
 * - **Real FPS:** when the phone cannot hold the limit, the limit goes down to what it holds (steady frames
 *   are better than frames that jump). When it holds it for a while, it tries 10 more. After a drop it waits
 *   (60 s, then more) before it tries again.
 * - **Same Hz as the FPS:** at every change the screen is put at the refresh rate that fits the limit.
 * - **Calibration:** the FPS that the phone held is saved (`tokitoCalibratedFps`) and is where the next game starts.
 * - **Background:** in Survival and Brutal, when the game goes to the background the limit goes to 10 FPS.
 *
 * Nothing is written in the options of the game.
 *
 * @param applyRefresh asks the activity to put the screen at the rate that fits the FPS limit
 * @param currentDisplayHz reads the refresh rate that the screen has now
 * @param maxDisplayHz highest refresh rate that the screen has
 */
class FpsGovernor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val maxDisplayHz: () -> Int,
    private val currentDisplayHz: () -> Int,
    private val applyRefresh: (fps: Int) -> Unit
) {
    private var job: Job? = null
    private var mode = OptimizationMode.OFF
    private var ceiling = 60
    private var cap = 0

    @Volatile
    private var inBackground = false

    private var holdUntil = 0L
    private var holdSeconds = HOLD_START_SECONDS
    private var lastLowerAt = 0L
    private var steadySeconds = 0
    private val samples = ArrayDeque<Int>()
    private var lastSavedCalibration = 0

    fun start() {
        if (job?.isActive == true) return
        mode = OptimizationProfile.currentMode()
        if (!mode.limitsFps) {
            ZLBridge.setFpsLimit(0)
            FpsGovernorState.reset()
            return
        }
        ceiling = computeCeiling()
        val calibrated = AllSettings.tokitoCalibratedFps.getValue()
        val benchmark = AllSettings.tokitoBenchmarkFps.getValue()
        val start = if (calibrated > 0) calibrated else if (benchmark > 0) benchmark else DEFAULT_START_FPS
        lastSavedCalibration = calibrated
        val heat = readHeat()
        cap = round10(minOf(start, heatCap(ceiling, heat), ceiling)).coerceAtLeast(MIN_FPS)
        apply(cap)
        Logger.info(TAG, "Mode $mode started: ceiling=$ceiling start=$cap heat=$heat")

        job = scope.launch {
            while (isActive) {
                delay(1000L)
                runCatching { tick() }.onFailure { Logger.warning(TAG, "Governor tick failed", it) }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        saveCalibration()
        ZLBridge.setFpsLimit(0)
        FpsGovernorState.reset()
    }

    /** The game went to the background (true) or came back (false) */
    fun setBackground(background: Boolean) {
        if (job?.isActive != true) return
        inBackground = background
        if (!mode.quietBackground) return
        if (background) {
            ZLBridge.setFpsLimit(BACKGROUND_FPS)
            FpsGovernorState.targetFps = BACKGROUND_FPS
        } else {
            apply(cap)
        }
    }

    private fun computeCeiling(): Int {
        val displayMax = round10(maxDisplayHz())
        val benchmark = AllSettings.tokitoBenchmarkFps.getValue().takeIf { it > 0 } ?: DEFAULT_START_FPS
        //the benchmark is an estimate: the governor may try a bit above it
        val limit = minOf(displayMax, mode.fpsCeilingCap, benchmark + PROBE_ABOVE_BENCHMARK)
        return round10(limit).coerceAtLeast(MIN_FPS)
    }

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        val heat = readHeat()
        val fps = CallbackBridge.getCurrentFps()
        FpsGovernorState.heat = heat
        FpsGovernorState.currentFps = fps
        FpsGovernorState.displayHz = currentDisplayHz()
        if (inBackground || fps <= 0) return

        //1. heat
        val hardCap = minOf(ceiling, heatCap(ceiling, heat))
        if (cap > hardCap) {
            lowerTo(hardCap, now, "heat $heat")
            return
        }

        //2. what the phone really holds
        samples.addLast(fps)
        while (samples.size > WINDOW_SECONDS) samples.removeFirst()
        if (samples.size < WINDOW_SECONDS) return
        val average = samples.average()

        when {
            //loading the world or a menu is not a sign that the phone cannot hold the limit
            average < LOADING_FPS -> {
                steadySeconds = 0
            }
            average < cap * 0.85 -> {
                steadySeconds = 0
                val holdable = round10(average.toInt() + 5).coerceIn(MIN_FPS, cap)
                val target = if (holdable >= cap) cap - STEP else holdable
                if (target >= MIN_FPS && target < cap) {
                    holdSeconds = if (now - lastLowerAt < 5 * 60_000L) {
                        (holdSeconds * 2).coerceAtMost(HOLD_MAX_SECONDS)
                    } else {
                        HOLD_START_SECONDS
                    }
                    lowerTo(target, now, "load (${average.toInt()} fps)")
                }
            }
            average >= cap - 2 -> {
                steadySeconds++
                if (steadySeconds >= SAVE_AFTER_STEADY_SECONDS) {
                    steadySeconds = 0
                    rememberStable(cap)
                }
                if (cap < hardCap && now >= holdUntil) {
                    cap = minOf(cap + STEP, hardCap)
                    samples.clear()
                    holdUntil = now + PROBE_PAUSE_SECONDS * 1000L
                    apply(cap)
                    Logger.info(TAG, "Trying $cap FPS")
                }
            }
            else -> steadySeconds = 0
        }
    }

    private fun lowerTo(target: Int, now: Long, reason: String) {
        cap = target.coerceAtLeast(MIN_FPS)
        samples.clear()
        steadySeconds = 0
        lastLowerAt = now
        holdUntil = now + holdSeconds * 1000L
        apply(cap)
        Logger.info(TAG, "Limit lowered to $cap FPS: $reason")
    }

    private fun apply(fps: Int) {
        if (inBackground && mode.quietBackground) return
        ZLBridge.setFpsLimit(fps)
        FpsGovernorState.targetFps = fps
        applyRefresh(fps)
    }

    /** What the phone held: it is where the next game starts */
    private fun rememberStable(fps: Int) {
        if (fps != lastSavedCalibration) {
            lastSavedCalibration = fps
            AllSettings.tokitoCalibratedFps.save(fps)
        }
    }

    private fun saveCalibration() {
        if (cap > 0 && steadySeconds > 0) rememberStable(cap)
    }

    /**
     * Heat of the phone from 0 (cool) to 4 (critical): the thermal state that Android reports (Android 10+) or
     * the temperature of the battery, whichever is higher.
     */
    private fun readHeat(): Int {
        var heat = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val status = context.getSystemService(PowerManager::class.java)?.currentThermalStatus ?: 0
            heat = when {
                status <= PowerManager.THERMAL_STATUS_NONE -> 0
                status == PowerManager.THERMAL_STATUS_LIGHT -> 1
                status == PowerManager.THERMAL_STATUS_MODERATE -> 2
                status == PowerManager.THERMAL_STATUS_SEVERE -> 3
                else -> 4
            }
        }
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tenths = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val byBattery = when {
            tenths >= 500 -> 4
            tenths >= 470 -> 3
            tenths >= 440 -> 2
            tenths >= 410 -> 1
            else -> 0
        }
        return maxOf(heat, byBattery)
    }

    companion object {
        private const val STEP = 10
        private const val MIN_FPS = 30
        private const val DEFAULT_START_FPS = 60
        private const val PROBE_ABOVE_BENCHMARK = 20
        private const val BACKGROUND_FPS = 10
        private const val LOADING_FPS = 15
        private const val WINDOW_SECONDS = 4
        private const val HOLD_START_SECONDS = 60
        private const val HOLD_MAX_SECONDS = 300
        private const val PROBE_PAUSE_SECONDS = 8
        private const val SAVE_AFTER_STEADY_SECONDS = 20

        private fun round10(value: Int): Int = value / 10 * 10

        /**
         * Highest FPS that is fine for a heat level, in steps of 10: warm = 80, hot = 70 (or 10 below the ceiling
         * on phones that stay under it), very hot = 50 (or 20 below), critical = 30.
         */
        fun heatCap(ceiling: Int, heat: Int): Int {
            val raw = when {
                heat <= 0 -> ceiling
                heat == 1 -> minOf(ceiling, 80)
                heat == 2 -> minOf(ceiling - 10, 70)
                heat == 3 -> minOf(ceiling - 20, 50)
                else -> MIN_FPS
            }
            return round10(raw).coerceAtLeast(MIN_FPS)
        }
    }
}
