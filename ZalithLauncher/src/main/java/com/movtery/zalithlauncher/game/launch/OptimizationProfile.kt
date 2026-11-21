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

import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.OptimizationMode
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

private const val TAG = "OptimizationProfile"

/**
 * Tokito Launcher: what the optimization modes and "light and fast chunks" change.
 *
 * Everything here is on the side of the launcher and of the phone. **Nothing is written in the options of
 * the game** (`options.txt`): the render distance, the shaders and the graphics stay as the player chose.
 *
 * - **FPS governor** (see [FpsGovernor]): frame limiter of the launcher in steps of 10 FPS, lowered when the
 *   phone gets hot or cannot hold the FPS, raised again when it can; the screen is put at the same Hz.
 * - **Sustained performance** while the game runs.
 * - **Shader cache** bigger for the Mesa based renderers (Survival and Brutal).
 * - **Light and fast chunks** (Brutal, or its own switch): JVM arguments that make the garbage collector
 *   pause less, so a chunk that loads does not freeze the game, and more headroom against out of memory.
 *
 * How many FPS it gives depends on the phone and on what is the bottleneck of it (CPU, GPU or memory).
 */
object OptimizationProfile {
    private const val BACKUP_FILE_NAME = "tokito_opt_backup.txt"
    private const val LEGACY_PVP_BACKUP_FILE_NAME = "tokito_pvp_backup.txt"

    /** Written in the backup for an option that was not in `options.txt` */
    private const val ABSENT = "~absent~"

    /** Size limit of the shader cache of the Mesa based renderers while Survival or Brutal is on */
    private const val BIG_SHADER_CACHE = "2G"

    fun currentMode(): OptimizationMode = AllSettings.launcherOptimizationMode.getValue()

    /** "Light and fast chunks": its own switch, or part of the mode that is on */
    fun lightChunks(mode: OptimizationMode = currentMode()): Boolean =
        mode.includesLightChunks || AllSettings.tokitoLightChunks.getValue()

    fun wantsSustainedPerformance(): Boolean = currentMode().keepsSteadyClock

    /**
     * Environment variables of the Mesa based renderers.
     */
    fun applyShaderCacheEnv(envMap: MutableMap<String, String>) {
        if (currentMode().growsShaderCache && !envMap.containsKey("MESA_SHADER_CACHE_MAX_SIZE")) {
            envMap["MESA_SHADER_CACHE_MAX_SIZE"] = BIG_SHADER_CACHE
        }
    }

