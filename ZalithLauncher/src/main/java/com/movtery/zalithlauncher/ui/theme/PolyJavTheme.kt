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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * "PolyJav Cyan": original dark theme with a cyan accent, deep teal surfaces and very rounded shapes.
 * All values were chosen for this project.
 */
object PolyJavColors {
    val Accent = Color(0xFF22D3EE)
    val AccentSoft = Color(0xFF67E8F9)
    val Page = Color(0xFF061216)
    val Text = Color(0xFFE6FAFF)
}

internal val polyJavDark: ColorScheme = darkColorScheme(
    primary = PolyJavColors.Accent,
    onPrimary = Color(0xFF031A20),
    primaryContainer = Color(0xFF0E4A57),
    onPrimaryContainer = Color(0xFFBDF4FF),
    secondary = PolyJavColors.AccentSoft,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF124650),
    onSecondaryContainer = Color(0xFFCFF8FF),
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF0F4D45),
    onTertiaryContainer = Color(0xFFC2F5EC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = PolyJavColors.Page,
    onBackground = PolyJavColors.Text,
    surface = PolyJavColors.Page,
    onSurface = PolyJavColors.Text,
    surfaceVariant = Color(0xFF10242B),
    onSurfaceVariant = Color(0xFF8FB3BD),
    outline = Color(0xFF4F7C88),
    outlineVariant = Color(0xFF1D3B44),
    scrim = Color(0xFF000000),
    inverseSurface = PolyJavColors.Text,
    inverseOnSurface = PolyJavColors.Page,
    inversePrimary = Color(0xFF00677A),
    surfaceDim = Color(0xFF040D10),
    surfaceBright = Color(0xFF1A3841),
    surfaceContainerLowest = Color(0xFF050F13),
    surfaceContainerLow = Color(0xFF0A1B21),
    surfaceContainer = Color(0xFF0F242B),
    surfaceContainerHigh = Color(0xFF152E36),
    surfaceContainerHighest = Color(0xFF1B3841),
)

internal val polyJavLight: ColorScheme = lightColorScheme(
    primary = Color(0xFF006B7D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F0FF),
    onPrimaryContainer = Color(0xFF001F26),
    secondary = Color(0xFF3B6470),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBFE9F5),
    onSecondaryContainer = Color(0xFF001F27),
    tertiary = Color(0xFF00695C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB2F0E4),
    onTertiaryContainer = Color(0xFF00201B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFE9FAFD),
    onBackground = Color(0xFF061216),
    surface = Color(0xFFE9FAFD),
    onSurface = Color(0xFF061216),
    surfaceVariant = Color(0xFFD3EEF4),
    onSurfaceVariant = Color(0xFF3F5A63),
    outline = Color(0xFF6F8F99),
    outlineVariant = Color(0xFFB4D2DA),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF0F242B),
    inverseOnSurface = Color(0xFFE6FAFF),
    inversePrimary = Color(0xFF67E8F9),
    surfaceDim = Color(0xFFCFE7ED),
    surfaceBright = Color(0xFFF4FDFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFE1F5F9),
    surfaceContainer = Color(0xFFD9F0F5),
    surfaceContainerHigh = Color(0xFFD0EAF0),
    surfaceContainerHighest = Color(0xFFC7E4EB),
)

/** Very rounded corners for cards, dialogs and chips (buttons are already fully round). */
internal val PolyJavShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp),
)
