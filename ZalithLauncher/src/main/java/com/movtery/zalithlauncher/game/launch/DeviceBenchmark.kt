/*
 * Tokito Launcher
 * Based on Zalith Launcher 2, Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

private const val TAG = "DeviceBenchmark"

/**
 * Tokito Launcher: the benchmark of the phone (about 10 seconds), run from its own tab.
 *
 * It is made to be precise and to show what is happening:
 * 1. **Processor** alone, one thread (like the main thread of the game): a small piece of "game work"
 *    (counting the faces of the blocks of a region, what a chunk mesher does) repeated as fast as it can.
 * 2. **Graphics chip** alone: a heavy shader on an off-screen surface.
 * 3. **Both at the same time**, with more threads: this is what shows a phone that throttles when it gets hot
 *    or that shares memory badly (the FPS falls while the test runs).
 *
 * Every 200 ms a reading goes to the screen (the FPS goes up and down live). The first readings of each part
 * are only warm-up (the code is compiled while it runs) and are left out; the number of a part is the **median**
 * of its readings, so one bad reading cannot move it.
 *
 * The result is an estimate of what the phone can do: the real number is found by the FPS governor while the
 * game runs (it moves the limit one step at a time with what the phone really holds, and saves it in
 * `tokitoCalibratedFps`).
 */
object DeviceBenchmark {
    enum class Phase { CPU, GPU, BOTH }

    /** One reading: the part of the test, the FPS of the reading, and how far the whole test is (0 to 1) */
    data class Sample(val phase: Phase, val fps: Int, val progress: Float)

    /**
     * @param tier 0 = weak (under 60 FPS), 1 = medium (under 100), 2 = strong
     * @param startFps where the FPS governor starts (never below the floor of the phone)
     * @param deviceFps FPS of the test
     * @param throttlePercent how much the FPS fell when everything ran together (heat, shared memory)
     */
    data class Result(
        val tier: Int,
        val startFps: Int,
        val deviceFps: Int,
        val cpuFps: Int,
        val gpuFps: Int?,
        val throttlePercent: Int,
        val cores: Int,
        val ramMb: Int,
        val maxHz: Int
    )

    fun hasResult(): Boolean = AllSettings.tokitoBenchmarkTier.getValue() >= 0

    suspend fun run(context: Context, onSample: (Sample) -> Unit = {}): Result = withContext(Dispatchers.Default) {
        val cores = Runtime.getRuntime().availableProcessors()
        val ramMb = totalRamMb(context)
        val maxHz = displayMaxHz(context)
        val blocks = makeBlocks()
        val totalMs = (CPU_MS + GPU_MS + BOTH_MS).toFloat()

        val units = Meter()
        val frames = Meter()

        //1. the processor alone (one thread)
        val cpuStop = AtomicBoolean(false)
        val cpuThreads = startCpuThreads(1, cpuStop, units.counter, blocks)
        val cpuReadings = readings(Phase.CPU, CPU_MS, 0f, totalMs, onSample) { seconds ->
            units.rate(seconds) * CPU_THREAD_SCALE
        }
        cpuStop.set(true)
        cpuThreads.forEach { it.join() }

        //2. the graphics chip alone
        val gpuStop = AtomicBoolean(false)
        val gpuThread = startGpuThread(gpuStop, frames.counter)
        val gpuReadings = readings(Phase.GPU, GPU_MS, CPU_MS.toFloat(), totalMs, onSample) { seconds ->
            frames.rate(seconds) / GPU_DIVISOR
        }
        gpuStop.set(true)
        gpuThread.join()
        val gpuWorks = gpuReadings.any { it > 0 }

        //3. everything at once: the threads of the game and the graphics (this is where the heat shows)
        val bothThreads = (cores / 2).coerceIn(2, 4)
        val bothStop = AtomicBoolean(false)
        units.reset()
        frames.reset()
        val workers = startCpuThreads(bothThreads, bothStop, units.counter, blocks)
        val gpuThread2 = if (gpuWorks) startGpuThread(bothStop, frames.counter) else null
        val bothReadings = readings(Phase.BOTH, BOTH_MS, (CPU_MS + GPU_MS).toFloat(), totalMs, onSample) { seconds ->
            val cpu = units.rate(seconds) * CPU_THREAD_SCALE / bothThreads
            if (gpuWorks) minOf(cpu, frames.rate(seconds) / GPU_DIVISOR) else cpu
        }
        bothStop.set(true)
        workers.forEach { it.join() }
        gpuThread2?.join()

        val cpuFps = median(cpuReadings)
        val gpuFps = if (gpuWorks) median(gpuReadings) else null
        val peak = minOf(cpuFps, gpuFps ?: cpuFps)
        val sustained = median(bothReadings)
        val deviceFps = (peak * 4 + sustained * 6) / 10
        val throttle = if (peak > 0) (100 - sustained * 100 / peak).coerceIn(0, 100) else 0
        val tier = when {
            deviceFps < 60 -> 0
            deviceFps < 100 -> 1
            else -> 2
        }
        //the governor starts at a fraction of the test (the test is more demanding than a game that can be lowered),
        //and moves one step at a time from there with what the phone really holds
        val startFps = DeviceProfile.roundToStep(minOf(maxHz, deviceFps / 2))
            .coerceAtLeast(DeviceProfile.floorFps)

        Logger.info(
            TAG,
            "cpu=$cpuFps gpu=$gpuFps both=$sustained device=$deviceFps throttle=$throttle% " +
                "ram=${ramMb}MB cores=$cores hz=$maxHz -> tier=$tier start=$startFps"
        )
        AllSettings.tokitoBenchmarkTier.save(tier)
        AllSettings.tokitoBenchmarkFps.save(startFps)
        AllSettings.tokitoBenchmarkScore.save(deviceFps)
        AllSettings.tokitoBenchmarkThrottle.save(throttle)
        //a new benchmark starts the calibration again
        AllSettings.tokitoCalibratedFps.save(0)
        Result(tier, startFps, deviceFps, cpuFps, gpuFps, throttle, cores, ramMb, maxHz)
    }

