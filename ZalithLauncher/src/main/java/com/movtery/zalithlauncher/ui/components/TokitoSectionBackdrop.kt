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

package com.movtery.zalithlauncher.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Seções do launcher que ganham um fundo próprio (Minecraft em pixel art). */
enum class BackdropSection { VERSIONS, MODS, TEXTURES, SHADERS, MODPACKS, GAMES, SAVES }

/** Escolhe o fundo pela tela de download aberta (o nome da chave de navegação). */
fun downloadBackdropSection(key: Any?): BackdropSection {
    val name = key?.let { it::class.simpleName }.orEmpty()
    return when {
        name.contains("ModPack", ignoreCase = true) -> BackdropSection.MODPACKS
        name.contains("Shader", ignoreCase = true) -> BackdropSection.SHADERS
        name.contains("ResourcePack", ignoreCase = true) || name.contains("Texture", ignoreCase = true) -> BackdropSection.TEXTURES
        name.contains("Mod", ignoreCase = true) -> BackdropSection.MODS
        name.contains("Save", ignoreCase = true) -> BackdropSection.SAVES
        else -> BackdropSection.GAMES
    }
}

/**
 * Fundo de uma seção: uma cena de Minecraft em pixel art (gerada uma vez, 200x96, ampliada sem
 * suavizar), com uma sombra por cima para o texto e os painéis de vidro ficarem legíveis.
 * É uma camada só atrás do conteúdo: não faz as listas recomporem.
 */
@Composable
fun TokitoSectionBackdrop(
    section: BackdropSection,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        Crossfade(
            targetState = section,
            modifier = Modifier.matchParentSize(),
            animationSpec = tween(durationMillis = 450),
            label = "tokito_section_backdrop"
        ) { target ->
            SectionScene(section = target, modifier = Modifier.fillMaxSize())
        }
        val scrim = remember {
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.34f), Color.Black.copy(alpha = 0.56f)))
        }
        Canvas(modifier = Modifier.matchParentSize()) { drawRect(brush = scrim) }
        content()
    }
}

private val sceneCache = ConcurrentHashMap<BackdropSection, ImageBitmap>()

