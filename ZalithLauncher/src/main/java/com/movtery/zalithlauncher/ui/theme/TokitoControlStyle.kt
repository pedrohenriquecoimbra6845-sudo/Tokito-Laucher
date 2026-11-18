/*
 * Tokito Launcher
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

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.movtery.layer_controller.data.ButtonShape
import com.movtery.layer_controller.data.ButtonStyle
import com.movtery.layer_controller.data.JoystickStyle
import com.movtery.layer_controller.layout.LocalButtonStyleTransform
import com.movtery.layer_controller.layout.LocalJoystickStyleTransform
import com.movtery.layer_controller.observable.ObservableButtonStyle
import com.movtery.layer_controller.observable.ObservableJoystickStyle
import com.movtery.zalithlauncher.setting.AllSettings

/**
 * Tokito Launcher: makes the on-screen game controls (buttons and joysticks) look like the launcher.
 *
 * The colors, borders and corners come from the current launcher style and color theme. The control layout
 * itself is not changed: the new look is only applied when the buttons are drawn, so turning the setting off
 * brings the layout back exactly as it was saved. The Zalith style keeps the original Zalith look.
 */
@Composable
fun ProvideTokitoControlStyle(content: @Composable () -> Unit) {
    val follow = AllSettings.controlsFollowLauncherStyle.state
    val style = AllSettings.launcherStyle.state
    val mode = AllSettings.liquidGlassMode.state
    val colors = MaterialTheme.colorScheme

    val enabled = follow && style != LauncherStyle.ZALITH

    val buttonTransform = remember(enabled, style, mode, colors) {
        if (enabled) {
            val look = buttonLook(style, mode, colors)
            val transform: (ObservableButtonStyle) -> ObservableButtonStyle = { base ->
                val source = base.pack()
                ObservableButtonStyle(
                    source.copy(
                        commonStyle = false,
                        lightStyle = look.applyTo(source.lightStyle),
                        darkStyle = look.applyTo(source.darkStyle)
                    )
                )
            }
            transform
        } else {
            null
        }
    }

    val joystickTransform = remember(enabled, style, mode, colors) {
        if (enabled) {
            val transform: (ObservableJoystickStyle) -> ObservableJoystickStyle = { base ->
                val source = base.pack()
                ObservableJoystickStyle(
                    source.copy(
                        commonStyle = false,
                        lightStyle = joystickLook(style, mode, colors, source.lightStyle),
                        darkStyle = joystickLook(style, mode, colors, source.darkStyle)
                    )
                )
            }
            transform
        } else {
            null
        }
    }

    CompositionLocalProvider(
        LocalButtonStyleTransform provides buttonTransform,
        LocalJoystickStyleTransform provides joystickTransform
    ) {
        content()
    }
}

/** How a button looks in a launcher style */
private class ButtonLook(
    val background: Color,
    val pressedBackground: Color,
    val content: Color,
    val pressedContent: Color,
    val borderWidth: Int,
    val border: Color,
    val pressedBorder: Color,
    /** biggest corner radius (dp); the corners the layout keeps square stay square */
    val corner: Float
) {
    fun applyTo(original: ButtonStyle.StyleConfig): ButtonStyle.StyleConfig = original.copy(
        backgroundColor = background,
        pressedBackgroundColor = pressedBackground,
        contentColor = content,
        pressedContentColor = pressedContent,
        borderWidth = borderWidth,
        pressedBorderWidth = borderWidth,
        borderColor = border,
        pressedBorderColor = pressedBorder,
        borderRadius = original.borderRadius.restyled(corner),
        pressedBorderRadius = original.pressedBorderRadius.restyled(corner)
    )
}

/** Keeps which corners are round (so groups of joined buttons still fit together), with the style's radius */
private fun ButtonShape.restyled(corner: Float): ButtonShape {
    fun map(radius: Float) = if (radius <= 0f) 0f else minOf(radius, corner)
    return ButtonShape(
        topStart = map(topStart),
        topEnd = map(topEnd),
        bottomEnd = map(bottomEnd),
        bottomStart = map(bottomStart)
    )
}

/** Pearl glass of the Liquid Glass "Leve" mode and navy glass of the "Frio" mode (readable over any game scene) */
private val PearlGlass = Color(0xFFF4F7FB)
private val PearlInk = Color(0xFF2B2F3A)
private val NavyGlass = Color(0xFF0B1A33)
private val IceInk = Color(0xFFE6F6FF)
private val IceLine = Color(0xFFBDE6FF)