    /**
     * Reads [read] every 200 ms for [durationMs] (a reading gets the seconds that passed) and sends each one to
     * [onSample]. The first readings are warm-up: they are shown but not returned.
     */
    private suspend fun readings(
        phase: Phase,
        durationMs: Long,
        doneMs: Float,
        totalMs: Float,
        onSample: (Sample) -> Unit,
        read: (seconds: Double) -> Double
    ): List<Int> {
        val kept = ArrayList<Int>()
        var index = 0
        var last = System.nanoTime()
        val end = last + durationMs * 1_000_000L
        while (System.nanoTime() < end) {
            delay(WINDOW_MS)
            coroutineContext.ensureActive()
            val now = System.nanoTime()
            val seconds = (now - last) / 1_000_000_000.0
            last = now
            val fps = read(seconds).toInt().coerceAtLeast(0)
            val elapsed = doneMs + (durationMs - (end - now) / 1_000_000L).coerceIn(0L, durationMs)
            onSample(Sample(phase, fps, (elapsed / totalMs).coerceIn(0f, 1f)))
            if (index >= WARMUP_READINGS) kept.add(fps)
            index++
        }
        return kept
    }

    /** A counter that the test threads add to, and the rate it had since the last reading */
    private class Meter {
        val counter = AtomicLong()
        private var last = 0L

        /** What the counter gained since the last time, per second */
        fun rate(seconds: Double): Double {
            val now = counter.get()
            val gained = now - last
            last = now
            return if (seconds > 0) gained / seconds else 0.0
        }

        fun reset() {
            counter.set(0)
            last = 0
        }
    }

    private fun median(values: List<Int>): Int =
        if (values.isEmpty()) 0 else values.sorted()[values.size / 2]

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

    // ---------------------------------------------------------------- processor

    /**
     * A region of blocks (a hill of solid blocks with some holes): what the processor test counts the faces of.
     */
    private fun makeBlocks(): IntArray {
        val blocks = IntArray(GRID * GRID * GRID)
        var seed = 987654321
        for (x in 0 until GRID) {
            for (z in 0 until GRID) {
                val height = 8 + ((x * 5) xor (z * 3)) % 9
                for (y in 0 until GRID) {
                    seed = seed * 1103515245 + 12345
                    val hole = (seed ushr 24) % 41 == 0
                    blocks[(x * GRID + y) * GRID + z] = if (y < height && !hole) 1 + (seed ushr 16 and 3) else 0
                }
            }
        }
        return blocks
    }

