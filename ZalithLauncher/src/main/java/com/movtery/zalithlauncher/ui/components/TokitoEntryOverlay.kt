/*
 * Tokito Laucher - unofficial modified version of Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors (Zalith Launcher 2)
 * Modifications Copyright (C) 2026 Tokito Laucher contributors
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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * State shared between the entry screen and the rest of the launcher.
 */
object TokitoEntry {
    /** true once the entry screen was shown during this app process (so it is not repeated on a recreate) */
    @Volatile
    var shownInProcess = false

    /** true once the entry screen is gone (or will not be shown at all); the first-run guide waits for it */
    val finished = MutableStateFlow(false)
}

/**
 * Tokito Laucher: the new way of entering the app.
 *
 * Every time the launcher is opened, the app icon spins in and the app name slides out on its right side.
 * - With an account: "Welcome back, <name>" and the launcher opens by itself (or on tap).
 * - Without an account: buttons to sign in with Microsoft, to use other ways to sign in,
 *   or to enter without an account.
 *
 * @param onLoginMicrosoft opens the account screen on the Microsoft login
 * @param onLoginOther opens the account screen on the other login methods
 */
@Composable
fun TokitoEntryOverlay(
    onLoginMicrosoft: () -> Unit,
    onLoginOther: () -> Unit
) {
    var visible by remember {
        val show = !TokitoEntry.shownInProcess
        TokitoEntry.shownInProcess = true
        mutableStateOf(show)
    }

    if (!visible) {
        LaunchedEffect(Unit) {
            TokitoEntry.finished.value = true
        }
        return
    }

    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    val logo = remember { Animatable(0f) }
    val name = remember { Animatable(0f) }
    val content = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    var ready by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { logo.animateTo(1f, tween(durationMillis = 550, easing = FastOutSlowInEasing)) }
        delay(250)
        launch { name.animateTo(1f, tween(durationMillis = 400, easing = FastOutSlowInEasing)) }
        delay(450)
        ready = true
    }

    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        content.animateTo(1f, tween(durationMillis = 300))
        //signed in already: say hello and open the launcher by itself
        if (AccountsManager.currentAccountFlow.value != null) {
            delay(700)
            leaving = true
        }
    }

    LaunchedEffect(leaving) {
        if (!leaving) return@LaunchedEffect
        fade.animateTo(0f, tween(durationMillis = 300))
        visible = false
        TokitoEntry.finished.value = true
    }

    BackHandler(enabled = !leaving) {
        leaving = true
    }

    val primary = MaterialTheme.colorScheme.primary
    val currentAccount = account

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                //tap to skip, only when the launcher would open by itself anyway
                if (ready && currentAccount != null) leaving = true
            },
        contentAlignment = Alignment.Center
    ) {
        //glow behind the logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.42f),
                    radius = size.minDimension * 0.9f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            //app icon + app name on its right side
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            val s = 0.4f + 0.6f * logo.value
                            scaleX = s
                            scaleY = s
                            rotationZ = (1f - logo.value) * -90f
                            alpha = logo.value
                        },
                    painter = painterResource(R.drawable.tokito_logo),
                    contentDescription = null
                )

                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = name.value
                        translationX = (1f - name.value) * -40.dp.toPx()
                    }
                ) {
                    Text(
                        text = BuildKeys.LAUNCHER_NAME.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = "v" + BuildConfig.VERSION_NAME.substringBefore("-"),
                        style = MaterialTheme.typography.labelLarge,
                        color = primary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.graphicsLayer { alpha = content.value },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (ready) {
                    if (currentAccount != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlayerFace(
                                account = currentAccount,
                                avatarSize = 32.dp
                            )
                            Text(
                                text = stringResource(R.string.tokito_entry_welcome_back, currentAccount.username),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = stringResource(R.string.tokito_entry_tap),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.tokito_entry_welcome, BuildKeys.LAUNCHER_NAME),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        Button(
                            modifier = Modifier.widthIn(min = 260.dp),
                            onClick = {
                                onLoginMicrosoft()
                                leaving = true
                            }
                        ) {
                            Text(text = stringResource(R.string.tokito_entry_microsoft))
                        }
                        OutlinedButton(
                            modifier = Modifier.widthIn(min = 260.dp),
                            onClick = {
                                onLoginOther()
                                leaving = true
                            }
                        ) {
                            Text(text = stringResource(R.string.tokito_entry_other))
                        }
                        TextButton(
                            onClick = { leaving = true }
                        ) {
                            Text(text = stringResource(R.string.tokito_entry_skip))
                        }
                    }
                }
            }
        }
    }
}
