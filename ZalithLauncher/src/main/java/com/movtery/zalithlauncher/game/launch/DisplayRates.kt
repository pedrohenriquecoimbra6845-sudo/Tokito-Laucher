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

import android.view.Display
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tokito Launcher: refresh rates of the screen. The phones that have less than 120 Hz are the same as the
 * others: the list of modes is read from the screen itself, nothing is assumed.
 */
object DisplayRates {
    /** Modes with the same resolution as the one in use (the rate changes, the size does not) */
    fun sameResolutionModes(display: Display): List<Display.Mode> {
        val current = display.mode
        return display.supportedModes.filter {
            it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight
        }.ifEmpty { listOf(current) }
    }

    /** Highest refresh rate of the screen, in whole Hz */
    fun maxHz(display: Display): Int {
        val rate = sameResolutionModes(display).maxOfOrNull { it.refreshRate } ?: display.refreshRate
        return rate.roundToInt().coerceAtLeast(30)
    }

    /**
     * The mode that fits [fps] best: the lowest rate that is a whole multiple of [fps] (60 FPS on 120 Hz,
     * 45 on 90...), otherwise the lowest rate that is not below it, otherwise the highest one.
     */
    fun bestModeFor(display: Display, fps: Int): Display.Mode {
        val modes = sameResolutionModes(display).sortedBy { it.refreshRate }
        val multiple = modes.firstOrNull { mode ->
            val rate = mode.refreshRate
            val times = (rate / fps).roundToInt()
            rate >= fps - 0.5f && times >= 1 && abs(rate - times * fps) < 0.6f
        }
        return multiple
            ?: modes.firstOrNull { it.refreshRate >= fps - 0.5f }
            ?: modes.last()
    }
}
