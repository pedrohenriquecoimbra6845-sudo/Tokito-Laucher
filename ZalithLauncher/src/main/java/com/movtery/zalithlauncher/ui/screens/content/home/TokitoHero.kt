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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
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
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.OptimizationMode
import com.movtery.zalithlauncher.ui.components.OptimizationDialog
import com.movtery.zalithlauncher.ui.components.PetModelView
import com.movtery.zalithlauncher.ui.components.TokitoSkinsDialog
import com.movtery.zalithlauncher.ui.guide.GuideKeys
import com.movtery.zalithlauncher.ui.screens.content.elements.CommonVersionInfoLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace
import com.movtery.zalithlauncher.ui.theme.LauncherStyle
import com.movtery.zalithlauncher.ui.theme.tokitoButtonFrame
import com.movtery.zalithlauncher.ui.theme.tokitoPanel
import com.movtery.zalithlauncher.viewmodel.backgroundVisible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tokito hero: the small card of the home screen (bottom right corner).
 *
 * A backdrop (the Minecraft-style default, or a photo or a video that the player chose) with the player's 3D skin
 * standing on it, the player name on top and the PLAY button (with a version dropdown) at the bottom. Two round
 * buttons on the sides of the character open the skins and the settings of the card (size and backdrop).
 * When the card is narrow it switches to a compact layout.
 *
 * @param onLaunchGame starts the game; null means "the selected version"
 * @param isVisible the home screen is the one that the player is looking at (a video backdrop only plays then)
 */
@Composable
fun TokitoHero(
    onLaunchGame: (Version?) -> Unit,
    toAccountManageScreen: () -> Unit,
    toVersionManageScreen: () -> Unit,
    toVersionSettingsScreen: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val refreshWardrobe by AccountsManager.refreshWardrobe.collectAsStateWithLifecycle()
    val version by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()

    val style = AllSettings.launcherStyle.state
    //PVP and Brutal modes: the character on the play screen is smaller
    val optimization = AllSettings.launcherOptimizationMode.state
    val shrink = optimization.shrinksLauncher
    var showOptimization by remember { mutableStateOf(false) }
    var showSkins by remember { mutableStateOf(false) }
    var showCardSettings by remember { mutableStateOf(false) }
    //tela inicial mais leve e suave: com as animações desligadas o boneco fica parado e a grade não se move
    val animated = AllSettings.homeAnimations.state
    val backgroundShown = backgroundVisible()
    val shape = RoundedCornerShape(style.panelCornerDp.dp)

    BoxWithConstraints(
        modifier = modifier.tokitoPanel(
            shape = shape,
            tint = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        //a narrow card (the default is small) uses smaller chips, a slimmer PLAY bar and less empty space
        val compact = maxWidth < COMPACT_WIDTH

        HomeHeroBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape),
            style = style,
            backgroundShown = backgroundShown,
            playVideo = isVisible
        )

        //with little height the 3D model would be tiny: show the face instead
        val showModel = maxHeight >= if (compact) 190.dp else 250.dp
        val currentAccount = account

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (compact) 10.dp else 16.dp,
                    end = if (compact) 10.dp else 16.dp,
                    top = if (compact) 8.dp else 12.dp,
                    bottom = if (compact) 10.dp else 14.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = if (compact) 6.dp else 8.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                TokitoPlayerChip(
                    modifier = Modifier.weight(1f, fill = false),
                    account = currentAccount,
                    compact = compact,
                    onClick = toAccountManageScreen
                )
                TokitoModeChip(
                    mode = optimization,
                    compact = compact,
                    onClick = { showOptimization = true }
                )
            }

            //The renderer of the character fits it by the WIDTH of its view (the body is about 0.73 times
            //as tall as the view is wide), so a wide and short view cut the head and the feet.
            //The view gets a size that always has room for the whole body.
            BoxWithConstraints(
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

                    val factor = if (shrink) CHARACTER_SCALE_SHRUNK else CHARACTER_SCALE
                    val viewHeight = maxHeight * factor
                    val viewWidth = minOf(maxWidth * factor, viewHeight / CHARACTER_VIEW_RATIO)

                    PetModelView(
                        modifier = Modifier.size(width = viewWidth, height = viewHeight),
                        skin = skinBytes,
                        cape = capeBytes,
                        arm = armModel,
                        yaw = Math.toRadians(-28.0).toFloat(),
                        animation = if (animated) PetAnimation.Idle else null,
                        animationSpeed = HOME_IDLE_SPEED,
                        tapReaction = PetTapReaction.Interact,
                        dragYaw = true,
                        gazeEnabled = animated
                    )
                } else if (currentAccount != null) {
                    PlayerFace(
                        account = currentAccount,
                        avatarSize = if (shrink) 52.dp else 72.dp
                    )
                }

                //the Skin button (change the skin) and the settings of the card (size and backdrop)
                HeroRoundButton(
                    modifier = Modifier.align(Alignment.BottomStart),
                    iconRes = R.drawable.ic_checkroom,
                    description = stringResource(R.string.tokito_main_item_skins),
                    onClick = { showSkins = true }
                )
                HeroRoundButton(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    iconRes = R.drawable.ic_edit_outlined,
                    description = stringResource(R.string.home_hero_settings_title),
                    onClick = { showCardSettings = true }
                )
            }

            TokitoLaunchBar(
                compact = compact,
                version = version,
                isRefreshing = isRefreshing,
                onLaunchGame = onLaunchGame,
                toVersionManageScreen = toVersionManageScreen,
                toVersionSettingsScreen = toVersionSettingsScreen
            )
        }
    }

    if (showOptimization) {
        OptimizationDialog(onDismiss = { showOptimization = false })
    }
    if (showSkins) {
        TokitoSkinsDialog(onDismiss = { showSkins = false })
    }
    if (showCardSettings) {
        HomeHeroSettingsDialog(onDismiss = { showCardSettings = false })
    }
}

