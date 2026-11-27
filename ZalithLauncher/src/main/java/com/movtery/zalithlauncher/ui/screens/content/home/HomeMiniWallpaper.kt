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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.ui.components.VideoPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.TokitoHomeMedia
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.TokitoEntry
import com.movtery.zalithlauncher.ui.theme.LauncherWallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** A horizontal (wide) Mini Wallpaper is shown in a frame with this width / height */
private const val ASPECT_WIDE = 1.6f

/** A vertical (tall) Mini Wallpaper is the same frame turned around */
private const val ASPECT_TALL = 0.625f

/** The frame is never wider than this */
private val WIDE_MAX_WIDTH = 440.dp
private val TALL_MAX_WIDTH = 280.dp

/** How long the flower takes to blow the bubbles until the picture is formed */
private const val INTRO_MS = 2300f

/** The animation draws at most 50 frames per second (20 ms between two frames) */
private const val FRAME_GAP_NS = 20_000_000L

/** The default ready-made picture */
private val DEFAULT_WALLPAPER = LauncherWallpaper.SAKURA

/** The animation of the Mini Wallpaper plays once each time the app opens (this lives as long as the process) */
private object MiniWallpaperIntro {
    @Volatile
    var played = false
}

/**
 * Tokito Launcher: the Mini Wallpaper. One picture, GIF or video chosen by the player, near the middle of the home
 * screen, in a horizontal (wide) or a vertical (tall) frame.
 *
 * A photo shows up with a flower animation: when the app opens, the flower in the middle blows bubbles that grow and
 * merge until the picture is formed (about two seconds, at most 50 frames per second); after that nothing moves and
 * the picture costs nothing. A GIF and a video just play (muted, in a loop, and a video only while the home screen is
 * the one on the screen). Over any of them the greeting talks to the player: Welcome, the time of the day and a
 * warrior phrase.
 *
 * @param visible whether the home screen is the one that the player is looking at
 * @param onClick tapping the picture opens its settings
 */
