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

package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.TokitoInstalledPack
import com.movtery.zalithlauncher.tokito.TokitoNotice
import com.movtery.zalithlauncher.tokito.TokitoNotices
import com.movtery.zalithlauncher.tokito.TokitoPackInfo
import com.movtery.zalithlauncher.tokito.TokitoUpdates
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Tokito Launcher: the "Avisos" window of the home screen. It shows the newest announcement of the creators
 * (it is read again when the window opens, so the player can open it any time to see it) and a button to the
 * news of this version. Opening it marks the announcement as read.
 */
@Composable
fun TokitoNoticesDialog(
    onOpenWhatsNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var loading by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf<TokitoNotice?>(TokitoNotices.latest.value) }

    LaunchedEffect(Unit) {
        if (notice == null) {
            notice = TokitoNotices.check(force = true)
        }
        loading = false
        notice?.let { AllSettings.tokitoLastNoticeId.save(it.id) }
    }

    SimpleAlertDialog(
        title = stringResource(R.string.home_notices),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val current = notice
                when {
                    current != null -> {
                        val versionName = current.versionName
                        val title = current.title.ifBlank {
                            if (versionName != null) {
                                stringResource(R.string.tokito_notice_update_available, versionName)
                            } else {
                                stringResource(R.string.tokito_notice_default_title)
                            }
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        current.url?.let { link ->
                            IconTextButton(
                                onClick = { runCatching { uriHandler.openUri(link) } },
                                painter = painterResource(R.drawable.ic_link),
                                text = stringResource(R.string.tokito_notice_open)
                            )
                        }
                    }

                    loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                    else -> Text(
                        text = stringResource(R.string.home_notices_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconTextButton(
                    onClick = onOpenWhatsNew,
                    painter = painterResource(R.drawable.ic_info_outlined),
                    text = stringResource(R.string.tokito_main_item_news)
                )
            }
        },
        onDismiss = onDismiss
    )
}

/**
 * Tokito Launcher: the "Modpacks atualizados" window. The first list has the installed versions that were
 * recognized as a modpack of Modrinth, with a "possible update" mark when the modpack published a version after
 * the player installed it; the second one has the modpacks that were changed most recently. Tapping one opens its
 * page, where it can be installed again (updated).
 */
@Composable
fun TokitoUpdatesDialog(
    installed: List<TokitoInstalledPack>,
    onOpenPack: (TokitoPackInfo) -> Unit,
    onOpenModpacks: () -> Unit,
    onDismiss: () -> Unit
) {
    val state by TokitoUpdates.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT) }

    //the result of the last hour is reused: this only does work when it is old
    LaunchedEffect(Unit) { TokitoUpdates.refresh(installed) }

    SimpleAlertDialog(
        title = stringResource(R.string.home_modpacks_updated),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.home_updates_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.checking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.home_updates_checking),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (state.failed) {
                    Text(
                        text = stringResource(R.string.home_updates_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = stringResource(R.string.home_updates_mine),
                    style = MaterialTheme.typography.titleSmall
                )
                if (state.mine.isEmpty() && !state.checking) {
                    Text(
                        text = stringResource(R.string.home_updates_mine_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.mine.forEach { match ->
                    val date = dateFormat.format(Date(match.pack.updatedMs))
                    val subtitle = if (match.hasUpdate) {
                        val newest = match.pack.latestVersionName
                        if (newest != null) {
                            stringResource(R.string.home_updates_possible_named, newest, date)
                        } else {
                            stringResource(R.string.home_updates_possible, date)
                        }
                    } else {
                        stringResource(R.string.home_updates_ok)
                    }
                    PackRow(
                        iconUrl = match.pack.iconUrl,
                        title = match.installedName,
                        subtitle = subtitle,
                        highlight = match.hasUpdate,
                        onClick = { onOpenPack(match.pack) }
                    )
                }

                Text(
                    text = stringResource(R.string.home_updates_recent),
                    style = MaterialTheme.typography.titleSmall
                )
                state.recent.forEach { pack ->
                    val date = dateFormat.format(Date(pack.updatedMs))
                    val subtitle = listOfNotNull(
                        pack.author.takeIf { it.isNotBlank() },
                        pack.gameVersion,
                        date
                    ).joinToString(" · ")
                    PackRow(
                        iconUrl = pack.iconUrl,
                        title = pack.title,
                        subtitle = subtitle,
                        highlight = false,
                        onClick = { onOpenPack(pack) }
                    )
                }

                Text(
                    text = stringResource(R.string.home_updates_mods_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconTextButton(
                        onClick = { scope.launch { TokitoUpdates.refresh(installed, force = true) } },
                        painter = painterResource(R.drawable.ic_update),
                        text = stringResource(R.string.home_updates_refresh),
                        enabled = !state.checking
                    )
                    IconTextButton(
                        onClick = onOpenModpacks,
                        painter = painterResource(R.drawable.ic_package_2_outlined),
                        text = stringResource(R.string.tokito_rail_modpacks)
                    )
                }
            }
        },
        onDismiss = onDismiss
    )
}

/** A modpack in the list: its icon, its name, a second line and an arrow */
@Composable
private fun PackRow(
    iconUrl: String?,
    title: String,
    subtitle: String,
    highlight: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            model = iconUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.ic_download_2_outlined),
            contentDescription = null,
            tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
