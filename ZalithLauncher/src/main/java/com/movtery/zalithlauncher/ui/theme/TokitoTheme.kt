/*
 * Tokito Laucher - unofficial modified version of Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors (Zalith Launcher 2)
 * Modifications Copyright (C) 2026 Tokito Laucher contributors
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

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * "Tokito Rose": original theme of Tokito Laucher.
 * Deep plum surfaces with hot pink, purple and red accents (the colors of the app icon).
 * All values were chosen for this project.
 */
object TokitoColors {
    val Red = Color(0xFFE3123F)
    val Pink = Color(0xFFFF4FA3)
    val PinkSoft = Color(0xFFFFB6DA)
    val Purple = Color(0xFFA25CFF)
    val PurpleDeep = Color(0xFF6C2BD9)
    val Page = Color(0xFF130813)
    val Text = Color(0xFFFFE9F3)
}

internal val tokitoDark: ColorScheme = darkColorScheme(
    primary = Color(0xFFFF4FA3),
    onPrimary = Color(0xFF3F0022),
    primaryContainer = Color(0xFF7A1650),
    onPrimaryContainer = Color(0xFFFFD6E8),
    secondary = Color(0xFFC7A3FF),
    onSecondary = Color(0xFF2B0E55),
    secondaryContainer = Color(0xFF47257F),
    onSecondaryContainer = Color(0xFFEADCFF),
    tertiary = Color(0xFFFF7A8A),
    onTertiary = Color(0xFF560014),
    tertiaryContainer = Color(0xFF8A1830),
    onTertiaryContainer = Color(0xFFFFDADE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = TokitoColors.Page,
    onBackground = TokitoColors.Text,
    surface = TokitoColors.Page,
    onSurface = TokitoColors.Text,
    surfaceVariant = Color(0xFF2B1632),
    onSurfaceVariant = Color(0xFFD9B8D0),
    outline = Color(0xFF9B6F92),
    outlineVariant = Color(0xFF4B2C4A),
    scrim = Color(0xFF000000),
    inverseSurface = TokitoColors.Text,
    inverseOnSurface = TokitoColors.Page,
    inversePrimary = Color(0xFFB0236B),
    surfaceDim = Color(0xFF0E050E),
    surfaceBright = Color(0xFF3B2142),
    surfaceContainerLowest = Color(0xFF0F060F),
    surfaceContainerLow = Color(0xFF1A0D1C),
    surfaceContainer = Color(0xFF231328),
    surfaceContainerHigh = Color(0xFF2D1B33),
    surfaceContainerHighest = Color(0xFF38243F),
)

internal val tokitoLight: ColorScheme = lightColorScheme(
    primary = Color(0xFFC21A68),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E8),
    onPrimaryContainer = Color(0xFF3E001F),
    secondary = Color(0xFF7B3FD0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEBDDFF),
    onSecondaryContainer = Color(0xFF260056),
    tertiary = Color(0xFFC0182F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDADC),
    onTertiaryContainer = Color(0xFF410008),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF3F8),
    onBackground = Color(0xFF26101C),
    surface = Color(0xFFFFF3F8),
    onSurface = Color(0xFF26101C),
    surfaceVariant = Color(0xFFF6DCE9),
    onSurfaceVariant = Color(0xFF574050),
    outline = Color(0xFF8A6F80),
    outlineVariant = Color(0xFFDBC1D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF3B2535),
    inverseOnSurface = Color(0xFFFFECF4),
    inversePrimary = Color(0xFFFF4FA3),
    surfaceDim = Color(0xFFEBD3DF),
    surfaceBright = Color(0xFFFFF8FB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFEAF3),
    surfaceContainer = Color(0xFFFBE2ED),
    surfaceContainerHigh = Color(0xFFF5DAE7),
    surfaceContainerHighest = Color(0xFFEED3E0),
)
