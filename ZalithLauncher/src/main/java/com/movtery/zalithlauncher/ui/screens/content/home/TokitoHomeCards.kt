/*
 * Tokito Laucher - unofficial modified version of Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors (Zalith Launcher 2)
 * Modifications Copyright (C) 2026 Tokito Laucher contributors
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

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.components.BackgroundCard

/** Categories reachable from the Tokito library card on the home screen */
enum class HomeLibraryCategory {
    MODS,
    RESOURCE_PACKS,
    MODPACKS,
    SHADERS,
    WORLDS
}

/** Opens the matching download category (provided by the launcher screen) */
val LocalHomeLibraryOpener = staticCompositionLocalOf<(HomeLibraryCategory) -> Unit> { {} }

/** Opens a link (provided by the launcher screen) */
val LocalHomeLinkOpener = staticCompositionLocalOf<(String) -> Unit> { {} }

/**
 * "Minecraft Rust Edition": a native port of Minecraft: Java Edition written in Rust
 * (Starling, formerly Horizon Android, MIT license, https://github.com/Vasysik/horizon-android).
 *
 * It is a separate Android app, so Tokito Laucher cannot embed it: it detects the installed app,
 * opens it, and links to its release page when it is not installed.
 */
object RustEdition {
    /** Release page of the app */
    const val RELEASES_URL = "https://github.com/JuanLuci/starling/releases/latest"

    /** Package ids used by the app (stable, preview, dev and the legacy Horizon Android id) */
    private val PACKAGES = listOf(
        "io.github.juanluci.starling",
        "io.github.juanluci.starling.preview",
        "io.github.juanluci.starling.dev",
        "dev.horizon.android"
    )

    /** Intent that opens the installed app, or null when it is not installed */
    fun launchIntent(context: Context): Intent? {
        val manager = context.packageManager
        for (packageName in PACKAGES) {
            val intent = runCatching { manager.getLaunchIntentForPackage(packageName) }.getOrNull()
            if (intent != null) return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return null
    }

    /** Opens the app. Returns false when it is not installed (or could not be opened) */
    fun open(context: Context): Boolean {
        val intent = launchIntent(context) ?: return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}

/**
 * Shortcuts to mods, textures (resource packs), modpacks, shaders and worlds.
 */
@Composable
fun TokitoLibraryCard() {
    val openCategory = LocalHomeLibraryOpener.current
    BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = stringResource(R.string.tokito_library_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LibraryTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_extension_outlined,
                    labelRes = R.string.tokito_rail_mods,
                    onClick = { openCategory(HomeLibraryCategory.MODS) }
                )
                LibraryTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_format_paint_outlined,
                    labelRes = R.string.tokito_rail_textures,
                    onClick = { openCategory(HomeLibraryCategory.RESOURCE_PACKS) }
                )
                LibraryTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_package_2_outlined,
                    labelRes = R.string.tokito_rail_modpacks,
                    onClick = { openCategory(HomeLibraryCategory.MODPACKS) }
                )
                LibraryTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_lightbulb,
                    labelRes = R.string.tokito_lib_shaders,
                    onClick = { openCategory(HomeLibraryCategory.SHADERS) }
                )
                LibraryTile(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_public,
                    labelRes = R.string.tokito_lib_worlds,
                    onClick = { openCategory(HomeLibraryCategory.WORLDS) }
                )
            }
        }
    }
}

@Composable
private fun LibraryTile(
    iconRes: Int,
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * "Minecraft Rust Edition": opens the app if it is installed, otherwise links to its download page.
 */
@Composable
fun TokitoRustEditionCard() {
    val context = LocalContext.current
    val openLink = LocalHomeLinkOpener.current
    val installed = RustEdition.launchIntent(context) != null

    BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.tokito_rust_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.tokito_rust_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = { openLink(RustEdition.RELEASES_URL) }) {
                    Text(text = stringResource(R.string.tokito_rust_download))
                }
                if (installed) {
                    Button(onClick = { RustEdition.open(context) }) {
                        Text(text = stringResource(R.string.tokito_rust_open))
                    }
                }
            }
        }
    }
}