/** Below this width the card uses the compact layout */
private val COMPACT_WIDTH = 260.dp

/** A small round button over the backdrop (dark glass, white icon) */
@Composable
private fun HeroRoundButton(
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.34f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = Color.White
        )
    }
}

/** Player name (and account type) shown above the character. Tap to manage the accounts. */
@Composable
private fun TokitoPlayerChip(
    account: Account?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val shape = MaterialTheme.shapes.large
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
            .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
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
            modifier = Modifier.widthIn(max = if (compact) 84.dp else 180.dp),
            text = account?.username ?: stringResource(R.string.account_add_new_account),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        //the type of the account ("Offline", "TokiAuth"...) does not fit in the compact card
        if (account != null && !compact) {
            Text(
                text = getAccountTypeName(account),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

/** Size of the 3D character (fraction of the room it has) */
private const val CHARACTER_SCALE = 0.92f

/** Size of the 3D character while the PVP or Brutal mode is on */
private const val CHARACTER_SCALE_SHRUNK = 0.66f

/** Height of the view of the character divided by its width: the body (0.73 of the width) fits with a margin */
private const val CHARACTER_VIEW_RATIO = 0.9f

/** The idle animation of the character on the home screen plays a little slower, for a softer feel */
private const val HOME_IDLE_SPEED = 0.85f

/** Milliseconds the floor lines take to glide from one row to the next (slow on purpose) */
private const val GRID_PERIOD_MS = 9000

/** Number of lines running across the floor */
private const val GRID_ROWS = 6

/** Chip of the optimization mode: lit when a mode is on, tap to choose the mode */
@Composable
private fun TokitoModeChip(
    mode: OptimizationMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val enabled = mode.isActive
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) colors.tertiary else colors.surfaceContainerHigh.copy(alpha = 0.85f))
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(mode.chipRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = if (enabled) colors.onTertiary else colors.onSurfaceVariant,
            maxLines = 1
        )
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
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    //the compact bar (narrow card) is a bit slimmer
    val barHeight = if (compact) 52.dp else 64.dp
    var showList by remember { mutableStateOf(false) }
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()

    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val buttonShape = RoundedCornerShape(AllSettings.launcherStyle.state.buttonCornerDp.dp)
    val canOpenSettings = !isRefreshing && version?.isValid() == true

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
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
                .tokitoButtonFrame(buttonShape)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onLaunchGame(null) }
                    .padding(horizontal = if (compact) 10.dp else 20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.tokito_play),
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = if (compact) 2.sp else 3.sp,
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
                    .width(if (compact) 38.dp else 56.dp)
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
                    modifier = Modifier.size(if (compact) 28.dp else 32.dp),
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
                .size(barHeight)
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
 * Backdrop of the hero. Tokito: a plum sky, a glow behind the character and a pink/purple perspective
 * grid on the floor that glides slowly towards the viewer. The other styles adapt it: faint and see-through
 * (Liquid Glass, Transparent), strong neon lines (Neon), a grass and dirt floor (Minecraft) or only the
 * sky and the glow (Zalith).
 *
 * Light and smooth: everything that stands still (sky, glow, lines into the distance, horizon) is drawn once
 * in its own layer, and only the few lines that move are redrawn, in a layer of their own, so the rest of the
 * home screen is not redrawn on every frame. With the home animations turned off nothing moves at all.
 */
@Composable
internal fun TokitoGridBackdrop(
    style: LauncherStyle,
    backgroundShown: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val top = MaterialTheme.colorScheme.background
    val bottom = MaterialTheme.colorScheme.surfaceContainer
    val animated = AllSettings.homeAnimations.state

    //how much of the wallpaper the sky hides (1 = all of it)
    val skyAlpha = if (!backgroundShown) {
        1f
    } else {
        when (style) {
            LauncherStyle.LIQUID_GLASS -> 0.30f
            LauncherStyle.TRANSPARENT -> 0.06f
            LauncherStyle.NEON, LauncherStyle.MINECRAFT -> 0.85f
            else -> 1f
        }
    }
    val gridAlpha = when (style) {
        LauncherStyle.LIQUID_GLASS -> 0.40f
        LauncherStyle.TRANSPARENT -> 0.26f
        LauncherStyle.NEON -> 1.2f
        else -> 0.85f
    }
    val glowAlpha = if (style == LauncherStyle.ZALITH) 0.12f else 0.24f
    val hasGrid = style != LauncherStyle.ZALITH && style != LauncherStyle.MINECRAFT

    Box(modifier = modifier.clipToBounds()) {
        //still layer
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { }) {
            val w = size.width
            val h = size.height
            val horizon = h * HORIZON

            //sky
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(top.copy(alpha = skyAlpha), bottom.copy(alpha = skyAlpha)),
                    startY = 0f,
                    endY = h
                )
            )

            //glow behind the character
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(w / 2f, horizon),
                    radius = w * 0.60f
                )
            )

            when (style) {
                LauncherStyle.ZALITH -> Unit
                LauncherStyle.MINECRAFT -> drawBlockFloor(
                    grass = primary,
                    dirt = secondary,
                    horizon = horizon
                )
                else -> drawGridStill(
                    primary = primary,
                    secondary = secondary,
                    horizon = horizon,
                    alphaScale = gridAlpha
                )
            }
        }

        //moving layer: the lines running across the floor
        if (hasGrid) {
            if (animated) {
                MovingGridRows(
                    modifier = Modifier.fillMaxSize(),
                    color = primary,
                    alphaScale = gridAlpha
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { }) {
                    drawGridRows(
                        color = primary,
                        scroll = STILL_SCROLL,
                        horizon = size.height * HORIZON,
                        alphaScale = gridAlpha
                    )
                }
            }
        }
    }
}

