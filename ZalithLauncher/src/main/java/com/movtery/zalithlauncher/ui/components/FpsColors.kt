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

package com.movtery.zalithlauncher.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Tokito Launcher: the colors of the FPS numbers.
 *
 * - **Game ball:** 10 FPS is red, then orange (30), yellow (50), green (70), cyan (90), and from 100 FPS it is rainbow.
 * - **Benchmark:** red below 60 FPS, yellow from 60, green from 100 and rainbow from 200.
 *
 * A `null` color means rainbow (the text is painted with [RainbowBrush]).
 */
val FpsRed = Color(0xFFF44336)
val FpsOrange = Color(0xFFFF9800)
val FpsYellow = Color(0xFFFFD600)
val FpsGreen = Color(0xFF4CAF50)
val FpsCyan = Color(0xFF00BCD4)

val RainbowColors = listOf(
    Color(0xFFFF3B30),
    Color(0xFFFF9500),
    Color(0xFFFFE600),
    Color(0xFF34C759),
    Color(0xFF00C7FF),
    Color(0xFF5856D6),
    Color(0xFFFF2D95)
)

val RainbowBrush: Brush = Brush.horizontalGradient(RainbowColors)

private val GAME_STOPS = listOf(10 to FpsRed, 30 to FpsOrange, 50 to FpsYellow, 70 to FpsGreen, 90 to FpsCyan)

/** Color of the FPS under the flower of the game (null = rainbow, from 100 FPS) */
fun gameFpsColor(fps: Int): Color? {
    if (fps >= 100) return null
    val first = GAME_STOPS.first()
    if (fps <= first.first) return first.second
    for (index in 1 until GAME_STOPS.size) {
        val high = GAME_STOPS[index]
        if (fps <= high.first) {
            val low = GAME_STOPS[index - 1]
            val fraction = (fps - low.first) / (high.first - low.first).toFloat()
            return lerp(low.second, high.second, fraction)
        }
    }
    return GAME_STOPS.last().second
}

/** Color of the FPS of the benchmark: red under 60, yellow from 60, green from 100, rainbow from 200 (null) */
fun benchmarkFpsColor(fps: Int): Color? = when {
    fps >= 200 -> null
    fps >= 100 -> FpsGreen
    fps >= 60 -> FpsYellow
    else -> FpsRed
}

/**
 * A text painted with the color of the FPS, or with the rainbow when [color] is null.
 * A soft shadow keeps small numbers readable over the game.
 */
@Composable
fun FpsColoredText(
    text: String,
    color: Color?,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    shadow: Boolean = false
) {
    val shadowed = if (shadow) {
        style.copy(shadow = Shadow(Color.Black.copy(alpha = 0.85f), Offset(0f, 1.5f), 4f))
    } else {
        style
    }
    if (color != null) {
        Text(text = text, modifier = modifier, style = shadowed.copy(color = color), textAlign = textAlign)
    } else {
        Text(text = text, modifier = modifier, style = shadowed.copy(brush = RainbowBrush), textAlign = textAlign)
    }
}
