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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.PopupImportResult
import com.movtery.zalithlauncher.tokito.TokitoPopup
import com.movtery.zalithlauncher.tokito.TokitoPopups
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Tokito Launcher: the pop-ups of the home screen, small floating frames with a photo or a video
 * (like the mini wallpapers of other launchers, but on the screen itself).
 *
 * - Drag a frame to move it. The X closes it until the next time the launcher is opened.
 * - A tap on a video stops or starts it.
 * - Videos play muted and in a loop, only while the home screen is on the screen, and at most
 *   [TokitoPopups.MAX_PLAYING_VIDEOS] at the same time. The others show a still frame.
 *
 * The layer does not catch any touch outside the frames.
 *
 * @param isVisible whether the home screen is the screen that the player is looking at
 */
@Composable
fun TokitoPopupLayer(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { TokitoPopups.load() }

    val enabled = AllSettings.tokitoPopupsEnabled.state
    val popups = TokitoPopups.popups
    val dismissed = TokitoPopups.dismissed
    //videos that the player stopped with a tap
    val stopped = remember { mutableStateListOf<String>() }

    AnimatedVisibility(
        visible = isVisible && enabled,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val areaWidth = maxWidth
            val areaHeight = maxHeight

            val shown = popups.filter { it.id !in dismissed }
            val playing = shown
                .filter { it.isVideo && it.id !in stopped }
                .take(TokitoPopups.MAX_PLAYING_VIDEOS)
                .map { it.id }

            shown.forEach { popup ->
                key(popup.id) {
                    PopupFrame(
                        popup = popup,
                        areaWidth = areaWidth,
                        areaHeight = areaHeight,
                        playing = popup.id in playing,
                        onToggleVideo = {
                            if (popup.id in stopped) stopped.remove(popup.id) else stopped.add(popup.id)
                        },
                        onClose = { dismissed.add(popup.id) }
                    )
                }
            }
        }
    }
}

/** Size of a frame: the width of its size option, made smaller when it would not fit in the screen */
private fun popupFrameSize(popup: TokitoPopup, areaWidth: Dp, areaHeight: Dp): Pair<Dp, Dp> {
    val base = when (popup.size) {
        0 -> 92.dp
        1 -> 132.dp
        else -> 188.dp
    }
    var width = base
    var height = width / popup.aspect

    val maxHeight = areaHeight * 0.5f
    if (height > maxHeight) {
        height = maxHeight
        width = height * popup.aspect
    }
    val maxWidth = areaWidth * 0.36f
    if (width > maxWidth) {
        width = maxWidth
        height = width / popup.aspect
    }
    return width to height
}

@Composable
private fun PopupFrame(
    popup: TokitoPopup,
    areaWidth: Dp,
    areaHeight: Dp,
    playing: Boolean,
    onToggleVideo: () -> Unit,
    onClose: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val (width, height) = remember(popup.size, popup.aspect, areaWidth, areaHeight) {
        popupFrameSize(popup, areaWidth, areaHeight)
    }

    //the frame moves inside the free space of the screen: 0 is the start and 1 is the end
    val freeWidth by rememberUpdatedState(with(density) { (areaWidth - width).toPx() }.coerceAtLeast(0f))
    val freeHeight by rememberUpdatedState(with(density) { (areaHeight - height).toPx() }.coerceAtLeast(0f))
    var fractionX by remember(popup.id) { mutableFloatStateOf(popup.x) }
    var fractionY by remember(popup.id) { mutableFloatStateOf(popup.y) }

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (freeWidth * fractionX).roundToInt(),
                    y = (freeHeight * fractionY).roundToInt()
                )
            }
            .size(width = width, height = height)
            .clip(shape)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.4f), shape = shape)
            .pointerInput(popup.id) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch { TokitoPopups.move(popup.id, fractionX, fractionY) }
                    },
                    onDragCancel = {
                        scope.launch { TokitoPopups.move(popup.id, fractionX, fractionY) }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    if (freeWidth > 0f) {
                        fractionX = (fractionX + dragAmount.x / freeWidth).coerceIn(0f, 1f)
                    }
                    if (freeHeight > 0f) {
                        fractionY = (fractionY + dragAmount.y / freeHeight).coerceIn(0f, 1f)
                    }
                }
            }
            .clickable(enabled = popup.isVideo, onClick = onToggleVideo)
    ) {
        //the photo, or the still frame of the video (it stays behind the video until the first picture is ready)
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = popup.previewFile,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        if (popup.isVideo) {
            if (playing) {
                VideoPlayer(
                    videoUri = Uri.fromFile(popup.mediaFile),
                    modifier = Modifier.fillMaxSize(),
                    autoPlay = true,
                    loop = true,
                    muted = true
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_play_arrow_filled),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.tokito_popup_close),
                tint = Color.White
            )
        }
    }
}

