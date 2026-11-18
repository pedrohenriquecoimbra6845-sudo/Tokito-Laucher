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

package com.movtery.zalithlauncher.ui.theme

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.BackgroundBlur
import com.movtery.zalithlauncher.ui.components.influencedByBackgroundColor
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import com.movtery.zalithlauncher.viewmodel.BackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.backgroundVisible

/**
 * How much the launcher density (DPI) is scaled while the PVP or Brutal mode is on.
 * A bit below 1 so everything gets slightly smaller and more of the screen is free.
 */
const val PVP_DENSITY_SCALE = 0.86f

/**
 * Mini wallpapers shipped with Tokito Launcher (original art made for this project).
 * Each one has the full image (res/raw) and a small thumbnail (res/drawable-nodpi).
 */
enum class LauncherWallpaper(
    val id: String,
    @RawRes val rawRes: Int,
    @DrawableRes val thumbRes: Int,
    @StringRes val nameRes: Int,
    /** false: the image belongs to a style (Liquid Glass modes) and is not listed in the wallpaper picker */
    val inPicker: Boolean = true
) {
    SAKURA("sakura", R.raw.wp_sakura, R.drawable.wpt_sakura, R.string.tokito_wp_sakura),
    NEON("neon", R.raw.wp_neon, R.drawable.wpt_neon, R.string.tokito_wp_neon),
    GLASS("glass", R.raw.wp_glass, R.drawable.wpt_glass, R.string.tokito_wp_glass),
    BLOCKS("blocks", R.raw.wp_blocks, R.drawable.wpt_blocks, R.string.tokito_wp_blocks),
    NIGHT("night", R.raw.wp_night, R.drawable.wpt_night, R.string.tokito_wp_night),
    AURORA("aurora", R.raw.wp_aurora, R.drawable.wpt_aurora, R.string.tokito_wp_aurora),
    SUNSET("sunset", R.raw.wp_sunset, R.drawable.wpt_sunset, R.string.tokito_wp_sunset),
    CUBES("cubes", R.raw.wp_cubes, R.drawable.wpt_cubes, R.string.tokito_wp_cubes),

    //backdrops of the Liquid Glass "Leve" (light) and "Frio" (cold) modes
    GLASS_LIGHT(
        "glass_light", R.raw.wp_glass_light, R.drawable.wpt_glass_light,
        R.string.tokito_wp_glass_light, inPicker = false
    ),
    GLASS_COLD(
        "glass_cold", R.raw.wp_glass_cold, R.drawable.wpt_glass_cold,
        R.string.tokito_wp_glass_cold, inPicker = false
    );

    companion object {
        fun fromId(id: String): LauncherWallpaper? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Colors of the Liquid Glass style: Normal (the color theme of the launcher), Leve (light, silver glass)
 * and Frio (cold, ice blue glass).
 */
enum class GlassMode(@StringRes val nameRes: Int) {
    NORMAL(R.string.tokito_glass_normal),
    LIGHT(R.string.tokito_glass_light),
    COLD(R.string.tokito_glass_cold)
}

/**
 * Visual styles of Tokito Launcher. The user picks one in Settings > Launcher.
 *
 * @param panelCornerDp corner radius of the big panels (hero, floating menu and top bar)
 * @param buttonCornerDp corner radius of the big buttons
 * @param wallpaper wallpaper that goes with the style (null: the style needs none)
 */
enum class LauncherStyle(
    @StringRes val nameRes: Int,
    val panelCornerDp: Int,
    val buttonCornerDp: Int,
    val wallpaper: LauncherWallpaper?
) {
    TOKITO(R.string.tokito_style_tokito, 28, 22, null),
    LIQUID_GLASS(R.string.tokito_style_liquid_glass, 40, 32, LauncherWallpaper.GLASS),
    TRANSPARENT(R.string.tokito_style_transparent, 32, 26, LauncherWallpaper.SAKURA),
    NEON(R.string.tokito_style_neon, 16, 10, LauncherWallpaper.NEON),
    MINECRAFT(R.string.tokito_style_minecraft, 0, 0, LauncherWallpaper.BLOCKS),
    ZALITH(R.string.tokito_style_zalith, 28, 22, null);

    /** Styles whose side menu and top bar float as separate panels (Tokito and Zalith keep the flat layout) */
    val isFloating: Boolean
        get() = this == LIQUID_GLASS || this == TRANSPARENT || this == NEON || this == MINECRAFT

    /** Wallpaper that goes with the style; Liquid Glass has one for each of its [GlassMode]s */
    fun wallpaperFor(mode: GlassMode): LauncherWallpaper? = when {
        this == LIQUID_GLASS -> when (mode) {
            GlassMode.NORMAL -> LauncherWallpaper.GLASS
            GlassMode.LIGHT -> LauncherWallpaper.GLASS_LIGHT
            GlassMode.COLD -> LauncherWallpaper.GLASS_COLD
        }
        else -> wallpaper
    }
}

/** Neon: near-black base with cyan, magenta and lime light */
internal val neonDark: ColorScheme = darkColorScheme(
    primary = Color(0xFF00F0FF),
    onPrimary = Color(0xFF00252A),
    primaryContainer = Color(0xFF0B3C44),
    onPrimaryContainer = Color(0xFF8CF7FF),
    secondary = Color(0xFFFF2BD6),
    onSecondary = Color(0xFF3A0030),
    secondaryContainer = Color(0xFF4A0E41),
    onSecondaryContainer = Color(0xFFFFB0EE),
    tertiary = Color(0xFFB4FF39),
    onTertiary = Color(0xFF1E2E00),
    tertiaryContainer = Color(0xFF2E4A00),
    onTertiaryContainer = Color(0xFFD7FF8F),
    error = Color(0xFFFF6B8A),
    onError = Color(0xFF3A0010),
    errorContainer = Color(0xFF6B0F26),
    onErrorContainer = Color(0xFFFFD9E0),
    background = Color(0xFF06050D),
    onBackground = Color(0xFFE8F6FF),
    surface = Color(0xFF0A0814),
    onSurface = Color(0xFFE8F6FF),
    surfaceVariant = Color(0xFF171329),
    onSurfaceVariant = Color(0xFFB9B3D6),
    outline = Color(0xFF6A5CA8),
    outlineVariant = Color(0xFF2A2250),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE8F6FF),
    inverseOnSurface = Color(0xFF06050D),
    inversePrimary = Color(0xFF006A73),
    surfaceDim = Color(0xFF05040A),
    surfaceBright = Color(0xFF241E42),
    surfaceContainerLowest = Color(0xFF05040A),
    surfaceContainerLow = Color(0xFF0C0A18),
    surfaceContainer = Color(0xFF110E20),
    surfaceContainerHigh = Color(0xFF171329),
    surfaceContainerHighest = Color(0xFF1E1936),
)

/** Minecraft: stone grays with grass green, dirt brown and diamond accents */
internal val minecraftDark: ColorScheme = darkColorScheme(
    primary = Color(0xFF7CB342),
    onPrimary = Color(0xFF10240A),
    primaryContainer = Color(0xFF2F5A1A),
    onPrimaryContainer = Color(0xFFC5F2A0),
    secondary = Color(0xFFC08A5A),
    onSecondary = Color(0xFF2B1A0B),
    secondaryContainer = Color(0xFF5A3B22),
    onSecondaryContainer = Color(0xFFF2D3B6),
    tertiary = Color(0xFF3DD6D0),
    onTertiary = Color(0xFF00302E),
    tertiaryContainer = Color(0xFF00504C),
    onTertiaryContainer = Color(0xFFA6F2EE),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0A06),
    errorContainer = Color(0xFF7A1A12),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1B1B1B),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF222222),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF3B3B3B),
    onSurfaceVariant = Color(0xFFC9C9C9),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF4A4A4A),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEDEDED),
    inverseOnSurface = Color(0xFF1B1B1B),
    inversePrimary = Color(0xFF3E6B1E),
    surfaceDim = Color(0xFF161616),
    surfaceBright = Color(0xFF444444),
    surfaceContainerLowest = Color(0xFF161616),
    surfaceContainerLow = Color(0xFF1F1F1F),
    surfaceContainer = Color(0xFF272727),
    surfaceContainerHigh = Color(0xFF303030),
    surfaceContainerHighest = Color(0xFF3A3A3A),
)

