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

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.TokitoHomeMedia
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.VideoPlayer
import com.movtery.zalithlauncher.ui.theme.LauncherStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Value of [AllSettings.homeHeroBackdrop] for a photo and for a video (anything else is the default backdrop) */
private const val BACKDROP_PHOTO = "photo"
private const val BACKDROP_VIDEO = "video"

/**
 * Tokito Launcher: what is behind the character in the small PLAY card.
 *
 * The default is the Minecraft-style grid floor. The player can choose a photo or a video instead (the video is
 * muted, loops, and only plays while the home screen is the one that is being looked at). A dark gradient keeps the
 * name, the chips and the PLAY bar readable on any picture.
 *
 * @param playVideo whether a video backdrop may play now (the home screen is visible)
 */
@Composable
internal fun HomeHeroBackdrop(
    style: LauncherStyle,
    backgroundShown: Boolean,
    playVideo: Boolean,
    modifier: Modifier = Modifier
) {
    val mode = AllSettings.homeHeroBackdrop.state
    val fileName = AllSettings.homeHeroBackdropFile.state
    val file = remember(fileName) { TokitoHomeMedia.fileOf(fileName) }

    when {
        mode == BACKDROP_PHOTO && file != null -> Box(modifier = modifier) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = file,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            HeroScrim()
        }

        mode == BACKDROP_VIDEO && file != null -> Box(modifier = modifier) {
            //a still gradient under the video: it is what shows until the first frame is ready or when the video is paused
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    )
            )
            if (playVideo) {
                VideoPlayer(
                    videoUri = Uri.fromFile(file),
                    modifier = Modifier.fillMaxSize(),
                    autoPlay = true,
                    loop = true,
                    muted = true
                )
            }
            HeroScrim()
        }

        else -> TokitoGridBackdrop(
            modifier = modifier,
            style = style,
            backgroundShown = backgroundShown
        )
    }
}

/** Dark at the top (chips) and at the bottom (PLAY bar), clear in the middle (the character) */
@Composable
private fun HeroScrim() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.30f),
                    0.45f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.55f)
                )
            )
    )
}

/**
 * Tokito Launcher: settings of the small PLAY card: its width (300 to 900 pixels of the screen) and its backdrop
 * (default, a photo or a video of the gallery).
 */
@Composable
fun HomeHeroSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedWidth = AllSettings.homeHeroWidthPx.state
    val mode = AllSettings.homeHeroBackdrop.state
    //while the slider moves only this value changes; it is saved when the finger is lifted
    var sliding by remember { mutableFloatStateOf(savedWidth.toFloat()) }
    var busy by remember { mutableStateOf(false) }

    fun handle(result: TokitoHomeMedia.Imported, newMode: String) {
        val name = result.name
        when {
            result.status == TokitoHomeMedia.Status.OK && name != null -> {
                AllSettings.homeHeroBackdropFile.save(name)
                AllSettings.homeHeroBackdrop.save(newMode)
                Toast.makeText(context, R.string.home_pick_done, Toast.LENGTH_SHORT).show()
            }

            result.status == TokitoHomeMedia.Status.TOO_BIG -> {
                val text = context.getString(R.string.home_pick_video_big, TokitoHomeMedia.MAX_VIDEO_MB)
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            }

            else -> Toast.makeText(context, R.string.home_pick_failed, Toast.LENGTH_LONG).show()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                handle(TokitoHomeMedia.importPhoto(context, uri, TokitoHomeMedia.SLOT_HERO), BACKDROP_PHOTO)
                busy = false
            }
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                handle(TokitoHomeMedia.importVideo(context, uri, TokitoHomeMedia.SLOT_HERO), BACKDROP_VIDEO)
                busy = false
            }
        }
    }

    SimpleAlertDialog(
        title = stringResource(R.string.home_hero_settings_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.home_hero_size, sliding.roundToInt()),
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = sliding,
                    onValueChange = { sliding = it },
                    onValueChangeFinished = { AllSettings.homeHeroWidthPx.save(sliding.roundToInt()) },
                    valueRange = 300f..900f
                )

                Text(
                    text = stringResource(R.string.home_hero_backdrop),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode != BACKDROP_PHOTO && mode != BACKDROP_VIDEO,
                        onClick = { AllSettings.homeHeroBackdrop.save("") },
                        label = { Text(stringResource(R.string.home_hero_bd_default)) }
                    )
                    FilterChip(
                        selected = mode == BACKDROP_PHOTO,
                        onClick = { photoPicker.launch(arrayOf("image/*")) },
                        label = { Text(stringResource(R.string.home_hero_bd_photo)) }
                    )
                    FilterChip(
                        selected = mode == BACKDROP_VIDEO,
                        onClick = { videoPicker.launch(arrayOf("video/*")) },
                        label = { Text(stringResource(R.string.home_hero_bd_video)) }
                    )
                }
                Text(
                    text = stringResource(R.string.home_hero_bd_hint, TokitoHomeMedia.MAX_VIDEO_MB),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                IconTextButton(
                    onClick = {
                        sliding = AllSettings.homeHeroWidthPx.defaultValue.toFloat()
                        AllSettings.homeHeroWidthPx.save(AllSettings.homeHeroWidthPx.defaultValue)
                        AllSettings.homeHeroBackdrop.save("")
                        AllSettings.homeHeroBackdropFile.save("")
                        scope.launch(Dispatchers.IO) { TokitoHomeMedia.clear(TokitoHomeMedia.SLOT_HERO) }
                    },
                    painter = painterResource(R.drawable.ic_update),
                    text = stringResource(R.string.home_reset)
                )
            }
        },
        onDismiss = onDismiss
    )
}
