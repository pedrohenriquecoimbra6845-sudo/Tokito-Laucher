/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
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

package com.movtery.layer_controller.layout

import androidx.compose.runtime.staticCompositionLocalOf
import com.movtery.layer_controller.observable.ObservableButtonStyle
import com.movtery.layer_controller.observable.ObservableJoystickStyle

/**
 * Tokito Launcher: lets the app change how the buttons look when they are drawn, without touching the
 * saved control layout. It receives the style the layout asks for and returns the style to draw.
 *
 * It is `null` by default (the layout is drawn as it was saved) and it is not provided in the control editor.
 */
val LocalButtonStyleTransform = staticCompositionLocalOf<((ObservableButtonStyle) -> ObservableButtonStyle)?> { null }

/**
 * Same as [LocalButtonStyleTransform], for the joysticks.
 */
val LocalJoystickStyleTransform = staticCompositionLocalOf<((ObservableJoystickStyle) -> ObservableJoystickStyle)?> { null }
