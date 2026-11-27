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

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.movtery.zalithlauncher.ui.screens.content.home.HomeLibraryCategory
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.OptimizationMode
import com.movtery.zalithlauncher.tokito.StreamPlatform
import com.movtery.zalithlauncher.tokito.TokitoStream

/**
 * Tokito Launcher: the Stream panel. It helps the player to make a live (TikTok, YouTube, Twitch) that is light
 * and keeps the FPS steady, and to talk (Discord call, or a voice mod in the game).
 *
 * What it really does: the "Live" mode of the launcher (FPS ceiling that leaves room for the video encoder, steady
 * clock, light chunks, bigger shader cache), the settings that work well for each platform with a shortcut to its
 * app and a button that copies the server address, and the shortcuts for the voice (Discord, microphone permission).
 * The video itself is captured and sent by the app of the platform: the launcher does not encode video.
 *
 * @param onOpenLibrary opens a category of the download screen (the mods, to look for a voice mod)
 * @param onOpenLink opens a link inside the launcher
 */
@Composable
fun TokitoStreamDialog(
    onOpenLibrary: (HomeLibraryCategory) -> Unit,
    onOpenLink: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_video_settings),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.stream_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.generic_close)
                        )
                    }
                }
                HorizontalDivider()
                TokitoStreamKeys(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                HorizontalDivider()

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val wide = maxWidth >= 640.dp
                    val scroll = rememberScrollState()
                    val left: @Composable (Modifier) -> Unit = { modifier ->
                        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.stream_intro),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            LiveModeCard()
                            StepsCard()
                            VoiceCard(onOpenMods = { onOpenLibrary(HomeLibraryCategory.MODS) })
                        }
                    }
                    val right: @Composable (Modifier) -> Unit = { modifier ->
                        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.stream_platforms),
                                style = MaterialTheme.typography.titleMedium
                            )
                            StreamPlatform.entries.forEach { platform ->
                                PlatformCard(platform = platform, onOpenLink = onOpenLink)
                            }
                            TipsCard()
                            Text(
                                text = stringResource(R.string.stream_honest),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (wide) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            //each side scrolls on its own
                            left(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                            )
                            right(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            left(Modifier)
                            right(Modifier)
                        }
                    }
                }
            }
        }
    }
}

