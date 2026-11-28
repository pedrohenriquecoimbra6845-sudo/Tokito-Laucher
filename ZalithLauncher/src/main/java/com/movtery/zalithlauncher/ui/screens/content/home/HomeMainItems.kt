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

package com.movtery.zalithlauncher.ui.screens.content.home

import com.movtery.zalithlauncher.R

/**
 * Tokito Launcher: the categories of the "Principais coisas" (main things) strip of the home screen. Every category
 * has its name above its tiles and a divider before it, and its own tab in the window "Editar menus". The order of
 * the categories is fixed; inside a category the player chooses what is shown and in which order.
 */
enum class HomeMainGroup(val labelRes: Int, val descRes: Int, val iconRes: Int) {
    UPDATES(R.string.home_cat_updates, R.string.home_catd_updates, R.drawable.ic_update),
    LIBRARY(R.string.home_cat_library, R.string.home_catd_library, R.drawable.ic_package_2_outlined),
    GAME(R.string.home_cat_game, R.string.home_catd_game, R.drawable.ic_videogame_asset_outlined),
    LIVE(R.string.home_cat_live, R.string.home_catd_live, R.drawable.ic_video_settings),
    MORE(R.string.home_cat_more, R.string.home_catd_more, R.drawable.ic_info_outlined)
}

/**
 * Tokito Launcher: the things that can be shown in the "Principais coisas" (main things) strip of the home screen.
 * The player chooses which ones are shown and in which order inside their category (see [HomeMainItems]). The order of
 * the entries here is the default order.
 *
 * The notice "Unofficial Modified Version" (About) that the license asks for and the button that opens the window to
 * edit the menus are not in this list: they are always shown.
 *
 * @param id name that is saved in the settings (never change it)
 */
enum class HomeMainItem(
    val id: String,
    val group: HomeMainGroup,
    val labelRes: Int,
    val descRes: Int,
    val iconRes: Int
) {
    NOTICES("notices", HomeMainGroup.UPDATES, R.string.home_notices, R.string.home_item_notices_desc, R.drawable.ic_chat_info),
    UPDATES("updates", HomeMainGroup.UPDATES, R.string.home_modpacks_updated, R.string.home_item_updates_desc, R.drawable.ic_update),
    LIBRARY("library", HomeMainGroup.LIBRARY, R.string.tokito_main_item_library, R.string.home_item_library_desc, R.drawable.ic_extension_outlined),
    MODES("modes", HomeMainGroup.GAME, R.string.tokito_main_item_modes, R.string.home_item_modes_desc, R.drawable.ic_videogame_asset_outlined),
    SKINS("skins", HomeMainGroup.GAME, R.string.tokito_main_item_skins, R.string.home_item_skins_desc, R.drawable.ic_checkroom),
    POPUPS("popups", HomeMainGroup.GAME, R.string.tokito_main_item_popups, R.string.home_item_wall_desc, R.drawable.ic_photo_library_outlined),
    NEWS("news", HomeMainGroup.GAME, R.string.tokito_main_item_news, R.string.home_item_news_desc, R.drawable.ic_info_outlined),
    STREAM("stream", HomeMainGroup.LIVE, R.string.home_stream, R.string.home_item_stream_desc, R.drawable.ic_video_settings),
    CHAT("chat", HomeMainGroup.LIVE, R.string.home_chat, R.string.home_item_chat_desc, R.drawable.ic_stream_chat),
    DISCORD("discord", HomeMainGroup.LIVE, R.string.home_discord, R.string.home_item_discord_desc, R.drawable.ic_link),
    CARDS("cards", HomeMainGroup.MORE, R.string.home_cards, R.string.home_item_cards_desc, R.drawable.ic_dashboard_outlined)
}

/** One line of the strip: the thing and whether it is shown */
data class HomeMainEntry(
    val item: HomeMainItem,
    val visible: Boolean
)

/**
 * Reads and writes the choice of the player.
 *
 * It is saved as a list of text: "id" for a visible item and "!id" for a hidden one, in the order of the strip.
 * An empty list means the default (every item, visible, in the order of [HomeMainItem]).
 *
 * The result of [resolve] always has the categories together and in their fixed order; the order that the player gave
 * is kept inside each category.
 */
object HomeMainItems {
    /**
     * The strip for the saved list: unknown names are ignored, the items that are not there (a new version can bring
     * new ones) come at the end of their category, and the categories are put in order.
     */
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
        //a stable sort: the order inside a category is the one of the list
        return entries.sortedBy { it.item.group.ordinal }
    }

    /** The text that is saved for the strip */
    fun encode(entries: List<HomeMainEntry>): List<String> =
        entries.map { entry -> if (entry.visible) entry.item.id else "!${entry.item.id}" }

    /** The entries of one category, in order */
    fun inGroup(entries: List<HomeMainEntry>, group: HomeMainGroup): List<HomeMainEntry> =
        entries.filter { it.item.group == group }

    /** The strip with [item] moved by [delta] places (-1 up, 1 down) among the entries of its own category */
    fun move(entries: List<HomeMainEntry>, item: HomeMainItem, delta: Int): List<HomeMainEntry> {
        val positions = entries.indices.filter { entries[it].item.group == item.group }
        val from = positions.indexOfFirst { entries[it].item == item }
        val to = from + delta
        if (from < 0 || to !in positions.indices) return entries
        val swapped = entries.toMutableList()
        val a = positions[from]
        val b = positions[to]
        val first = swapped[a]
        swapped[a] = swapped[b]
        swapped[b] = first
        return swapped
    }

    /** The strip with [item] shown or hidden */
    fun setVisible(entries: List<HomeMainEntry>, item: HomeMainItem, visible: Boolean): List<HomeMainEntry> =
        entries.map { if (it.item == item) it.copy(visible = visible) else it }

    /** The strip with every item of the category [group] shown or hidden */
    fun setGroupVisible(entries: List<HomeMainEntry>, group: HomeMainGroup, visible: Boolean): List<HomeMainEntry> =
        entries.map { if (it.item.group == group) it.copy(visible = visible) else it }
}
