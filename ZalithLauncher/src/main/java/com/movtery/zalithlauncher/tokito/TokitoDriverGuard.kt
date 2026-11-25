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

package com.movtery.zalithlauncher.tokito

import com.movtery.zalithlauncher.game.plugin.driver.Driver
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

/**
 * Tokito Launcher: protection against a custom graphics driver that keeps the game from opening.
 *
 * The idea comes from the driver manager of Horizon / Starling (MIT license, `gpu_drivers.rs`): a driver that
 * "repeated crashes set aside". Every start of the game with a custom driver (a driver plugin) is counted when it
 * begins and cleared when the game reaches its title screen ([noteFirstFrame]). After [MAX_FAILED_STARTS] starts in
 * a row that never got there, the driver is set aside: the built in driver (Turnip) is used instead and the player
 * is told once ([takeNotice]). The driver gets a new chance after [BENCH_DAYS] days, when its plugin is updated, or
 * when the player chooses it again after using another one.
 *
 * The state is in small files of the private folder, so the launcher process and the game process share it.
 */
object TokitoDriverGuard {
    private const val TAG = "TokitoDriverGuard"
    private const val STATE_FILE = "tokito_driver_guard.txt"
    private const val CURRENT_FILE = "tokito_driver_current.txt"
    private const val LAST_FILE = "tokito_driver_last.txt"
    private const val NOTICE_FILE = "tokito_driver_notice.txt"

    /** Starts in a row without reaching the title screen before a custom driver is set aside */
    const val MAX_FAILED_STARTS = 3

    private const val BENCH_DAYS = 7L
    private const val BUILT_IN_KEY = "built-in"

    private class Entry(var fails: Int, var benchedAt: Long)

    private fun dir(): File = PathManager.DIR_FILES_PRIVATE

    private fun load(): MutableMap<String, Entry> {
        val map = LinkedHashMap<String, Entry>()
        val file = File(dir(), STATE_FILE)
        if (!file.exists()) return map
        runCatching {
            file.readLines().forEach { line ->
                val parts = line.split('\t')
                if (parts.size == 3) {
                    map[parts[0]] = Entry(parts[1].toIntOrNull() ?: 0, parts[2].toLongOrNull() ?: 0L)
                }
            }
        }
        return map
    }

    private fun save(map: Map<String, Entry>) {
        runCatching {
            val file = File(dir(), STATE_FILE)
            val tmp = File(dir(), "$STATE_FILE.tmp")
            tmp.writeText(map.entries.joinToString("\n") { "${it.key}\t${it.value.fails}\t${it.value.benchedAt}" })
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }.onFailure {
            Logger.warning(TAG, "Could not save the driver guard", it)
        }
    }

    private fun readText(name: String): String? = runCatching {
        File(dir(), name).takeIf { it.exists() }?.readText()?.trim()?.ifEmpty { null }
    }.getOrNull()

    private fun writeText(name: String, text: String) {
        runCatching { File(dir(), name).writeText(text) }
    }

    /**
     * The game is about to start with [driver]. Returns the driver that must really be used: [driver] itself, or the
     * built in one when [driver] was set aside.
     */
    @Synchronized
    fun begin(driver: Driver): Driver {
        val builtIn = DriverPluginManager.getDriverList().first()
        if (driver.isLauncher) {
            writeText(LAST_FILE, BUILT_IN_KEY)
            File(dir(), CURRENT_FILE).delete()
            return driver
        }
        val key = driver.id + "@" + driver.appVersion
        val map = load()
        val entry = map.getOrPut(key) { Entry(0, 0L) }
        val now = System.currentTimeMillis()
        //the player used another driver and chose this one again: a new chance
        if (readText(LAST_FILE) != key && entry.benchedAt > 0L) {
            entry.fails = 0
            entry.benchedAt = 0L
        }
        writeText(LAST_FILE, key)
        if (entry.benchedAt > 0L) {
            if (now - entry.benchedAt < BENCH_DAYS * 24L * 3_600_000L) {
                save(map)
                File(dir(), CURRENT_FILE).delete()
                return builtIn
            }
            entry.fails = 0
            entry.benchedAt = 0L
        }
        if (entry.fails >= MAX_FAILED_STARTS) {
            entry.benchedAt = now
            save(map)
            writeText(NOTICE_FILE, driver.name)
            File(dir(), CURRENT_FILE).delete()
            Logger.warning(TAG, "Driver ${driver.name} set aside after ${entry.fails} starts that never opened the game")
            return builtIn
        }
        entry.fails++
        save(map)
        writeText(CURRENT_FILE, key)
        return driver
    }

    /** The game reached its title screen: the driver of this start works, so its count of failed starts is cleared. */
    @Synchronized
    fun noteFirstFrame() {
        val key = readText(CURRENT_FILE) ?: return
        File(dir(), CURRENT_FILE).delete()
        val map = load()
        val entry = map[key] ?: return
        if (entry.fails != 0 || entry.benchedAt != 0L) {
            entry.fails = 0
            entry.benchedAt = 0L
            save(map)
        }
    }

    /** Name of a driver that was set aside and the player does not know yet (read only once), or null */
    @Synchronized
    fun takeNotice(): String? {
        val name = readText(NOTICE_FILE) ?: return null
        File(dir(), NOTICE_FILE).delete()
        return name
    }
}
