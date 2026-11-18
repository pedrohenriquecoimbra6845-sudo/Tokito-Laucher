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
 * Tokito Launcher: optimization modes (what each one changes is in `OptimizationProfile`).
 *
 * - [OFF]: nothing is changed.
 * - [PVP]: steady FPS (locked at the refresh rate), smaller launcher DPI and character.
 * - [SURVIVAL]: fast and light chunks, bigger shader cache.
 * - [BRUTAL]: everything of the other modes, plus a lower render resolution and lighter visuals,
 *   to keep a lot of chunks playable.
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
    BRUTAL(R.string.tokito_opt_brutal, R.string.tokito_opt_chip_brutal, R.string.tokito_opt_brutal_desc);

    /** The game FPS is locked at the refresh rate of the screen and v-sync is off */
    val locksFps: Boolean get() = this == PVP || this == BRUTAL

    /** Lower DPI of the launcher and a smaller character on the home screen */
    val shrinksLauncher: Boolean get() = this == PVP || this == BRUTAL

    /** Sustained performance mode and the highest refresh rate while the game runs */
    val keepsSteadyClock: Boolean get() = this == PVP || this == BRUTAL

    /** The tweaks of "light and fast chunks" are part of the mode */
    val includesLightChunks: Boolean get() = this == SURVIVAL || this == BRUTAL

    /** Bigger shader cache for the Mesa based renderers */
    val growsShaderCache: Boolean get() = this == SURVIVAL || this == BRUTAL

    /** Whether the mode is on */
    val isActive: Boolean get() = this != OFF
}
