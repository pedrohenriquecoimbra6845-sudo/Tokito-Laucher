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

import android.os.Build

/**
 * Tokito Launcher: what the FPS governor needs to know about the phone itself.
 *
 * - **Step of the FPS limit:** 10 FPS by default; 5 FPS on Xiaomi (Redmi and POCO too), Infinix and RedMagic,
 *   the brands whose screens and governors take the finer steps well.
 * - **Old phone:** a phone from 2020 or before. It is told by the Android version the phone was made with
 *   (Android 10 or older). Only these phones may go below 60 FPS (and then the player is warned that the phone
 *   is in danger). Newer phones are held at 60 FPS or more.
 */
object DeviceProfile {
    /** FPS limit moves in steps of this many FPS (5 or 10) */
    val fpsStep: Int by lazy { if (usesFineStep()) 5 else 10 }

    /** The phone is from 2020 or before (it was made with Android 10 or older) */
    val isOldPhone: Boolean by lazy { initialSdk() <= LAST_OLD_SDK }

    /** Lowest FPS limit that the governor may use on this phone */
    val floorFps: Int get() = if (isOldPhone) OLD_PHONE_FLOOR else NEW_PHONE_FLOOR

    /** Round to a multiple of the step of this phone */
    fun roundToStep(value: Int): Int = value / fpsStep * fpsStep

    private fun usesFineStep(): Boolean {
        val brand = listOf(Build.MANUFACTURER, Build.BRAND).joinToString(" ") { it.orEmpty() }.lowercase()
        val model = Build.MODEL.orEmpty().lowercase()
        return FINE_STEP_BRANDS.any { it in brand } || "redmagic" in model || "red magic" in model
    }

    /** The Android version (API level) that the phone was made with */
    private fun initialSdk(): Int {
        val fromProperty = readProperty("ro.product.first_api_level")?.toIntOrNull()
        return fromProperty?.takeIf { it > 0 } ?: Build.VERSION.SDK_INT
    }

    private fun readProperty(key: String): String? = try {
        val process = ProcessBuilder("getprop", key).redirectErrorStream(true).start()
        process.inputStream.bufferedReader().use { it.readLine()?.trim() }
    } catch (_: Throwable) {
        null
    }

    private const val LAST_OLD_SDK = 29
    private const val NEW_PHONE_FLOOR = 60
    private const val OLD_PHONE_FLOOR = 30
    private val FINE_STEP_BRANDS = listOf("xiaomi", "redmi", "poco", "infinix", "redmagic", "nubia")
}