/** Liquid Glass "Leve": silver and pearl white glass with dark text and a rose accent */
internal val glassLight: ColorScheme = lightColorScheme(
    primary = Color(0xFFD6336C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E4),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF7B5CE0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6DEFF),
    onSecondaryContainer = Color(0xFF22005D),
    tertiary = Color(0xFF1E88D8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCDE8FF),
    onTertiaryContainer = Color(0xFF001E2F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFEDEFF5),
    onBackground = Color(0xFF1B1D26),
    surface = Color(0xFFF4F5FA),
    onSurface = Color(0xFF1B1D26),
    surfaceVariant = Color(0xFFE1E4EE),
    onSurfaceVariant = Color(0xFF4A4F60),
    outline = Color(0xFF8A90A5),
    outlineVariant = Color(0xFFC9CDDB),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2E3039),
    inverseOnSurface = Color(0xFFF0F1F6),
    inversePrimary = Color(0xFFFFB1CB),
    surfaceDim = Color(0xFFD9DCE5),
    surfaceBright = Color(0xFFFAFBFE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F4F9),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE5E8F0),
    surfaceContainerHighest = Color(0xFFDEE1EB),
)

/** Liquid Glass "Frio": deep navy with ice blue, periwinkle and aqua light */
internal val glassCold: ColorScheme = darkColorScheme(
    primary = Color(0xFF6FD6FF),
    onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF004D6B),
    onPrimaryContainer = Color(0xFFC2EBFF),
    secondary = Color(0xFF9DB4FF),
    onSecondary = Color(0xFF1A2A6B),
    secondaryContainer = Color(0xFF31428A),
    onSecondaryContainer = Color(0xFFDCE2FF),
    tertiary = Color(0xFF7CF2E0),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF005046),
    onTertiaryContainer = Color(0xFFA0FFEF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF081420),
    onBackground = Color(0xFFE2EEF8),
    surface = Color(0xFF0B1826),
    onSurface = Color(0xFFE2EEF8),
    surfaceVariant = Color(0xFF1B2D40),
    onSurfaceVariant = Color(0xFFB4C6D8),
    outline = Color(0xFF6F8398),
    outlineVariant = Color(0xFF2A4057),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE2EEF8),
    inverseOnSurface = Color(0xFF0B1826),
    inversePrimary = Color(0xFF00668A),
    surfaceDim = Color(0xFF07111B),
    surfaceBright = Color(0xFF1E3248),
    surfaceContainerLowest = Color(0xFF060F18),
    surfaceContainerLow = Color(0xFF0D1B2A),
    surfaceContainer = Color(0xFF11202F),
    surfaceContainerHigh = Color(0xFF172A3C),
    surfaceContainerHighest = Color(0xFF1E3347),
)

