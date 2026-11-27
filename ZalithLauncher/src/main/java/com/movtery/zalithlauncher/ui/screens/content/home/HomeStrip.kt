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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.isTokiAuthAccount
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.TokitoInstalledPack
import com.movtery.zalithlauncher.tokito.TokitoLinks
import com.movtery.zalithlauncher.tokito.TokitoNotices
import com.movtery.zalithlauncher.tokito.TokitoPackInfo
import com.movtery.zalithlauncher.tokito.TokitoUpdates
import com.movtery.zalithlauncher.ui.components.OptimizationDialog
import com.movtery.zalithlauncher.ui.components.TokiAuthPasswordDialog
import com.movtery.zalithlauncher.ui.components.TokitoEntry
import com.movtery.zalithlauncher.ui.components.TokitoNoticesDialog
import com.movtery.zalithlauncher.ui.components.TokitoSkinsDialog
import com.movtery.zalithlauncher.ui.components.TokitoStreamDialog
import com.movtery.zalithlauncher.ui.components.TokitoUpdatesDialog
import com.movtery.zalithlauncher.ui.components.TokitoWhatsNewDialog
import com.movtery.zalithlauncher.ui.components.chat.TokitoChatDialog
import com.movtery.zalithlauncher.ui.components.chat.rememberTokitoChatUnread
import com.movtery.zalithlauncher.ui.theme.tokitoPanel
import kotlinx.coroutines.flow.first

/** Which window of the strip is open */
private enum class StripDialog { NOTICES, UPDATES, MODES, STREAM, SKINS, NEWS, TOKIAUTH, CHAT, ABOUT }

/** One tile of the strip */
private class StripTile(
    val key: String,
    val iconRes: Int,
    val label: String,
    val onClick: () -> Unit,
    /** A number in the corner (0: none) */
    val badge: Int = 0,
    /** A small dot in the corner, when there is something new but no number */
    val dot: Boolean = false,
    /** The tile is lit (a mode is on, there is something to do) */
    val highlight: Boolean = false
)

private val TileWidth = 88.dp
private val TileHeight = 66.dp

/**
 * Tokito Launcher: the "Principais coisas" (main things) strip, at the top of the home screen.
 *
 * It is one row of tiles that the player drags sideways (to the right side, where the other tiles are). The first
 * tiles are what needs attention: the notices of the creators ("Avisos") and the modpacks that got a new version
 * ("Modpacks atualizados", with the number in the corner), so updating is one tap away. After them come the library
 * (mods, textures, modpacks, shaders, worlds), the modes, the skins, the Mini Wallpaper and the news, in the order that
 * the player chose, and then the stream panel, the chat, Discord, the old grid of cards and the notice of the license.
 *
 * @param onOpenLibrary opens a category of the download screen
 * @param onOpenModpackPage opens the page of a modpack of Modrinth, where it can be installed again (updated)
 * @param onOpenWallpaperSettings opens the settings of the Mini Wallpaper
 * @param onOpenAllCards opens the old grid of cards (the versions and the servers of the player)
 * @param onCustomize opens the window where the player chooses what is shown and in which order
 */
