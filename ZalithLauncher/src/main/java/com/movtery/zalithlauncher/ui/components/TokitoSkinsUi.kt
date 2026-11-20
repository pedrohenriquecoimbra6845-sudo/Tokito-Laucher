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

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.wardrobe.isSlimModel
import com.movtery.zalithlauncher.tokito.SkinImportResult
import com.movtery.zalithlauncher.tokito.SkinSearchResult
import com.movtery.zalithlauncher.tokito.TokitoSkinSearch
import com.movtery.zalithlauncher.tokito.TokitoSkins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.floor
import kotlin.math.roundToInt

/** A skin that was read, ready to be drawn */
private class SkinPreviewData(
    val image: ImageBitmap,
    val slim: Boolean
)

/**
 * Tokito Launcher: the front of a skin (64x64 PNG), drawn with sharp pixels.
 * It shows the body with the second layer (hat, jacket, sleeves and pants). Nothing is drawn when the file
 * is missing or is not a 64x64 skin.
 *
 * @param refreshKey change it to read the file again
 */
@Composable
fun SkinFrontPreview(
    file: File,
    modifier: Modifier = Modifier,
    refreshKey: Any? = null
) {
    val data by produceState<SkinPreviewData?>(initialValue = null, file, refreshKey) {
        value = withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap == null || bitmap.width != 64 || bitmap.height != 64) {
                    null
                } else {
                    SkinPreviewData(image = bitmap.asImageBitmap(), slim = file.isSlimModel())
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Canvas(modifier = modifier) {
        data?.let { skin -> drawSkinFront(skin.image, skin.slim) }
    }
}

/**
 * The figure is 16 x 32 skin pixels (arm, body, arm and head, body, legs), made as big as the space allows.
 * The thin (Alex) model has arms of 3 pixels instead of 4.
 */
private fun DrawScope.drawSkinFront(image: ImageBitmap, slim: Boolean) {
    val unit = floor(minOf(size.width / 16f, size.height / 32f)).toInt().coerceAtLeast(1)
    val left = ((size.width - 16 * unit) / 2f).roundToInt()
    val top = ((size.height - 32 * unit) / 2f).roundToInt()
    val armWidth = if (slim) 3 else 4

    //source position and size in the skin, then the place in the figure (in skin pixels)
    fun part(sx: Int, sy: Int, sw: Int, sh: Int, dx: Int, dy: Int) {
        drawImage(
            image = image,
            srcOffset = IntOffset(sx, sy),
            srcSize = IntSize(sw, sh),
            dstOffset = IntOffset(left + dx * unit, top + dy * unit),
            dstSize = IntSize(sw * unit, sh * unit),
            filterQuality = FilterQuality.None
        )
    }

    //first layer
    part(8, 8, 8, 8, 4, 0) //head
    part(20, 20, 8, 12, 4, 8) //body
    part(44, 20, armWidth, 12, 4 - armWidth, 8) //right arm (on the left of the picture)
    part(36, 52, armWidth, 12, 12, 8) //left arm
    part(4, 20, 4, 12, 4, 20) //right leg
    part(20, 52, 4, 12, 8, 20) //left leg

    //second layer, over the first one
    part(40, 8, 8, 8, 4, 0) //hat
    part(20, 36, 8, 12, 4, 8) //jacket
    part(44, 36, armWidth, 12, 4 - armWidth, 8) //right sleeve
    part(52, 52, armWidth, 12, 12, 8) //left sleeve
    part(4, 36, 4, 12, 4, 20) //right pants
    part(4, 52, 4, 12, 8, 20) //left pants
}

/**
 * Tokito Launcher: the window of the Skins category.
 *
 * Add skins (a PNG or a ZIP with PNG files, 64x64), keep the skin that the account uses now,
 * and put a skin on the Offline or TokiAuth account that is selected. Online accounts get their skin
 * from their own service, so they can not use these skins.
 */
@Composable
fun TokitoSkinsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    var skins by remember { mutableStateOf(TokitoSkins.list()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    //goes up every time a skin is put on the account, to draw the picture of the current skin again
    var appliedCount by remember { mutableStateOf(0) }
    //0 = my skins (library), 1 = search
    var category by remember { mutableStateOf(0) }
    var filter by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            busy = true
            message = null
            scope.launch {
                val result = TokitoSkins.import(context, uri)
                skins = TokitoSkins.list()
                message = importMessage(context, result)
                busy = false
            }
        }
    }

    val current = account
    val local = current?.isLocalAccount() == true

    SimpleAlertDialog(
        title = stringResource(R.string.tokito_skins_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.tokito_skins_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                //the account that will get the skin, with its current skin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (current != null && local) {
                        SkinFrontPreview(
                            modifier = Modifier.size(width = 36.dp, height = 72.dp),
                            file = current.getSkinFile(),
                            refreshKey = appliedCount
                        )
                    }
                    Text(
                        modifier = Modifier.weight(1f),
                        text = when {
                            current == null -> stringResource(R.string.tokito_skins_no_account)
                            local -> stringResource(R.string.tokito_skins_account, current.username)
                            else -> stringResource(R.string.tokito_skins_account_online)
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                //categories of the Skins section
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckChip(
                        selected = category == 0,
                        onClick = { category = 0 },
                        label = { Text(text = stringResource(R.string.tokito_skins_cat_mine)) }
                    )
                    CheckChip(
                        selected = category == 1,
                        onClick = { category = 1 },
                        label = { Text(text = stringResource(R.string.tokito_skins_cat_search)) }
                    )
                }

                if (category == 0) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconTextButton(
                            onClick = { picker.launch(arrayOf("*/*")) },
                            painter = painterResource(R.drawable.ic_add),
                            text = stringResource(R.string.tokito_skins_add),
                            enabled = !busy
                        )
                        IconTextButton(
                            onClick = {
                                val target = current ?: return@IconTextButton
                                busy = true
                                message = null
                                scope.launch {
                                    val saved = TokitoSkins.saveCurrent(target)
                                    skins = TokitoSkins.list()
                                    message = context.getString(
                                        if (saved) R.string.tokito_skins_saved else R.string.tokito_skins_save_failed
                                    )
                                    busy = false
                                }
                            },
                            painter = painterResource(R.drawable.ic_save_filled),
                            text = stringResource(R.string.tokito_skins_save_current),
                            enabled = local && !busy
                        )
                    }

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = filter,
                        onValueChange = { filter = it.take(40) },
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.tokito_skins_filter_hint)) }
                    )
                }

                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                message?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (category == 1) {
                    SkinSearchSection(
                        canUse = local && !busy,
                        onKeep = { skins = TokitoSkins.list() },
                        onUse = { file ->
                            val target = current ?: return@SkinSearchSection
                            busy = true
                            message = null
                            scope.launch {
                                val applied = TokitoSkins.apply(target, file)
                                if (applied) appliedCount++
                                message = context.getString(
                                    if (applied) R.string.tokito_skins_applied else R.string.tokito_skins_apply_failed
                                )
                                busy = false
                            }
                        }
                    )
                } else if (skins.isEmpty()) {
                    Text(
                        text = stringResource(R.string.tokito_skins_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        skins.filter { filter.isBlank() || TokitoSkins.nameOf(it).contains(filter.trim(), ignoreCase = true) }.forEach { file ->
                            SkinTile(
                                file = file,
                                canUse = local && !busy,
                                onUse = {
                                    val target = current ?: return@SkinTile
                                    busy = true
                                    message = null
                                    scope.launch {
                                        val applied = TokitoSkins.apply(target, file)
                                        if (applied) appliedCount++
                                        message = context.getString(
                                            if (applied) R.string.tokito_skins_applied else R.string.tokito_skins_apply_failed
                                        )
                                        busy = false
                                    }
                                },
                                onDelete = {
                                    TokitoSkins.delete(file)
                                    skins = TokitoSkins.list()
                                }
                            )
                        }
                    }
                }
            }
        },
        onDismiss = onDismiss
    )
}

