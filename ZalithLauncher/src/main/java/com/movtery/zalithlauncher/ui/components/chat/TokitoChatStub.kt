/*
 * Tokito Launcher
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com>, the Tokito Launcher creators and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.components.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog

/*
 * TEMPORARY: the friends chat (home screen and in game) is not written yet. These two functions are the
 * contract that the home screen already uses. When the chat is written, only this file is replaced.
 */

/** How many messages of friends were not read yet (the badge of the Chat tile) */
@Composable
fun rememberTokitoChatUnread(): State<Int> = remember { mutableIntStateOf(0) }

/** The chat as a tab of the menu inside the game */
@Composable
fun TokitoChatGameTab(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.home_chat_soon), style = MaterialTheme.typography.bodyMedium)
    }
}

/** The window of the friends chat */
@Composable
fun TokitoChatDialog(onDismiss: () -> Unit) {
    SimpleAlertDialog(
        title = stringResource(R.string.home_chat),
        text = { Text(text = stringResource(R.string.home_chat_soon)) },
        onDismiss = onDismiss
    )
}
