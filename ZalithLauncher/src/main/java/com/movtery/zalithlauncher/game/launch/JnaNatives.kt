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

import com.movtery.zalithlauncher.path.LibPath
import java.io.File

/**
 * Tokito Launcher: picks the folder of the bundled JNA native library (`libjnidispatch.so`) for a game.
 *
 * The launcher keeps one folder for each JNA version (`jna/5.13.0`, `jna/5.16.0`...). The game used to get
 * a folder only when its JNA version matched a bundled one exactly, so any other version (an older modpack,
 * a loader that brings its own JNA, an old Minecraft that has no JNA at all) was left without natives
 * and JNA then tried to unpack a PC library that cannot run on Android.
 *
 * JNA accepts a native library that has the same major version and an equal or newer minor version than
 * the Java side, so a newer bundled folder is a safe replacement for a missing one.
 */
object JnaNatives {
    private const val LIBRARY_FILE = "libjnidispatch.so"

    /**
     * @param dir folder to give to `jna.boot.library.path`
     * @param version name of that folder (the JNA version of the natives)
     * @param exact false when the natives come from another version than the one asked for
     */
    data class Choice(
        val dir: File,
        val version: String,
        val exact: Boolean
    )

    /**
     * @param requestedVersion JNA version listed by the game (null when the game lists none)
     * @param root folder that holds one folder for each bundled version
     * @return the best folder, or null when no usable natives are installed
     */
    fun choose(requestedVersion: String?, root: File = LibPath.JNA): Choice? {
        val installed = root.listFiles()
            ?.filter { it.isDirectory && File(it, LIBRARY_FILE).isFile }
            ?.map { dir -> Candidate(dir, parseVersion(dir.name)) }
            ?.sortedWith(compareBy({ it.parts[0] }, { it.parts[1] }, { it.parts[2] }))
            .orEmpty()
        if (installed.isEmpty()) return null

        if (requestedVersion != null) {
            installed.firstOrNull { it.dir.name == requestedVersion }?.let {
                return Choice(it.dir, it.dir.name, exact = true)
            }

            //the closest bundled version that is compatible with the one asked for
            val wanted = parseVersion(requestedVersion)
            installed.firstOrNull { it.parts[0] == wanted[0] && it.parts[1] >= wanted[1] }?.let {
                return Choice(it.dir, it.dir.name, exact = false)
            }
        }

        //nothing compatible (or the game lists no JNA): the newest natives are the best guess
        return installed.last().let { Choice(it.dir, it.dir.name, exact = false) }
    }

    private class Candidate(val dir: File, val parts: IntArray)

    /** "5.13.0" becomes [5, 13, 0]. Missing or odd pieces (like "5.13.0-SNAPSHOT") count as numbers when they can. */
    private fun parseVersion(version: String): IntArray {
        val pieces = version.split('.')
        return IntArray(3) { index ->
            pieces.getOrNull(index)
                ?.takeWhile { it.isDigit() }
                ?.toIntOrNull()
                ?: 0
        }
    }
}