@Composable
fun HomeMiniWallpaper(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!AllSettings.homeMiniWallpaperOn.state) return

    val tall = AllSettings.homeMiniWallpaperShape.state == 1
    val aspect = if (tall) ASPECT_TALL else ASPECT_WIDE
    val fileName = AllSettings.homeMiniWallpaperFile.state
    val custom = remember(fileName) { TokitoHomeMedia.fileOf(fileName) }
    val kind = remember(custom) { wallpaperKindOf(custom) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val frameWidth = minOf(
            maxWidth * 0.94f,
            maxHeight * 0.86f * aspect,
            if (tall) TALL_MAX_WIDTH else WIDE_MAX_WIDTH
        )
        if (frameWidth < (if (tall) 90.dp else 120.dp)) return@BoxWithConstraints
        val frameHeight = frameWidth / aspect

        val density = LocalDensity.current
        val frameWidthPx = with(density) { frameWidth.toPx() }
        val frameHeightPx = with(density) { frameHeight.toPx() }
        val shape = RoundedCornerShape(24.dp)

        val isPhoto = kind == WallpaperKind.PHOTO
        val bitmap by rememberMiniWallpaperBitmap(
            maxSidePx = (max(frameWidthPx, frameHeightPx) * 1.1f).roundToInt().coerceIn(256, 1600),
            enabled = isPhoto
        )
        val bubbles = remember(frameWidthPx, frameHeightPx) { makeBubbles(frameWidthPx, frameHeightPx) }

        val wantsIntro = isPhoto && AllSettings.homeMiniWallpaperIntro.state && !MiniWallpaperIntro.played
        var progress by remember(isPhoto) { mutableFloatStateOf(if (wantsIntro) 0f else 1f) }
        val running by remember { derivedStateOf { progress < 1f } }

        LaunchedEffect(bitmap != null, visible) {
            if (bitmap == null || !visible || progress >= 1f) return@LaunchedEffect
            //the animation waits for the entry screen to go away
            TokitoEntry.finished.first { it }
            val start = withFrameNanos { it }
            var last = start
            while (true) {
                val now = withFrameNanos { it }
                if (now - last < FRAME_GAP_NS) continue
                last = now
                val t = ((now - start) / 1_000_000f / INTRO_MS).coerceIn(0f, 1f)
                progress = t
                if (t >= 1f) break
            }
            MiniWallpaperIntro.played = true
        }

        val flowerColor = MaterialTheme.colorScheme.primary
        val ringWidth = with(density) { 1.5.dp.toPx() }

        Box(
            modifier = Modifier
                .size(width = frameWidth, height = frameHeight)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
                .clickable(onClick = onClick)
        ) {
            when (kind) {
                WallpaperKind.PHOTO -> Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            //the bubbles are a mask for the picture: it needs its own layer, but only while the animation runs
                            compositingStrategy = if (running) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                        }
                ) {
                    val image = bitmap ?: return@Canvas
                    val p = progress
                    if (p >= 1f) {
                        drawCover(image)
                        return@Canvas
                    }

                    //1. the bubbles (white) are the mask, 2. the picture is drawn only where the bubbles are
                    val center = Offset(size.width / 2f, size.height / 2f)
                    bubbles.forEach { bubble ->
                        val t = bubble.localProgress(p)
                        if (t > 0f) {
                            val e = easeOutCubic(t)
                            val wobble = 1f + 0.06f * sin(p * 18f + bubble.phase) * (1f - t)
                            drawCircle(
                                color = Color.White,
                                radius = bubble.radius * e * wobble,
                                center = Offset(
                                    x = center.x + (bubble.cx - center.x) * e,
                                    y = center.y + (bubble.cy - center.y) * e
                                )
                            )
                        }
                    }
                    drawCover(image, blendMode = BlendMode.SrcIn)

                    //3. thin shining rings of the bubbles that are still growing
                    bubbles.forEach { bubble ->
                        val t = bubble.localProgress(p)
                        if (t > 0f && t < 1f) {
                            val e = easeOutCubic(t)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.5f * (1f - t)),
                                radius = bubble.radius * e,
                                center = Offset(
                                    x = center.x + (bubble.cx - center.x) * e,
                                    y = center.y + (bubble.cy - center.y) * e
                                ),
                                style = Stroke(width = ringWidth)
                            )
                        }
                    }

                    //4. the flower in the middle, blowing: it turns and fades away while the picture forms
                    val fade = 1f - smoothStep(0.35f, 0.8f, p)
                    drawFlower(
                        center = center,
                        radius = size.minDimension * 0.16f * (1f + 0.4f * p),
                        rotationDeg = p * 220f,
                        alpha = fade,
                        color = flowerColor
                    )
                }

                WallpaperKind.GIF -> if (custom != null) {
                    //the GIF decoder of Coil is registered for the whole app, so this plays
                    AsyncImage(
                        model = custom,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                WallpaperKind.VIDEO -> if (custom != null && visible) {
                    VideoPlayer(
                        videoUri = Uri.fromFile(custom),
                        modifier = Modifier.fillMaxSize(),
                        autoPlay = true,
                        loop = true,
                        muted = true
                    )
                }
            }

            if (!isPhoto) {
                //a transparent layer above the player: the tap always reaches the frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onClick)
                )
            }

            if (AllSettings.homeGreeting.state) {
                MiniWallpaperGreeting(
                    visible = visible,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/** What the Mini Wallpaper shows: a still photo (with the flower animation), a GIF or a video */
private enum class WallpaperKind { PHOTO, GIF, VIDEO }

/** The kind comes from the name of the file that was saved when the player chose it */
private fun wallpaperKindOf(file: File?): WallpaperKind = when (file?.extension?.lowercase()) {
    "gif" -> WallpaperKind.GIF
    "mp4" -> WallpaperKind.VIDEO
    else -> WallpaperKind.PHOTO
}

/** A bubble of the animation: where it ends up, when it starts and how big it gets */
private class Bubble(
    val cx: Float,
    val cy: Float,
    val delay: Float,
    val radius: Float,
    val phase: Float
) {
    /** 0 before the bubble starts, 1 when it is done, for the progress [p] (0..1) of the whole animation */
    fun localProgress(p: Float): Float = ((p - delay) / (1f - delay)).coerceIn(0f, 1f)
}

/**
 * The bubbles: one for each cell of a 7 x 5 grid, with a bit of randomness. Every bubble is big enough that,
 * when they are all done, the whole picture is covered. The ones closer to the flower start first.
 */
private fun makeBubbles(width: Float, height: Float): List<Bubble> {
    val cols = 7
    val rows = 5
    val cellW = width / cols
    val cellH = height / rows
    val reach = hypot(cellW, cellH) * 0.95f
    val maxDist = hypot(width / 2f, height / 2f)
    val random = Random(11)
    val list = ArrayList<Bubble>(cols * rows)
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val cx = (col + 0.5f + (random.nextFloat() - 0.5f) * 0.44f) * cellW
            val cy = (row + 0.5f + (random.nextFloat() - 0.5f) * 0.44f) * cellH
            val dist = hypot(cx - width / 2f, cy - height / 2f) / maxDist
            val delay = (0.03f + dist * 0.5f + random.nextFloat() * 0.12f).coerceAtMost(0.62f)
            list += Bubble(
                cx = cx,
                cy = cy,
                delay = delay,
                radius = reach * (0.92f + random.nextFloat() * 0.16f),
                phase = random.nextFloat() * (2f * PI.toFloat())
            )
        }
    }
    return list
}