/** Where the floor starts (fraction of the height of the hero) */
private const val HORIZON = 0.56f

/** Position of the floor lines when they do not move */
private const val STILL_SCROLL = 0.35f

/** The lines running across the floor, gliding towards the viewer. Only this small layer is redrawn per frame. */
@Composable
private fun MovingGridRows(
    color: Color,
    alphaScale: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "tokito_grid")
    val scroll by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = GRID_PERIOD_MS, easing = LinearEasing)
        ),
        label = "tokito_grid_scroll"
    )

    Canvas(modifier = modifier.graphicsLayer { }) {
        //the value is read while drawing: only this layer is drawn again on each frame
        drawGridRows(
            color = color,
            scroll = scroll,
            horizon = size.height * HORIZON,
            alphaScale = alphaScale
        )
    }
}

/** Lines running across the floor (they get closer together towards the horizon and fade out there) */
private fun DrawScope.drawGridRows(
    color: Color,
    scroll: Float,
    horizon: Float,
    alphaScale: Float
) {
    val w = size.width
    val floor = size.height - horizon
    val strokeWidth = 1.dp.toPx()

    for (k in 0 until GRID_ROWS) {
        val t = (k + scroll) / GRID_ROWS
        val y = horizon + floor * t * t
        drawLine(
            color = color.copy(alpha = (0.42f * t * alphaScale).coerceIn(0f, 1f)),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = strokeWidth
        )
    }
}

/** The part of the perspective grid that never moves: lines into the distance and the glow of the horizon */
private fun DrawScope.drawGridStill(
    primary: Color,
    secondary: Color,
    horizon: Float,
    alphaScale: Float
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val strokeWidth = 1.dp.toPx()

    //floor: lines running into the distance (one brush for all of them: they fade the same way)
    val fade = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            secondary.copy(alpha = (0.34f * alphaScale).coerceIn(0f, 1f))
        ),
        startY = horizon,
        endY = h
    )
    val spacing = w / 9f
    for (i in -10..10) {
        drawLine(
            brush = fade,
            start = Offset(cx + i * spacing * 0.22f, horizon),
            end = Offset(cx + i * spacing, h),
            strokeWidth = strokeWidth
        )
    }

    //horizon glow
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                primary.copy(alpha = (0.55f * alphaScale).coerceIn(0f, 1f)),
                Color.Transparent
            )
        ),
        start = Offset(0f, horizon),
        end = Offset(w, horizon),
        strokeWidth = strokeWidth * 1.5f
    )
}

/** Minecraft floor: a strip of grass on top of a checkerboard of dirt blocks */
private fun DrawScope.drawBlockFloor(
    grass: Color,
    dirt: Color,
    horizon: Float
) {
    val w = size.width
    val h = size.height
    val tile = 20.dp.toPx()

    drawRect(
        color = grass.copy(alpha = 0.9f),
        topLeft = Offset(0f, horizon),
        size = Size(w, tile * 0.5f)
    )

    var y = horizon + tile * 0.5f
    var row = 0
    while (y < h) {
        var x = 0f
        var col = 0
        while (x < w) {
            val shade = if ((row + col) % 2 == 0) 0.42f else 0.30f
            drawRect(
                color = dirt.copy(alpha = shade),
                topLeft = Offset(x, y),
                size = Size(tile, tile)
            )
            x += tile
            col += 1
        }
        y += tile
        row += 1
    }
}
