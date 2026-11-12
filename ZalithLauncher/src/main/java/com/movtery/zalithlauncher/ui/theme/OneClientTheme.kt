/*
 * PolyJav Laucher - unofficial modified version of Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors (Zalith Launcher 2)
 * Modifications Copyright (C) 2026 PolyJav Laucher contributors
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
 * Colors of the "OneClient" look used by PolyJav Laucher.
 *
 * The base values (page, card, text and the blue accent) follow the "PolyGlass" theme
 * of OneClient by Polyfrost (https://polyfrost.org/projects/oneclient, GPL-3.0):
 *  - dark:  page #11171C, elevated #151C22, card #1A2229, text #D5DBFF, accent #2B4BFF
 *  - light: page #D0D7F3, elevated #D9E0F7, card #E3EAFB, component #EBF2FF, accent #2B4BFF
 *
 * Only color values are reused here; no OneClient code or artwork is included.
 */
object OneClientColors {
    val Accent = Color(0xFF2B4BFF)

    // Dark ("PolyGlass Dark")
    val DarkPage = Color(0xFF11171C)
    val DarkElevated = Color(0xFF151C22)
    val DarkCard = Color(0xFF1A2229)
    val DarkText = Color(0xFFD5DBFF)
    val DarkTextSecondary = Color(0xFF757883)

    // Light ("PolyGlass Light")
    val LightPage = Color(0xFFD0D7F3)
    val LightElevated = Color(0xFFD9E0F7)
    val LightCard = Color(0xFFE3EAFB)
    val LightComponent = Color(0xFFEBF2FF)
    val LightTextSecondary = Color(0xFF4E5661)
}

internal val oneClientDark: ColorScheme = darkColorScheme(
    primary = OneClientColors.Accent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E33B8),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFB7C0F0),
    onSecondary = Color(0xFF1C2447),
    secondaryContainer = Color(0xFF2A3358),
    onSecondaryContainer = Color(0xFFDDE1FF),
    tertiary = Color(0xFF7FD8FF),
    onTertiary = Color(0xFF00344A),
    tertiaryContainer = Color(0xFF004C6B),
    onTertiaryContainer = Color(0xFFC6EAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = OneClientColors.DarkPage,
    onBackground = OneClientColors.DarkText,
    surface = OneClientColors.DarkPage,
    onSurface = OneClientColors.DarkText,
    surfaceVariant = OneClientColors.DarkCard,
    onSurfaceVariant = Color(0xFF9AA0B8),
    outline = OneClientColors.DarkTextSecondary,
    outlineVariant = Color(0xFF2E373F),
    scrim = Color(0xFF000000),
    inverseSurface = OneClientColors.DarkText,
    inverseOnSurface = OneClientColors.DarkPage,
    inversePrimary = OneClientColors.Accent,
    surfaceDim = Color(0xFF0D1216),
    surfaceBright = Color(0xFF2D3842),
    surfaceContainerLowest = Color(0xFF0E1318),
    surfaceContainerLow = OneClientColors.DarkElevated,
    surfaceContainer = OneClientColors.DarkCard,
    surfaceContainerHigh = Color(0xFF202931),
    surfaceContainerHighest = Color(0xFF27313A),
)

internal val oneClientLight: ColorScheme = lightColorScheme(
    primary = OneClientColors.Accent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF00164F),
    secondary = Color(0xFF4A5AA8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE1FF),
    onSecondaryContainer = Color(0xFF0A1448),
    tertiary = Color(0xFF00677F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB8EAFF),
    onTertiaryContainer = Color(0xFF001F29),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = OneClientColors.LightPage,
    onBackground = Color(0xFF0A0E1A),
    surface = OneClientColors.LightPage,
    onSurface = Color(0xFF0A0E1A),
    surfaceVariant = OneClientColors.LightCard,
    onSurfaceVariant = OneClientColors.LightTextSecondary,
    outline = Color(0xFF7A8290),
    outlineVariant = Color(0xFFB3B9CF),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF11171C),
    inverseOnSurface = Color(0xFFD5DBFF),
    inversePrimary = Color(0xFFB7C0FF),
    surfaceDim = Color(0xFFC3CAE6),
    surfaceBright = Color(0xFFEBF2FF),
    surfaceContainerLowest = Color(0xFFF3F7FF),
    surfaceContainerLow = OneClientColors.LightElevated,
    surfaceContainer = OneClientColors.LightCard,
    surfaceContainerHigh = Color(0xFFE7EEFF),
    surfaceContainerHighest = OneClientColors.LightComponent,
)