private fun easeOutCubic(t: Float): Float {
    val u = 1f - t
    return 1f - u * u * u
}

private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** The picture fills the whole frame (the middle part of it, like "crop") */
private fun DrawScope.drawCover(image: ImageBitmap, blendMode: BlendMode = BlendMode.SrcOver) {
    val frameAspect = size.width / size.height
    val imageAspect = image.width.toFloat() / image.height.toFloat()
    val srcW: Int
    val srcH: Int
    if (imageAspect > frameAspect) {
        srcH = image.height
        srcW = (image.height * frameAspect).roundToInt().coerceIn(1, image.width)
    } else {
        srcW = image.width
        srcH = (image.width / frameAspect).roundToInt().coerceIn(1, image.height)
    }
    drawImage(
        image = image,
        srcOffset = IntOffset((image.width - srcW) / 2, (image.height - srcH) / 2),
        srcSize = IntSize(srcW, srcH),
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        filterQuality = FilterQuality.Medium,
        blendMode = blendMode
    )
}

/** A small five petal flower */
private fun DrawScope.drawFlower(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    alpha: Float,
    color: Color
) {
    if (alpha <= 0.01f) return
    rotate(degrees = rotationDeg, pivot = center) {
        for (i in 0 until 5) {
            rotate(degrees = i * 72f, pivot = center) {
                drawOval(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(center.x - radius * 0.28f, center.y - radius),
                    size = Size(radius * 0.56f, radius * 0.95f)
                )
            }
        }
        drawCircle(
            color = Color(0xFFFFE08A).copy(alpha = alpha),
            radius = radius * 0.22f,
            center = center
        )
    }
}

/** The picture of the Mini Wallpaper (the player's photo or the ready-made one), decoded at the size that is needed */
@Composable
private fun rememberMiniWallpaperBitmap(maxSidePx: Int, enabled: Boolean): State<ImageBitmap?> {
    val context = LocalContext.current
    val fileName = AllSettings.homeMiniWallpaperFile.state
    val id = AllSettings.homeMiniWallpaper.state
    return produceState<ImageBitmap?>(initialValue = null, fileName, id, maxSidePx, enabled) {
        //a GIF and a video are played by their own players, they need no bitmap
        value = if (enabled) withContext(Dispatchers.IO) { loadMiniWallpaper(context, fileName, id, maxSidePx) } else null
    }
}

private fun loadMiniWallpaper(context: Context, fileName: String, id: String, maxSide: Int): ImageBitmap? {
    val custom = TokitoHomeMedia.fileOf(fileName)
    val ready = LauncherWallpaper.fromId(id)?.takeIf { it.inPicker } ?: DEFAULT_WALLPAPER
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openWallpaper(context, custom, ready)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        var sample = 1
        while (longest / (sample * 2) >= maxSide) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        openWallpaper(context, custom, ready)?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}

private fun openWallpaper(context: Context, custom: File?, ready: LauncherWallpaper): InputStream? =
    if (custom != null) custom.inputStream() else context.resources.openRawResource(ready.rawRes)

/**
 * Tokito Launcher: settings of the Mini Wallpaper. Show or hide it, turn the animation on or off, choose the shape
 * (horizontal or vertical), the greeting, one of the ready-made pictures or a photo, GIF or video of the gallery.
 */
