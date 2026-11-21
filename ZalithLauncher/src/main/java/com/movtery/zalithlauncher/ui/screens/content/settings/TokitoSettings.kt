/*
 * Tokito Launcher
 * Based on Zalith Launcher 2 (Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors).
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

package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.PackImportResult
import com.movtery.zalithlauncher.tokito.TokitoPack
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.LightChunksRow
import com.movtery.zalithlauncher.ui.components.OptimizationPanel
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.ui.theme.GlassMode
import com.movtery.zalithlauncher.ui.theme.LauncherStyle
import com.movtery.zalithlauncher.ui.theme.LauncherWallpaper
import com.movtery.zalithlauncher.ui.theme.applyLauncherStylePreset
import com.movtery.zalithlauncher.ui.theme.ownPalette
import com.movtery.zalithlauncher.ui.theme.tokitoPanelFor
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.viewmodel.BackgroundViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "TokitoSettings"

/**
 * Tokito Launcher settings: launcher style (and the colors of the Liquid Glass), mini wallpapers, ZIP packs
 * (mouse pointers, wallpapers and control HUD), a calmer home screen, the optimization modes (PVP, Survival, Brutal and
 * light and fast chunks) and the style of the game controls.
 *
 * @param backgroundViewModel launcher background (null: the wallpapers cannot be used)
 */