private val GlassShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(36.dp),
    extraLarge = RoundedCornerShape(44.dp),
)

private val NeonShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

private val BlockShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

/** Shapes (corner sizes) of every component for the style */
internal fun LauncherStyle.shapes(): Shapes = when (this) {
    LauncherStyle.TOKITO, LauncherStyle.TRANSPARENT -> PolyJavShapes
    LauncherStyle.LIQUID_GLASS -> GlassShapes
    LauncherStyle.NEON -> NeonShapes
    LauncherStyle.MINECRAFT -> BlockShapes
    LauncherStyle.ZALITH -> Shapes()
}

/**
 * Palette that belongs to the style itself, or null when the style uses the color theme chosen by the user.
 * Neon and Minecraft only exist in the dark theme; Liquid Glass "Leve" is always light and "Frio" always dark
 * (see [forcedDarkTheme]).
 *
 * @param mode mode of the Liquid Glass style
 * @param darkTheme whether the launcher is in the dark theme
 */
internal fun LauncherStyle.ownPalette(
    mode: GlassMode = GlassMode.NORMAL,
    darkTheme: Boolean = true
): ColorScheme? = when (this) {
    LauncherStyle.NEON -> if (darkTheme) neonDark else null
    LauncherStyle.MINECRAFT -> if (darkTheme) minecraftDark else null
    LauncherStyle.LIQUID_GLASS -> when (mode) {
        GlassMode.NORMAL -> null
        GlassMode.LIGHT -> glassLight
        GlassMode.COLD -> glassCold
    }
    else -> null
}

