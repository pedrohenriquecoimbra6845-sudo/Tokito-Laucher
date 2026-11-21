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
import androidx.compose.runtime.mutableStateOf
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

    /** An old phone (2020 or before) that is hot or had to go below 60 FPS: the player is warned */
    var danger by mutableStateOf(false)

    fun reset() {
        targetFps = 0
        currentFps = 0
        heat = 0
        danger = false
    }
}

/**
 * Tokito Launcher: the FPS governor of the optimization modes. It runs in the game process and does what the
 * old modes did by changing the options of the game, but on the side of the launcher:
 *
 * - The FPS limit is set in the native frame limiter ([ZLBridge.setFpsLimit]) and moves **in steps of 10**
 *   (5 on Xiaomi, Infinix and RedMagic, see [DeviceProfile]).
 * - **Floor:** the limit does not go below 60 FPS. Only phones from 2020 or before may go lower, and then
 *   the player is warned that the phone is in danger ([FpsGovernorState.danger]).
 * - **Heat:** hot phone, lower limit (about 80, then 70, then 60; see [heatCap]).
 * - **Real FPS:** when the phone cannot hold the limit, the limit goes down to what it holds (steady frames
 *   are better than frames that jump). When it holds it for a while, it tries one step more. A limit that
 *   failed is not tried again for a long time, so the FPS stays locked instead of going up and down.
 * - **Same Hz as the FPS:** only when the player turns it on (`tokitoSyncHz`): at every change the screen is
 *   put at the refresh rate that fits the limit.
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
    private val applyRefresh: (fps: Int) -> Unit,
    private val onDanger: () -> Unit = {}
) {
    private var job: Job? = null

    /** The FPS limit moves in steps of this many FPS (10, or 5 on some brands) */
    private val step = DeviceProfile.fpsStep

    /** Lowest FPS limit on this phone (60, or lower on a phone from 2020 or before) */
    private val floor = DeviceProfile.floorFps

    /** Lowest limit that failed to hold; it and everything above it is not tried until [failedUntil] */
    private var failedAt = Int.MAX_VALUE
    private var failedUntil = 0L
    private var lastDangerWarning = 0L
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

    /** "Light and fast chunks" is on: dips of a chunk that loads are even less able to lower the limit */
    private var lightChunks = false
    private var startedAt = 0L

    fun start() {
        if (job?.isActive == true) return
        mode = OptimizationProfile.currentMode()
        if (!mode.limitsFps) {
            ZLBridge.setFpsLimit(0)
            FpsGovernorState.reset()
            return
        }
        failedAt = Int.MAX_VALUE
        lightChunks = OptimizationProfile.lightChunks(mode)
        startedAt = SystemClock.elapsedRealtime()
        ceiling = computeCeiling()
        val calibrated = AllSettings.tokitoCalibratedFps.getValue()
        val benchmark = AllSettings.tokitoBenchmarkFps.getValue()
        val start = if (calibrated > 0) calibrated else if (benchmark > 0) benchmark else DEFAULT_START_FPS
        lastSavedCalibration = calibrated
        val heat = readHeat()
        cap = DeviceProfile.roundToStep(minOf(start, heatCap(ceiling, heat), ceiling)).coerceAtLeast(floor)
        apply(cap)
        Logger.info(TAG, "Mode $mode started: ceiling=$ceiling start=$cap heat=$heat")

        job = scope.launch {
            while (isActive) {
                delay(1000L)
                runCatching { tick() }.onFailure { Logger.warning(TAG, "Governor tick failed", it) }
            }
        }
    }

    /** The mode was changed while the game runs (from the game menu): start again with the new one */
    fun restart() {
        stop()
        start()
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

    /**
     * Highest FPS the governor may try: what the screen can show (the benchmark only decides where it starts,
     * the governor finds the real limit by itself, so the phone is never held below what it can do).
     */
    private fun computeCeiling(): Int {
        val limit = minOf(maxDisplayHz(), mode.fpsCeilingCap)
        return DeviceProfile.roundToStep(limit).coerceAtLeast(floor)
    }

    /** An old phone that is hot or below 60 FPS: warn the player (at most once in a while) */
    private fun updateDanger(heat: Int, now: Long) {
        val danger = DeviceProfile.isOldPhone && (heat >= 3 || (cap in 1 until DANGER_BELOW_FPS))
        if (danger != FpsGovernorState.danger) FpsGovernorState.danger = danger
        if (danger && now - lastDangerWarning > DANGER_WARNING_GAP_MS) {
            lastDangerWarning = now
            onDanger()
        }
    }

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        val heat = readHeat()
        val fps = CallbackBridge.getCurrentFps()
        FpsGovernorState.heat = heat
        FpsGovernorState.currentFps = fps
        FpsGovernorState.displayHz = currentDisplayHz()
        updateDanger(heat, now)
        if (inBackground || fps <= 0) return

        //1. heat
        val hardCap = minOf(ceiling, heatCap(ceiling, heat))
        if (cap > hardCap) {
            lowerTo(hardCap, now, "heat $heat")
            return
        }

        //2. what the phone really holds
        val windowSeconds = if (lightChunks) LIGHT_WINDOW_SECONDS else WINDOW_SECONDS
        samples.addLast(fps)
        while (samples.size > windowSeconds) samples.removeFirst()
        if (samples.size < windowSeconds) return
        val average = samples.average()
        //the middle value of the window: a chunk that loads makes the FPS fall for a second or two,
        //and that dip alone must not lower the limit (the limit only goes down when most of the window is low)
        val typical = samples.sorted()[samples.size / 2]
        //the game is still loading (world, resources): the FPS of this time says nothing about the phone
        val loadingGrace = now - startedAt < GRACE_SECONDS * 1000L

        when {
            //loading the world or a menu is not a sign that the phone cannot hold the limit
            typical < LOADING_FPS -> {
                steadySeconds = 0
            }
            typical < cap * 0.85 -> {
                steadySeconds = 0
                if (loadingGrace) return
                val holdable = DeviceProfile.roundToStep(typical + step / 2).coerceIn(floor, cap)
                val wanted = if (holdable >= cap) cap - step else holdable
                //it goes down at most two steps at a time, so the FPS does not fall at once
                val target = maxOf(wanted, cap - 2 * step).coerceAtLeast(floor)
                if (target >= floor && target < cap) {
                    //this limit failed: neither it nor a higher one is tried again for a good while
                    failedAt = minOf(failedAt, cap)
                    failedUntil = now + FAILED_BAN_SECONDS * 1000L
                    holdSeconds = if (now - lastLowerAt < 5 * 60_000L) {
                        (holdSeconds * 2).coerceAtMost(HOLD_MAX_SECONDS)
                    } else {
                        HOLD_START_SECONDS
                    }
                    lowerTo(target, now, "load ($typical fps)")
                }
            }
            average >= cap - 2 -> {
                steadySeconds++
                if (steadySeconds >= SAVE_AFTER_STEADY_SECONDS) {
                    steadySeconds = 0
                    rememberStable(cap)
                }
                if (now >= failedUntil) failedAt = Int.MAX_VALUE
                val next = minOf(cap + step, hardCap)
                if (next > cap && next < failedAt && now >= holdUntil) {
                    cap = next
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
        cap = target.coerceAtLeast(floor)
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

    /**
     * Highest FPS that is fine for a heat level: warm = 80, hot = 70, very hot = 60 (50 on a phone from 2020
     * or before), critical = the floor of the phone. Always in steps of the phone and never below the floor.
     */
    private fun heatCap(ceiling: Int, heat: Int): Int {
        val raw = when {
            heat <= 0 -> ceiling
            heat == 1 -> minOf(ceiling, 80)
            heat == 2 -> minOf(ceiling, 70)
            heat == 3 -> minOf(ceiling, if (DeviceProfile.isOldPhone) 50 else 60)
            else -> floor
        }
        return DeviceProfile.roundToStep(raw).coerceAtLeast(floor)
    }

    companion object {
        private const val DEFAULT_START_FPS = 60
        private const val BACKGROUND_FPS = 10
        private const val LOADING_FPS = 15
        private const val WINDOW_SECONDS = 4
        private const val LIGHT_WINDOW_SECONDS = 10
        private const val GRACE_SECONDS = 45
        private const val HOLD_START_SECONDS = 60
        private const val HOLD_MAX_SECONDS = 600
        private const val FAILED_BAN_SECONDS = 600
        private const val PROBE_PAUSE_SECONDS = 8
        private const val SAVE_AFTER_STEADY_SECONDS = 20
        private const val DANGER_BELOW_FPS = 60
        private const val DANGER_WARNING_GAP_MS = 3 * 60_000L
    }
}