@Composable
fun TokitoStyleSettings(
    backgroundViewModel: BackgroundViewModel?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentStyle = AllSettings.launcherStyle.state
    val currentMode = AllSettings.liquidGlassMode.state

    //changes every time the wallpaper library of the ZIP packs changes
    var libraryVersion by remember { mutableStateOf(0) }
    val library by produceState(emptyList<File>(), libraryVersion) {
        value = withContext(Dispatchers.IO) { TokitoPack.listWallpapers() }
    }

    fun useWallpaper(action: suspend () -> Unit) {
        //over 90% the wallpaper would be hidden by the screens
        if (AllSettings.launcherBackgroundOpacity.getValue() > 90) {
            AllSettings.launcherBackgroundOpacity.save(80)
        }
        scope.launch {
            runCatching { action() }.onFailure {
                Logger.warning(TAG, "Failed to use the wallpaper!", it)
            }
        }
    }

    SettingsCardColumn(modifier = modifier) {
        //launcher style
        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TitleAndSummary(
                    title = stringResource(R.string.tokito_style_title),
                    summary = stringResource(R.string.tokito_style_summary)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LauncherStyle.entries.forEach { style ->
                        StylePreviewTile(
                            style = style,
                            mode = currentMode,
                            label = stringResource(style.nameRes),
                            selected = style == currentStyle,
                            onClick = {
                                if (style != currentStyle) {
                                    AllSettings.launcherStyle.save(style)
                                    //opacity, blur and the wallpaper that go with the style
                                    scope.launch {
                                        runCatching {
                                            applyLauncherStylePreset(context, backgroundViewModel, style, currentMode)
                                        }.onFailure {
                                            Logger.warning(TAG, "Failed to apply the style preset!", it)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                //Liquid Glass: Normal, Leve and Frio
                if (currentStyle == LauncherStyle.LIQUID_GLASS) {
                    TitleAndSummary(
                        title = stringResource(R.string.tokito_glass_mode_title),
                        summary = stringResource(R.string.tokito_glass_mode_summary)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassMode.entries.forEach { mode ->
                            StylePreviewTile(
                                style = LauncherStyle.LIQUID_GLASS,
                                mode = mode,
                                label = stringResource(mode.nameRes),
                                selected = mode == currentMode,
                                onClick = {
                                    if (mode != currentMode) {
                                        AllSettings.liquidGlassMode.save(mode)
                                        scope.launch {
                                            runCatching {
                                                applyLauncherStylePreset(
                                                    context, backgroundViewModel, LauncherStyle.LIQUID_GLASS, mode
                                                )
                                            }.onFailure {
                                                Logger.warning(TAG, "Failed to apply the glass mode!", it)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        //mini wallpapers (the ones of the app and the ones that came in ZIP packs)
        if (backgroundViewModel != null) {
            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TitleAndSummary(
                        title = stringResource(R.string.tokito_wallpapers_title),
                        summary = stringResource(R.string.tokito_wallpapers_summary)
                    )

                    val usedWallpaper = AllSettings.launcherWallpaper.state
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LauncherWallpaper.entries.filter { it.inPicker }.forEach { wallpaper ->
                            WallpaperTile(
                                wallpaper = wallpaper,
                                selected = backgroundViewModel.isValid && usedWallpaper == wallpaper.id,
                                onClick = {
                                    useWallpaper {
                                        backgroundViewModel.importWallpaper(context, wallpaper, pickedByHand = true)
                                    }
                                }
                            )
                        }
                    }

                    if (library.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.tokito_pack_library_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            library.forEach { file ->
                                val id = TokitoPack.wallpaperId(file)
                                LibraryWallpaperTile(
                                    file = file,
                                    selected = backgroundViewModel.isValid && usedWallpaper == id,
                                    onClick = {
                                        useWallpaper { backgroundViewModel.importWallpaperFile(file, id) }
                                    },
                                    onDelete = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) { TokitoPack.deleteWallpaper(file) }
                                            libraryVersion++
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        //ZIP pack: mouse pointers, wallpapers and control HUD
        PackCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            onImported = { libraryVersion++ }
        )

        //calmer home screen
        SwitchSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            unit = AllSettings.homeAnimations,
            title = stringResource(R.string.tokito_home_anim_title),
            summary = stringResource(R.string.tokito_home_anim_summary)
        )

        //optimization modes: PVP, Survival, Brutal and the light and fast chunks
        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TitleAndSummary(
                    title = stringResource(R.string.tokito_opt_title),
                    summary = stringResource(R.string.tokito_opt_summary)
                )
                OptimizationPanel()
            }
        }

        //fast boot: the game opens sooner (goal: 35 seconds)
        SwitchSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            unit = AllSettings.tokitoFastBoot,
            title = stringResource(R.string.tokito_fastboot_title),
            summary = stringResource(R.string.tokito_fastboot_summary)
        )

        //game controls in the style of the launcher
        SwitchSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            unit = AllSettings.controlsFollowLauncherStyle,
            title = stringResource(R.string.tokito_controls_style_title),
            summary = stringResource(R.string.tokito_controls_style_summary)
        )
    }
}

/** Imports a ZIP with mouse pointers, wallpapers and a control HUD */
@Composable
private fun PackCard(
    position: CardPosition,
    onImported: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var importing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PackImportResult?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !importing) {
            importing = true
            scope.launch {
                try {
                    result = TokitoPack.import(context, uri)
                    onImported()
                } catch (e: Exception) {
                    Logger.warning(TAG, "Failed to import the ZIP pack!", e)
                    failure = e.message ?: e.toString()
                } finally {
                    importing = false
                }
            }
        }
    }

    SettingsCard(
        modifier = modifier,
        position = position
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TitleAndSummary(
                title = stringResource(R.string.tokito_pack_title),
                summary = stringResource(R.string.tokito_pack_summary)
            )

            IconTextButton(
                onClick = { picker.launch(arrayOf("*/*")) },
                painter = painterResource(R.drawable.ic_folder_zip_outlined),
                text = stringResource(if (importing) R.string.tokito_pack_importing else R.string.tokito_pack_import),
                enabled = !importing
            )
        }
    }

    result?.let { done ->
        SimpleAlertDialog(
            title = stringResource(R.string.tokito_pack_result_title),
            text = packResultText(done),
            onDismiss = { result = null }
        )
    }

    failure?.let { message ->
        SimpleAlertDialog(
            title = stringResource(R.string.tokito_pack_failed_title),
            text = stringResource(R.string.tokito_pack_failed, message),
            onDismiss = { failure = null }
        )
    }
}

@Composable
private fun packResultText(result: PackImportResult): String {
    val context = LocalContext.current
    val lines = ArrayList<String>()

    if (result.isEmpty) {
        lines += stringResource(R.string.tokito_pack_result_empty)
    }
    if (result.pointers.isNotEmpty()) {
        val names = result.pointers.joinToString(", ") { context.getString(it.nameRes) }
        lines += stringResource(R.string.tokito_pack_result_pointers, result.pointers.size, names)
    }
    if (result.wallpapers > 0) {
        lines += stringResource(R.string.tokito_pack_result_wallpapers, result.wallpapers)
    }
    if (result.huds > 0) {
        lines += stringResource(R.string.tokito_pack_result_huds, result.huds)
    }
    if (result.hudsTooNew > 0) {
        lines += stringResource(R.string.tokito_pack_result_huds_new, result.hudsTooNew)
    }
    if (result.ignored > 0) {
        lines += stringResource(R.string.tokito_pack_result_ignored, result.ignored)
    }
    return lines.joinToString("\n")
}

/**
 * A small drawing of the launcher (rail, top bar, hero and PLAY button) in the given style.
 *
 * @param mode colors of the Liquid Glass style (only used by that style)
 * @param label text under the drawing
 */
@Composable
private fun StylePreviewTile(
    style: LauncherStyle,
    mode: GlassMode,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    //Neon, Minecraft and the Liquid Glass modes Leve and Frio show their own colors
    val scheme = style.ownPalette(mode, true) ?: MaterialTheme.colorScheme
    val frameShape = RoundedCornerShape(12.dp)
    val panelShape = RoundedCornerShape((style.panelCornerDp / 4).dp)
    val gap = if (style.isFloating) 3.dp else 1.dp
    val wallpaper = style.wallpaperFor(mode)

    Column(
        modifier = modifier
            .width(104.dp)
            .clip(frameShape)
            .clickable(onClick = onClick)
            .padding(all = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(frameShape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = frameShape
                )
        ) {
            if (wallpaper != null) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(wallpaper.thumbRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(scheme.background, scheme.surfaceContainer)))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                PreviewPanel(
                    modifier = Modifier
                        .width(11.dp)
                        .fillMaxHeight(),
                    style = style,
                    mode = mode,
                    scheme = scheme,
                    shape = panelShape,
                    withWallpaper = wallpaper != null
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    PreviewPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp),
                        style = style,
                        mode = mode,
                        scheme = scheme,
                        shape = panelShape,
                        withWallpaper = wallpaper != null
                    )

                    PreviewPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        style = style,
                        mode = mode,
                        scheme = scheme,
                        shape = panelShape,
                        withWallpaper = wallpaper != null
                    ) {
                        //PLAY button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(all = 3.dp)
                                .fillMaxWidth(0.7f)
                                .height(6.dp)
                                .clip(panelShape)
                                .background(scheme.primary)
                        )
                    }
                }
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun PreviewPanel(
    style: LauncherStyle,
    mode: GlassMode,
    scheme: ColorScheme,
    shape: Shape,
    withWallpaper: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier.tokitoPanelFor(
            style = style,
            colors = scheme,
            shape = shape,
            tint = scheme.surfaceContainerLow,
            fill = scheme.surfaceContainerLow.copy(alpha = if (withWallpaper) 0.8f else 1f),
            backgroundShown = withWallpaper,
            bordered = true,
            glassBlur = 0,
            applyGlass = false,
            glassMode = mode,
            borderScale = 0.35f
        ),
        content = content
    )
}

@Composable
private fun WallpaperTile(
    wallpaper: LauncherWallpaper,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .width(112.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(all = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(shape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = shape
                ),
            painter = painterResource(wallpaper.thumbRes),
            contentDescription = stringResource(wallpaper.nameRes),
            contentScale = ContentScale.Crop
        )
        Text(
            text = stringResource(wallpaper.nameRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** A wallpaper that came in a ZIP pack, with a small button to remove it from the library */
@Composable
private fun LibraryWallpaperTile(
    file: File,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .width(112.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(all = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(shape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = shape
                )
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = file,
                contentDescription = file.nameWithoutExtension,
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(all = 4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.tokito_pack_delete),
                    tint = Color.White
                )
            }
        }
        Text(
            text = file.nameWithoutExtension,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