/**
 * Liquid Glass "Leve" is always in the light theme and "Frio" always in the dark theme, whatever the
 * system says. Null: the user's own dark/light choice decides.
 */
fun forcedDarkTheme(style: LauncherStyle, mode: GlassMode): Boolean? = when {
    style != LauncherStyle.LIQUID_GLASS -> null
    mode == GlassMode.LIGHT -> false
    mode == GlassMode.COLD -> true
    else -> null
}

/**
 * How the Liquid Glass panels are lit in each [GlassMode]: a veil of light that fades from top to bottom,
 * a thin rim that catches the light on the top left and the bottom right, a soft band just inside the rim
 * (the thickness of the glass), a bright glint on the top edge and a faint rainbow fringe on the bottom edge.
 */
private class GlassLook(
    val veil: Color,
    val veilTop: Float,
    val veilMid: Float,
    val veilBottom: Float,
    /** alpha of the panel when no wallpaper is visible behind it */
    val solidAlpha: Float,
    val rim: List<Color>,
    val band: Color,
    val glint: Color,
    val fringeA: Color,
    val fringeB: Color
)

private val IceWhite = Color(0xFFD6F1FF)
private val SlateEdge = Color(0xFF55607A)

private fun glassLook(mode: GlassMode): GlassLook = when (mode) {
    GlassMode.NORMAL -> GlassLook(
        veil = Color.White,
        veilTop = 0.22f, veilMid = 0.08f, veilBottom = 0.03f,
        solidAlpha = 0.80f,
        rim = listOf(
            Color.White.copy(alpha = 0.80f), Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.60f)
        ),
        band = Color.White.copy(alpha = 0.05f),
        glint = Color.White.copy(alpha = 0.55f),
        fringeA = Color(0xFFFF8FD0).copy(alpha = 0.34f),
        fringeB = Color(0xFF8FE3FF).copy(alpha = 0.34f)
    )

    GlassMode.LIGHT -> GlassLook(
        veil = Color.White,
        veilTop = 0.60f, veilMid = 0.30f, veilBottom = 0.14f,
        solidAlpha = 0.78f,
        rim = listOf(
            Color.White, SlateEdge.copy(alpha = 0.16f),
            SlateEdge.copy(alpha = 0.16f), Color.White.copy(alpha = 0.95f)
        ),
        band = SlateEdge.copy(alpha = 0.06f),
        glint = Color.White.copy(alpha = 0.95f),
        fringeA = Color(0xFFFF9BC8).copy(alpha = 0.40f),
        fringeB = Color(0xFF8FC8FF).copy(alpha = 0.40f)
    )

    GlassMode.COLD -> GlassLook(
        veil = IceWhite,
        veilTop = 0.22f, veilMid = 0.08f, veilBottom = 0.03f,
        solidAlpha = 0.80f,
        rim = listOf(
            IceWhite.copy(alpha = 0.85f), IceWhite.copy(alpha = 0.10f),
            IceWhite.copy(alpha = 0.10f), IceWhite.copy(alpha = 0.65f)
        ),
        band = IceWhite.copy(alpha = 0.06f),
        glint = IceWhite.copy(alpha = 0.60f),
        fringeA = Color(0xFF7FD8FF).copy(alpha = 0.38f),
        fringeB = Color(0xFF8CFFE9).copy(alpha = 0.38f)
    )
}

/**
 * Draws a panel (side menu, top bar, hero, chips...) the way the current style wants it:
 * flat plum (Tokito), frosted glass, almost transparent, neon outline, Minecraft bevel or plain Zalith.
 *
 * @param tint base color of the panel
 * @param bordered Tokito style only: draw the thin outline
 */
@Composable
fun Modifier.tokitoPanel(
    shape: Shape,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    bordered: Boolean = true
): Modifier = tokitoPanelFor(
    style = AllSettings.launcherStyle.state,
    colors = MaterialTheme.colorScheme,
    shape = shape,
    tint = tint,
    fill = influencedByBackgroundColor(tint),
    backgroundShown = backgroundVisible(),
    bordered = bordered,
    glassBlur = AllSettings.backgroundBlur.state,
    applyGlass = true,
    glassMode = AllSettings.liquidGlassMode.state
)

