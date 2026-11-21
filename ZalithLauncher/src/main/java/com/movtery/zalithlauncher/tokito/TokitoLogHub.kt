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

package com.movtery.zalithlauncher.tokito

import com.movtery.zalithlauncher.bridge.LoggerBridge
import java.util.concurrent.ConcurrentHashMap

/**
 * Tokito Launcher: the native logger of the game takes only one listener at a time, and both the log window and
 * the loading screen want the lines of the game. This keeps them all: each one has a key, and the lines go to
 * every listener.
 */
object TokitoLogHub {
    private val listeners = ConcurrentHashMap<String, (String) -> Unit>()

    @Volatile
    private var installed = false

    /** Put the hub as the listener of the native logger (after `LoggerBridge.start`) */
    fun install() {
        if (installed) return
        installed = true
        LoggerBridge.setListener { line ->
            for (listener in listeners.values) {
                runCatching { listener(line) }
            }
        }
    }

    /** Sets the listener of [key] (the old one of the same key is replaced) */
    fun set(key: String, listener: (String) -> Unit) {
        listeners[key] = listener
        install()
    }

    fun remove(key: String) {
        listeners.remove(key)
    }
}
