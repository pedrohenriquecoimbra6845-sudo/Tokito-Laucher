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

package com.movtery.zalithlauncher.ui.screens.content.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.OptimizationMode
import com.movtery.zalithlauncher.tokito.TokitoPopups
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.CheckChip
import com.movtery.zalithlauncher.ui.components.OptimizationDialog
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.TokitoPopupsDialog
import com.movtery.zalithlauncher.ui.components.TokitoSkinsDialog
import com.movtery.zalithlauncher.ui.components.TokitoWhatsNewDialog
import com.movtery.zalithlauncher.ui.components.shareTokitoAnnouncement

/**
 * Tokito Launcher: a card of the "Principais coisas" panel, with an icon and a title.
 */
@Composable
private fun PanelCard(
    iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BackgroundCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

/**
 * The optimization modes (Off, PVP, Survival and Brutal): tap one to choose it,
 * "more options" opens the window with the descriptions and the light chunks switch.
 */
@Composable
fun TokitoModesCard() {
    val mode = AllSettings.launcherOptimizationMode.state
    var showDialog by remember { mutableStateOf(false) }

    PanelCard(
        iconRes = R.drawable.ic_rocket_launch_filled,
        title = stringResource(R.string.tokito_modes_card_title)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            OptimizationMode.entries.forEach { entry ->
                CheckChip(
                    selected = entry == mode,
                    onClick = { AllSettings.launcherOptimizationMode.save(entry) },
                    label = { Text(text = stringResource(entry.chipRes)) }
                )
            }
        }
        Text(
            text = stringResource(mode.descRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            modifier = Modifier.align(Alignment.End),
            onClick = { showDialog = true }
        ) {
            Text(text = stringResource(R.string.tokito_modes_more))
        }
    }

    if (showDialog) {
        OptimizationDialog(onDismiss = { showDialog = false })
    }
}

/** The library of skins (opens the window of skins) */
@Composable
fun TokitoSkinsCard() {
    var showDialog by remember { mutableStateOf(false) }

    PanelCard(
        iconRes = R.drawable.ic_checkroom,
        title = stringResource(R.string.tokito_skins_card_title)
    ) {
        Text(
            text = stringResource(R.string.tokito_skins_card_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            modifier = Modifier.align(Alignment.End),
            onClick = { showDialog = true }
        ) {
            Text(text = stringResource(R.string.tokito_open))
        }
    }

    if (showDialog) {
        TokitoSkinsDialog(onDismiss = { showDialog = false })
    }
}

/** The floating photos and videos of the home screen: a switch to show them and the window to manage them */
@Composable
fun TokitoPopupsCard() {
    var showDialog by remember { mutableStateOf(false) }
    val enabled = AllSettings.tokitoPopupsEnabled.state

    LaunchedEffect(Unit) { TokitoPopups.load() }

    PanelCard(
        iconRes = R.drawable.ic_photo_library_outlined,
        title = stringResource(R.string.tokito_popups_card_title)
    ) {
        Text(
            text = stringResource(R.string.tokito_popups_card_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tokito_popups_enabled),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(
                        R.string.tokito_popups_card_count,
                        TokitoPopups.popups.size,
                        TokitoPopups.MAX_POPUPS
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { AllSettings.tokitoPopupsEnabled.save(it) }
            )
        }
        TextButton(
            modifier = Modifier.align(Alignment.End),
            onClick = { showDialog = true }
        ) {
            Text(text = stringResource(R.string.tokito_popups_manage))
        }
    }

    if (showDialog) {
        TokitoPopupsDialog(onDismiss = { showDialog = false })
    }
}

/** The news of this version (opens the "Novidades" window) and a button to share the news with other players */
@Composable
fun TokitoNewsCard() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    PanelCard(
        iconRes = R.drawable.ic_article_outlined,
        title = stringResource(R.string.tokito_news_card_title)
    ) {
        Text(
            text = stringResource(
                R.string.tokito_news_card_summary,
                BuildConfig.VERSION_NAME.substringBefore("-")
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { shareTokitoAnnouncement(context) }) {
                Text(text = stringResource(R.string.tokito_whatsnew_share))
            }
            Button(onClick = { showDialog = true }) {
                Text(text = stringResource(R.string.tokito_news_card_button))
            }
        }
    }

    if (showDialog) {
        TokitoWhatsNewDialog(onDismiss = { showDialog = false })
    }
}

/**
 * The window to customize the "Principais coisas" panel: show or hide each thing and move it up or down.
 */
@Composable
fun TokitoMainCustomizeDialog(onDismiss: () -> Unit) {
    val saved = AllSettings.tokitoMainItems.state
    val entries = remember(saved) { HomeMainItems.resolve(saved) }
    val save: (List<HomeMainEntry>) -> Unit = { changed ->
        AllSettings.tokitoMainItems.save(HomeMainItems.encode(changed))
    }

    SimpleAlertDialog(
        title = stringResource(R.string.tokito_main_customize_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    modifier = Modifier.padding(bottom = 6.dp),
                    text = stringResource(R.string.tokito_main_customize_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Switch(
                            checked = entry.visible,
                            onCheckedChange = { checked ->
                                save(HomeMainItems.setVisible(entries, index, checked))
                            }
                        )
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            text = stringResource(entry.item.labelRes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(
                            enabled = index > 0,
                            onClick = { save(HomeMainItems.move(entries, index, -1)) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_upward),
                                contentDescription = null
                            )
                        }
                        IconButton(
                            enabled = index < entries.lastIndex,
                            onClick = { save(HomeMainItems.move(entries, index, 1)) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_downward),
                                contentDescription = null
                            )
                        }
                    }
                }
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { AllSettings.tokitoMainItems.reset() }
                ) {
                    Text(text = stringResource(R.string.tokito_main_customize_reset))
                }
            }
        },
        onDismiss = onDismiss
    )
}
