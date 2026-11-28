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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings

/**
 * Tokito Launcher: the window "Editar menus" of the strip at the top of the home screen.
 *
 * There is one tab for each category of the strip (notices and updates, library, game and looks, live and friends,
 * account and more). Under a tab every thing of the category is a card with its icon, its name and what it does, the
 * arrows to move it up or down inside the category and a switch to show or hide it; the two buttons above the cards
 * show or hide the whole category. At the bottom there is a small preview of the strip with the icons in the order
 * that it will have, and the button that restores the default.
 */
@Composable
fun TokitoMainCustomizeDialog(onDismiss: () -> Unit) {
    val saved = AllSettings.tokitoMainItems.state
    val entries = remember(saved) { HomeMainItems.resolve(saved) }
    val save: (List<HomeMainEntry>) -> Unit = { changed ->
        AllSettings.tokitoMainItems.save(HomeMainItems.encode(changed))
    }
    val groups = HomeMainGroup.entries
    var tab by remember { mutableIntStateOf(0) }
    val group = groups[tab.coerceIn(0, groups.lastIndex)]
    val inGroup = HomeMainItems.inGroup(entries, group)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                //title and close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(R.drawable.ic_edit_outlined),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.tokito_main_customize_title),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(R.string.tokito_main_customize_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.generic_close)
                        )
                    }
                }

                //the tabs: one for each category, dragged sideways
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEachIndexed { index, entry ->
                        val all = HomeMainItems.inGroup(entries, entry)
                        EditTab(
                            group = entry,
                            count = "${all.count { it.visible }}/${all.size}",
                            selected = index == tab,
                            onClick = { tab = index }
                        )
                    }
                }

                //what the category is and the buttons for all of it
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(group.descRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (inGroup.isNotEmpty()) {
                        TextButton(onClick = { save(HomeMainItems.setGroupVisible(entries, group, true)) }) {
                            Text(text = stringResource(R.string.home_edit_show_all))
                        }
                        TextButton(onClick = { save(HomeMainItems.setGroupVisible(entries, group, false)) }) {
                            Text(text = stringResource(R.string.home_edit_hide_all))
                        }
                    }
                }

                //the things of the category
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = inGroup, key = { it.item.id }) { entry ->
                        val position = inGroup.indexOf(entry)
                        EditItemCard(
                            entry = entry,
                            canMoveUp = position > 0,
                            canMoveDown = position < inGroup.lastIndex,
                            onToggle = { checked -> save(HomeMainItems.setVisible(entries, entry.item, checked)) },
                            onMoveUp = { save(HomeMainItems.move(entries, entry.item, -1)) },
                            onMoveDown = { save(HomeMainItems.move(entries, entry.item, 1)) }
                        )
                    }
                    //the two things that are always in the strip
                    if (group == HomeMainGroup.MORE) {
                        item(key = "fixed_about") {
                            FixedItemCard(
                                iconRes = R.drawable.ic_info_filled,
                                labelRes = R.string.home_about,
                                descRes = R.string.home_edit_about_desc
                            )
                        }
                        item(key = "fixed_edit") {
                            FixedItemCard(
                                iconRes = R.drawable.ic_edit_outlined,
                                labelRes = R.string.tokito_main_customize,
                                descRes = R.string.home_edit_edit_desc
                            )
                        }
                    }
                }

                //the preview of the strip, the default and done
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EditPreview(entries = entries, modifier = Modifier.weight(1f))
                    TextButton(onClick = { AllSettings.tokitoMainItems.reset() }) {
                        Text(text = stringResource(R.string.tokito_main_customize_reset))
                    }
                    Button(onClick = onDismiss) {
                        Text(text = stringResource(R.string.generic_done))
                    }
                }
            }
        }
    }
}

/** The tab of a category: its icon, its name and how many of its things are shown (for example 2/3) */
@Composable
private fun EditTab(
    group: HomeMainGroup,
    count: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(group.iconRes),
                contentDescription = null,
                tint = tint
            )
            Text(
                text = stringResource(group.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                maxLines = 1
            )
            Text(
                text = count,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

/** A thing of the strip: icon, name, what it does, the arrows and the switch */
@Composable
private fun EditItemCard(
    entry: HomeMainEntry,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val item = entry.item
    val on = entry.visible
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (on) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconBadge(iconRes = item.iconRes, on = on)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(item.descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                modifier = Modifier.size(36.dp),
                enabled = canMoveUp,
                onClick = onMoveUp
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_arrow_upward),
                    contentDescription = stringResource(R.string.stream_keys_move_up)
                )
            }
            IconButton(
                modifier = Modifier.size(36.dp),
                enabled = canMoveDown,
                onClick = onMoveDown
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_arrow_downward),
                    contentDescription = stringResource(R.string.stream_keys_move_down)
                )
            }
            Switch(checked = on, onCheckedChange = onToggle)
        }
    }
}

/** A thing that cannot be hidden (the notice of the license and the button of this window) */
@Composable
private fun FixedItemCard(iconRes: Int, labelRes: Int, descRes: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconBadge(iconRes = iconRes, on = false)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stringResource(R.string.home_edit_always),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** The icon of a thing, in a rounded square that is colored when the thing is shown */
@Composable
private fun IconBadge(iconRes: Int, on: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (on) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The strip in small: the icons of what is shown, in order, with a divider between the categories */
@Composable
private fun EditPreview(entries: List<HomeMainEntry>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.home_edit_preview),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        var first = true
        HomeMainGroup.entries.forEach { group ->
            val shown = entries.filter { it.item.group == group && it.visible }
            if (shown.isEmpty()) return@forEach
            if (!first) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(1.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            first = false
            shown.forEach { entry ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(17.dp),
                        painter = painterResource(entry.item.iconRes),
                        contentDescription = stringResource(entry.item.labelRes),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
