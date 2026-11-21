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

package com.movtery.zalithlauncher.ui.screens.game.elements

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.launch.FpsGovernorState
import com.movtery.zalithlauncher.setting.enums.FpsDisplayMode
import com.movtery.zalithlauncher.ui.components.FloatingBall
import com.movtery.zalithlauncher.ui.components.FpsColoredText
import com.movtery.zalithlauncher.ui.components.FpsRed
import com.movtery.zalithlauncher.ui.components.gameFpsColor
import com.movtery.zalithlauncher.ui.screens.content.elements.MemoryPreview

@Composable
fun DraggableGameBall(
    position: Offset,
    onPositionChanged: (Offset) -> Unit,
    onSavePos: () -> Unit,
    gameFps: () -> Int?,
    fpsDisplayMode: FpsDisplayMode,
    fpsHistory: () -> List<Int>,
    fpsMax: () -> Int,
    fpsMin: () -> Int,
    showMemory: Boolean,
    opened: Boolean,
    alpha: Float = 1f,
    onClick: () -> Unit = {}
) {
    FloatingBall(
        modifier = Modifier.focusProperties {
            canFocus = false
        },
        position = position,
        onPositionChanged = onPositionChanged,
        onSavePos = onSavePos,
        onClick = onClick,
        alpha = alpha
    ) {
        GameBallContent(
            gameFps = gameFps,
            fpsDisplayMode = fpsDisplayMode,
            fpsHistory = fpsHistory,
            fpsMax = fpsMax,
            fpsMin = fpsMin,
            showMemory = showMemory,
            opened = opened,
        )
    }
}

/**
 * Tokito Launcher: the menu ball of the game is the flower of the launcher, and the FPS is under it with the
 * color of its level (10 FPS is red, then orange, yellow, green, cyan, and rainbow from 100 FPS).
 * The flower turns a quarter when the menu is open. An old phone that is in danger gets a red warning sign.
 */
@Composable
private fun GameBallContent(
    gameFps: () -> Int?,
    fpsDisplayMode: FpsDisplayMode,
    fpsHistory: () -> List<Int>,
    fpsMax: () -> Int,
    fpsMin: () -> Int,
    showMemory: Boolean,
    opened: Boolean,
) {
    val fps = gameFps()
    val history = fpsHistory()
    val fpsMaxValue = fpsMax()
    val fpsMinValue = fpsMin()
    val spin by animateFloatAsState(
        targetValue = if (opened) 90f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "flower-spin"
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .animateContentSize()
            .width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier
                .size(30.dp)
                .graphicsLayer { rotationZ = spin },
            painter = painterResource(R.drawable.tokito_logo),
            contentDescription = null
        )

        if (fps != null) {
            Spacer(Modifier.height(2.dp))
            if (fpsDisplayMode == FpsDisplayMode.CHART) {
                FpsChart(
                    history = history,
                    fpsMax = fpsMaxValue,
                    fpsMin = fpsMinValue
                )
            } else {
                FpsColoredText(
                    text = fps.toString(),
                    color = gameFpsColor(fps),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    shadow = true
                )
            }
        }

        if (FpsGovernorState.danger) {
            Text(
                text = "\u26A0",
                style = MaterialTheme.typography.labelMedium,
                color = FpsRed
            )
        }

        if (showMemory) {
            Spacer(Modifier.height(4.dp))
            MemoryPreview(
                modifier = Modifier
                    .defaultMinSize(minWidth = 168.dp)
                    .fillMaxWidth(),
                mainColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                textStyle = MaterialTheme.typography.labelSmall,
                usedText = { usedMemory, totalMemory ->
                    "${usedMemory.toInt()}MB/${totalMemory.toInt()}MB"
                }
            )
        }
    }
}
