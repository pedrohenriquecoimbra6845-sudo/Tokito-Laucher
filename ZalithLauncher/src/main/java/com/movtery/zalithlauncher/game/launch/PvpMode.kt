/*
 * Tokito Laucher
 * Based on Zalith Launcher 2 (Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors).
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
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File
import kotlin.math.roundToInt

private const val TAG = "PvpMode"

/**
 * Tokito Laucher: PVP mode.
 *
 * While it is on:
 * - the game FPS is locked at the highest refresh rate of the screen (frame limiter = refresh rate,
 *   v-sync off) so the frame rate stays steady instead of swinging;
 * - the launcher asks the system for the highest refresh rate and sustained performance;
 * - the launcher UI gets a slightly lower DPI and the character on the home screen is smaller
 *   (see `ZalithLauncherTheme` and `TokitoHero`).
 *
 * The two `options.txt` values it changes are saved in a small file inside the game folder, and put
 * back the next time the game starts with the PVP mode off.
 */
object PvpMode {
    private const val BACKUP_FILE_NAME = "tokito_pvp_backup.txt"

    //Minecraft defaults, used when the option was not in options.txt
    private const val DEFAULT_MAX_FPS = "120"
    private const val DEFAULT_VSYNC = "true"

    private const val MIN_FPS = 30
    private const val MAX_FPS = 260 //Minecraft treats 260 as "unlimited"

    fun isEnabled(): Boolean = AllSettings.launcherPvpMode.getValue()

    /**
     * Applies (or undoes) the PVP mode in the `options.txt` that [MCOptions] has just loaded.
     * It only changes values in memory: the caller saves them with [MCOptions.save].
     */
    fun applyToGameOptions(activity: Activity, version: Version) {
        val backup = runCatching { File(version.getGameDir(), BACKUP_FILE_NAME) }.getOrNull() ?: return

        if (isEnabled()) {
            if (!backup.exists()) {
                runCatching {
                    backup.parentFile?.mkdirs()
                    backup.writeText(
                        "maxFps:${MCOptions.get("maxFps") ?: DEFAULT_MAX_FPS}\n" +
                            "enableVsync:${MCOptions.get("enableVsync") ?: DEFAULT_VSYNC}"
                    )
                }.onFailure {
                    Logger.warning(TAG, "Failed to back up the options before the PVP mode!", it)
                }
            }
            val fps = targetFps(activity)
            MCOptions.set("maxFps", fps.toString())
            MCOptions.set("enableVsync", "false")
            Logger.info(TAG, "PVP mode on: FPS locked at $fps")
        } else if (backup.exists()) {
            runCatching {
                backup.readLines().forEach { line ->
                    val index = line.indexOf(':')
                    if (index > 0) {
                        MCOptions.set(line.take(index), line.substring(index + 1))
                    }
                }
            }.onFailure {
                Logger.warning(TAG, "Failed to restore the options after the PVP mode!", it)
            }
            backup.delete()
            Logger.info(TAG, "PVP mode off: game options restored")
        }
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
