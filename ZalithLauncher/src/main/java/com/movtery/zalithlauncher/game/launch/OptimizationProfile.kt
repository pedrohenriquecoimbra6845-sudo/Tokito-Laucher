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

import android.app.Activity
import android.os.Build
import android.view.Display
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.OptimizationMode
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File
import kotlin.math.roundToInt

private const val TAG = "OptimizationProfile"

/**
 * Tokito Launcher: what the optimization modes and "light and fast chunks" change.
 *
 * Nothing here is a magic switch: every change is a setting that really lowers the work the phone has
 * to do (fewer or lighter chunks, less to draw, steadier clocks). How many FPS that gives depends on
 * the phone and on what is the bottleneck of it (CPU, GPU or memory).
 *
 * - **PVP**: FPS locked at the refresh rate (v-sync off), sustained performance, highest refresh rate,
 *   smaller launcher DPI and character.
 * - **Light and fast chunks**: lower simulation distance, cheaper chunk meshing (no smooth lighting, small
 *   biome blend, fast graphics, no entity shadows) and threaded chunk updates.
 * - **Survival**: light chunks + a render distance cap, fewer entity details and particles, bigger shader
 *   cache for the Mesa based renderers.
 * - **Brutal**: everything above (PVP + Survival), but the render distance is not capped. To make a lot
 *   of chunks playable it also lowers the render resolution to 80%, the simulation distance, the entity
 *   distance, the particles and turns the clouds off. Cost: a simpler picture and a hotter phone.
 *
 * The values it changes in `options.txt` are written to a small backup file inside the game folder. The
 * next time the game starts they are put back, except the ones that the player changed in the game.
 */
object OptimizationProfile {
    private const val BACKUP_FILE_NAME = "tokito_opt_backup.txt"
    private const val LEGACY_PVP_BACKUP_FILE_NAME = "tokito_pvp_backup.txt"

    /** Written in the backup for an option that was not in `options.txt` */
    private const val ABSENT = "~absent~"

    private const val MIN_FPS = 30
    private const val MAX_FPS = 260 //Minecraft treats 260 as "unlimited"

    /** Share of the chosen resolution that the Brutal mode renders */
    private const val BRUTAL_RESOLUTION_SCALE = 0.8f
    private const val MIN_RESOLUTION_PERCENT = 25

    /** Size limit of the shader cache of the Mesa based renderers while Survival or Brutal is on */
    private const val BIG_SHADER_CACHE = "2G"

    fun currentMode(): OptimizationMode = AllSettings.launcherOptimizationMode.getValue()

    /** "Light and fast chunks": its own switch, or part of the mode that is on */
    fun lightChunks(mode: OptimizationMode = currentMode()): Boolean =
        mode.includesLightChunks || AllSettings.tokitoLightChunks.getValue()

    fun wantsSustainedPerformance(): Boolean = currentMode().keepsSteadyClock

    fun wantsMaxRefreshRate(): Boolean = currentMode().keepsSteadyClock

    /**
     * Resolution (percentage) the game really renders at. Only the Brutal mode lowers it.
     */
    fun effectiveResolutionPercent(percentage: Int, mode: OptimizationMode = currentMode()): Int {
        if (mode != OptimizationMode.BRUTAL) return percentage
        val lowered = (percentage * BRUTAL_RESOLUTION_SCALE).roundToInt()
        return lowered.coerceAtLeast(minOf(percentage, MIN_RESOLUTION_PERCENT))
    }

    /**
     * Environment variables of the Mesa based renderers.
     */
    fun applyShaderCacheEnv(envMap: MutableMap<String, String>) {
        if (currentMode().growsShaderCache && !envMap.containsKey("MESA_SHADER_CACHE_MAX_SIZE")) {
            envMap["MESA_SHADER_CACHE_MAX_SIZE"] = BIG_SHADER_CACHE
        }
    }

    // ---- options.txt ------------------------------------------------------------------------

    /**
     * One change of an option. It only touches an option that is in `options.txt` and has the
     * kind of value that the change expects, so a version that writes it in another way is left alone.
     */
    private sealed class Tweak(val key: String) {
        /** A number is lowered to [max] when it is higher */
        class Cap(key: String, val max: Double) : Tweak(key)

        /** A number is raised to [min] when it is lower */
        class Floor(key: String, val min: Int) : Tweak(key)

        /** A number is set to [value] */
        class Exact(key: String, val value: Int) : Tweak(key)

        /** A switch is turned off, whichever way the game writes it (true/false, 0/1, "true"/"false"/"fast") */
        class Off(key: String) : Tweak(key)
    }

    private val LIGHT_CHUNK_TWEAKS = listOf(
        Tweak.Cap("simulationDistance", 6.0),
        Tweak.Cap("biomeBlendRadius", 1.0),
        Tweak.Cap("mipmapLevels", 2.0),
        Tweak.Off("ao"),
        Tweak.Off("fancyGraphics"),
        Tweak.Exact("graphicsMode", 0),
        Tweak.Exact("prioritizeChunkUpdates", 0),
        Tweak.Off("entityShadows")
    )

    private val SURVIVAL_TWEAKS = listOf(
        Tweak.Cap("entityDistanceScaling", 0.75),
        Tweak.Floor("particles", 1)
    )

    private val BRUTAL_TWEAKS = listOf(
        Tweak.Cap("simulationDistance", 4.0),
        Tweak.Cap("entityDistanceScaling", 0.5),
        Tweak.Floor("particles", 2),
        Tweak.Cap("biomeBlendRadius", 0.0),
        Tweak.Cap("mipmapLevels", 1.0),
        Tweak.Off("renderClouds"),
        Tweak.Off("clouds")
    )

