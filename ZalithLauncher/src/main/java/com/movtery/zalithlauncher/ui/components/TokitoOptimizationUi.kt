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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.OptimizationMode

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
 * Tokito Launcher: the modes and the light chunks switch in one window
 * (opened from the chip on the home screen).
 */
@Composable
fun OptimizationDialog(onDismiss: () -> Unit) {
    val mode = AllSettings.launcherOptimizationMode.state
    SimpleAlertDialog(
        title = stringResource(R.string.tokito_opt_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.tokito_opt_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OptimizationModeList(
                    selected = mode,
                    onSelect = { AllSettings.launcherOptimizationMode.save(it) }
                )
                HorizontalDivider()
                LightChunksRow(mode = mode)
            }
        },
        onDismiss = onDismiss
    )
}
