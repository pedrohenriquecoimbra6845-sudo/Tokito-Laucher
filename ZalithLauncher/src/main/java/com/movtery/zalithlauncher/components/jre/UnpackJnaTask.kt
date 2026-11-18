/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
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

package com.movtery.zalithlauncher.components.jre

import android.content.Context
import com.movtery.zalithlauncher.components.AssetManifest
import com.movtery.zalithlauncher.components.ComponentFile
import com.movtery.zalithlauncher.components.UnpackSingleTask
import com.movtery.zalithlauncher.components.toComponentFile
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException

private const val TAG = "UnpackJnaTask"

class UnpackJnaTask(context: Context) : UnpackSingleTask(
    context = context,
    rootDir = PathManager.DIR_JNA,
    assetsDirName = "runtimes/jna",
    fileDirName = "jna"
) {
    /**
     * Tokito Launcher: a JNA version that has no natives for this processor is skipped instead of failing
     * the whole task. The game then uses the closest compatible version that was installed
     * (see `JnaNatives`). Only a launcher without any JNA natives for this processor fails.
     */
    override fun selectFiles(manifest: AssetManifest): List<ComponentFile> {
        val abi = Architecture.archAsStringAndroid(Architecture.getDeviceArchitecture())
        val files = manifest.files.mapNotNull { file ->
            val parts = file.path.split('/')
            require(parts.size == 4 && parts[1] == "natives" && parts[3] == "libjnidispatch.so") {
                "Unsupported JNA asset path: ${file.path}"
            }
            if (parts[2] != abi) return@mapNotNull null
            file.toComponentFile("${parts[0]}/libjnidispatch.so")
        }
        require(files.isNotEmpty()) {
            "JNA assets are incomplete for ABI: $abi"
        }
        return files
    }

    /**
     * Tokito Launcher: JNA is only needed by some games and mods, so a problem while installing it must not
     * keep the whole launcher stuck on the first start screen. The natives are checked again, and installed
     * again if needed, every time a game starts.
     */
    override suspend fun run() {
        try {
            super.run()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.warning(TAG, "Failed to install the JNA natives, they will be tried again when a game starts", e)
            updateMessage(null)
        }
    }
}
