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

package com.movtery.zalithlauncher.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.tokito.TokitoNotice
import com.movtery.zalithlauncher.tokito.TokitoNotices
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/**
 * Tokito Launcher: tells the player that the launcher was updated and shows the announcements of the creators.
 *
 * - After an update, the "Novidades" window opens once (it does not open on a new install).
 * - Then the launcher looks for an announcement in the repository (at most every 6 hours, only when there is
 *   internet) and shows it once.
 *
 * Nothing opens while the entry animation is running or while another screen is on top of the home screen.
 *
 * @param isVisible whether the home screen is the screen that the player is looking at
 */
@Composable
fun TokitoNoticeHost(isVisible: Boolean) {
    val context = LocalContext.current
    var showWhatsNew by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<TokitoNotice?>(null) }

    LaunchedEffect(Unit) {
        //not over the entry animation
        TokitoEntry.finished.first { it }

        val currentVersion = BuildConfig.VERSION_CODE
        if (AllSettings.tokitoLastSeenVersion.getValue() < currentVersion) {
            if (isFreshInstall(context)) {
                //a new player did not update anything: there is nothing new to tell
                AllSettings.tokitoLastSeenVersion.save(currentVersion)
            } else {
                showWhatsNew = true
            }
        }

        val found = TokitoNotices.check() ?: return@LaunchedEffect
        if (found.id <= AllSettings.tokitoLastNoticeId.getValue()) return@LaunchedEffect
        if (found.versionCode in 1..currentVersion) {
            //it announces a version that is already installed
            AllSettings.tokitoLastNoticeId.save(found.id)
            return@LaunchedEffect
        }
        notice = found
    }

    if (!isVisible) return

    if (showWhatsNew) {
        TokitoWhatsNewDialog(
            onDismiss = {
                showWhatsNew = false
                AllSettings.tokitoLastSeenVersion.save(BuildConfig.VERSION_CODE)
            }
        )
    } else {
        notice?.let { found ->
            TokitoNoticeDialog(
                notice = found,
                onDismiss = {
                    AllSettings.tokitoLastNoticeId.save(found.id)
                    notice = null
                }
            )
        }
    }
}

/**
 * The news of this version. The second button shares an announcement text with other players.
 */
@Composable
fun TokitoWhatsNewDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    SimpleAlertDialog(
        title = stringResource(R.string.tokito_whatsnew_title, BuildConfig.VERSION_NAME.substringBefore("-")),
        text = {
            Text(
                text = stringResource(R.string.tokito_whatsnew_body),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmText = stringResource(R.string.tokito_whatsnew_close),
        dismissText = stringResource(R.string.tokito_whatsnew_share),
        onConfirm = onDismiss,
        onCancel = { shareTokitoAnnouncement(context) },
        onDismissRequest = onDismiss
    )
}

/** An announcement of the creators, with a button to open its link when it has one */
@Composable
private fun TokitoNoticeDialog(
    notice: TokitoNotice,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val versionName = notice.versionName
    val title = notice.title.ifBlank {
        if (versionName != null) {
            stringResource(R.string.tokito_notice_update_available, versionName)
        } else {
            stringResource(R.string.tokito_notice_default_title)
        }
    }
    val link = notice.url

    if (link != null) {
        SimpleAlertDialog(
            title = title,
            text = {
                Text(
                    text = notice.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmText = stringResource(R.string.tokito_notice_open),
            dismissText = stringResource(R.string.tokito_whatsnew_close),
            onConfirm = {
                runCatching { uriHandler.openUri(link) }
                onDismiss()
            },
            onCancel = onDismiss,
            onDismissRequest = onDismiss
        )
    } else {
        SimpleAlertDialog(
            title = title,
            text = {
                Text(
                    text = notice.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmText = stringResource(R.string.tokito_whatsnew_close),
            onDismiss = onDismiss
        )
    }
}

/**
 * Opens the share menu of the phone with a ready announcement text, to tell the other players
 * that the launcher was updated.
 */
fun shareTokitoAnnouncement(context: Context) {
    val text = context.getString(R.string.tokito_announce_text, BuildConfig.VERSION_NAME.substringBefore("-"))
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, context.getString(R.string.tokito_whatsnew_share_chooser))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        //no app can share text
    }
}

/** The launcher was installed now and never updated (the install time and the update time are the same) */
private fun isFreshInstall(context: Context): Boolean = try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    abs(info.lastUpdateTime - info.firstInstallTime) < 5_000L
} catch (_: Exception) {
    false
}
