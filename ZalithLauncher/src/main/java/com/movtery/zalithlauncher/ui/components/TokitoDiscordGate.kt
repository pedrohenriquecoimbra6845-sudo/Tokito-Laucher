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


package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.TokitoLinks

private val DiscordBlurple = Color(0xFF5865F2)

/**
 * Tokito Launcher: invitation to the Discord of the community, shown on top of the launcher until the player has
 * opened the invite and confirmed.
 *
 * Discord does not tell an app who joined a server, so this cannot check the membership: it is a gate that asks the
 * player to open the invite first (the confirm button stays off until the link was opened) and is not shown again.
 */
@Composable
fun TokitoDiscordGate(
    modifier: Modifier = Modifier
) {
    if (AllSettings.tokitoDiscordDone.state) return

    val context = LocalContext.current
    var opened by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B1030), Color(0xFF3A1C5A), Color(0xFF12081F))
                )
            )
            //takes every touch: the launcher below cannot be used until the player goes through here
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.tokito_logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = stringResource(R.string.tokito_discord_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.tokito_discord_text),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val ok = TokitoLinks.open(context, TokitoLinks.DISCORD)
                    opened = ok
                    failed = !ok
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DiscordBlurple,
                    contentColor = Color.White
                )
            ) {
                Text(text = stringResource(R.string.tokito_discord_join))
            }
            Text(
                text = TokitoLinks.DISCORD,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = opened,
                onClick = { AllSettings.tokitoDiscordDone.save(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.92f),
                    contentColor = Color(0xFF2A1445),
                    disabledContainerColor = Color.White.copy(alpha = 0.18f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                Text(text = stringResource(R.string.tokito_discord_done))
            }
            Text(
                text = stringResource(
                    if (failed) R.string.tokito_discord_failed else R.string.tokito_discord_wait
                ),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = if (opened) 0f else 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
