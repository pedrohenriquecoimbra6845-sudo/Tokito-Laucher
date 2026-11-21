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

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.tokito.TokitoBoot
import com.movtery.zalithlauncher.ui.components.FpsGreen
import com.movtery.zalithlauncher.ui.components.FpsYellow
import kotlinx.coroutines.delay

/**
 * Tokito Launcher: the loading screen of the game. It covers the loading bar of Minecraft with the flower of the
 * launcher, the real progress (read from the log of the game), the time that passed and the goal of 35 seconds.
 * It goes away by itself when the game says that it is ready, and the time that the game took is shown in a message.
 *
 * It does not animate anything all the time (the game is loading and needs the phone): the bar moves when the
 * progress changes and the time changes once a second.
 */
@Composable
fun TokitoLoadingOverlay() {
    val context = LocalContext.current
    val loading = TokitoBoot.loading

    //the game said that it is ready: a short pause, then the loading screen goes away
    LaunchedEffect(TokitoBoot.ready) {
        if (TokitoBoot.ready) {
            delay(3500L)
            val seconds = TokitoBoot.elapsedSeconds()
            TokitoBoot.finish(context)
            Toast.makeText(
                context,
                context.getString(R.string.tokito_boot_done, seconds, TokitoBoot.TARGET_SECONDS),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    //the game never said it: do not keep the player waiting for ever
    LaunchedEffect(loading) {
        if (loading) {
            delay(TokitoBoot.MAX_LOADING_SECONDS * 1000L)
            TokitoBoot.finish(context)
        }
    }

    AnimatedVisibility(
        visible = loading,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(450))
    ) {
        LoadingContent(onSkip = { TokitoBoot.finish(context) })
    }
}

@Composable
private fun LoadingContent(onSkip: () -> Unit) {
    var elapsed by remember { mutableIntStateOf(TokitoBoot.elapsedSeconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            elapsed = TokitoBoot.elapsedSeconds()
            delay(1000L)
        }
    }
    val progress by animateFloatAsState(
        targetValue = TokitoBoot.progress,
        animationSpec = tween(durationMillis = 900),
        label = "boot-progress"
    )
    val stageText = stringResource(
        when (TokitoBoot.stage) {
            TokitoBoot.Stage.START -> R.string.tokito_boot_stage_start
            TokitoBoot.Stage.JAVA -> R.string.tokito_boot_stage_java
            TokitoBoot.Stage.RESOURCES -> R.string.tokito_boot_stage_resources
            TokitoBoot.Stage.TEXTURES -> R.string.tokito_boot_stage_textures
            TokitoBoot.Stage.SOUND -> R.string.tokito_boot_stage_sound
        }
    )
    val tip = stringResource(
        when ((elapsed / 6) % 3) {
            0 -> R.string.tokito_boot_tip_1
            1 -> R.string.tokito_boot_tip_2
            else -> R.string.tokito_boot_tip_3
        }
    )
    //green while it is inside the goal, yellow after it
    val timeColor = if (elapsed <= TokitoBoot.TARGET_SECONDS) FpsGreen else FpsYellow

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1E1022), Color(0xFF4A1E3D))))
            //the loading screen must not let a tap through to the game that is still loading
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier.size(88.dp),
                painter = painterResource(R.drawable.tokito_logo),
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(R.string.tokito_boot_title),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = BuildConfig.VERSION_NAME,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(0.7f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFF8FB8), Color(0xFFFFD1E3))))
                )
            }
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = stageText,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(R.string.tokito_boot_time, elapsed, TokitoBoot.TARGET_SECONDS),
                color = timeColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                modifier = Modifier.padding(top = 18.dp),
                text = tip,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        if (elapsed >= TokitoBoot.SKIP_AFTER_SECONDS) {
            TextButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                onClick = onSkip
            ) {
                Text(text = stringResource(R.string.tokito_boot_skip), color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}
