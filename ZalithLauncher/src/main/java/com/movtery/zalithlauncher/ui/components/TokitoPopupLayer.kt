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
