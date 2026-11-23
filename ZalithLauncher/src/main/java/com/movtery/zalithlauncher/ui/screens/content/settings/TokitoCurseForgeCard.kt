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


package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.TokitoLinks
import com.movtery.zalithlauncher.ui.components.OwnOutlinedTextField
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard

/**
 * Tokito Launcher: the CurseForge key.
 *
 * CurseForge refuses every request of an app that has no key, and then the list of mods comes empty. A build made by
 * a fork has no key of its own, so the player can paste a free key here (https://console.curseforge.com/).
 * When the build already has a key, only a note is shown.
 */
@Composable
fun CurseForgeKeyCard(
    position: CardPosition,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val typed = AllSettings.tokitoCurseForgeKey.state
    val builtIn = BuildKeys.CURSEFORGE_API.isNotBlank()

    SettingsCard(
        modifier = modifier,
        position = position
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TitleAndSummary(
                title = stringResource(R.string.tokito_curseforge_title),
                summary = stringResource(R.string.tokito_curseforge_summary)
            )

            Text(
                text = stringResource(
                    when {
                        builtIn -> R.string.tokito_curseforge_status_build
                        typed.isNotBlank() -> R.string.tokito_curseforge_status_saved
                        else -> R.string.tokito_curseforge_status_none
                    }
                ),
                style = MaterialTheme.typography.labelMedium,
                color = if (builtIn || typed.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )

            if (!builtIn) {
                OwnOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = typed,
                    textStyle = MaterialTheme.typography.labelMedium,
                    onValueChange = { AllSettings.tokitoCurseForgeKey.save(it.trim()) },
                    label = { Text(text = stringResource(R.string.tokito_curseforge_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
                FilledTonalButton(
                    onClick = { TokitoLinks.open(context, TokitoLinks.CURSEFORGE_CONSOLE) }
                ) {
                    Text(text = stringResource(R.string.tokito_curseforge_get))
                }
            }
        }
    }
}
