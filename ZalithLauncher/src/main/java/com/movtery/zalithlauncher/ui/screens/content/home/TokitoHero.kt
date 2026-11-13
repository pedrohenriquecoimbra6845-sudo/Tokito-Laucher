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

package com.movtery.zalithlauncher.ui.screens.content.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.guide.GuideSide
import com.movtery.guide.guideNode
import com.movtery.petlib.PetAnimation
import com.movtery.petlib.PetTapReaction
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.getAccountTypeName
import com.movtery.zalithlauncher.game.account.wardrobe.toArmModel
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.components.PetModelView
import com.movtery.zalithlauncher.ui.guide.GuideKeys
import com.movtery.zalithlauncher.ui.screens.content.elements.CommonVersionInfoLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tokito hero: the big center of the home screen.
 *
 * A perspective grid "floor" with the player's 3D skin standing on it, the player name on top
 * and a big PLAY button (with a version dropdown) at the bottom.
 *
 * @param onLaunchGame starts the game; null means "the selected version"
 */
@Composable
fun TokitoHero(
    onLaunchGame: (Version?) -> Unit,
    toAccountManageScreen: () -> Unit,
    toVersionManageScreen: () -> Unit,
    toVersionSettingsScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val refreshWardrobe by AccountsManager.refreshWardrobe.collectAsStateWithLifecycle()
    val version by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()

    val shape = RoundedCornerShape(28.dp)

    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = shape)
    ) {
        TokitoGridBackdrop(modifier = Modifier.fillMaxSize())

        //with little height the 3D model would be tiny: show the face instead
        val showModel = maxHeight >= 250.dp
        val currentAccount = account

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TokitoPlayerChip(
                account = currentAccount,
                onClick = toAccountManageScreen
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (showModel) {
                    val armModel = currentAccount?.skinModelType?.toArmModel()
                    val skinStamp = currentAccount?.getSkinFile()?.lastModified()
                    val capeStamp = currentAccount?.getCapeFile()?.lastModified()
                    val skinBytes by produceState<ByteArray?>(null, currentAccount, refreshWardrobe, skinStamp) {
                        value = withContext(Dispatchers.IO) {
                            currentAccount?.getSkinFile()?.takeIf { it.exists() }
                                ?.inputStream()?.use { it.readBytes() }
                        }
                    }
                    val capeBytes by produceState<ByteArray?>(null, currentAccount, refreshWardrobe, capeStamp) {
                        value = withContext(Dispatchers.IO) {
                            currentAccount?.getCapeFile()?.takeIf { it.exists() }
                                ?.inputStream()?.use { it.readBytes() }
                        }
                    }

                    PetModelView(
                        modifier = Modifier.fillMaxSize(),
                        skin = skinBytes,
                        cape = capeBytes,
                        arm = armModel,
                        yaw = Math.toRadians(-28.0).toFloat(),
                        animation = PetAnimation.Idle,
                        tapReaction = PetTapReaction.Interact,
                        dragYaw = true
                    )
                } else if (currentAccount != null) {
                    PlayerFace(
                        account = currentAccount,
                        avatarSize = 72.dp
                    )
                }
            }

            TokitoLaunchBar(
                version = version,
                isRefreshing = isRefreshing,
                onLaunchGame = onLaunchGame,
                toVersionManageScreen = toVersionManageScreen,
                toVersionSettingsScreen = toVersionSettingsScreen
            )
        }
    }
}

