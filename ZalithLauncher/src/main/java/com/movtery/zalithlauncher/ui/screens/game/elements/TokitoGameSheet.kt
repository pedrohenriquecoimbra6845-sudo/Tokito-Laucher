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

package com.movtery.zalithlauncher.ui.screens.game.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.launch.FpsGovernorState
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.OptimizationMode
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.FpsRed
import com.movtery.zalithlauncher.ui.components.SyncHzRow
import com.movtery.zalithlauncher.ui.theme.backgroundColor
import com.movtery.zalithlauncher.ui.theme.onBackgroundColor
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Tokito Launcher: the game menu that opens at the bottom of the screen when the flower is tapped.
 *
 * - **Drag it:** pull the handle down to close it, or up to open the full menu (the old two-sided menu).
 * - **Modes:** change the optimization mode (Off, PVP, Survival, Brutal) while you play; the FPS governor of
 *   the launcher restarts with the new one.
 * - **Same Hz as the FPS:** switch.
 * - **Quick actions:** log, input method, send keycode, force close and the full menu.
 *
 * @param visible the menu is open
 * @param onClose close the menu
 * @param onMore open the full menu
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TokitoGameSheet(
    visible: Boolean,
    onClose: () -> Unit,
    onMore: () -> Unit,
    onSwitchLog: () -> Unit,
    onInputMethod: () -> Unit,
    onSendKeycode: () -> Unit,
    onForceClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    val maxUp = with(density) { 160.dp.toPx() }
    val moreThreshold = with(density) { 70.dp.toPx() }

    //the sheet comes back to its place after it was closed by dragging
    LaunchedEffect(visible) {
        if (!visible) {
            delay(400L)
            dragOffset = 0f
        }
    }

    val dragState = rememberDraggableState { delta ->
        dragOffset = (dragOffset + delta).coerceIn(-maxUp, sheetHeight.toFloat().coerceAtLeast(1f))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            //shadow layer: a tap outside the sheet closes it
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onClose
                    )
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = visible,
            enter = slideInVertically(animationSpec = tween(220)) { it } + fadeIn(tween(160)),
            exit = slideOutVertically(animationSpec = tween(180)) { it } + fadeOut(tween(140))
        ) {
            BackgroundCard(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                influencedByBackground = false,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .offset { IntOffset(0, dragOffset.roundToInt().coerceAtLeast(-maxUp.roundToInt())) }
                    .onSizeChanged { sheetHeight = it.height }
                    //a tap inside the sheet must not reach the shadow layer
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {}
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor(),
                    contentColor = onBackgroundColor()
                )
            ) {
                //handle and title: this is the part that is dragged
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity ->
                                val closeBy = sheetHeight * 0.3f
                                when {
                                    dragOffset > closeBy || velocity > 1800f -> {
                                        animate(dragOffset, sheetHeight.toFloat(), animationSpec = tween(160)) { v, _ ->
                                            dragOffset = v
                                        }
                                        onClose()
                                    }
                                    dragOffset < -moreThreshold || velocity < -1800f -> {
                                        animate(dragOffset, 0f, animationSpec = tween(160)) { v, _ -> dragOffset = v }
                                        onMore()
                                    }
                                    else -> animate(dragOffset, 0f, animationSpec = tween(180)) { v, _ -> dragOffset = v }
                                }
                            }
                        )
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(onBackgroundColor().copy(alpha = 0.35f))
                    )
                    Text(
                        modifier = Modifier.padding(top = 6.dp),
                        text = stringResource(R.string.tokito_sheet_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModesSection()
                    HorizontalDivider()
                    SyncHzRow()
                    StatusLine()

                    Text(
                        text = stringResource(R.string.tokito_sheet_actions),
                        style = MaterialTheme.typography.titleSmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(onClick = onSwitchLog) {
                            Text(stringResource(R.string.game_menu_option_switch_log))
                        }
                        FilledTonalButton(onClick = onInputMethod) {
                            Text(stringResource(R.string.game_menu_option_input_method))
                        }
                        FilledTonalButton(onClick = onSendKeycode) {
                            Text(stringResource(R.string.game_menu_option_send_keycode))
                        }
                        FilledTonalButton(onClick = onForceClose) {
                            Text(stringResource(R.string.game_button_force_close))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.tokito_sheet_more_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onMore) {
                            Text(stringResource(R.string.tokito_sheet_more))
                        }
                    }
                }
            }
        }
    }
}

/** The four modes as chips: tap one to change the mode while the game runs */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModesSection() {
    val mode = AllSettings.launcherOptimizationMode.state
    Text(
        text = stringResource(R.string.tokito_sheet_modes),
        style = MaterialTheme.typography.titleSmall
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OptimizationMode.entries.forEach { entry ->
            FilterChip(
                selected = entry == mode,
                onClick = { AllSettings.launcherOptimizationMode.save(entry) },
                label = { Text(stringResource(entry.chipRes)) }
            )
        }
    }
    Text(
        text = stringResource(mode.descRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** What the governor is doing now (limit, screen Hz, heat) and the warning of an old phone in danger */
@Composable
private fun StatusLine() {
    val mode = AllSettings.launcherOptimizationMode.state
    if (!mode.isActive) return
    val heatName = stringResource(
        when (FpsGovernorState.heat) {
            0 -> R.string.tokito_heat_0
            1 -> R.string.tokito_heat_1
            2 -> R.string.tokito_heat_2
            3 -> R.string.tokito_heat_3
            else -> R.string.tokito_heat_4
        }
    )
    Text(
        text = stringResource(
            R.string.tokito_game_status,
            stringResource(mode.labelRes),
            FpsGovernorState.targetFps,
            FpsGovernorState.displayHz,
            heatName
        ),
        style = MaterialTheme.typography.labelMedium
    )
    if (FpsGovernorState.danger) {
        Text(
            text = stringResource(R.string.tokito_danger_toast),
            style = MaterialTheme.typography.labelMedium,
            color = FpsRed
        )
    }
}