    /**
     * JVM arguments of the optimization modes. They are only added when the player did not set the same thing,
     * and `IgnoreUnrecognizedVMOptions` makes the JVM skip an option that it does not know (the JVMs of the
     * launcher are not all the same).
     *
     * - **Fast boot** (every mode but Off): the bytecode of the game is not verified again at every start
     *   (it is the same jar that already ran), which makes the game open sooner.
     * - **Light and fast chunks:** the G1 garbage collector tuned for short pauses. A chunk that loads makes a lot
     *   of short lived objects; with these settings the collector cleans them in small bites, so the game does not
     *   freeze (and the FPS does not fall) at the moment a chunk arrives.
     */
    fun applyJvmArgs(args: MutableList<String>) {
        val mode = currentMode()
        val light = lightChunks(mode)
        if (!mode.isActive && !light) return
        fun has(prefix: String) = args.any { it.startsWith(prefix) }

        if (!has("-XX:+IgnoreUnrecognizedVMOptions") && !has("-XX:-IgnoreUnrecognizedVMOptions")) {
            args.add("-XX:+IgnoreUnrecognizedVMOptions")
        }

        if (mode.isActive && !has("-XX:-BytecodeVerificationRemote") && !has("-XX:+BytecodeVerificationRemote")) {
            args.add("-XX:-BytecodeVerificationRemote")
        }

        if (!light) return
        if (!has("-XX:+UnlockExperimentalVMOptions")) args.add("-XX:+UnlockExperimentalVMOptions")
        if (!has("-XX:+UseG1GC") && !has("-XX:+UseSerialGC") && !has("-XX:+UseParallelGC") &&
            !has("-XX:+UseZGC") && !has("-XX:+UseShenandoahGC") && !has("-XX:+UseConcMarkSweepGC")
        ) {
            args.add("-XX:+UseG1GC")
        }
        //shorter garbage collector pauses
        if (!has("-XX:MaxGCPauseMillis")) args.add("-XX:MaxGCPauseMillis=30")
        if (!has("-XX:G1NewSizePercent")) args.add("-XX:G1NewSizePercent=30")
        if (!has("-XX:G1MaxNewSizePercent")) args.add("-XX:G1MaxNewSizePercent=45")
        if (!has("-XX:G1ReservePercent")) args.add("-XX:G1ReservePercent=20")
        if (!has("-XX:G1MixedGCCountTarget")) args.add("-XX:G1MixedGCCountTarget=4")
        if (!has("-XX:InitiatingHeapOccupancyPercent")) args.add("-XX:InitiatingHeapOccupancyPercent=20")
        if (!has("-XX:G1MixedGCLiveThresholdPercent")) args.add("-XX:G1MixedGCLiveThresholdPercent=90")
        if (!has("-XX:G1RSetUpdatingPauseTimePercent")) args.add("-XX:G1RSetUpdatingPauseTimePercent=5")
        if (!has("-XX:SurvivorRatio")) args.add("-XX:SurvivorRatio=32")
        if (!has("-XX:MaxTenuringThreshold")) args.add("-XX:MaxTenuringThreshold=1")
        if (!has("-XX:+ParallelRefProcEnabled") && !has("-XX:-ParallelRefProcEnabled")) {
            args.add("-XX:+ParallelRefProcEnabled")
        }
        //cleans the classes that are no longer used, so the memory of the game does not fill up with them
        if (!has("-XX:+ClassUnloading") && !has("-XX:-ClassUnloading")) args.add("-XX:+ClassUnloading")
        Logger.info(TAG, "Light and fast chunks: JVM arguments added")
    }

    /**
     * Before 0.11.5 the modes changed `options.txt`. This puts back what an old launch changed
     * (and the backup of the old PVP mode), so the options are the ones of the player again.
     * It only changes values in memory: the caller saves them with [MCOptions.save].
     */
    fun restoreOldOptionChanges(version: Version) {
        val gameDir = runCatching { version.getGameDir() }.getOrNull() ?: return
        restoreLegacyPvpBackup(File(gameDir, LEGACY_PVP_BACKUP_FILE_NAME))
        restorePreviousChanges(File(gameDir, BACKUP_FILE_NAME))
    }

    /**
     * Puts back the options saved by an old launch. An option that the player changed in the game
     * after that (it no longer has the value that was written) is left as the player chose.
     */
    private fun restorePreviousChanges(backupFile: File) {
        if (!backupFile.exists()) return
        runCatching {
            backupFile.readLines().forEach { line ->
                val parts = line.split('\t', limit = 3)
                if (parts.size != 3) return@forEach
                val (key, originalValue, appliedValue) = parts
                if (MCOptions.get(key) != appliedValue) return@forEach
                if (originalValue == ABSENT) {
                    MCOptions.remove(key)
                } else {
                    MCOptions.set(key, originalValue)
                }
            }
        }.onFailure {
            Logger.warning(TAG, "Failed to restore the options changed by an old optimization mode!", it)
        }
        backupFile.delete()
    }

    /** The backup file of the PVP mode of Tokito Launcher 0.11.3 (`key:value` lines) */
    private fun restoreLegacyPvpBackup(file: File) {
        if (!file.exists()) return
        runCatching {
            file.readLines().forEach { line ->
                val index = line.indexOf(':')
                if (index > 0) {
                    MCOptions.set(line.take(index), line.substring(index + 1))
                }
            }
        }.onFailure {
            Logger.warning(TAG, "Failed to restore the options of the old PVP mode!", it)
        }
        file.delete()
    }
}
