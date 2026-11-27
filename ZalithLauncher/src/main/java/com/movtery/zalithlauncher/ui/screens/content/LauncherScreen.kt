/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
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

package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.movtery.cardgrid.state.rememberCardGridState
import com.movtery.guide.guideNode
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.addon.platform.Platform
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformClasses
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.TokitoPackInfo
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.TokitoEntry
import com.movtery.zalithlauncher.ui.components.TokitoNoticeHost
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.rememberAnimatedContext
import com.movtery.zalithlauncher.ui.guide.GuideKeys
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.home.HomeGrid
import com.movtery.zalithlauncher.ui.screens.content.home.HomeLibraryCategory
import com.movtery.zalithlauncher.ui.screens.content.home.HomeMainStrip
import com.movtery.zalithlauncher.ui.screens.content.home.HomeMiniWallpaper
import com.movtery.zalithlauncher.ui.screens.content.home.HomeMiniWallpaperDialog
import com.movtery.zalithlauncher.ui.screens.content.home.LocalHomeLibraryOpener
import com.movtery.zalithlauncher.ui.screens.content.home.LocalHomeLinkOpener
import com.movtery.zalithlauncher.ui.screens.content.home.TokitoHero
import com.movtery.zalithlauncher.ui.screens.content.home.TokitoMainCustomizeDialog
import com.movtery.zalithlauncher.ui.screens.content.home.server.LocalServerCardQuickPlay
import com.movtery.zalithlauncher.ui.screens.content.home.version.LocalHomeCardLauncher
import com.movtery.zalithlauncher.ui.screens.content.home.version.LocalHomeCardVersionSettings
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.flow.first

/** Tokito Launcher: the PLAY card never gets narrower than this, whatever the player chose (and never wider than 60% of the screen) */
private val MinHeroWidth = 168.dp

/** Height of the PLAY card, in widths (3D character, name and the PLAY button fit in it) */
private const val HeroHeightRatio = 1.45f

/**
 * Tokito Launcher: the home screen.
 *
 * - On top: the "Principais coisas" strip (notices, updated modpacks, library, modes, skins, stream, chat...).
 * - Near the middle: the Mini Wallpaper (one picture that the player chooses).
 * - Bottom right: the small PLAY card (3D character, PLAY, account and version), with the size and the background that
 *   the player chose.
 * - At the bottom: the notice of the unofficial version (the cards of versions and servers are in "Meus cartões").
 */
