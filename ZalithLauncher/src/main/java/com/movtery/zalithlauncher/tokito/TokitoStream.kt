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

package com.movtery.zalithlauncher.tokito

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.OptimizationMode

/**
 * Tokito Launcher: the platforms of the stream panel, with the settings that work well for a phone that plays
 * Minecraft and streams at the same time. The numbers are suggestions (the platforms change their rules), the
 * panel says so.
 *
 * @param serverUrl the address of the server that receives the video (null: the app shows it when the live starts)
 * @param packages the apps of the platform, in the order they are tried
 * @param webUrl where the player gets the key (or the page of the platform)
 */
enum class StreamPlatform(
    val labelRes: Int,
    val bestRes: Int,
    val keyRes: Int,
    val serverUrl: String?,
    val packages: List<String>,
    val webUrl: String
) {
    TWITCH(
        labelRes = R.string.stream_twitch,
        bestRes = R.string.stream_twitch_best,
        keyRes = R.string.stream_twitch_key,
        serverUrl = "rtmp://live.twitch.tv/app",
        packages = listOf("tv.twitch.android.app"),
        webUrl = "https://dashboard.twitch.tv/"
    ),
    YOUTUBE(
        labelRes = R.string.stream_youtube,
        bestRes = R.string.stream_youtube_best,
        keyRes = R.string.stream_youtube_key,
        serverUrl = "rtmp://a.rtmp.youtube.com/live2",
        packages = listOf("com.google.android.youtube"),
        webUrl = "https://studio.youtube.com/"
    ),
    TIKTOK(
        labelRes = R.string.stream_tiktok,
        bestRes = R.string.stream_tiktok_best,
        keyRes = R.string.stream_tiktok_key,
        serverUrl = null,
        packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        webUrl = "https://www.tiktok.com/live"
    )
}

object TokitoStream {
    /** The app of Discord */
    val DISCORD_PACKAGES = listOf("com.discord")

    /**
     * Opens the first of the apps that is installed.
     * @return false when none of them is installed (or it could not be opened)
     */
    fun openApp(context: Context, packages: List<String>): Boolean {
        for (name in packages) {
            val intent = context.packageManager.getLaunchIntentForPackage(name) ?: continue
            return try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    /** Copies a text to the clipboard of the phone */
    fun copy(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Tokito", text))
    }

    /** Opens the app of Discord. Without the app, the invite page opens in the browser of the phone */
    fun openDiscord(context: Context) {
        if (openApp(context, DISCORD_PACKAGES)) return
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(TokitoLinks.DISCORD)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            //no browser either: nothing else to do
        }
    }

    private fun audio(context: Context): AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /** The microphone of the phone is muted (for every app, not only for the launcher) */
    fun isMicMuted(context: Context): Boolean = try {
        audio(context)?.isMicrophoneMute ?: false
    } catch (_: Exception) {
        false
    }

    /** Mutes or unmutes the microphone of the phone. Some phones ignore it: [isMicMuted] tells what really happened */
    fun setMicMuted(context: Context, muted: Boolean) {
        try {
            audio(context)?.isMicrophoneMute = muted
        } catch (_: Exception) {
            //not allowed on this phone
        }
    }

    /** The media volume (the sound of the game and of the music) is muted */
    fun isSoundMuted(context: Context): Boolean = try {
        audio(context)?.isStreamMute(AudioManager.STREAM_MUSIC) ?: false
    } catch (_: Exception) {
        false
    }

    fun setSoundMuted(context: Context, muted: Boolean) {
        try {
            audio(context)?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (muted) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
                0
            )
        } catch (_: Exception) {
            //not allowed right now (for example, Do Not Disturb)
        }
    }

    /** Turns the Live mode on (the mode that was on is saved) or off (the saved mode comes back) */
    fun setLive(on: Boolean) {
        val mode = AllSettings.launcherOptimizationMode.getValue()
        if (on) {
            if (mode != OptimizationMode.LIVE) AllSettings.tokitoStreamPrevMode.save(mode)
            AllSettings.launcherOptimizationMode.save(OptimizationMode.LIVE)
        } else {
            val before = AllSettings.tokitoStreamPrevMode.getValue()
            AllSettings.launcherOptimizationMode.save(if (before == OptimizationMode.LIVE) OptimizationMode.PVP else before)
        }
    }
}
