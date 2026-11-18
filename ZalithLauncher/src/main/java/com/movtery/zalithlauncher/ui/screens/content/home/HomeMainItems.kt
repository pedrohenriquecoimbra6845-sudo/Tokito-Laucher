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

package com.movtery.zalithlauncher.ui.screens.content.home

import com.movtery.zalithlauncher.R

/**
 * Tokito Launcher: the things that can be shown in the "Principais coisas" (main things) panel of the home screen.
 * The player chooses which ones are shown and in which order (see [HomeMainItems]).
 *
 * The notice "Unofficial Modified Version" that the license asks for is not in this list: it is always shown.
 *
 * @param id name that is saved in the settings (never change it)
 */
enum class HomeMainItem(val id: String, val labelRes: Int) {
    LIBRARY("library", R.string.tokito_main_item_library),
    MODES("modes", R.string.tokito_main_item_modes),
    SKINS("skins", R.string.tokito_main_item_skins),
    POPUPS("popups", R.string.tokito_main_item_popups),
    NEWS("news", R.string.tokito_main_item_news)
}

/** One line of the panel: the thing and whether it is shown */
data class HomeMainEntry(
    val item: HomeMainItem,
    val visible: Boolean
)

/**
 * Reads and writes the choice of the player.
 *
 * It is saved as a list of text: "id" for a visible item and "!id" for a hidden one, in the order of the panel.
 * An empty list means the default (every item, visible, in the order of [HomeMainItem]).
 */
object HomeMainItems {
    /** The panel for the saved list: unknown names are ignored and the items that are not there come at the end */
    fun resolve(saved: List<String>): List<HomeMainEntry> {
        val entries = mutableListOf<HomeMainEntry>()
        saved.forEach { raw ->
            val hidden = raw.startsWith("!")
            val item = HomeMainItem.entries.firstOrNull { it.id == raw.removePrefix("!") } ?: return@forEach
            if (entries.none { it.item == item }) {
                entries += HomeMainEntry(item = item, visible = !hidden)
            }
        }
        HomeMainItem.entries.forEach { item ->
            if (entries.none { it.item == item }) {
                entries += HomeMainEntry(item = item, visible = true)
            }
        }
        return entries
    }

    /** The text that is saved for the panel */
    fun encode(entries: List<HomeMainEntry>): List<String> =
        entries.map { entry -> if (entry.visible) entry.item.id else "!${entry.item.id}" }

    /** The panel with the line [index] moved by [delta] places (-1 up, 1 down) */
    fun move(entries: List<HomeMainEntry>, index: Int, delta: Int): List<HomeMainEntry> {
        val target = index + delta
        if (index !in entries.indices || target !in entries.indices) return entries
        val moved = entries.toMutableList()
        val entry = moved.removeAt(index)
        moved.add(target, entry)
        return moved
    }

    /** The panel with the line [index] shown or hidden */
    fun setVisible(entries: List<HomeMainEntry>, index: Int, visible: Boolean): List<HomeMainEntry> {
        if (index !in entries.indices) return entries
        val changed = entries.toMutableList()
        changed[index] = changed[index].copy(visible = visible)
        return changed
    }
}
