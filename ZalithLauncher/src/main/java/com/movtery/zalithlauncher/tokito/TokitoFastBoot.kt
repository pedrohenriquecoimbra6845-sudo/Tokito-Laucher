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

import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

private const val TAG = "TokitoFastBoot"

/**
 * Tokito Launcher: opening the game sooner.
 *
 * Before every launch the launcher checks (and repairs) all the files of the game, which on a phone can take a
 * good part of the 35 seconds. When the same version was checked without problems in the last days and its main
 * files are as they were (same size), the check is skipped. The check always runs again when the version is new,
 * when its files changed, after [MAX_AGE_DAYS] days, or when the player turns "Fast boot" off.
 *
 * The other half of the fast boot is in [com.movtery.zalithlauncher.game.launch.OptimizationProfile.applyJvmArgs]
 * (the bytecode of the game is not verified again at every start).
 */
object TokitoFastBoot {
    private const val STAMP_FILE = "tokito_verified.txt"
    private const val MAX_AGE_DAYS = 7L

    fun enabled(): Boolean = AllSettings.tokitoFastBoot.getValue()

    /** The check of the files of [version] can be skipped this time */
    fun canSkipVerify(version: Version): Boolean {
        if (!enabled()) return false
        return runCatching {
            val stamp = File(version.getVersionPath(), STAMP_FILE)
            if (!stamp.exists()) return false
            val parts = stamp.readText().trim().split(';')
            if (parts.size != 3) return false
            val checkedAt = parts[0].toLong()
            val ageMs = System.currentTimeMillis() - checkedAt
            if (ageMs < 0 || ageMs > MAX_AGE_DAYS * 24 * 3600 * 1000L) return false
            val jar = version.getClientJar()
            jar.exists() && jar.length() == parts[1].toLong() && jar.lastModified() == parts[2].toLong()
        }.getOrDefault(false)
    }

    /** The files of [version] were checked now and are fine */
    fun markVerified(version: Version) {
        if (!enabled()) return
        runCatching {
            val jar = version.getClientJar()
            if (!jar.exists()) return
            File(version.getVersionPath(), STAMP_FILE)
                .writeText("${System.currentTimeMillis()};${jar.length()};${jar.lastModified()}")
        }.onFailure {
            Logger.warning(TAG, "Could not save the check of the game files", it)
        }
    }
}