    /** Highest render distance (in chunks) while the mode is on; null means the player's value is kept */
    private fun renderDistanceCap(mode: OptimizationMode, lightChunks: Boolean): Double? = when {
        //the Brutal mode keeps a lot of chunks playable by making everything around them cheaper
        mode == OptimizationMode.BRUTAL -> null
        mode == OptimizationMode.SURVIVAL -> 10.0
        lightChunks -> 12.0
        else -> null
    }

    private fun tweaksFor(mode: OptimizationMode, lightChunks: Boolean): List<Tweak> {
        val tweaks = mutableListOf<Tweak>()
        renderDistanceCap(mode, lightChunks)?.let { tweaks += Tweak.Cap("renderDistance", it) }
        if (lightChunks) tweaks += LIGHT_CHUNK_TWEAKS
        if (mode == OptimizationMode.SURVIVAL || mode == OptimizationMode.BRUTAL) tweaks += SURVIVAL_TWEAKS
        if (mode == OptimizationMode.BRUTAL) tweaks += BRUTAL_TWEAKS
        return tweaks
    }

    /** @return the new text of the value, or null when it stays as it is */
    private fun Tweak.newValue(current: String): String? = when (this) {
        is Tweak.Cap -> {
            val number = current.trim().toDoubleOrNull()
            if (number != null && number > max) formatLike(current, max) else null
        }
        is Tweak.Floor -> {
            val number = current.trim().toDoubleOrNull()
            if (number != null && number < min) min.toString() else null
        }
        is Tweak.Exact -> {
            val number = current.trim().toDoubleOrNull()
            if (number != null && number != value.toDouble()) value.toString() else null
        }
        is Tweak.Off -> offValue(current)
    }

    /** Writes [number] the way [current] was written: a whole number or a decimal one */
    private fun formatLike(current: String, number: Double): String {
        return if (current.contains('.')) {
            number.toString()
        } else {
            number.roundToInt().toString()
        }
    }

    private fun offValue(current: String): String? {
        val raw = current.trim()
        val quoted = raw.length >= 2 && raw.startsWith("\"") && raw.endsWith("\"")
        val inner = if (quoted) raw.substring(1, raw.length - 1) else raw
        val off = when (inner.lowercase()) {
            "true", "fast", "fancy" -> "false"
            "false", "0" -> return null
            else -> if (inner.toIntOrNull() != null) "0" else return null
        }
        return if (quoted) "\"$off\"" else off
    }

    /**
     * Puts back what the last launch changed and then applies the mode that is on now, in the
     * `options.txt` that [MCOptions] has just loaded. It only changes values in memory:
     * the caller saves them with [MCOptions.save].
     */
    fun applyToGameOptions(activity: Activity, version: Version) {
        val gameDir = runCatching { version.getGameDir() }.getOrNull() ?: return
        val backupFile = File(gameDir, BACKUP_FILE_NAME)

        restoreLegacyPvpBackup(File(gameDir, LEGACY_PVP_BACKUP_FILE_NAME))
        restorePreviousChanges(backupFile)

        val mode = currentMode()
        val light = lightChunks(mode)
        if (!mode.isActive && !light) return

        val original = LinkedHashMap<String, String>()
        val applied = LinkedHashMap<String, String>()

        fun change(key: String, value: String) {
            if (!original.containsKey(key)) original[key] = MCOptions.get(key) ?: ABSENT
            applied[key] = value
        }

        if (mode.locksFps) {
            val fps = targetFps(activity)
            change("maxFps", fps.toString())
            change("enableVsync", "false")
        }

        tweaksFor(mode, light).forEach { tweak ->
            val current = applied[tweak.key] ?: MCOptions.get(tweak.key) ?: return@forEach
            tweak.newValue(current)?.let { change(tweak.key, it) }
        }

        //an option that ends with the value it started with does not need a backup
        val changed = applied.filter { (key, value) -> original[key] != value }
        if (changed.isEmpty()) return

        runCatching {
            backupFile.parentFile?.mkdirs()
            backupFile.writeText(
                changed.entries.joinToString("\n") { (key, value) ->
                    "$key\t${original[key]}\t$value"
                }
            )
        }.onFailure {
            //without the backup the options could not be put back, so nothing is changed
            Logger.warning(TAG, "Failed to back up the options before the optimization mode!", it)
            return
        }

        changed.forEach { (key, value) -> MCOptions.set(key, value) }
        Logger.info(TAG, "Optimization mode $mode (light chunks: $light) changed: $changed")
    }

    /**
     * Puts back the options saved by the last launch. An option that the player changed in the game
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
            Logger.warning(TAG, "Failed to restore the options after the optimization mode!", it)
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

    /**
     * FPS the game is locked at: the highest refresh rate the screen offers (same resolution).
     */
    fun targetFps(activity: Activity): Int {
        val display = currentDisplay(activity)
        val rate = display?.let {
            val current = it.mode
            it.supportedModes
                .filter { mode ->
                    mode.physicalWidth == current.physicalWidth &&
                        mode.physicalHeight == current.physicalHeight
                }
                .maxOfOrNull { mode -> mode.refreshRate }
                ?: it.refreshRate
        } ?: 60f
        return rate.roundToInt().coerceIn(MIN_FPS, MAX_FPS)
    }

    private fun currentDisplay(activity: Activity): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        }
    }
}
