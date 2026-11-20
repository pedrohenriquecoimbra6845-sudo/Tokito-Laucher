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

package com.movtery.zalithlauncher.game.launch

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.view.Display
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.min

private const val TAG = "DeviceBenchmark"

/**
 * Tokito Launcher: a short benchmark (about 2 seconds) that looks at the phone before the game: processor,
 * graphics chip, memory and refresh rate. It gives the highest FPS that the optimization modes start from.
 *
 * It is an estimate: the real number is found by the FPS governor while the game runs (it lowers and raises
 * the limit by 10 with what the phone really holds, and saves it in `tokitoCalibratedFps`).
 */
object DeviceBenchmark {
    /** @param tier 0 = weak, 1 = medium, 2 = strong */
    data class Result(
        val tier: Int,
        val ceilingFps: Int,
        val cpuMips: Int,
        val gpuFps: Int?,
        val cores: Int,
        val ramMb: Int,
        val maxHz: Int
    )

    fun hasResult(): Boolean = AllSettings.tokitoBenchmarkTier.getValue() >= 0

    suspend fun run(context: Context): Result = withContext(Dispatchers.Default) {
        val cores = Runtime.getRuntime().availableProcessors()
        val ramMb = totalRamMb(context)
        val maxHz = displayMaxHz(context)

        val cpuMips = cpuScore()
        val gpuFps = gpuScore()

        val cpuTier = when {
            cpuMips < 500 -> 0
            cpuMips < 900 -> 1
            else -> 2
        }
        val gpuTier = when {
            gpuFps == null -> 1
            gpuFps < 150 -> 0
            gpuFps < 600 -> 1
            else -> 2
        }
        val ramTier = when {
            ramMb < 3000 -> 0
            ramMb < 6000 -> 1
            else -> 2
        }
        val tier = minOf(cpuTier, gpuTier, ramTier)
        val tierFps = when (tier) {
            0 -> 60
            1 -> 90
            else -> 240
        }
        val ceiling = (min(tierFps, maxHz) / 10 * 10).coerceAtLeast(30)

        Logger.info(TAG, "cpu=$cpuMips Mit/s gpu=$gpuFps fps ram=${ramMb}MB cores=$cores hz=$maxHz -> tier=$tier ceiling=$ceiling")
        AllSettings.tokitoBenchmarkTier.save(tier)
        AllSettings.tokitoBenchmarkFps.save(ceiling)
        //a new benchmark starts the calibration again
        AllSettings.tokitoCalibratedFps.save(0)
        Result(tier, ceiling, cpuMips, gpuFps, cores, ramMb, maxHz)
    }

    private fun totalRamMb(context: Context): Int = try {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        (info.totalMem / (1024 * 1024)).toInt()
    } catch (_: Exception) {
        4096
    }

    private fun displayMaxHz(context: Context): Int = try {
        val display: Display? = context.getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
        display?.let { DisplayRates.maxHz(it) } ?: 60
    } catch (_: Exception) {
        60
    }

    /**
     * Millions of simple operations per second of the fastest of two threads (the work can end up in
     * a slow or a fast core of the phone; the best one is what the game main thread gets).
     */
    private suspend fun cpuScore(): Int = withContext(Dispatchers.Default) {
        fun spin(durationMs: Long): Long {
            var x = 12345L
            var acc = 0L
            var count = 0L
            val end = System.nanoTime() + durationMs * 1_000_000L
            while (System.nanoTime() < end) {
                for (i in 0 until 20_000) {
                    x = x * 6364136223846793005L + 1442695040888963407L
                    acc += (x ushr 33) xor i.toLong()
                }
                count += 20_000
            }
            return if (acc == Long.MIN_VALUE) -1 else count
        }
        spin(120) //warm up (the code is compiled while it runs)
        val results = LongArray(2)
        val threads = (0 until 2).map { index ->
            Thread { results[index] = spin(450) }.also { it.start() }
        }
        threads.forEach { it.join() }
        val best = results.max()
        (best / 450L / 1000L).toInt() //operations per millisecond / 1000 = millions per second
    }

    /**
     * Frames per second of a heavy shader on a small off-screen surface. Null when the test cannot run
     * on this phone (then the graphics chip counts as medium).
     */
    private fun gpuScore(): Int? {
        var display = EGL14.EGL_NO_DISPLAY
        var surface = EGL14.EGL_NO_SURFACE
        var context = EGL14.EGL_NO_CONTEXT
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return null
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return null

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, count, 0) || count[0] < 1) return null
            val config = configs[0] ?: return null

            surface = EGL14.eglCreatePbufferSurface(
                display, config,
                intArrayOf(EGL14.EGL_WIDTH, SIZE, EGL14.EGL_HEIGHT, SIZE, EGL14.EGL_NONE), 0
            )
            context = EGL14.eglCreateContext(
                display, config, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
            )
            if (surface == EGL14.EGL_NO_SURFACE || context == EGL14.EGL_NO_CONTEXT) return null
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) return null

            val program = buildProgram() ?: return null
            GLES20.glUseProgram(program)
            val position = GLES20.glGetAttribLocation(program, "p")
            val time = GLES20.glGetUniformLocation(program, "t")
            val quad: FloatBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            quad.put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)).position(0)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, quad)
            GLES20.glViewport(0, 0, SIZE, SIZE)

            //warm up
            repeat(3) {
                GLES20.glUniform1f(time, it.toFloat())
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            }
            GLES20.glFinish()

            var frames = 0
            val start = System.nanoTime()
            val end = start + 600_000_000L
            while (System.nanoTime() < end) {
                GLES20.glUniform1f(time, frames * 0.01f)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                GLES20.glFinish()
                frames++
            }
            val seconds = (System.nanoTime() - start) / 1_000_000_000.0
            if (GLES20.glGetError() != GLES20.GL_NO_ERROR || frames == 0) return null
            return (frames / seconds).toInt()
        } catch (e: Throwable) {
            Logger.warning(TAG, "The graphics test could not run", e)
            return null
        } finally {
            try {
                if (display != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
                    if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                    EGL14.eglTerminate(display)
                }
            } catch (_: Throwable) {
            }
        }
    }

    private const val SIZE = 512

    private fun buildProgram(): Int? {
        val vertex = compile(
            GLES20.GL_VERTEX_SHADER,
            "attribute vec2 p; void main(){ gl_Position = vec4(p, 0.0, 1.0); }"
        ) ?: return null
        val fragment = compile(
            GLES20.GL_FRAGMENT_SHADER,
            """
            precision mediump float;
            uniform float t;
            void main() {
                vec2 uv = gl_FragCoord.xy * 0.002;
                float a = 0.0;
                for (int i = 0; i < 48; i++) {
                    a += sin(uv.x * float(i) + t) * cos(uv.y * float(i) - t);
                    uv = uv.yx * 0.99 + vec2(a * 0.001);
                }
                gl_FragColor = vec4(a * 0.5 + 0.5, uv.x, uv.y, 1.0);
            }
            """.trimIndent()
        ) ?: return null
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        return if (status[0] == GLES20.GL_TRUE) program else null
    }

    private fun compile(type: Int, source: String): Int? {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        return if (status[0] == GLES20.GL_TRUE) shader else null
    }
}
