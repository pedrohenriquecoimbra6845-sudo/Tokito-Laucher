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

package com.movtery.zalithlauncher.ui.screens.main

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.screens.BackStackNavKey
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage

/** Places that can be opened from the Tokito side rail */
internal enum class TokitoDestination {
    HOME,
    VERSIONS,
    DOWNLOAD,
    MODS,
    TEXTURES,
    MODPACKS,
    RUST,
    SETTINGS
}

/** Which rail item matches the screen that is open right now (null when none does) */
internal fun tokitoDestinationOf(key: TitledNavKey?): TokitoDestination? {
    if (key == null || key is NormalNavKey.LauncherMain) return TokitoDestination.HOME
    if (key is NormalNavKey.VersionsManager || key is NestedNavKey.VersionSettings) return TokitoDestination.VERSIONS
    if (key is NestedNavKey.Download) {
        return when (key.currentKey) {
            is NestedNavKey.DownloadMod -> TokitoDestination.MODS
            is NestedNavKey.DownloadResourcePack -> TokitoDestination.TEXTURES
            is NestedNavKey.DownloadModPack -> TokitoDestination.MODPACKS
            else -> TokitoDestination.DOWNLOAD
        }
    }
    if (key is NestedNavKey.Settings) return TokitoDestination.SETTINGS
    return null
}

/**
 * Tokito side rail: the app icon on top, then the main places of the launcher
 * (home, versions, downloads, mods, textures, modpacks, Rust Edition) and the settings at the bottom.
 */
@Composable
internal fun TokitoSideRail(
    selected: TokitoDestination?,
    modifier: Modifier = Modifier,
    onSelect: (TokitoDestination) -> Unit
) {
    val middle = listOf(
        TokitoDestination.HOME,
        TokitoDestination.VERSIONS,
        TokitoDestination.DOWNLOAD,
        TokitoDestination.MODS,
        TokitoDestination.TEXTURES,
        TokitoDestination.MODPACKS,
        TokitoDestination.RUST
    )

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //the app icon; the app name is written on its right side, in the top bar
        Image(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelect(TokitoDestination.HOME) },
            painter = painterResource(R.drawable.tokito_logo),
            contentDescription = BuildKeys.LAUNCHER_NAME
        )

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            middle.forEach { destination ->
                TokitoRailEntry(
                    destination = destination,
                    selected = selected,
                    onSelect = onSelect
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        TokitoRailEntry(
            destination = TokitoDestination.SETTINGS,
            selected = selected,
            onSelect = onSelect
        )
    }
}