    /** One pass over the region: counts the faces that show (a block with air beside it) */
    private fun scanRegion(blocks: IntArray, seed: Int): Int {
        var faces = 0
        val n = GRID
        val area = n * n
        for (x in 1 until n - 1) {
            for (y in 1 until n - 1) {
                var i = x * area + y * n + 1
                for (z in 1 until n - 1) {
                    if (blocks[i] != 0) {
                        if (blocks[i - 1] == 0) faces++
                        if (blocks[i + 1] == 0) faces++
                        if (blocks[i - n] == 0) faces++
                        if (blocks[i + n] == 0) faces++
                        if (blocks[i - area] == 0) faces++
                        if (blocks[i + area] == 0) faces++
                    }
                    i++
                }
            }
        }
        return faces + seed
    }

    /** Threads that repeat the "frame of work" until [stop]; every finished frame adds one to [units] */
    private fun startCpuThreads(
        count: Int,
        stop: AtomicBoolean,
        units: AtomicLong,
        blocks: IntArray
    ): List<Thread> = (0 until count).map { index ->
        Thread {
            var sink = index
            while (!stop.get()) {
                repeat(PASSES_PER_FRAME) { sink = scanRegion(blocks, sink) }
                units.incrementAndGet()
            }
            if (sink == Int.MIN_VALUE) Logger.info(TAG, "") //keeps the result in use
        }.also {
            it.name = "TokitoBench-cpu-$index"
            it.isDaemon = true
            it.start()
        }
    }

    // ---------------------------------------------------------------- graphics chip

    /** A thread that draws the heavy shader on an off-screen surface until [stop]; every frame adds one to [frames] */
    private fun startGpuThread(stop: AtomicBoolean, frames: AtomicLong): Thread = Thread {
        val gpu = GpuBench()
        try {
            if (gpu.create()) {
                var frame = 0
                while (!stop.get()) {
                    if (!gpu.draw(frame++)) break
                    frames.incrementAndGet()
                }
            }
        } catch (e: Throwable) {
            Logger.warning(TAG, "The graphics test could not run", e)
        } finally {
            gpu.release()
        }
    }.also {
        it.name = "TokitoBench-gpu"
        it.isDaemon = true
        it.start()
    }

    /** The EGL surface and the shader of the graphics test. Everything must run on the same thread. */
    private class GpuBench {
        private var display = EGL14.EGL_NO_DISPLAY
        private var surface = EGL14.EGL_NO_SURFACE
        private var context = EGL14.EGL_NO_CONTEXT
        private var timeLocation = -1

        /** False when the graphics test cannot run on this phone (the processor alone decides then) */
        fun create(): Boolean {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return false
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return false

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, count, 0) || count[0] < 1) return false
            val config = configs[0] ?: return false

            surface = EGL14.eglCreatePbufferSurface(
                display, config,
                intArrayOf(EGL14.EGL_WIDTH, DeviceBenchmark.SIZE, EGL14.EGL_HEIGHT, DeviceBenchmark.SIZE, EGL14.EGL_NONE), 0
            )
            context = EGL14.eglCreateContext(
                display, config, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
            )
            if (surface == EGL14.EGL_NO_SURFACE || context == EGL14.EGL_NO_CONTEXT) return false
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) return false

            val program = buildProgram() ?: return false
            GLES20.glUseProgram(program)
            val position = GLES20.glGetAttribLocation(program, "p")
            timeLocation = GLES20.glGetUniformLocation(program, "t")
            val quad: FloatBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            quad.put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)).position(0)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, quad)
            GLES20.glViewport(0, 0, DeviceBenchmark.SIZE, DeviceBenchmark.SIZE)
            return true
        }

        /** Draws one frame and waits for it to finish. False when the phone refuses to draw. */
        fun draw(frame: Int): Boolean {
            GLES20.glUniform1f(timeLocation, frame * 0.01f)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glFinish()
            return GLES20.glGetError() == GLES20.GL_NO_ERROR
        }

        fun release() {
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

    private const val SIZE = 512

    /** Side of the region of blocks of the processor test */
    private const val GRID = 30

    /** Passes over the region that make one "frame of work" (a few milliseconds on a fast phone) */
    private const val PASSES_PER_FRAME = 150

    /** Frames of the graphics test that count as 1 FPS of the benchmark */
    private const val GPU_DIVISOR = 6.0

    private const val CPU_THREAD_SCALE = 1.0

    private const val WINDOW_MS = 200L
    private const val WARMUP_READINGS = 3
    private const val CPU_MS = 3000L
    private const val GPU_MS = 2600L
    private const val BOTH_MS = 4600L
}
