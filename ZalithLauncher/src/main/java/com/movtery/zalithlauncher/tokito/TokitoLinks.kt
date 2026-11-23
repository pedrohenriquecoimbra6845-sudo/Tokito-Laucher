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

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Tokito Launcher: links of the launcher and a way to open them without the confirmation dialog.
 */
object TokitoLinks {
    /** Discord of the Tokito community */
    const val DISCORD = "https://discord.gg/u9zxQQWpwn"

    /** Where the player gets a free CurseForge API key */
    const val CURSEFORGE_CONSOLE = "https://console.curseforge.com/"

    /**
     * Opens [url] in the browser (or in the app that handles it, like Discord).
     * @return false when nothing on the phone could open the link
     */
    fun open(context: Context, url: String): Boolean = try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: Throwable) {
        false
    }
}