/**
 * Same as [tokitoPanel] with every input given explicitly (used by the style previews in the settings).
 *
 * @param fill color of the flat panels (already adjusted to the background opacity)
 * @param backgroundShown whether a wallpaper is visible behind the panel
 * @param applyGlass whether to use the real background blur of the launcher (not for previews)
 * @param glassMode colors of the Liquid Glass style
 */
@Composable
internal fun Modifier.tokitoPanelFor(
    style: LauncherStyle,
    colors: ColorScheme,
    shape: Shape,
    tint: Color,
    fill: Color,
    backgroundShown: Boolean,
    bordered: Boolean,
    glassBlur: Int,
    applyGlass: Boolean,
    glassMode: GlassMode = GlassMode.NORMAL,
    borderScale: Float = 1f
): Modifier {
    //real frosted glass: only when the user has a background and the "foreground" blur on.
    //Neon and Minecraft keep their own solid look.
    val glassActive = applyGlass &&
        backgroundShown &&
        glassBlur > 0 &&
        style != LauncherStyle.NEON &&
        style != LauncherStyle.MINECRAFT &&
        AllSettings.backgroundBlurType.state == BackgroundBlur.Foreground

    var panel: Modifier = this.clip(shape)
    if (glassActive) {
        panel = panel.backgroundGlass(blur = glassBlur, color = fill)
    }

    //flat color of the panel; the glass already paints its own tint
    fun flat(color: Color): Modifier = if (glassActive) Modifier else Modifier.background(color)

    return when (style) {
        LauncherStyle.TOKITO -> {
            val base = panel.then(flat(fill))
            if (bordered) base.border(1.dp * borderScale, colors.outlineVariant, shape) else base
        }

        LauncherStyle.ZALITH -> panel.then(flat(fill))

        LauncherStyle.LIQUID_GLASS -> {
            val look = glassLook(glassMode)
            panel
                .then(flat(if (backgroundShown) fill else tint.copy(alpha = look.solidAlpha)))
                //the veil of light: stronger at the top, almost gone at the bottom
                .background(
                    Brush.verticalGradient(
                        listOf(
                            look.veil.copy(alpha = look.veilTop),
                            look.veil.copy(alpha = look.veilMid),
                            look.veil.copy(alpha = look.veilBottom)
                        )
                    )
                )
                .drawBehind {
                    val line = (1.2.dp.toPx() * borderScale).coerceAtLeast(1f)
                    //glint on the top edge
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.5f to look.glint,
                            1f to Color.Transparent
                        ),
                        topLeft = Offset(size.width * 0.12f, line * 1.2f),
                        size = Size(size.width * 0.76f, line)
                    )
                    //faint rainbow fringe on the bottom edge (light bending in the glass)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.3f to look.fringeA,
                            0.7f to look.fringeB,
                            1f to Color.Transparent
                        ),
                        topLeft = Offset(size.width * 0.08f, size.height - line * 2.2f),
                        size = Size(size.width * 0.84f, line * 1.6f)
                    )
                }
                //thickness of the glass: soft bands just inside the rim
                .border(5.dp * borderScale, look.band, shape)
                .border(2.5.dp * borderScale, look.band, shape)
                .border(
                    width = 1.2.dp * borderScale,
                    brush = Brush.linearGradient(look.rim),
                    shape = shape
                )
        }

        LauncherStyle.TRANSPARENT -> panel
            .then(flat(if (backgroundShown) fill else tint.copy(alpha = 0.55f)))
            .border(1.dp * borderScale, Color.White.copy(alpha = 0.20f), shape)

        LauncherStyle.NEON -> panel
            .background(if (backgroundShown) fill else tint.copy(alpha = 0.96f))
            .border(7.dp * borderScale, colors.primary.copy(alpha = 0.07f), shape)
            .border(4.dp * borderScale, colors.primary.copy(alpha = 0.14f), shape)
            .border(
                width = 1.5.dp * borderScale,
                brush = Brush.linearGradient(listOf(colors.primary, colors.secondary)),
                shape = shape
            )

        LauncherStyle.MINECRAFT -> {
            val light = Color.White.copy(alpha = 0.22f)
            val dark = Color.Black.copy(alpha = 0.35f)
            panel
                .background(if (backgroundShown) fill else tint)
                .drawBehind {
                    //bevel lit from the top left, like the game's own menus
                    val t = 2.dp.toPx() * borderScale
                    drawRect(color = light, topLeft = Offset(0f, 0f), size = Size(size.width, t))
                    drawRect(color = light, topLeft = Offset(0f, 0f), size = Size(t, size.height))
                    drawRect(color = dark, topLeft = Offset(0f, size.height - t), size = Size(size.width, t))
                    drawRect(color = dark, topLeft = Offset(size.width - t, 0f), size = Size(t, size.height))
                }
                .border(2.dp * borderScale, Color.Black.copy(alpha = 0.85f), shape)
        }
    }
}