/**
 * Tokito Launcher: the window to manage the pop-ups. Add photos and videos, choose the size of each one,
 * bring back the ones closed with the X, remove them, or turn them all off.
 */
@Composable
fun TokitoPopupsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val popups = TokitoPopups.popups
    val dismissed = TokitoPopups.dismissed
    val enabled = AllSettings.tokitoPopupsEnabled.state

    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { TokitoPopups.load() }

    val addMedia: (Uri?, Boolean) -> Unit = { uri, video ->
        if (uri != null && !busy) {
            busy = true
            message = null
            scope.launch {
                val result = TokitoPopups.import(context, uri, video)
                message = when (result) {
                    PopupImportResult.ADDED -> context.getString(R.string.tokito_popups_added)
                    PopupImportResult.FULL -> context.getString(R.string.tokito_popups_full, TokitoPopups.MAX_POPUPS)
                    PopupImportResult.TOO_BIG -> context.getString(R.string.tokito_popups_too_big)
                    PopupImportResult.FAILED -> context.getString(R.string.tokito_popups_failed)
                }
                busy = false
            }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        addMedia(uri, false)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        addMedia(uri, true)
    }

    SimpleAlertDialog(
        title = stringResource(R.string.tokito_popups_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.tokito_popups_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.tokito_popups_enabled),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { AllSettings.tokitoPopupsEnabled.save(it) }
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconTextButton(
                        onClick = { photoPicker.launch(arrayOf("image/*")) },
                        painter = painterResource(R.drawable.ic_image_outlined),
                        text = stringResource(R.string.tokito_popups_add_photo),
                        enabled = !busy
                    )
                    IconTextButton(
                        onClick = { videoPicker.launch(arrayOf("video/*")) },
                        painter = painterResource(R.drawable.ic_movie_filled),
                        text = stringResource(R.string.tokito_popups_add_video),
                        enabled = !busy
                    )
                }

                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                message?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (popups.isEmpty()) {
                    Text(
                        text = stringResource(R.string.tokito_popups_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    popups.forEach { popup ->
                        key(popup.id) {
                            PopupRow(
                                popup = popup,
                                hidden = popup.id in dismissed,
                                onSize = { size -> scope.launch { TokitoPopups.setSize(popup.id, size) } },
                                onShowAgain = { dismissed.remove(popup.id) },
                                onRemove = { scope.launch { TokitoPopups.remove(popup.id) } }
                            )
                        }
                    }
                }
            }
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun PopupRow(
    popup: TokitoPopup,
    hidden: Boolean,
    onSize: (Int) -> Unit,
    onShowAgain: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.medium),
            model = popup.previewFile,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(if (popup.isVideo) R.string.tokito_popups_video else R.string.tokito_popups_photo),
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(
                    0 to R.string.tokito_popups_size_small,
                    1 to R.string.tokito_popups_size_medium,
                    2 to R.string.tokito_popups_size_large
                ).forEach { (value, labelRes) ->
                    CheckChip(
                        selected = popup.size == value,
                        onClick = { onSize(value) },
                        label = { Text(text = stringResource(labelRes)) }
                    )
                }
            }
            if (hidden) {
                TextButton(onClick = onShowAgain) {
                    Text(text = stringResource(R.string.tokito_popups_show_again))
                }
            }
        }

        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_delete_outlined),
                contentDescription = stringResource(R.string.tokito_popups_remove)
            )
        }
    }
}