@Composable
private fun SectionScene(section: BackdropSection, modifier: Modifier = Modifier) {
    val image by produceState<ImageBitmap?>(initialValue = sceneCache[section], key1 = section) {
        if (value == null) {
            val built = withContext(Dispatchers.Default) { buildScene(section).asImageBitmap() }
            sceneCache[section] = built
            value = built
        }
    }
    Canvas(modifier = modifier) {
        val img = image ?: return@Canvas
        val iw = img.width
        val ih = img.height
        val scale = max(size.width / iw, size.height / ih)
        val dw = (iw * scale).roundToInt()
        val dh = (ih * scale).roundToInt()
        drawImage(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(iw, ih),
            dstOffset = IntOffset(((size.width - dw) / 2f).roundToInt(), ((size.height - dh) / 2f).roundToInt()),
            dstSize = IntSize(dw, dh),
            filterQuality = FilterQuality.None
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Cenas em pixel art (cada pixel da imagem vira um "bloco" grande na tela)
// ---------------------------------------------------------------------------------------------

private const val SW = 200
private const val SH = 96

private fun argb(r: Int, g: Int, b: Int): Int =
    (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

private fun cr(c: Int) = (c shr 16) and 0xFF
private fun cg(c: Int) = (c shr 8) and 0xFF
private fun cb(c: Int) = c and 0xFF

private fun mix(a: Int, b: Int, t: Float): Int {
    val k = t.coerceIn(0f, 1f)
    return argb(
        (cr(a) + (cr(b) - cr(a)) * k).toInt(),
        (cg(a) + (cg(b) - cg(a)) * k).toInt(),
        (cb(a) + (cb(b) - cb(a)) * k).toInt()
    )
}

private fun shade(c: Int, f: Float): Int = argb((cr(c) * f).toInt(), (cg(c) * f).toInt(), (cb(c) * f).toInt())

private fun hash(x: Int, y: Int, seed: Int): Int {
    var h = x * 374761393 + y * 668265263 + seed * 1274126177
    h = (h xor (h ushr 13)) * 1274126177
    return h xor (h ushr 16)
}

private fun rnd(x: Int, y: Int, seed: Int): Float = (hash(x, y, seed) and 0xFFFF) / 65535f

private class Pix(val w: Int, val h: Int) {
    val px = IntArray(w * h)

    fun put(x: Int, y: Int, c: Int) {
        if (x in 0 until w && y in 0 until h) px[y * w + x] = c
    }

    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
        for (y in max(y0, 0)..min(y1, h - 1)) for (x in max(x0, 0)..min(x1, w - 1)) px[y * w + x] = c
    }

    fun blend(x: Int, y: Int, c: Int, a: Float) {
        if (x in 0 until w && y in 0 until h) px[y * w + x] = mix(px[y * w + x], c, a)
    }

    fun add(x: Int, y: Int, r: Int, g: Int, b: Int) {
        if (x in 0 until w && y in 0 until h) {
            val o = px[y * w + x]
            px[y * w + x] = argb(cr(o) + r, cg(o) + g, cb(o) + b)
        }
    }

    fun glow(cx: Int, cy: Int, radius: Float, c: Int, strength: Float) {
        val r = radius.toInt() + 1
        for (y in cy - r..cy + r) for (x in cx - r..cx + r) {
            val d = sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toFloat())
            if (d < radius) {
                val f = 1f - d / radius
                val k = f * f * strength
                add(x, y, (cr(c) * k).toInt(), (cg(c) * k).toInt(), (cb(c) * k).toInt())
            }
        }
    }

    /** Céu em degradê vertical (com um tremido leve para não ter faixas). */
    fun sky(top: Int, mid: Int, bottom: Int, seed: Int) {
        for (y in 0 until h) {
            val t = y / (h - 1f)
            val base = if (t < 0.55f) mix(top, mid, t / 0.55f) else mix(mid, bottom, (t - 0.55f) / 0.45f)
            for (x in 0 until w) px[y * w + x] = shade(base, 1f + (rnd(x, y, seed) - 0.5f) * 0.035f)
        }
    }
}

private fun Pix.stars(count: Int, maxFrac: Float, seed: Int) {
    for (i in 0 until count) {
        val sx = (rnd(i, 1, seed) * w).toInt()
        val sy = (rnd(i, 2, seed) * h * maxFrac).toInt()
        val a = 0.35f + 0.65f * rnd(i, 3, seed)
        blend(sx, sy, argb(245, 250, 255), a)
        if (rnd(i, 4, seed) > 0.93f) {
            blend(sx + 1, sy, argb(245, 250, 255), a * 0.6f)
            blend(sx, sy + 1, argb(245, 250, 255), a * 0.6f)
        }
    }
}

private fun Pix.sun(cx: Int, cy: Int, half: Int, core: Int, rim: Int) {
    rect(cx - half, cy - half, cx + half, cy + half, rim)
    rect(cx - half + 2, cy - half + 2, cx + half - 2, cy + half - 2, core)
}

private fun Pix.cloud(cx: Int, cy: Int, wd: Int, c: Int, a: Float) {
    for (y in cy..cy + 2) for (x in cx - wd / 2..cx + wd / 2) blend(x, y, c, a)
    for (y in cy - 2..cy - 1) for (x in cx - wd / 4..cx + wd / 4 + 2) blend(x, y, c, a)
}

/** Uma camada de colinas de blocos (grama em cima, terra embaixo). Devolve a altura de cada coluna. */
private fun Pix.hills(
    base: Float, amp: Float, freq: Float, phase: Float,
    grass: Int, dirt: Int, seed: Int, grassDepth: Int = 3
): IntArray {
    val tops = IntArray(w)
    for (x in 0 until w) {
        val v = base + amp * sin(x * freq + phase) + amp * 0.45f * sin(x * freq * 2.3f + phase * 1.7f)
        val top = v.toInt()
        tops[x] = top
        for (y in max(top, 0) until h) {
            val d = y - top
            val n = rnd(x / 2, y / 2, seed)
            px[y * w + x] = if (d < grassDepth) shade(grass, 0.86f + 0.28f * n) else shade(dirt, 0.78f + 0.3f * n)
        }
    }
    return tops
}

private fun Pix.tree(x: Int, groundY: Int, trunkH: Int, leaf: Int, trunk: Int, seed: Int) {
    rect(x - 1, groundY - trunkH, x + 1, groundY + 1, shade(trunk, 0.9f))
    rect(x, groundY - trunkH, x, groundY + 1, shade(trunk, 1.12f))
    val top = groundY - trunkH
    for (yy in top - 7..top + 1) for (xx in x - 5..x + 5) {
        if (rnd(xx, yy, seed) > 0.16f) put(xx, yy, shade(leaf, 0.75f + 0.5f * rnd(xx + 9, yy + 4, seed)))
    }
}

private fun Pix.treesOn(tops: IntArray, firstX: Int, step: Int, leaf: Int, trunk: Int, seed: Int) {
    var i = 0
    var x = firstX
    while (x < w) {
        val xx = x + (rnd(i, 7, seed) * 8).toInt()
        if (xx in 0 until w) tree(xx, tops[xx], 9 + (rnd(i, 8, seed) * 4).toInt(), leaf, trunk, seed + i)
        x += step
        i++
    }
}

private fun Pix.sceneDusk() {
    sky(argb(24, 14, 66), argb(156, 54, 128), argb(255, 154, 112), 1)
    stars(70, 0.42f, 11)
    val sx = (w * 0.70f).toInt()
    val sy = (h * 0.60f).toInt()
    glow(sx, sy, 46f, argb(255, 130, 70), 0.95f)
    sun(sx, sy, 7, argb(255, 240, 190), argb(255, 205, 140))
    cloud(30, 20, 26, argb(255, 190, 220), 0.35f)
    cloud(120, 14, 22, argb(255, 170, 210), 0.32f)
    cloud(168, 30, 30, argb(255, 160, 200), 0.3f)
    hills(58f, 6f, 0.030f, 0.6f, argb(96, 60, 140), argb(70, 44, 110), 3, 2)
    hills(66f, 7f, 0.045f, 2.2f, argb(80, 100, 120), argb(70, 60, 84), 4, 2)
    val near = hills(76f, 8f, 0.038f, 4.1f, argb(62, 150, 66), argb(110, 74, 52), 5, 3)
    treesOn(near, 8, 22, argb(44, 118, 70), argb(88, 60, 40), 7)
}

private fun Pix.sceneCave() {
    val floor = IntArray(w)
    val ceil = IntArray(w)
    for (x in 0 until w) {
        floor[x] = (70 + 5 * sin(x * 0.07f) + 3 * sin(x * 0.19f + 1f)).toInt()
        ceil[x] = (15 + 6 * sin(x * 0.05f + 2f) + 2 * sin(x * 0.21f)).toInt()
    }
    for (y in 0 until h) for (x in 0 until w) {
        val inside = y >= ceil[x] && y <= floor[x]
        val n = rnd(x / 2, y / 2, 21)
        val n2 = rnd(x, y, 22)
        val base = if (inside) mix(argb(20, 16, 42), argb(36, 28, 70), n) else mix(argb(52, 46, 78), argb(82, 72, 112), n)
        put(x, y, shade(base, 0.86f + 0.28f * n2))
    }
    for (i in 0 until 130) {
        val x = (rnd(i, 1, 24) * w).toInt()
        val y = (rnd(i, 2, 24) * h).toInt()
        if (x !in 0 until w || (y > ceil[x] - 2 && y < floor[x] + 2)) continue
        val kind = (rnd(i, 3, 24) * 5).toInt()
        val c = when (kind) {
            0 -> argb(30, 30, 34)
            1 -> argb(214, 170, 130)
            2 -> argb(250, 214, 70)
            3 -> argb(80, 235, 230)
            else -> argb(220, 40, 50)
        }
        put(x, y, c)
        put(x + 1, y, c)
        put(x, y + 1, shade(c, 0.85f))
        if (rnd(i, 4, 24) > 0.5f) put(x + 1, y + 1, c)
        if (kind == 4) glow(x, y, 6f, argb(255, 40, 50), 0.5f)
    }
    for (x in 0 until w) {
        val ly = 84 + (3 * sin(x * 0.11f)).toInt()
        for (y in ly until h) put(x, y, mix(argb(255, 96, 16), argb(255, 196, 64), rnd(x / 2, y, 30)))
    }
    for (i in 0 until 6) glow(15 + i * 33, 86, 22f, argb(255, 110, 20), 0.55f)
    for (x in 10 until w - 10) {
        val fy = floor[x] - 1
        val lit = (x / 6) % 3 != 0
        put(x, fy, if (lit) argb(235, 30, 30) else argb(110, 20, 20))
        if (lit && x % 6 == 0) glow(x, fy, 7f, argb(255, 30, 30), 0.35f)
    }
    for (i in 0 until 4) {
        val tx = 22 + i * 48
        val ty = floor[tx] - 1
        rect(tx, ty - 7, tx, ty, argb(120, 84, 48))
        rect(tx - 1, ty - 10, tx + 1, ty - 8, argb(255, 190, 60))
        put(tx, ty - 11, argb(255, 240, 150))
        glow(tx, ty - 9, 26f, argb(255, 160, 50), 0.8f)
    }
}

private fun Pix.tile(x0: Int, y0: Int, kind: Int, seed: Int) {
    for (yy in 0 until 16) for (xx in 0 until 16) {
        val n = rnd(xx, yy, seed)
        val n2 = rnd(xx / 2, yy / 2, seed + 5)
        val stone = shade(argb(128, 128, 134), 0.8f + 0.36f * n)
        val c = when (kind) {
            0 -> if (yy < 3 || n2 > 0.85f) shade(argb(96, 176, 62), 0.85f + 0.3f * n) else shade(argb(124, 86, 56), 0.8f + 0.3f * n)
            1 -> shade(argb(124, 86, 56), 0.78f + 0.36f * n)
            2 -> stone
            3 -> if (yy % 4 == 3 || (xx + (yy / 4) * 5) % 8 == 0) shade(argb(104, 74, 38), 0.9f + 0.1f * n) else shade(argb(176, 132, 76), 0.88f + 0.2f * n)
            4 -> shade(argb(120, 120, 124), (if (xx % 4 == 0 || yy % 4 == 0) 0.62f else 0.85f) + 0.25f * rnd(xx / 4, yy / 4, seed + 9))
            5 -> if (yy % 4 == 3 || (xx + (if ((yy / 4) % 2 == 0) 0 else 4)) % 8 == 7) argb(170, 160, 150) else shade(argb(160, 70, 56), 0.85f + 0.3f * n)
            6 -> if (n2 > 0.78f) shade(argb(250, 214, 70), 0.9f + 0.2f * n) else stone
            7 -> if (n2 > 0.80f) shade(argb(80, 235, 230), 0.9f + 0.2f * n) else stone
            8 -> shade(argb(226, 210, 150), 0.88f + 0.22f * n)
            9 -> shade(argb(52, 96, 220), 0.8f + 0.4f * n)
            10 -> shade(argb(30, 14, 52), 0.7f + 0.8f * n)
            else -> shade(argb(130, 52, 52), 0.8f + 0.4f * n)
        }
        put(x0 + xx, y0 + yy, c)
    }
}

private fun Pix.sceneTextures() {
    rect(0, 0, w - 1, h - 1, argb(24, 16, 40))
    val ox = (w - 12 * 16) / 2
    for (ty in 0 until 6) for (tx in 0 until 12) {
        tile(ox + tx * 16, ty * 16, (rnd(tx, ty, 41) * 12).toInt(), tx * 7 + ty * 13)
    }
}

private fun Pix.sceneShaders() {
    sky(argb(18, 10, 58), argb(176, 48, 120), argb(255, 176, 84), 31)
    val sx = (w * 0.5f).toInt()
    val sy = (h * 0.58f).toInt()
    for (y in 0 until (h * 0.72f).toInt()) for (x in 0 until w) {
        val dx = (x - sx).toFloat()
        val dy = (y - sy).toFloat()
        val dist = sqrt(dx * dx + dy * dy)
        val ang = atan2(dy, dx)
        val ray = 0.5f + 0.5f * sin(ang * 17f) * sin(ang * 5.3f + 1.2f)
        val k = ray * ray * exp(-dist / 62f) * 0.55f
        add(x, y, (255 * k).toInt(), (178 * k).toInt(), (96 * k).toInt())
    }
    glow(sx, sy, 56f, argb(255, 150, 80), 0.9f)
    sun(sx, sy, 8, argb(255, 246, 206), argb(255, 214, 150))
    hills(62f, 5f, 0.04f, 0.3f, argb(70, 40, 110), argb(50, 30, 90), 32, 1)
    hills(69f, 6f, 0.05f, 2.4f, argb(52, 30, 92), argb(40, 24, 76), 33, 1)
    hills(76f, 5f, 0.06f, 4.6f, argb(34, 22, 70), argb(28, 18, 58), 34, 1)
    val wy = (h * 0.80f).toInt()
    for (y in wy until h) for (x in 0 until w) {
        val t = (y - wy) / (h - wy).toFloat()
        val base = mix(argb(70, 40, 110), argb(24, 16, 56), t)
        val streak = rnd(x / 3, y, 36) > 0.55f - 0.3f * (1f - t)
        val reflect = exp(-abs(x - sx).toFloat() / (10f + 18f * t)) * (if (streak) 1f else 0.25f)
        put(x, y, mix(base, argb(255, 196, 120), reflect * 0.85f))
    }
}

private fun Pix.scenePortal() {
    sky(argb(10, 8, 34), argb(40, 20, 84), argb(92, 40, 132), 51)
    stars(90, 0.55f, 52)
    val ground = hills(74f, 3f, 0.03f, 1.0f, argb(40, 96, 52), argb(60, 44, 40), 53, 2)
    val cx = w / 2
    val pw = 32
    val ph = 40
    val x0 = cx - pw / 2
    val y0 = ground[cx] - ph
    glow(cx, y0 + ph / 2, 70f, argb(150, 40, 220), 0.9f)
    for (y in y0 until y0 + ph) for (x in x0 until x0 + pw) {
        val inner = x >= x0 + 8 && x < x0 + pw - 8 && y >= y0 + 8 && y < y0 + ph - 8
        if (inner) {
            val sw = sin((x - cx) * 0.55f + (y - y0) * 0.35f) * 0.5f + 0.5f
            put(x, y, mix(argb(70, 10, 150), argb(200, 80, 255), sw * 0.6f + rnd(x, y, 55) * 0.4f))
        } else {
            put(x, y, shade(argb(30, 14, 52), 0.7f + 0.8f * rnd(x, y, 54)))
        }
    }
    for (i in 0 until 40) {
        val px = cx + ((rnd(i, 1, 56) - 0.5f) * 90f).toInt()
        val py = y0 + ph - (rnd(i, 2, 56) * 60f).toInt()
        blend(px, py, argb(220, 140, 255), 0.4f + 0.5f * rnd(i, 3, 56))
    }
}

private fun Pix.sceneDay() {
    sky(argb(86, 150, 236), argb(150, 202, 250), argb(214, 238, 255), 61)
    val sx = (w * 0.82f).toInt()
    val sy = (h * 0.2f).toInt()
    glow(sx, sy, 34f, argb(255, 240, 180), 0.5f)
    sun(sx, sy, 7, argb(255, 252, 214), argb(255, 240, 170))
    cloud(26, 18, 30, argb(255, 255, 255), 0.85f)
    cloud(92, 26, 24, argb(255, 255, 255), 0.8f)
    cloud(140, 12, 28, argb(255, 255, 255), 0.85f)
    hills(58f, 5f, 0.03f, 0.2f, argb(120, 170, 190), argb(100, 130, 150), 62, 2)
    hills(66f, 6f, 0.045f, 1.9f, argb(96, 170, 70), argb(120, 84, 56), 63, 2)
    val near = hills(76f, 7f, 0.035f, 4.4f, argb(74, 160, 54), argb(116, 80, 52), 64, 3)
    treesOn(near, 10, 24, argb(52, 140, 56), argb(100, 70, 44), 65)
}

private fun Pix.sceneNight() {
    sky(argb(6, 8, 30), argb(20, 28, 74), argb(44, 62, 110), 71)
    stars(130, 0.7f, 72)
    val mx = (w * 0.2f).toInt()
    val my = (h * 0.2f).toInt()
    glow(mx, my, 30f, argb(120, 140, 220), 0.5f)
    sun(mx, my, 6, argb(236, 240, 255), argb(200, 208, 230))
    put(mx - 2, my - 1, argb(180, 188, 210))
    put(mx + 2, my + 2, argb(180, 188, 210))
    hills(60f, 6f, 0.032f, 0.9f, argb(24, 44, 70), argb(20, 30, 50), 73, 2)
    val near = hills(74f, 7f, 0.04f, 3.3f, argb(26, 70, 62), argb(40, 36, 44), 74, 3)
    treesOn(near, 6, 21, argb(20, 60, 56), argb(50, 36, 30), 75)
    for (i in 0 until 6) {
        val lx = 120 + i * 9
        val ly = near[lx] - 4 - (i % 2) * 2
        rect(lx, ly, lx + 2, ly + 2, argb(255, 214, 110))
        glow(lx + 1, ly + 1, 9f, argb(255, 190, 80), 0.5f)
    }
    for (i in 0 until 16) {
        blend((rnd(i, 1, 76) * w).toInt(), 50 + (rnd(i, 2, 76) * 30f).toInt(), argb(220, 255, 120), 0.8f)
    }
}

private fun buildScene(section: BackdropSection): Bitmap {
    val g = Pix(SW, SH)
    when (section) {
        BackdropSection.VERSIONS -> g.sceneDusk()
        BackdropSection.MODS -> g.sceneCave()
        BackdropSection.TEXTURES -> g.sceneTextures()
        BackdropSection.SHADERS -> g.sceneShaders()
        BackdropSection.MODPACKS -> g.scenePortal()
        BackdropSection.GAMES -> g.sceneDay()
        BackdropSection.SAVES -> g.sceneNight()
    }
    val bmp = Bitmap.createBitmap(SW, SH, Bitmap.Config.ARGB_8888)
    bmp.setPixels(g.px, 0, SW, 0, 0, SW, SH)
    return bmp
}
