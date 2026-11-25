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

package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.launch.DeviceBenchmark
import com.movtery.zalithlauncher.game.launch.DeviceProfile
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.OptimizationMode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Tokito Launcher: one row for each optimization mode (name and what it does). Tap to choose it.
 */
@Composable
fun OptimizationModeList(
    selected: OptimizationMode,
    onSelect: (OptimizationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.large
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OptimizationMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (isSelected) highlight else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(mode.labelRes),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(mode.descRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Tokito Launcher: switch of "light and fast chunks". Survival and Brutal already include it,
 * so the switch is on and locked while one of them is chosen.
 */
@Composable
fun LightChunksRow(
    mode: OptimizationMode,
    modifier: Modifier = Modifier
) {
    val included = mode.includesLightChunks
    val own = AllSettings.tokitoLightChunks.state
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = !included) { AllSettings.tokitoLightChunks.save(!own) }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.tokito_light_chunks_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    if (included) R.string.tokito_light_chunks_included else R.string.tokito_light_chunks_summary
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = included || own,
            onCheckedChange = null,
            enabled = !included
        )
    }
}

/**
 * Tokito Launcher: "Same Hz as the FPS". The screen is put at the refresh rate that fits the FPS limit of the
 * mode (it keeps the FPS locked). It is also a switch of the game menu, and is off by default.
 */
@Composable
fun SyncHzRow(modifier: Modifier = Modifier) {
    val on = AllSettings.tokitoSyncHz.state
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { AllSettings.tokitoSyncHz.save(!on) }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.tokito_sync_hz_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.tokito_sync_hz_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = on, onCheckedChange = null)
    }
}

/**
 * Tokito Launcher: the benchmark of the device, in its own tab. When it runs, the FPS goes up and down live
 * (red under 60 FPS, yellow from 60, green from 100, rainbow from 200) and the bars show the last readings.
 * It runs when the player taps the button, and can be run again at any time.
 */