@Composable
fun HomeMainStrip(
    onOpenLibrary: (HomeLibraryCategory) -> Unit,
    onOpenModpackPage: (TokitoPackInfo) -> Unit,
    onOpenWallpaperSettings: () -> Unit,
    onOpenAllCards: () -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openLink = LocalHomeLinkOpener.current
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val updates by TokitoUpdates.state.collectAsStateWithLifecycle()
    val latestNotice by TokitoNotices.latest.collectAsStateWithLifecycle()
    val chatUnread by rememberTokitoChatUnread()
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val savedItems = AllSettings.tokitoMainItems.state
    val entries = remember(savedItems) { HomeMainItems.resolve(savedItems) }
    val lastSeenNotice = AllSettings.tokitoLastNoticeId.state
    val mode = AllSettings.launcherOptimizationMode.state

    var dialog by remember { mutableStateOf<StripDialog?>(null) }

    val installed = remember(versions) {
        versions.map { TokitoInstalledPack(name = it.getVersionName(), dir = it.getVersionPath()) }
    }
    //looks for the new versions of the modpacks of the player (once an hour, after the entry screen)
    LaunchedEffect(installed) {
        TokitoEntry.finished.first { it }
        TokitoUpdates.refresh(installed)
    }

    val tiles = buildList {
        add(
            StripTile(
                key = "notices",
                iconRes = R.drawable.ic_chat_info,
                label = stringResource(R.string.home_notices),
                onClick = { dialog = StripDialog.NOTICES },
                dot = latestNotice?.let { it.id > lastSeenNotice } == true,
                highlight = latestNotice?.let { it.id > lastSeenNotice } == true
            )
        )
        add(
            StripTile(
                key = "updates",
                iconRes = R.drawable.ic_update,
                label = stringResource(R.string.home_modpacks_updated),
                onClick = { dialog = StripDialog.UPDATES },
                badge = updates.updateCount,
                highlight = updates.updateCount > 0
            )
        )
        entries.forEach { entry ->
            if (!entry.visible) return@forEach
            when (entry.item) {
                HomeMainItem.LIBRARY -> {
                    add(libraryTile("lib_mods", R.drawable.ic_extension_outlined, stringResource(R.string.tokito_rail_mods)) { onOpenLibrary(HomeLibraryCategory.MODS) })
                    add(libraryTile("lib_textures", R.drawable.ic_format_paint_outlined, stringResource(R.string.tokito_rail_textures)) { onOpenLibrary(HomeLibraryCategory.RESOURCE_PACKS) })
                    add(libraryTile("lib_modpacks", R.drawable.ic_package_2_outlined, stringResource(R.string.tokito_rail_modpacks)) { onOpenLibrary(HomeLibraryCategory.MODPACKS) })
                    add(libraryTile("lib_shaders", R.drawable.ic_lightbulb, stringResource(R.string.tokito_lib_shaders)) { onOpenLibrary(HomeLibraryCategory.SHADERS) })
                    add(libraryTile("lib_worlds", R.drawable.ic_public, stringResource(R.string.tokito_lib_worlds)) { onOpenLibrary(HomeLibraryCategory.WORLDS) })
                }

                HomeMainItem.MODES -> add(
                    StripTile(
                        key = "modes",
                        iconRes = R.drawable.ic_videogame_asset_outlined,
                        label = stringResource(R.string.tokito_main_item_modes),
                        onClick = { dialog = StripDialog.MODES },
                        highlight = mode.isActive
                    )
                )

                HomeMainItem.SKINS -> add(
                    StripTile(
                        key = "skins",
                        iconRes = R.drawable.ic_checkroom,
                        label = stringResource(R.string.tokito_main_item_skins),
                        onClick = { dialog = StripDialog.SKINS }
                    )
                )

                //"Pop-ups" became the Mini Wallpaper: one still photo near the middle of the home screen
                HomeMainItem.POPUPS -> add(
                    StripTile(
                        key = "wallpaper",
                        iconRes = R.drawable.ic_photo_library_outlined,
                        label = stringResource(R.string.tokito_main_item_popups),
                        onClick = onOpenWallpaperSettings
                    )
                )

                HomeMainItem.NEWS -> add(
                    StripTile(
                        key = "news",
                        iconRes = R.drawable.ic_info_outlined,
                        label = stringResource(R.string.tokito_main_item_news),
                        onClick = { dialog = StripDialog.NEWS }
                    )
                )
            }
        }
        add(
            StripTile(
                key = "stream",
                iconRes = R.drawable.ic_video_settings,
                label = stringResource(R.string.home_stream),
                onClick = { dialog = StripDialog.STREAM },
                highlight = mode == com.movtery.zalithlauncher.setting.enums.OptimizationMode.LIVE
            )
        )
        add(
            StripTile(
                key = "chat",
                iconRes = R.drawable.ic_chat_info,
                label = stringResource(R.string.home_chat),
                onClick = { dialog = StripDialog.CHAT },
                badge = chatUnread
            )
        )
        add(
            StripTile(
                key = "discord",
                iconRes = R.drawable.ic_link,
                label = stringResource(R.string.home_discord),
                onClick = { openLink(TokitoLinks.DISCORD) }
            )
        )
        if (account?.isTokiAuthAccount() == true) {
            add(
                StripTile(
                    key = "tokiauth",
                    iconRes = R.drawable.ic_check,
                    label = stringResource(R.string.home_tokiauth),
                    onClick = { dialog = StripDialog.TOKIAUTH }
                )
            )
        }
        add(
            StripTile(
                key = "cards",
                iconRes = R.drawable.ic_dashboard_outlined,
                label = stringResource(R.string.home_cards),
                onClick = onOpenAllCards
            )
        )
        //the notice "Unofficial Modified Version" that the license asks for is always one tap away (and always on the screen too)
        add(
            StripTile(
                key = "about",
                iconRes = R.drawable.ic_info_filled,
                label = stringResource(R.string.home_about),
                onClick = { dialog = StripDialog.ABOUT }
            )
        )
        add(
            StripTile(
                key = "customize",
                iconRes = R.drawable.ic_edit_outlined,
                label = stringResource(R.string.tokito_main_customize),
                onClick = onCustomize
            )
        )
    }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = tiles, key = { it.key }) { tile ->
            StripTileView(tile = tile)
        }
    }

    when (dialog) {
        StripDialog.NOTICES -> TokitoNoticesDialog(
            onOpenWhatsNew = { dialog = StripDialog.NEWS },
            onDismiss = { dialog = null }
        )

        StripDialog.UPDATES -> TokitoUpdatesDialog(
            installed = installed,
            onOpenPack = { pack ->
                dialog = null
                onOpenModpackPage(pack)
            },
            onOpenModpacks = {
                dialog = null
                onOpenLibrary(HomeLibraryCategory.MODPACKS)
            },
            onDismiss = { dialog = null }
        )

        StripDialog.MODES -> OptimizationDialog(onDismiss = { dialog = null })

        StripDialog.STREAM -> TokitoStreamDialog(
            onOpenLibrary = { category ->
                dialog = null
                onOpenLibrary(category)
            },
            onOpenLink = openLink,
            onDismiss = { dialog = null }
        )

        StripDialog.SKINS -> TokitoSkinsDialog(onDismiss = { dialog = null })
        StripDialog.NEWS -> TokitoWhatsNewDialog(onDismiss = { dialog = null })
        StripDialog.TOKIAUTH -> {
            val name = account?.username
            if (name != null) TokiAuthPasswordDialog(userName = name, onDismiss = { dialog = null })
        }

        StripDialog.CHAT -> TokitoChatDialog(onDismiss = { dialog = null })
        StripDialog.ABOUT -> HomeAboutDialog(onDismiss = { dialog = null })
        null -> Unit
    }
}

private fun libraryTile(key: String, iconRes: Int, label: String, onClick: () -> Unit) =
    StripTile(key = key, iconRes = iconRes, label = label, onClick = onClick)

@Composable
private fun StripTileView(tile: StripTile) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(width = TileWidth, height = TileHeight)
            .tokitoPanel(shape = shape, tint = MaterialTheme.colorScheme.surfaceContainerLow)
            .clip(shape)
            .clickable(onClick = tile.onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(22.dp),
                painter = painterResource(tile.iconRes),
                contentDescription = null,
                tint = if (tile.highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tile.label,
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 12.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                color = if (tile.highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        if (tile.badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    text = if (tile.badge > 99) "99+" else tile.badge.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else if (tile.dot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * The notice of the license ("Unofficial Modified Version", the creators, the credit of OneClient).
 * It is the same card that the old grid shows, so the text is only written once.
 */
@Composable
private fun HomeAboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.widthIn(max = 440.dp)) {
            val card = HomeCards.systemCards(emptyList()).firstOrNull()
            if (card != null) card.content()
        }
    }
}