/** Outline of the big buttons (PLAY...) that goes with the current style */
@Composable
fun Modifier.tokitoButtonFrame(shape: Shape): Modifier {
    val colors = MaterialTheme.colorScheme
    return when (AllSettings.launcherStyle.state) {
        LauncherStyle.NEON -> border(
            width = 1.5.dp,
            brush = Brush.linearGradient(listOf(colors.primary, colors.secondary)),
            shape = shape
        )

        LauncherStyle.MINECRAFT -> border(
            width = 2.dp,
            color = Color.Black.copy(alpha = 0.85f),
            shape = shape
        )

        LauncherStyle.LIQUID_GLASS -> {
            val look = glassLook(AllSettings.liquidGlassMode.state)
            //a light gloss over the button and the glass rim around it
            this
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.03f))
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(look.rim),
                    shape = shape
                )
        }

        else -> this
    }
}

/**
 * Applies what goes with a style: translucency, blur and, when the user has no image of their own,
 * the wallpaper that matches the style. The style itself is saved by the caller.
 *
 * @param backgroundViewModel the launcher background (null: only the numbers are applied)
 */
suspend fun applyLauncherStylePreset(
    context: Context,
    backgroundViewModel: BackgroundViewModel?,
    style: LauncherStyle,
    mode: GlassMode = AllSettings.liquidGlassMode.getValue()
) {
    when (style) {
        LauncherStyle.LIQUID_GLASS -> when (mode) {
            GlassMode.LIGHT -> setBackgroundLook(opacity = 46, blur = 16, type = BackgroundBlur.Foreground)
            else -> setBackgroundLook(opacity = 40, blur = 18, type = BackgroundBlur.Foreground)
        }
        LauncherStyle.TRANSPARENT -> setBackgroundLook(opacity = 25, blur = 0, type = BackgroundBlur.Background)
        LauncherStyle.NEON -> setBackgroundLook(opacity = 80, blur = 0, type = BackgroundBlur.Background)
        LauncherStyle.MINECRAFT -> setBackgroundLook(opacity = 88, blur = 0, type = BackgroundBlur.Background)
        LauncherStyle.TOKITO, LauncherStyle.ZALITH -> setBackgroundLook(opacity = 80, blur = 0, type = BackgroundBlur.Background)
    }

    val vm = backgroundViewModel ?: return
    val presetInUse = AllSettings.launcherWallpaper.getValue().isNotEmpty()
    val pickedByHand = AllSettings.launcherWallpaperManual.getValue()
    //an image or video chosen by the user (a file, an imported one or a mini wallpaper picked by hand)
    //is never replaced by the wallpaper of a style
    val userOwnsBackground = vm.isValid && (!presetInUse || pickedByHand)
    if (userOwnsBackground) return

    val wallpaper = style.wallpaperFor(mode)
    when {
        wallpaper != null -> vm.importWallpaper(context, wallpaper)
        presetInUse -> vm.delete()
    }
}

private fun setBackgroundLook(opacity: Int, blur: Int, type: BackgroundBlur) {
    AllSettings.launcherBackgroundOpacity.save(opacity)
    AllSettings.backgroundBlur.save(blur)
    AllSettings.backgroundBlurType.save(type)
}