/**
 * Search category: type the name of a Minecraft Java player, see the skin and keep it in the library
 * or use it on the current account.
 */
@Composable
private fun SkinSearchSection(
    canUse: Boolean,
    onKeep: () -> Unit,
    onUse: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<SkinSearchResult.Found?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    fun search() {
        if (searching) return
        searching = true
        status = null
        found = null
        scope.launch {
            when (val result = TokitoSkinSearch.search(query)) {
                is SkinSearchResult.Found -> found = result
                SkinSearchResult.NotFound -> status = context.getString(R.string.tokito_skins_search_notfound)
                SkinSearchResult.OldFormat -> status = context.getString(R.string.tokito_skins_search_old)
                SkinSearchResult.BadName -> status = context.getString(R.string.tokito_skins_search_badname)
                SkinSearchResult.Failed -> status = context.getString(R.string.tokito_skins_search_error)
            }
            searching = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.tokito_skins_search_source),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = query,
                onValueChange = { query = it.take(16) },
                singleLine = true,
                label = { Text(text = stringResource(R.string.tokito_skins_search_hint)) }
            )
            TextButton(
                onClick = { search() },
                enabled = !searching && query.isNotBlank()
            ) {
                Text(text = stringResource(R.string.tokito_skins_search_button))
            }
        }
        if (searching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        found?.let { result ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkinFrontPreview(
                    modifier = Modifier.size(width = 48.dp, height = 96.dp),
                    file = result.file
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tokito_skins_search_found, result.name),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val kept = TokitoSkins.addDownloaded(result.file, result.name)
                                    status = context.getString(
                                        if (kept != null) R.string.tokito_skins_saved else R.string.tokito_skins_save_failed
                                    )
                                    onKeep()
                                }
                            }
                        ) {
                            Text(text = stringResource(R.string.tokito_skins_keep))
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val kept = TokitoSkins.addDownloaded(result.file, result.name)
                                    onKeep()
                                    if (kept != null) {
                                        onUse(kept)
                                    } else {
                                        //the library is full: the skin is still put on the account from the temporary file
                                        onUse(result.file)
                                    }
                                }
                            },
                            enabled = canUse
                        ) {
                            Text(text = stringResource(R.string.tokito_skins_use))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinTile(
    file: File,
    canUse: Boolean,
    onUse: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SkinFrontPreview(
            modifier = Modifier.size(width = 48.dp, height = 96.dp),
            file = file
        )
        Text(
            text = TokitoSkins.nameOf(file),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TextButton(
                onClick = onUse,
                enabled = canUse
            ) {
                Text(text = stringResource(R.string.tokito_skins_use))
            }
            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = onDelete
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_delete_outlined),
                    contentDescription = stringResource(R.string.tokito_skins_delete)
                )
            }
        }
    }
}

/** The text that tells what happened when skins were added */
private fun importMessage(context: Context, result: SkinImportResult): String {
    val ignored = result.oldFormat + result.invalid + result.full
    return when {
        result.added > 0 && ignored > 0 ->
            context.getString(R.string.tokito_skins_added, result.added) + " " +
                    context.getString(R.string.tokito_skins_ignored, ignored)

        result.added > 0 -> context.getString(R.string.tokito_skins_added, result.added)
        else -> context.getString(R.string.tokito_skins_none_valid)
    }
}
