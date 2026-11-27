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

package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.StreamKeyConfig
import com.movtery.zalithlauncher.tokito.StreamKeyId
import com.movtery.zalithlauncher.tokito.StreamKeyPrefs
import com.movtery.zalithlauncher.ui.screens.content.home.SwitchRow
import kotlin.math.roundToInt

/** How many letters fit in a row of the letter picker */
private const val LETTERS_PER_ROW = 9

/**
 * Tokito Launcher: the editor of the Stream keys and of the black screen.
 *
 * Every key can be turned on or off, moved up or down, and gets the letter that the player wants (a letter that is
 * already used swaps places with this key, so a letter is never used twice). The black screen has its own settings:
 * color, clock, message and brightness, and a button to try it.
 *
 * @param showLive false inside the game: the Live mode key does not exist there, so it is not listed
 */
@Composable
fun TokitoStreamKeysDialog(showLive: Boolean, onDismiss: () -> Unit) {
    val saved = AllSettings.tokitoStreamKeys.state
    val keys = remember(saved) { StreamKeyPrefs.parse(saved) }
    val shown = remember(keys, showLive) { keys.filter { showLive || it.id.inGame } }
    var picking by remember { mutableStateOf<StreamKeyId?>(null) }
    var testBlack by remember { mutableStateOf(false) }

    fun update(list: List<StreamKeyConfig>) {
        AllSettings.tokitoStreamKeys.save(StreamKeyPrefs.encode(list))
    }

    SimpleAlertDialog(
        title = stringResource(R.string.stream_keys_edit_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.stream_keys_edit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                shown.forEachIndexed { index, key ->
                    KeyRow(
                        config = key,
                        canMoveUp = index > 0,
                        canMoveDown = index < shown.lastIndex,
                        picking = picking == key.id,
                        onToggle = {
                            update(keys.map { if (it.id == key.id) it.copy(enabled = !it.enabled) else it })
                        },
                        onMove = { delta ->
                            update(StreamKeyPrefs.move(keys, key.id, delta) { showLive || it.id.inGame })
                        },
                        onPick = { picking = if (picking == key.id) null else key.id },
                        onLetter = { letter ->
                            update(StreamKeyPrefs.withLetter(keys, key.id, letter))
                            picking = null
                        }
                    )
                }
                TextButton(
                    onClick = {
                        picking = null
                        AllSettings.tokitoStreamKeys.save("")
                    }
                ) {
                    Text(text = stringResource(R.string.stream_keys_reset))
                }
                HorizontalDivider()
                BlackScreenSettings(onTest = { testBlack = true })
            }
        },
        confirmText = stringResource(R.string.generic_done),
        onDismiss = onDismiss
    )

    if (testBlack) TokitoBlackScreen(onClose = { testBlack = false })
}

/** One key in the editor: icon, name, letter, arrows and the switch; the letter picker opens below the row */
@Composable
private fun KeyRow(
    config: StreamKeyConfig,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    picking: Boolean,
    onToggle: () -> Unit,
    onMove: (Int) -> Unit,
    onPick: () -> Unit,
    onLetter: (Char) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(config.id.iconRes),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp),
                    text = stringResource(config.id.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    onClick = onPick,
                    shape = RoundedCornerShape(8.dp),
                    color = if (picking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (picking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        text = config.letter.toString(),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                IconButton(
                    modifier = Modifier.size(36.dp),
                    enabled = canMoveUp,
                    onClick = { onMove(-1) }
                ) {
                    Icon(
                        modifier = Modifier.rotate(180f),
                        painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                        contentDescription = stringResource(R.string.stream_keys_move_up)
                    )
                }
                IconButton(
                    modifier = Modifier.size(36.dp),
                    enabled = canMoveDown,
                    onClick = { onMove(1) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                        contentDescription = stringResource(R.string.stream_keys_move_down)
                    )
                }
                Switch(checked = config.enabled, onCheckedChange = { onToggle() })
            }
            if (picking) {
                LetterPicker(selected = config.letter, onPick = onLetter)
            }
        }
    }
}

/** A to Z in rows; the chosen letter is highlighted */
@Composable
private fun LetterPicker(selected: Char, onPick: (Char) -> Unit) {
    Column(
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ('A'..'Z').toList().chunked(LETTERS_PER_ROW).forEach { rowLetters ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowLetters.forEach { letter ->
                    val chosen = letter == selected
                    Surface(
                        onClick = { onPick(letter) },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (chosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = letter.toString(), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                repeat(LETTERS_PER_ROW - rowLetters.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** The names of the colors of the black screen, in the same order as [BlackColors] */
private val BlackColorNames = listOf(
    R.string.stream_black_c0,
    R.string.stream_black_c1,
    R.string.stream_black_c2,
    R.string.stream_black_c3,
    R.string.stream_black_c4,
    R.string.stream_black_c5
)

/** The settings of the black screen: color, clock, message and brightness */
@Composable
private fun BlackScreenSettings(onTest: () -> Unit) {
    val color = AllSettings.tokitoBlackColor.state.coerceIn(0, BlackColors.lastIndex)
    val clock = AllSettings.tokitoBlackClock.state
    var message by remember { mutableStateOf(AllSettings.tokitoBlackText.getValue()) }
    val brightness = AllSettings.tokitoBlackBrightness.state
    var slider by remember(brightness) { mutableFloatStateOf(brightness.toFloat()) }

    Text(
        text = stringResource(R.string.stream_black_title),
        style = MaterialTheme.typography.titleSmall
    )
    Text(
        text = stringResource(R.string.stream_black_color, stringResource(BlackColorNames[color])),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BlackColors.forEachIndexed { index, backdrop ->
            val chosen = index == color
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(backdrop)
                    .border(
                        width = if (chosen) 3.dp else 1.dp,
                        color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { AllSettings.tokitoBlackColor.save(index) }
            )
        }
    }
    SwitchRow(
        label = stringResource(R.string.stream_black_clock),
        checked = clock,
        onChange = { AllSettings.tokitoBlackClock.save(it) }
    )
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = message,
        onValueChange = {
            message = it.take(40)
            AllSettings.tokitoBlackText.save(message)
        },
        singleLine = true,
        label = { Text(text = stringResource(R.string.stream_black_message)) }
    )
    Text(
        text = stringResource(R.string.stream_black_brightness, slider.roundToInt()) + "%",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Slider(
        value = slider,
        onValueChange = { slider = it },
        valueRange = 1f..40f,
        onValueChangeFinished = { AllSettings.tokitoBlackBrightness.save(slider.roundToInt()) }
    )
    IconTextButton(
        onClick = onTest,
        painter = painterResource(R.drawable.ic_stream_screen),
        text = stringResource(R.string.stream_black_test)
    )
}