/** Player name (and account type) shown above the character. Tap to manage the accounts. */
@Composable
private fun TokitoPlayerChip(
    account: Account?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .guideNode(
                key = GuideKeys.Main.Step.Account,
                preferSide = GuideSide.Below,
                holeRadius = 24.dp
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (account != null) {
            PlayerFace(
                account = account,
                avatarSize = 22.dp
            )
        } else {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            modifier = Modifier.widthIn(max = 180.dp),
            text = account?.username ?: stringResource(R.string.account_add_new_account),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (account != null) {
            Text(
                text = getAccountTypeName(account),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

/**
 * Big PLAY button: [ PLAY + version name | v ] followed by the version settings button.
 * The arrow opens the list of installed versions.
 */
@Composable
private fun TokitoLaunchBar(
    version: Version?,
    isRefreshing: Boolean,
    onLaunchGame: (Version?) -> Unit,
    toVersionManageScreen: () -> Unit,
    toVersionSettingsScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showList by remember { mutableStateOf(false) }
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()

    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val buttonShape = RoundedCornerShape(22.dp)
    val canOpenSettings = !isRefreshing && version?.isValid() == true

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(buttonShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            primary,
                            lerp(primary, MaterialTheme.colorScheme.tertiary, 0.45f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onLaunchGame(null) }
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.tokito_play),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    color = onPrimary,
                    maxLines = 1
                )
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        text = version?.getVersionName() ?: stringResource(R.string.versions_manage_no_versions),
                        style = MaterialTheme.typography.labelMedium,
                        color = onPrimary.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.14f))
                    .guideNode(
                        key = GuideKeys.Main.Step.VersionList,
                        preferSide = GuideSide.Above,
                        holeRadius = 22.dp
                    )
                    .clickable { showList = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                    contentDescription = stringResource(R.string.tokito_rail_versions),
                    tint = onPrimary
                )

                DropdownMenu(
                    expanded = showList,
                    onDismissRequest = { showList = false },
                    modifier = Modifier.width(280.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    versions.forEach { version0 ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CommonVersionInfoLayout(
                                        modifier = Modifier.weight(1f),
                                        version = version0,
                                        iconSize = 28.dp
                                    )
                                    IconButton(
                                        onClick = {
                                            onLaunchGame(version0)
                                            showList = false
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_play_arrow_filled),
                                            contentDescription = stringResource(R.string.main_launch_game),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                if (version != version0) VersionsManager.saveVersion(version0)
                                showList = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.tokito_rail_versions)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_sports_esports_filled),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showList = false
                            toVersionManageScreen()
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(buttonShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(enabled = canOpenSettings, onClick = toVersionSettingsScreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(26.dp),
                painter = painterResource(R.drawable.ic_settings_filled),
                contentDescription = stringResource(R.string.versions_manage_settings),
                tint = if (canOpenSettings) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

/**
 * Backdrop of the hero: a plum sky, a glow behind the character and a pink/purple perspective grid
 * on the floor that slowly moves towards the viewer.
 */
@Composable
private fun TokitoGridBackdrop(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val top = MaterialTheme.colorScheme.background
    val bottom = MaterialTheme.colorScheme.surfaceContainer

    val transition = rememberInfiniteTransition(label = "tokito_grid")
    val scroll by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing)
        ),
        label = "tokito_grid_scroll"
    )

    Canvas(modifier = modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val horizon = h * 0.56f
        val floor = h - horizon
        val strokeWidth = 1.2.dp.toPx()

        //sky
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(top, bottom),
                startY = 0f,
                endY = h
            )
        )

        //glow behind the character
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(cx, horizon),
                radius = w * 0.55f
            )
        )

        //floor: lines running across (they move towards the viewer)
        val rows = 8
        for (k in 0 until rows) {
            val t = (k + scroll) / rows
            val y = horizon + floor * t * t
            drawLine(
                color = primary.copy(alpha = 0.55f * t),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = strokeWidth
            )
        }

        //floor: lines running into the distance
        val spacing = w / 9f
        for (i in -14..14) {
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, secondary.copy(alpha = 0.5f)),
                    startY = horizon,
                    endY = h
                ),
                start = Offset(cx + i * spacing * 0.22f, horizon),
                end = Offset(cx + i * spacing, h),
                strokeWidth = strokeWidth
            )
        }

        //horizon glow
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, primary.copy(alpha = 0.8f), Color.Transparent)
            ),
            start = Offset(0f, horizon),
            end = Offset(w, horizon),
            strokeWidth = strokeWidth * 1.5f
        )
    }
}
