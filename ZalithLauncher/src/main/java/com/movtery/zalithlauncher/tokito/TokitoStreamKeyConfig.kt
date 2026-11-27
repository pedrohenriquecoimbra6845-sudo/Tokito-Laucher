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

import com.movtery.zalithlauncher.R

/**
 * Tokito Launcher: every key the Stream panel can show.
 *
 * @param defaultLetter the letter of a physical keyboard that presses the key (the player can change it)
 * @param defaultOn whether the key is on until the player customizes the keys
 * @param inGame false: the key exists only in the panel of the launcher (the Live mode is chosen before the game opens)
 */
enum class StreamKeyId(
    val labelRes: Int,
    val iconRes: Int,
    val defaultLetter: Char,
    val defaultOn: Boolean,
    val inGame: Boolean = true
) {
    MIC(R.string.stream_key_mic, R.drawable.ic_stream_mic, 'M', true),
    SOUND(R.string.stream_key_sound, R.drawable.ic_stream_volume, 'S', true),
    BLACK(R.string.stream_key_black, R.drawable.ic_stream_screen, 'B', true),
    LIVE(R.string.stream_key_live, R.drawable.ic_stream_bolt, 'L', true, inGame = false),
    DISCORD(R.string.stream_key_discord, R.drawable.ic_chat_info, 'D', true),
    CHAT(R.string.stream_key_chat, R.drawable.ic_stream_chat, 'C', true),
    TWITCH(R.string.stream_twitch, R.drawable.ic_videogame_asset_outlined, 'T', false),
    YOUTUBE(R.string.stream_youtube, R.drawable.ic_play_arrow_filled, 'Y', false),
    TIKTOK(R.string.stream_tiktok, R.drawable.ic_video_settings, 'K', false)
}

/** One key the way the player set it: which letter presses it and whether it is shown */
data class StreamKeyConfig(
    val id: StreamKeyId,
    val letter: Char,
    val enabled: Boolean
)

/**
 * Tokito Launcher: reads and writes the customized keys of the Stream panel.
 *
 * The setting is a text like `MIC:M:1,SOUND:S:1,BLACK:B:1,...` (key, letter, 1 on / 0 off) in the order the keys are
 * shown. A text that is empty, old or edited by hand still gives a valid list: unknown keys are dropped, keys that
 * a newer version added come at the end with their defaults and a letter is never used twice.
 */
object StreamKeyPrefs {
    fun defaults(): List<StreamKeyConfig> =
        StreamKeyId.entries.map { StreamKeyConfig(it, it.defaultLetter, it.defaultOn) }

    fun parse(raw: String): List<StreamKeyConfig> {
        val result = ArrayList<StreamKeyConfig>()
        val seen = HashSet<StreamKeyId>()
        for (part in raw.split(',')) {
            val fields = part.trim().split(':')
            if (fields.size < 3) continue
            val id = StreamKeyId.entries.firstOrNull { it.name == fields[0] } ?: continue
            if (!seen.add(id)) continue
            val letter = fields[1].firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: id.defaultLetter
            result += StreamKeyConfig(id, letter, fields[2] == "1")
        }
        for (id in StreamKeyId.entries) {
            if (seen.add(id)) result += StreamKeyConfig(id, id.defaultLetter, id.defaultOn)
        }
        return uniqueLetters(result)
    }

    fun encode(keys: List<StreamKeyConfig>): String =
        keys.joinToString(",") { "${it.id.name}:${it.letter}:${if (it.enabled) 1 else 0}" }

    /** Gives [letter] to the key [id]. When another key had that letter the two swap, so no letter is used twice */
    fun withLetter(keys: List<StreamKeyConfig>, id: StreamKeyId, letter: Char): List<StreamKeyConfig> {
        val mine = keys.firstOrNull { it.id == id } ?: return keys
        val other = keys.firstOrNull { it.id != id && it.letter == letter }
        return keys.map {
            when {
                it.id == id -> it.copy(letter = letter)
                other != null && it.id == other.id -> it.copy(letter = mine.letter)
                else -> it
            }
        }
    }

    /**
     * Moves the key [id] up ([delta] = -1) or down ([delta] = 1) by swapping it with the next key that is [visible]
     * in that direction (keys that are not shown stay where they are).
     */
    fun move(
        keys: List<StreamKeyConfig>,
        id: StreamKeyId,
        delta: Int,
        visible: (StreamKeyConfig) -> Boolean = { true }
    ): List<StreamKeyConfig> {
        val from = keys.indexOfFirst { it.id == id }
        if (from < 0) return keys
        var to = from + delta
        while (to in keys.indices && !visible(keys[to])) to += delta
        if (to !in keys.indices) return keys
        val list = keys.toMutableList()
        list[from] = keys[to]
        list[to] = keys[from]
        return list
    }

    private fun uniqueLetters(keys: List<StreamKeyConfig>): List<StreamKeyConfig> {
        val used = HashSet<Char>()
        return keys.map { key ->
            if (used.add(key.letter)) {
                key
            } else {
                val free = (listOf(key.id.defaultLetter) + ('A'..'Z')).first { it !in used }
                used += free
                key.copy(letter = free)
            }
        }
    }
}