@Composable
fun BenchmarkPanel(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var liveFps by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf<DeviceBenchmark.Phase?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    val history = remember { mutableStateListOf<Int>() }
    val tier = AllSettings.tokitoBenchmarkTier.state
    val startFps = AllSettings.tokitoBenchmarkFps.state
    val score = AllSettings.tokitoBenchmarkScore.state
    val throttle = AllSettings.tokitoBenchmarkThrottle.state
    val calibrated = AllSettings.tokitoCalibratedFps.state

    fun measure() {
        if (running) return
        running = true
        history.clear()
        liveFps = 0
        progress = 0f
        scope.launch {
            try {
                DeviceBenchmark.run(context.applicationContext) { sample ->
                    scope.launch {
                        liveFps = sample.fps
                        phase = sample.phase
                        progress = sample.progress
                        history.add(sample.fps)
                        while (history.size > BENCH_BARS) history.removeAt(0)
                    }
                }
                liveFps = AllSettings.tokitoBenchmarkScore.getValue()
            } catch (_: Exception) {
                //the settings stay as they were
            } finally {
                running = false
                phase = null
            }
        }
    }

    //the number counts up and down instead of jumping
    val shown by animateFloatAsState(
        targetValue = if (running || liveFps > 0) liveFps.toFloat() else score.toFloat(),
        animationSpec = tween(durationMillis = 260),
        label = "benchmark-fps"
    )
    val shownInt = shown.roundToInt()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //Tokito: a Minecraft dimension behind the FPS: 1+ Nether, 60+ Overworld, 100+ End, 200+ Aether
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            TokitoDimensionBackdrop(fps = shownInt, modifier = Modifier.matchParentSize())
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FpsColoredText(
                    text = shownInt.toString(),
                    color = benchmarkFpsColor(shownInt),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black)
                )
                Text(
                    modifier = Modifier.padding(bottom = 10.dp),
                    text = "FPS",
                    style = MaterialTheme.typography.titleMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, bottom = 6.dp),
                text = stringResource(
                    when {
                        shownInt >= 200 -> R.string.bd_dim_aether
                        shownInt >= 100 -> R.string.bd_dim_end
                        shownInt >= 60 -> R.string.bd_dim_overworld
                        else -> R.string.bd_dim_nether
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
            )
        }

        //bars of the last readings: they rise and fall with the phone
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        ) {
            val barWidth = size.width / BENCH_BARS
            val top = maxOf(240, history.maxOrNull() ?: 0).toFloat()
            history.forEachIndexed { index, value ->
                val height = size.height * (value / top).coerceIn(0.04f, 1f)
                val color = benchmarkFpsColor(value) ?: RainbowColors[index % RainbowColors.size]
                drawRect(
                    color = color,
                    topLeft = Offset(index * barWidth + 1f, size.height - height),
                    size = Size((barWidth - 2f).coerceAtLeast(1f), height)
                )
            }
        }

        if (running) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }

        val phaseText = when (phase) {
            DeviceBenchmark.Phase.CPU -> stringResource(R.string.tokito_bench_phase_cpu)
            DeviceBenchmark.Phase.GPU -> stringResource(R.string.tokito_bench_phase_gpu)
            DeviceBenchmark.Phase.BOTH -> stringResource(R.string.tokito_bench_phase_both)
            null -> null
        }
        val tierName = when (tier) {
            0 -> stringResource(R.string.tokito_bench_tier_0)
            1 -> stringResource(R.string.tokito_bench_tier_1)
            else -> stringResource(R.string.tokito_bench_tier_2)
        }
        val text = when {
            running -> phaseText ?: stringResource(R.string.tokito_bench_running)
            tier < 0 -> stringResource(R.string.tokito_bench_idle)
            else -> stringResource(R.string.tokito_bench_result, tierName, startFps, DeviceProfile.fpsStep) +
                " " + stringResource(R.string.tokito_bench_detail, score, throttle) +
                if (calibrated > 0) " " + stringResource(R.string.tokito_bench_calibrated, calibrated) else ""
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(
                if (DeviceProfile.isOldPhone) R.string.tokito_bench_floor_old else R.string.tokito_bench_floor_new
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.tokito_bench_scale),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        TextButton(
            onClick = { measure() },
            enabled = !running
        ) {
            Text(stringResource(if (tier < 0) R.string.tokito_bench_run else R.string.tokito_bench_rerun))
        }
    }
}

private const val BENCH_BARS = 48

/**
 * Tokito Launcher: the optimization window in two tabs. "Modes" has the modes, the light and fast chunks and
 * "same Hz as the FPS"; "Benchmark" has the live test of the phone.
 */
@Composable
fun OptimizationPanel(modifier: Modifier = Modifier) {
    val mode = AllSettings.launcherOptimizationMode.state
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = tab == 0,
                onClick = { tab = 0 },
                label = { Text(stringResource(R.string.tokito_opt_tab_modes)) }
            )
            FilterChip(
                selected = tab == 1,
                onClick = { tab = 1 },
                label = { Text(stringResource(R.string.tokito_opt_tab_bench)) }
            )
        }
        if (tab == 0) {
            OptimizationModeList(
                selected = mode,
                onSelect = { AllSettings.launcherOptimizationMode.save(it) }
            )
            HorizontalDivider()
            LightChunksRow(mode = mode)
            SyncHzRow()
        } else {
            BenchmarkPanel()
        }
    }
}

/**
 * Tokito Launcher: the modes and the light chunks switch in one window
 * (opened from the chip on the home screen).
 */
@Composable
fun OptimizationDialog(onDismiss: () -> Unit) {
    SimpleAlertDialog(
        title = stringResource(R.string.tokito_opt_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.tokito_opt_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OptimizationPanel()
            }
        },
        onDismiss = onDismiss
    )
}