private fun buttonLook(style: LauncherStyle, mode: GlassMode, c: ColorScheme): ButtonLook {
    val corner = style.buttonCornerDp.toFloat()
    return when (style) {
        LauncherStyle.LIQUID_GLASS -> when (mode) {
            GlassMode.NORMAL -> ButtonLook(
                background = Color.White.copy(alpha = 0.16f),
                pressedBackground = Color.White.copy(alpha = 0.34f),
                content = Color.White,
                pressedContent = Color.White,
                borderWidth = 1,
                border = Color.White.copy(alpha = 0.55f),
                pressedBorder = Color.White.copy(alpha = 0.85f),
                corner = corner
            )

            GlassMode.LIGHT -> ButtonLook(
                background = PearlGlass.copy(alpha = 0.62f),
                pressedBackground = c.primary.copy(alpha = 0.85f),
                content = PearlInk,
                pressedContent = c.onPrimary,
                borderWidth = 1,
                border = Color.White.copy(alpha = 0.90f),
                pressedBorder = Color.White,
                corner = corner
            )

            GlassMode.COLD -> ButtonLook(
                background = NavyGlass.copy(alpha = 0.50f),
                pressedBackground = c.primary.copy(alpha = 0.55f),
                content = IceInk,
                pressedContent = Color.White,
                borderWidth = 1,
                border = IceLine.copy(alpha = 0.65f),
                pressedBorder = IceLine,
                corner = corner
            )
        }

        LauncherStyle.TRANSPARENT -> ButtonLook(
            background = Color.White.copy(alpha = 0.08f),
            pressedBackground = Color.White.copy(alpha = 0.24f),
            content = Color.White,
            pressedContent = Color.White,
            borderWidth = 1,
            border = Color.White.copy(alpha = 0.35f),
            pressedBorder = Color.White.copy(alpha = 0.70f),
            corner = corner
        )

        LauncherStyle.NEON -> ButtonLook(
            background = c.surface.copy(alpha = 0.55f),
            pressedBackground = c.primary.copy(alpha = 0.35f),
            content = c.primary,
            pressedContent = Color.White,
            borderWidth = 2,
            border = c.primary,
            pressedBorder = c.secondary,
            corner = corner
        )

        LauncherStyle.MINECRAFT -> ButtonLook(
            background = c.surfaceContainerHigh.copy(alpha = 0.85f),
            pressedBackground = c.primary.copy(alpha = 0.90f),
            content = Color.White,
            pressedContent = Color.White,
            borderWidth = 2,
            border = Color.Black.copy(alpha = 0.85f),
            pressedBorder = Color.Black,
            corner = corner
        )

        //Tokito (and Zalith, which never gets here): the plum surface with the pink accent
        else -> ButtonLook(
            background = c.surfaceContainerHigh.copy(alpha = 0.78f),
            pressedBackground = c.primary.copy(alpha = 0.85f),
            content = c.onSurface,
            pressedContent = c.onPrimary,
            borderWidth = 1,
            border = c.outlineVariant,
            pressedBorder = c.primary,
            corner = corner
        )
    }
}

private fun joystickLook(
    style: LauncherStyle,
    mode: GlassMode,
    c: ColorScheme,
    original: JoystickStyle.StyleConfig
): JoystickStyle.StyleConfig = when (style) {
    LauncherStyle.LIQUID_GLASS -> when (mode) {
        GlassMode.NORMAL -> original.copy(
            backgroundColor = Color.White.copy(alpha = 0.14f),
            joystickColor = Color.White.copy(alpha = 0.55f),
            joystickCanLockColor = c.tertiary.copy(alpha = 0.75f),
            joystickLockedColor = c.secondary.copy(alpha = 0.75f),
            lockMarkColor = Color.White,
            borderWidthRatio = 2,
            borderColor = Color.White.copy(alpha = 0.50f)
        )

        GlassMode.LIGHT -> original.copy(
            backgroundColor = PearlGlass.copy(alpha = 0.40f),
            joystickColor = c.primary.copy(alpha = 0.85f),
            joystickCanLockColor = c.tertiary.copy(alpha = 0.85f),
            joystickLockedColor = c.secondary.copy(alpha = 0.85f),
            lockMarkColor = PearlInk,
            borderWidthRatio = 2,
            borderColor = Color.White.copy(alpha = 0.90f)
        )

        GlassMode.COLD -> original.copy(
            backgroundColor = NavyGlass.copy(alpha = 0.45f),
            joystickColor = c.primary.copy(alpha = 0.80f),
            joystickCanLockColor = c.tertiary.copy(alpha = 0.85f),
            joystickLockedColor = c.secondary.copy(alpha = 0.85f),
            lockMarkColor = IceInk,
            borderWidthRatio = 2,
            borderColor = IceLine.copy(alpha = 0.65f)
        )
    }

    LauncherStyle.TRANSPARENT -> original.copy(
        backgroundColor = Color.White.copy(alpha = 0.06f),
        joystickColor = Color.White.copy(alpha = 0.35f),
        joystickCanLockColor = c.tertiary.copy(alpha = 0.65f),
        joystickLockedColor = c.secondary.copy(alpha = 0.65f),
        lockMarkColor = Color.White,
        borderWidthRatio = 2,
        borderColor = Color.White.copy(alpha = 0.35f)
    )

    LauncherStyle.NEON -> original.copy(
        backgroundColor = c.surface.copy(alpha = 0.50f),
        joystickColor = c.primary.copy(alpha = 0.85f),
        joystickCanLockColor = c.tertiary.copy(alpha = 0.90f),
        joystickLockedColor = c.secondary.copy(alpha = 0.90f),
        lockMarkColor = Color.White,
        borderWidthRatio = 3,
        borderColor = c.primary
    )

    LauncherStyle.MINECRAFT -> original.copy(
        backgroundColor = c.surfaceContainerHigh.copy(alpha = 0.80f),
        joystickColor = c.primary.copy(alpha = 0.90f),
        joystickCanLockColor = c.tertiary.copy(alpha = 0.90f),
        joystickLockedColor = c.secondary.copy(alpha = 0.90f),
        lockMarkColor = Color.White,
        borderWidthRatio = 3,
        borderColor = Color.Black.copy(alpha = 0.85f),
        backgroundShape = 10,
        joystickShape = 10
    )

    else -> original.copy(
        backgroundColor = c.surfaceContainerHigh.copy(alpha = 0.60f),
        joystickColor = c.primary.copy(alpha = 0.80f),
        joystickCanLockColor = c.tertiary.copy(alpha = 0.85f),
        joystickLockedColor = c.secondary.copy(alpha = 0.85f),
        lockMarkColor = c.onSurface,
        borderWidthRatio = 2,
        borderColor = c.outlineVariant
    )
}
