/*
 * PolyJav Laucher - unofficial modified version of Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors (Zalith Launcher 2)
 * Modifications Copyright (C) 2026 PolyJav Laucher contributors
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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import kotlinx.coroutines.delay

/**
 * PolyJav Laucher: full-screen animation shown while Minecraft is being opened.
 *
 * It fades in as soon as the launch flow starts, keeps a rotating ring, a breathing glow and an
 * indeterminate progress bar running, and fades out shortly after the flow ends.
 * The task list dialog of the launch flow is composed after this one, so it stays on top.
 *
 * @param visible true while the game launch flow is running
 */
@Composable
fun PrestigieLaunchAnimation(visible: Boolean) {
    //keep the overlay alive for a moment after [visible] turns false, to play the fade-out
    var shown by remember { mutableStateOf(false) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            shown = true
            fade.animateTo(1f, tween(durationMillis = 350))
        } else if (shown) {
            fade.animateTo(0f, tween(durationMillis = 350))
            delay(50)
            shown = false
        }
    }

    if (!shown) return

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val accent = MaterialTheme.colorScheme.primary
        val accentSecondary = MaterialTheme.colorScheme.tertiary
        val pageColor = MaterialTheme.colorScheme.background

        val transition = rememberInfiniteTransition(label = "prestigie_launch")
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing)
            ),
            label = "ring_rotation"
        )
        val glow by transition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_pulse"
        )
        val breathe by transition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logo_breathe"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fade.value }
                .background(pageColor.copy(alpha = 0.97f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 6.dp.toPx()
                        val inset = stroke / 2f
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        val glowRadius = size.minDimension / 2f * glow

                        //breathing glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.40f), Color.Transparent),
                                center = center,
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = center
                        )

                        //ring track
                        drawArc(
                            color = accent.copy(alpha = 0.16f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = stroke)
                        )

                        //rotating sweep
                        rotate(degrees = rotation, pivot = center) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    0f to Color.Transparent,
                                    0.55f to accent,
                                    0.80f to accentSecondary,
                                    center = center
                                ),
                                startAngle = 0f,
                                sweepAngle = 290f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }

                    //simple monogram in the middle (original, no third-party artwork)
                    Text(
                        modifier = Modifier.graphicsLayer {
                            scaleX = breathe
                            scaleY = breathe
                        },
                        text = "P",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = BuildKeys.LAUNCHER_NAME,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.prestigie_launch_starting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(50)),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.18f)
                )
            }
        }
    }
}