@Composable
fun HomeMiniWallpaperDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shown = AllSettings.homeMiniWallpaperOn.state
    val intro = AllSettings.homeMiniWallpaperIntro.state
    val tall = AllSettings.homeMiniWallpaperShape.state == 1
    val greeting = AllSettings.homeGreeting.state
    val voice = AllSettings.homeGreetingVoice.state
    val fileName = AllSettings.homeMiniWallpaperFile.state
    val readyId = AllSettings.homeMiniWallpaper.state
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            busy = true
            message = null
            scope.launch {
                val result = TokitoHomeMedia.importAny(context, uri, TokitoHomeMedia.SLOT_WALL)
                val name = result.name
                message = when {
                    result.status == TokitoHomeMedia.Status.OK && name != null -> {
                        AllSettings.homeMiniWallpaperFile.save(name)
                        AllSettings.homeMiniWallpaperOn.save(true)
                        context.getString(R.string.home_pick_done)
                    }
                    result.status == TokitoHomeMedia.Status.TOO_BIG -> context.getString(
                        R.string.home_pick_big,
                        TokitoHomeMedia.MAX_GIF_MB,
                        TokitoHomeMedia.MAX_VIDEO_MB
                    )
                    else -> context.getString(R.string.home_pick_failed)
                }
                busy = false
            }
        }
    }

    SimpleAlertDialog(
        title = stringResource(R.string.home_wall_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.home_wall_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SwitchRow(
                    label = stringResource(R.string.home_wall_show),
                    checked = shown,
                    onChange = { AllSettings.homeMiniWallpaperOn.save(it) }
                )
                SwitchRow(
                    label = stringResource(R.string.home_wall_intro),
                    checked = intro,
                    onChange = { AllSettings.homeMiniWallpaperIntro.save(it) }
                )

                Text(
                    text = stringResource(R.string.home_wall_shape),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShapeChoice(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.home_wall_wide),
                        tall = false,
                        selected = !tall,
                        onClick = { AllSettings.homeMiniWallpaperShape.save(0) }
                    )
                    ShapeChoice(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.home_wall_tall),
                        tall = true,
                        selected = tall,
                        onClick = { AllSettings.homeMiniWallpaperShape.save(1) }
                    )
                }

                SwitchRow(
                    label = stringResource(R.string.home_wall_greeting),
                    checked = greeting,
                    onChange = { AllSettings.homeGreeting.save(it) }
                )
                if (greeting) {
                    SwitchRow(
                        label = stringResource(R.string.home_wall_voice),
                        checked = voice,
                        onChange = { AllSettings.homeGreetingVoice.save(it) }
                    )
                }

                Text(
                    text = stringResource(R.string.home_wall_ready),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LauncherWallpaper.entries.filter { it.inPicker }.forEach { wallpaper ->
                        val selected = fileName.isEmpty() &&
                                (readyId == wallpaper.id || (readyId.isEmpty() && wallpaper == DEFAULT_WALLPAPER))
                        val tile = RoundedCornerShape(12.dp)
                        Image(
                            modifier = Modifier
                                .size(width = 84.dp, height = 52.dp)
                                .clip(tile)
                                .then(
                                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, tile)
                                    else Modifier
                                )
                                .clickable {
                                    AllSettings.homeMiniWallpaper.save(wallpaper.id)
                                    AllSettings.homeMiniWallpaperFile.save("")
                                    AllSettings.homeMiniWallpaperOn.save(true)
                                    scope.launch(Dispatchers.IO) { TokitoHomeMedia.clear(TokitoHomeMedia.SLOT_WALL) }
                                },
                            painter = painterResource(wallpaper.thumbRes),
                            contentDescription = stringResource(wallpaper.nameRes),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                IconTextButton(
                    onClick = { picker.launch(arrayOf("image/*", "video/*")) },
                    painter = painterResource(R.drawable.ic_image_outlined),
                    text = stringResource(R.string.home_wall_gallery),
                    enabled = !busy
                )
                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        onDismiss = onDismiss
    )
}

/** One of the two shapes of the frame, with a small drawing of it (wide or tall) */
@Composable
private fun ShapeChoice(
    label: String,
    tall: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = if (tall) 14.dp else 24.dp, height = if (tall) 24.dp else 14.dp)
                    .border(2.dp, tint, RoundedCornerShape(3.dp))
            )
            Text(text = label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** A line with a text and a switch */
@Composable
internal fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.titleSmall
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
