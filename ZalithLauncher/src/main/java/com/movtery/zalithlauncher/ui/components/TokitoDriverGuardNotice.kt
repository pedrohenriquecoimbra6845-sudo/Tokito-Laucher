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

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.tokito.TokitoDriverGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Tokito Launcher: tells the player, once, that a graphics driver was set aside because the game could not open with it. */
@Composable
fun TokitoDriverGuardNotice() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val name = withContext(Dispatchers.IO) { TokitoDriverGuard.takeNotice() } ?: return@LaunchedEffect
        Toast.makeText(context, context.getString(R.string.tokito_driver_benched, name), Toast.LENGTH_LONG).show()
    }
}
