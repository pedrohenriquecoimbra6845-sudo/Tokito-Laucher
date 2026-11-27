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

package com.movtery.zalithlauncher.setting.enums

import com.movtery.zalithlauncher.R

/**
 * Tokito Launcher: optimization modes. They work on the launcher and on the phone (frame limiter, refresh
 * rate, heat, shader cache, JVM). They never change the options inside the game (`options.txt`).
 * What each one does is in `OptimizationProfile` and `FpsGovernor`.
 *
 * - [OFF]: nothing is changed.
 * - [PVP]: steady FPS: the FPS limit moves in steps of 10 with the heat of the phone and with what the phone
 *   really holds, and the screen follows it (same Hz as the FPS).
 * - [SURVIVAL]: the same governor with a calmer ceiling, FPS calibrated outside the game, bigger shader cache
 *   and nothing of the launcher running in the background.
 * - [BRUTAL]: everything of the other modes plus light and fast chunks (JVM tuned so a chunk that loads does
 *   not freeze or crash the game).
 * - [LIVE]: for streaming (TikTok, YouTube, Twitch) while playing: the FPS never goes over 60 (what the video
 *   encoder of the phone follows well), steady clock, light chunks, bigger shader cache, and the game keeps its
 *   speed in the background (the live app may be in front). The Stream panel turns it on.
 *
 * @param labelRes full name of the mode
 * @param chipRes short name, shown on the chip of the home screen
 * @param descRes what the mode does
 */
enum class OptimizationMode(
    val labelRes: Int,
    val chipRes: Int,
    val descRes: Int
) {
    OFF(R.string.tokito_opt_off, R.string.tokito_opt_chip_off, R.string.tokito_opt_off_desc),
    PVP(R.string.tokito_opt_pvp, R.string.tokito_opt_chip_pvp, R.string.tokito_opt_pvp_desc),
    SURVIVAL(R.string.tokito_opt_survival, R.string.tokito_opt_chip_survival, R.string.tokito_opt_survival_desc),
    BRUTAL(R.string.tokito_opt_brutal, R.string.tokito_opt_chip_brutal, R.string.tokito_opt_brutal_desc),
    LIVE(R.string.tokito_opt_live, R.string.tokito_opt_chip_live, R.string.tokito_opt_live_desc);

    /** The FPS governor (limit in steps of 10, heat and real FPS) runs while the game is open */
    val limitsFps: Boolean get() = this != OFF

    /** Highest FPS of the governor: Survival is calmer (90) and Live stays where the video encoder of the phone follows (60) */
    val fpsCeilingCap: Int
        get() = when (this) {
            SURVIVAL -> 90
            LIVE -> 60
            else -> Int.MAX_VALUE
        }

    /** Lower DPI of the launcher and a smaller character on the home screen */
    val shrinksLauncher: Boolean get() = this == PVP || this == BRUTAL

    /** Sustained performance mode while the game runs */
    val keepsSteadyClock: Boolean get() = this != OFF

    /** Bigger shader cache for the Mesa based renderers */
    val growsShaderCache: Boolean get() = this == SURVIVAL || this == BRUTAL || this == LIVE

    /** Light and fast chunks: JVM tuned against freezes and crashes when a chunk loads */
    val includesLightChunks: Boolean get() = this == BRUTAL || this == LIVE

    /** While the game is in the background the limit drops to 10 FPS (less heat and battery) and comes back when it returns */
    val quietBackground: Boolean get() = this == SURVIVAL || this == BRUTAL

    /** Whether the mode is on */
    val isActive: Boolean get() = this != OFF
}