/** A rounded block of the panel */
@Composable
private fun PanelBlock(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

/** The Live mode: one switch that sets the optimization mode of the launcher (the old mode comes back when it is turned off) */
@Composable
private fun LiveModeCard() {
    val mode = AllSettings.launcherOptimizationMode.state
    val liveOn = mode == OptimizationMode.LIVE

    PanelBlock(title = stringResource(R.string.stream_live_title)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(if (liveOn) R.string.stream_live_on else R.string.stream_live_off),
                style = MaterialTheme.typography.titleSmall
            )
            Switch(
                checked = liveOn,
                onCheckedChange = { on -> TokitoStream.setLive(on) }
            )
        }
        Text(
            text = stringResource(R.string.stream_live_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepsCard() {
    PanelBlock(title = stringResource(R.string.stream_steps_title)) {
        Text(
            text = stringResource(R.string.stream_steps),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TipsCard() {
    PanelBlock(title = stringResource(R.string.stream_tips_title)) {
        Text(
            text = stringResource(R.string.stream_tips),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** One platform: the settings that work well, the server, where the key is, and the buttons */
@Composable
private fun PlatformCard(
    platform: StreamPlatform,
    onOpenLink: (String) -> Unit
) {
    val context = LocalContext.current
    PanelBlock(title = stringResource(platform.labelRes)) {
        Text(
            text = stringResource(platform.bestRes),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(
                R.string.stream_server_line,
                platform.serverUrl ?: stringResource(R.string.stream_server_unknown)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(platform.keyRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTextButton(
                onClick = {
                    //not installed: the page of the platform opens in the launcher
                    if (!TokitoStream.openApp(context, platform.packages)) onOpenLink(platform.webUrl)
                },
                painter = painterResource(R.drawable.ic_play_arrow_filled),
                text = stringResource(R.string.stream_open_app)
            )
            val server = platform.serverUrl
            if (server != null) {
                IconTextButton(
                    onClick = {
                        TokitoStream.copy(context, server)
                        Toast.makeText(context, R.string.stream_copied, Toast.LENGTH_SHORT).show()
                    },
                    painter = painterResource(R.drawable.ic_content_copy_filled),
                    text = stringResource(R.string.stream_copy_server)
                )
            }
        }
    }
}

/** The voice: the Discord call (the safe way), the microphone permission and the voice mods of the game */
@Composable
private fun VoiceCard(onOpenMods: () -> Unit) {
    val context = LocalContext.current
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
    }

    PanelBlock(title = stringResource(R.string.stream_voice_title)) {
        Text(
            text = stringResource(R.string.stream_voice_discord),
            style = MaterialTheme.typography.bodySmall
        )
        IconTextButton(
            onClick = { TokitoStream.openDiscord(context) },
            painter = painterResource(R.drawable.ic_link),
            text = stringResource(R.string.stream_open_discord)
        )

        Text(
            text = stringResource(if (micGranted) R.string.stream_mic_ok else R.string.stream_mic_no),
            style = MaterialTheme.typography.bodySmall,
            color = if (micGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!micGranted) {
            IconTextButton(
                onClick = { askMic.launch(Manifest.permission.RECORD_AUDIO) },
                painter = painterResource(R.drawable.ic_check),
                text = stringResource(R.string.stream_mic_allow)
            )
        }

        Text(
            text = stringResource(R.string.stream_voice_ingame),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconTextButton(
            onClick = onOpenMods,
            painter = painterResource(R.drawable.ic_extension_outlined),
            text = stringResource(R.string.stream_find_voice_mod)
        )
    }
}

/**
 * Tokito Launcher: the keys of the Stream panel. Big buttons for what the player needs while the live is on:
 * mute the microphone, mute the sound, close the screen (all black), the Live mode and Discord. Each key also
 * answers to a letter of a physical keyboard (M, S, B, L, D) while it is on the screen.
 */
@Composable
fun TokitoStreamKeys(modifier: Modifier = Modifier, showLive: Boolean = true) {
    val context = LocalContext.current
    val liveOn = AllSettings.launcherOptimizationMode.state == OptimizationMode.LIVE
    var micMuted by remember { mutableStateOf(TokitoStream.isMicMuted(context)) }
    var soundMuted by remember { mutableStateOf(TokitoStream.isSoundMuted(context)) }
    var blackScreen by remember { mutableStateOf(false) }

    val toggleMic = {
        val target = !micMuted
        TokitoStream.setMicMuted(context, target)
        //what the phone really did: some phones do not let an app mute the microphone
        micMuted = TokitoStream.isMicMuted(context)
        if (micMuted != target) {
            Toast.makeText(context, R.string.stream_key_mic_denied, Toast.LENGTH_LONG).show()
        }
    }
    val toggleSound = {
        TokitoStream.setSoundMuted(context, !soundMuted)
        soundMuted = TokitoStream.isSoundMuted(context)
    }
    val toggleLive = { TokitoStream.setLive(!liveOn) }
    val openDiscord = { TokitoStream.openDiscord(context) }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focus.requestFocus()
        } catch (_: Exception) {
            //the keys were not on the screen yet: the touch keys still work
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.M -> {
                        toggleMic()
                        true
                    }
                    Key.S -> {
                        toggleSound()
                        true
                    }
                    Key.B -> {
                        blackScreen = true
                        true
                    }
                    Key.L -> {
                        if (showLive) toggleLive()
                        showLive
                    }
                    Key.D -> {
                        openDiscord()
                        true
                    }
                    else -> false
                }
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StreamKey(
                modifier = Modifier.weight(1f),
                label = stringResource(if (micMuted) R.string.stream_key_mic_off else R.string.stream_key_mic),
                hint = "M",
                painter = painterResource(if (micMuted) R.drawable.ic_stream_mic_off else R.drawable.ic_stream_mic),
                active = micMuted,
                onClick = toggleMic
            )
            StreamKey(
                modifier = Modifier.weight(1f),
                label = stringResource(if (soundMuted) R.string.stream_key_sound_off else R.string.stream_key_sound),
                hint = "S",
                painter = painterResource(
                    if (soundMuted) R.drawable.ic_stream_volume_off else R.drawable.ic_stream_volume
                ),
                active = soundMuted,
                onClick = toggleSound
            )
            StreamKey(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stream_key_black),
                hint = "B",
                painter = painterResource(R.drawable.ic_stream_screen),
                active = false,
                onClick = { blackScreen = true }
            )
            if (showLive) {
                StreamKey(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.stream_key_live),
                    hint = "L",
                    painter = painterResource(R.drawable.ic_stream_bolt),
                    active = liveOn,
                    onClick = toggleLive
                )
            }
            StreamKey(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stream_key_discord),
                hint = "D",
                painter = painterResource(R.drawable.ic_chat_info),
                active = false,
                onClick = openDiscord
            )
        }
        Text(
            text = stringResource(if (showLive) R.string.stream_keys_hint else R.string.stream_keys_hint_game),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (blackScreen) TokitoBlackScreen(onClose = { blackScreen = false })
}

/** One key: a rounded button with an icon, a short name and, in the corner, the letter of the physical keyboard */
@Composable
private fun StreamKey(
    label: String,
    hint: String,
    painter: Painter,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val content = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(14.dp),
        color = background,
        contentColor = content
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painter,
                    contentDescription = null
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 6.dp),
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.55f)
            )
        }
    }
}

/**
 * "Close the screen": everything black, the lowest brightness, and the screen stays on (so the live keeps running).
 * Two taps (or the back button) bring it back. The launcher only covers its own screen: a live that captures the
 * whole screen shows it black too.
 */
@Composable
internal fun TokitoBlackScreen(onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val params = window.attributes
                params.screenBrightness = 0.01f
                window.attributes = params
                val controller = WindowCompat.getInsetsController(window, view)
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
            onDispose { }
        }

        var hint by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(4000)
            hint = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onClose() })
                },
            contentAlignment = Alignment.Center
        ) {
            if (hint) {
                Text(
                    text = stringResource(R.string.stream_black_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF3A3A3A)
                )
            }
        }
    }
}

/** The Stream keys as a page of the in-game menu (the Live mode is chosen before the game opens, so it is not here) */
@Composable
fun TokitoStreamGameTab(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.stream_title),
            style = MaterialTheme.typography.titleMedium
        )
        TokitoStreamKeys(showLive = false)
        Text(
            text = stringResource(R.string.stream_game_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
