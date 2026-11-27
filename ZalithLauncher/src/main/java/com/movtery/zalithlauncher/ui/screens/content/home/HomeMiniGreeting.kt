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

package com.movtery.zalithlauncher.ui.screens.content.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.GreetingVoice
import com.movtery.zalithlauncher.tokito.TokitoGreeting
import com.movtery.zalithlauncher.ui.components.TokitoEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** How long each letter of the speech takes to show up */
private const val LETTER_MS = 32L

/** How long the speech stays on the screen after it was written */
private const val STAY_MS = 9_000L

/** How often the part of the day is checked while the home screen is open */
private const val PERIOD_CHECK_MS = 30_000L

/**
 * Tokito Launcher: the speech of the Mini Wallpaper. A bubble over the bottom of the picture that talks to the player:
 * "Welcome, <name>! Good night!" and a warrior phrase, written letter by letter. It shows up when the launcher opens
 * (after the entry screen), stays a few seconds and fades away. When the part of the day changes while the launcher is
 * open (morning, afternoon, night) it speaks again with the new greeting and a new phrase.
 *
 * @param visible whether the home screen is the one that the player is looking at
 */
@Composable
internal fun MiniWallpaperGreeting(visible: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val name = account?.username?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_greet_player)
    val voiceOn = AllSettings.homeGreetingVoice.state

    var period by remember { mutableStateOf(TokitoGreeting.periodNow()) }
    var firstTime by remember { mutableStateOf(true) }
    var show by remember { mutableStateOf(false) }
    var typed by remember { mutableIntStateOf(0) }
    val phraseRes = remember(period) { TokitoGreeting.randomPhrase() }

    val word = stringResource(period.wordRes)
    val title = if (firstTime) {
        stringResource(R.string.home_greet_welcome, name, word)
    } else {
        stringResource(R.string.home_greet_period, name, word)
    }
    val phrase = stringResource(phraseRes)

    //the voice exists only while the player wants it
    var voice by remember { mutableStateOf<GreetingVoice?>(null) }
    DisposableEffect(voiceOn) {
        val created = if (voiceOn) GreetingVoice(context) else null
        voice = created
        onDispose { created?.release() }
    }

    //the part of the day is checked now and then while the home screen is open
    LaunchedEffect(visible) {
        while (visible) {
            delay(PERIOD_CHECK_MS)
            val now = TokitoGreeting.periodNow()
            if (now != period) period = now
        }
    }

    //the speech: it waits for the entry screen, is written letter by letter, stays and fades away
    LaunchedEffect(visible, period) {
        if (!visible) {
            show = false
            return@LaunchedEffect
        }
        TokitoEntry.finished.first { it }
        delay(700)
        typed = 0
        show = true
        voice?.speak("$title. $phrase")
        val total = title.length + phrase.length
        while (typed < total) {
            delay(LETTER_MS)
            typed++
        }
        delay(STAY_MS)
        show = false
        firstTime = false
    }

    val bubble = Color.Black.copy(alpha = 0.58f)
    AnimatedVisibility(
        modifier = modifier.padding(10.dp),
        visible = show && visible,
        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 },
        exit = fadeOut(tween(500))
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bubble)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TypedText(
                    full = title,
                    count = typed,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2
                )
                TypedText(
                    full = phrase,
                    count = typed - title.length,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 5
                )
            }
            //the small tail of the speech bubble
            Canvas(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .size(width = 14.dp, height = 8.dp)
            ) {
                val tail = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(tail, bubble)
            }
        }
    }
}

/**
 * A text that is written letter by letter. The full text is measured (invisible) so the bubble keeps its size
 * while the letters appear, instead of growing all the time.
 */
@Composable
private fun TypedText(
    full: String,
    count: Int,
    style: TextStyle,
    color: Color,
    maxLines: Int
) {
    Box {
        Text(text = full, style = style, color = Color.Transparent, maxLines = maxLines)
        Text(text = full.take(count.coerceAtLeast(0)), style = style, color = color, maxLines = maxLines)
    }
}