@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    onLaunchGame: (Version?) -> Unit,
    onQuickPlayServer: (Version?, String) -> Unit,
    onOpenLink: (String) -> Unit,
    startGuideOnce: (GuideKeys.Keys) -> Unit,
) {
    LaunchedEffect(Unit) {
        //Tokito Launcher: the guide starts only after the entry screen is gone
        TokitoEntry.finished.first { it }
        //发起新手引导
        startGuideOnce(GuideKeys.Main)
    }

    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        val toAccountManageScreen: () -> Unit = {
            backStackViewModel.mainScreen.navigateTo(
                screenKey = NormalNavKey.AccountManager(FirstLoginMenu.NONE)
            )
        }
        val toVersionManageScreen: () -> Unit = {
            backStackViewModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NormalNavKey.VersionsManager
            )
        }
        val toVersionSettingsScreen: () -> Unit = {
            VersionsManager.currentVersion.value?.let { version ->
                navigateToVersions(version)
            }
        }
        val openLibrary: (HomeLibraryCategory) -> Unit = { category ->
            backStackViewModel.navigateToDownload(
                when (category) {
                    HomeLibraryCategory.MODS -> backStackViewModel.downloadModScreen
                    HomeLibraryCategory.RESOURCE_PACKS -> backStackViewModel.downloadResourcePackScreen
                    HomeLibraryCategory.MODPACKS -> backStackViewModel.downloadModPackScreen
                    HomeLibraryCategory.SHADERS -> backStackViewModel.downloadShadersScreen
                    HomeLibraryCategory.WORLDS -> backStackViewModel.downloadSavesScreen
                }
            )
        }
        //the page of a modpack of Modrinth, where it can be installed again (updated)
        val openModpackPage: (TokitoPackInfo) -> Unit = { pack ->
            backStackViewModel.navigateToDownload(
                targetScreen = backStackViewModel.downloadModPackScreen.apply {
                    navigateTo(
                        NormalNavKey.DownloadAddon(
                            platform = Platform.MODRINTH,
                            projectId = pack.projectId,
                            classes = PlatformClasses.MOD_PACK,
                            iconUrl = pack.iconUrl
                        )
                    )
                }
            )
        }

        var showCustomize by remember { mutableStateOf(false) }
        var showWallpaper by remember { mutableStateOf(false) }
        var showCards by remember { mutableStateOf(false) }

        CompositionLocalProvider(
            LocalUriHandler provides object : UriHandler {
                override fun openUri(uri: String) {
                    onOpenLink(uri)
                }
            },
            LocalHomeLinkOpener provides onOpenLink,
            LocalHomeLibraryOpener provides openLibrary
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    //"Principais coisas": one strip at the top, dragged sideways
                    HomeMainStrip(
                        modifier = Modifier
                            .fillMaxWidth()
                            .guideNode(
                                key = GuideKeys.Main.Step.CardTip,
                                holeRadius = 0.dp
                            ),
                        onOpenLibrary = openLibrary,
                        onOpenModpackPage = openModpackPage,
                        onOpenWallpaperSettings = { showWallpaper = true },
                        onOpenAllCards = { showCards = true },
                        onCustomize = { showCustomize = true }
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        //the size that the player chose (in pixels of the screen), kept inside what fits
                        val chosen = with(LocalDensity.current) { AllSettings.homeHeroWidthPx.state.toDp() }
                        val heroWidth = chosen.coerceAtLeast(MinHeroWidth).coerceAtMost(maxWidth * 0.6f)
                        val heroHeight = minOf(heroWidth * HeroHeightRatio, maxHeight)

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            //Mini Wallpaper: near the middle, in the space that the PLAY card leaves
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                HomeMiniWallpaper(
                                    visible = isVisible,
                                    onClick = { showWallpaper = true }
                                )
                            }

                            //the small PLAY card, at the bottom right
                            Box(
                                modifier = Modifier
                                    .width(heroWidth)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                TokitoHero(
                                    modifier = Modifier
                                        .width(heroWidth)
                                        .height(heroHeight),
                                    onLaunchGame = onLaunchGame,
                                    toAccountManageScreen = toAccountManageScreen,
                                    toVersionManageScreen = toVersionManageScreen,
                                    toVersionSettingsScreen = toVersionSettingsScreen,
                                    isVisible = isVisible
                                )
                            }
                        }
                    }

                    //the license asks for this notice to stay visible
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.home_footer_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                //"Novidades" after an update and the announcements of the creators
                TokitoNoticeHost(isVisible = isVisible)
            }

            if (showCustomize) {
                TokitoMainCustomizeDialog(onDismiss = { showCustomize = false })
            }
            if (showWallpaper) {
                HomeMiniWallpaperDialog(onDismiss = { showWallpaper = false })
            }
            if (showCards) {
                AllCardsDialog(
                    onLaunchGame = onLaunchGame,
                    onQuickPlayServer = onQuickPlayServer,
                    onOpenVersionSettings = navigateToVersions,
                    onOpenLibrary = openLibrary,
                    onOpenLink = onOpenLink,
                    onDismiss = { showCards = false }
                )
            }
        }
    }
}

/**
 * "Meus cartões": the old grid of the home screen (the versions and the servers of the player, the
 * license notice) in a window of its own. Anything that leaves the home screen closes it.
 */
@Composable
private fun AllCardsDialog(
    onLaunchGame: (Version) -> Unit,
    onQuickPlayServer: (Version?, String) -> Unit,
    onOpenVersionSettings: (Version) -> Unit,
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
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(R.drawable.ic_dashboard_outlined),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.home_cards),
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

                ContentMenu(
                    modifier = Modifier.weight(1f),
                    isVisible = true,
                    onLaunchGame = { version ->
                        onDismiss()
                        onLaunchGame(version)
                    },
                    onQuickPlayServer = { version, address ->
                        onDismiss()
                        onQuickPlayServer(version, address)
                    },
                    onOpenVersionSettings = { version ->
                        onDismiss()
                        onOpenVersionSettings(version)
                    },
                    onOpenLibrary = { category ->
                        onDismiss()
                        onOpenLibrary(category)
                    },
                    onOpenLink = onOpenLink
                )
            }
        }
    }
}

@Composable
private fun ContentMenu(
    isVisible: Boolean,
    onLaunchGame: (Version) -> Unit,
    onQuickPlayServer: (Version?, String) -> Unit,
    onOpenVersionSettings: (Version) -> Unit,
    onOpenLibrary: (HomeLibraryCategory) -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)
    val gridState = rememberCardGridState()

    CompositionLocalProvider(
        LocalHomeCardLauncher provides onLaunchGame,
        LocalHomeCardVersionSettings provides onOpenVersionSettings,
        LocalServerCardQuickPlay provides onQuickPlayServer,
        LocalHomeLibraryOpener provides onOpenLibrary,
        LocalHomeLinkOpener provides onOpenLink
    ) {
        HomeGrid(
            state = gridState,
            isVisible = isVisible,
            modifier = modifier
                .fillMaxSize()
                .animated(ctx)
        )
    }
}
