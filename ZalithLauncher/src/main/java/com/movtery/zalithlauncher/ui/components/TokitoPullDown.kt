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
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.isTokiAuthAccount
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.TokitoPopup
import com.movtery.zalithlauncher.tokito.TokitoPopups
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class PullDialog { NONE, MODES, SKINS, POPUPS, NEWS, TOKIAUTH }

/**
 * Tokito Launcher: the pull-down drawer of the home screen.
 *
 * The pop-ups no longer float over the home screen. A small tab stays parked in one corner (top right,
 * with a mini picture of the first pop-up). Drag it down (or tap it) and the drawer opens with the
 * "Principais coisas" (modes, skins, pop-ups, news) and with the pop-ups. Drag the bar at the bottom of the
 * drawer up, tap the dark area or tap the bar to close it.
 *
 * Videos only play while the drawer is open, one at a time, muted and in a loop.
 *
 * @param isVisible whether the home screen is the screen that the player is looking at
 */
@Composable
fun TokitoPullDown(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { TokitoPopups.load() }

    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var dialog by remember { mutableStateOf(PullDialog.NONE) }

    LaunchedEffect(isVisible) {
        if (!isVisible) progress.snapTo(0f)
    }

    when (dialog) {
        PullDialog.MODES -> OptimizationDialog(onDismiss = { dialog = PullDialog.NONE })
        PullDialog.SKINS -> TokitoSkinsDialog(onDismiss = { dialog = PullDialog.NONE })
        PullDialog.POPUPS -> TokitoPopupsDialog(onDismiss = { dialog = PullDialog.NONE })
        PullDialog.NEWS -> TokitoWhatsNewDialog(onDismiss = { dialog = PullDialog.NONE })
        PullDialog.TOKIAUTH -> {
            val name = AccountsManager.currentAccountFlow.value?.username
            if (name != null) TokiAuthPasswordDialog(userName = name, onDismiss = { dialog = PullDialog.NONE })
        }
        PullDialog.NONE -> {}
    }

    if (!isVisible) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val panelHeight = minOf(maxHeight * 0.8f, 340.dp)
        val panelPx = with(LocalDensity.current) { panelHeight.toPx() }

        fun settle() {
            scope.launch { progress.animateTo(if (progress.value > 0.35f) 1f else 0f) }
        }

        val dragModifier = Modifier.pointerInput(panelPx) {
            detectVerticalDragGestures(
                onDragEnd = { settle() },
                onDragCancel = { settle() }
            ) { change, dy ->
                change.consume()
                scope.launch { progress.snapTo((progress.value + dy / panelPx).coerceIn(0f, 1f)) }
            }
        }

        val open by remember { derivedStateOf { progress.value > 0.001f } }

        //dark area behind the drawer
        if (open) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f * progress.value))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { scope.launch { progress.animateTo(0f) } }
            )
        }

        //the drawer
        if (open) {
            val shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (-(1f - progress.value) * panelPx).roundToInt()) }
                    .fillMaxWidth(0.9f)
                    .height(panelHeight)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                DrawerContent(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    showMedia = progress.value >= 0.99f,
                    onOpenDialog = {
                        dialog = it
                        scope.launch { progress.animateTo(0f) }
                    }
                )
                //bar: drag up or tap to close
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .then(dragModifier)
                        .clickable { scope.launch { progress.animateTo(0f) } },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    )
                }
            }
        }

        //the tab parked in the corner
        if (!open || progress.value < 0.5f) {
            ParkedTab(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 28.dp)
                    .then(dragModifier),
                onClick = { scope.launch { progress.animateTo(1f) } }
            )
        }
    }
}

@Composable
private fun ParkedTab(
    modifier: Modifier,
    onClick: () -> Unit
) {
    val popups = TokitoPopups.popups
    val first = popups.firstOrNull()
    val label = stringResource(R.string.tokito_pulldown_open)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (first != null && AllSettings.tokitoPopupsEnabled.state) {
            AsyncImage(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                model = first.previewFile,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(R.drawable.ic_keyboard_arrow_down),
            contentDescription = label,
            tint = Color.White
        )
    }
}

@Composable
private fun DrawerContent(
    modifier: Modifier,
    showMedia: Boolean,
    onOpenDialog: (PullDialog) -> Unit
) {
    val popups = TokitoPopups.popups
    val enabled = AllSettings.tokitoPopupsEnabled.state
    var playingId by remember { mutableStateOf<String?>(null) }
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val tokiAccount = account?.isTokiAuthAccount() == true

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.tokito_main_title),
            style = MaterialTheme.typography.titleMedium
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconTextButton(
                onClick = { onOpenDialog(PullDialog.MODES) },
                painter = painterResource(R.drawable.ic_rocket_launch_filled),
                text = stringResource(R.string.tokito_main_item_modes)
            )
            IconTextButton(
                onClick = { onOpenDialog(PullDialog.SKINS) },
                painter = painterResource(R.drawable.ic_checkroom),
                text = stringResource(R.string.tokito_main_item_skins)
            )
            IconTextButton(
                onClick = { onOpenDialog(PullDialog.POPUPS) },
                painter = painterResource(R.drawable.ic_photo_library_outlined),
                text = stringResource(R.string.tokito_popups_title)
            )
            if (tokiAccount) {
                IconTextButton(
                    onClick = { onOpenDialog(PullDialog.TOKIAUTH) },
                    painter = painterResource(R.drawable.ic_login),
                    text = stringResource(R.string.tokiauth_set_title)
                )
            }
            IconTextButton(
                onClick = { onOpenDialog(PullDialog.NEWS) },
                painter = painterResource(R.drawable.ic_article_outlined),
                text = stringResource(R.string.tokito_news_card_title)
            )
        }

        Text(
            text = stringResource(R.string.tokito_popups_title),
            style = MaterialTheme.typography.titleSmall
        )
        val shown = popups.filter { it.id !in TokitoPopups.dismissed }
        if (!enabled || shown.isEmpty()) {
            Text(
                text = stringResource(if (enabled) R.string.tokito_popups_empty else R.string.tokito_pulldown_popups_off),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shown, key = { it.id }) { popup ->
                    DrawerPopupCard(
                        popup = popup,
                        playing = showMedia && playingId == popup.id,
                        onClick = {
                            if (popup.isVideo) playingId = if (playingId == popup.id) null else popup.id
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerPopupCard(
    popup: TokitoPopup,
    playing: Boolean,
    onClick: () -> Unit
) {
    val height = 150.dp
    val width = (height * popup.aspect).coerceIn(70.dp, 240.dp)
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
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
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(R.drawable.ic_play_arrow_filled),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}