@Composable
private fun TokitoRailEntry(
    destination: TokitoDestination,
    selected: TokitoDestination?,
    onSelect: (TokitoDestination) -> Unit
) {
    val iconRes = when (destination) {
        TokitoDestination.HOME -> R.drawable.ic_home_filled
        TokitoDestination.VERSIONS -> R.drawable.ic_sports_esports_filled
        TokitoDestination.DOWNLOAD -> R.drawable.ic_download_2_filled
        TokitoDestination.MODS -> R.drawable.ic_extension_outlined
        TokitoDestination.TEXTURES -> R.drawable.ic_format_paint_outlined
        TokitoDestination.MODPACKS -> R.drawable.ic_package_2_outlined
        TokitoDestination.RUST -> R.drawable.ic_rocket_launch_filled
        TokitoDestination.SETTINGS -> R.drawable.ic_settings_filled
    }
    val labelRes = when (destination) {
        TokitoDestination.HOME -> R.string.tokito_rail_home
        TokitoDestination.VERSIONS -> R.string.tokito_rail_versions
        TokitoDestination.DOWNLOAD -> R.string.tokito_rail_download
        TokitoDestination.MODS -> R.string.tokito_rail_mods
        TokitoDestination.TEXTURES -> R.string.tokito_rail_textures
        TokitoDestination.MODPACKS -> R.string.tokito_rail_modpacks
        TokitoDestination.RUST -> R.string.tokito_rail_rust
        TokitoDestination.SETTINGS -> R.string.tokito_rail_settings
    }

    val isSelected = destination == selected
    val accent = MaterialTheme.colorScheme.primary
    val contentColor = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) accent.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
            .then(
                if (isSelected) Modifier.border(width = 1.5.dp, color = accent, shape = shape) else Modifier
            )
            .clickable { onSelect(destination) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TokitoPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

/**
 * Tokito top bar: the app name and version (written on the right side of the app icon of the rail),
 * the title of the open screen, the version/account selectors and the quick buttons.
 */
@Composable
internal fun TokitoTopBar(
    mainScreenKey: TitledNavKey?,
    inLauncherScreen: Boolean,
    taskRunning: Boolean,
    isTasksExpanded: Boolean,
    modifier: Modifier = Modifier,
    onScreenBack: () -> Unit,
    toAccountScreen: () -> Unit,
    toVersionsScreen: () -> Unit,
    toMultiplayerScreen: () -> Unit,
    openFileManager: () -> Unit,
    changeExpandedState: () -> Unit,
) {
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val version by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val inMultiplayerScreen = mainScreenKey is NormalNavKey.Multiplayer

    Row(
        modifier = modifier.padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(visible = !inLauncherScreen) {
            IconButton(
                onClick = {
                    if (!inLauncherScreen) {
                        //only allowed to go back when not on the home screen
                        backDispatcher?.onBackPressed() ?: run {
                            onScreenBack()
                        }
                    }
                }
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.generic_back)
                )
            }
        }

        //app name + version
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = BuildKeys.LAUNCHER_NAME.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                maxLines = 1
            )
            Text(
                text = "v" + BuildConfig.VERSION_NAME.substringBefore("-"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }

        //title of the open screen (settings, downloads...)
        if (!inLauncherScreen) {
            val parentRes = mainScreenKey?.title
            val childRes = (mainScreenKey as? BackStackNavKey<*>)?.currentKey?.title

            Crossfade(
                modifier = Modifier.weight(1f, fill = false),
                targetState = parentRes to childRes
            ) { (parent, child) ->
                if (parent != null) {
                    val titleText = if (child != null) {
                        androidText(parent, androidText(" - "), child)
                    } else {
                        parent
                    }

                    AndroidStringText(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(
            visible = !(isTasksExpanded || taskRunning),
            enter = slideInVertically(
                initialOffsetY = { -50 }
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -50 }
            ) + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .clip(shape = MaterialTheme.shapes.large)
                    .clickable { changeExpandedState() }
                    .padding(all = 8.dp)
                    .width(100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(modifier = Modifier.weight(1f))
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.ic_assignment_filled),
                    contentDescription = stringResource(R.string.main_task_menu)
                )
            }
        }

        //version selector
        TokitoPill(onClick = toVersionsScreen) {
            VersionIconImage(
                version = version,
                modifier = Modifier.size(20.dp)
            )
            Text(
                modifier = Modifier.widthIn(max = 120.dp),
                text = version?.getVersionName() ?: stringResource(R.string.versions_manage_no_versions),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                contentDescription = null
            )
        }

        //account selector
        TokitoPill(onClick = toAccountScreen) {
            val currentAccount = account
            if (currentAccount != null) {
                PlayerFace(
                    account = currentAccount,
                    avatarSize = 24.dp
                )
            } else {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null
                )
            }
            Text(
                modifier = Modifier.widthIn(max = 100.dp),
                text = currentAccount?.username ?: stringResource(R.string.account_add_new_account),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                contentDescription = null
            )
        }

        IconButton(onClick = openFileManager) {
            Icon(
                painter = painterResource(R.drawable.ic_folder_filled),
                contentDescription = null
            )
        }

        IconButton(
            onClick = {
                if (!inMultiplayerScreen) toMultiplayerScreen()
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_group_filled),
                contentDescription = stringResource(R.string.terracotta),
                tint = if (inMultiplayerScreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
