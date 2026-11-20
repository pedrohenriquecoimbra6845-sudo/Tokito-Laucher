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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.tokito.TokiAuthPasswords

/**
 * Tokito Launcher: asks for the password of a TokiAuth account before the game opens.
 */
@Composable
fun TokiAuthUnlockDialog(
    userName: String,
    onUnlocked: () -> Unit,
    onCancel: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    fun tryUnlock() {
        if (TokiAuthPasswords.unlock(userName, password)) {
            onUnlocked()
        } else {
            wrong = true
        }
    }

    SimpleAlertDialog(
        title = stringResource(R.string.tokiauth_unlock_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.tokiauth_unlock_text, userName),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = password,
                    onValueChange = {
                        password = it.take(TokiAuthPasswords.MAX_LENGTH)
                        wrong = false
                    },
                    singleLine = true,
                    isError = wrong,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    label = { Text(text = stringResource(R.string.tokiauth_password_label)) },
                    supportingText = {
                        if (wrong) Text(text = stringResource(R.string.tokiauth_unlock_wrong))
                    }
                )
            }
        },
        confirmText = stringResource(R.string.tokiauth_unlock_button),
        onConfirm = { tryUnlock() },
        onCancel = onCancel,
        onDismissRequest = onCancel
    )
}

/**
 * Tokito Launcher: sets, changes or removes the password of the TokiAuth account that is in use
 * (also for the accounts that were created before the passwords existed).
 */
@Composable
fun TokiAuthPasswordDialog(
    userName: String,
    onDismiss: () -> Unit
) {
    val hasPassword = remember(userName) { TokiAuthPasswords.has(userName) }
    var current by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    SimpleAlertDialog(
        title = stringResource(R.string.tokiauth_set_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.tokiauth_set_text, userName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasPassword) {
                    PasswordField(
                        value = current,
                        onValueChange = { current = it; error = null },
                        label = stringResource(R.string.tokiauth_password_current)
                    )
                }
                PasswordField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = stringResource(R.string.tokiauth_password_label)
                )
                PasswordField(
                    value = confirm,
                    onValueChange = { confirm = it; error = null },
                    label = stringResource(R.string.tokiauth_password_confirm)
                )
                error?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (hasPassword) {
                    TextButton(
                        onClick = {
                            if (TokiAuthPasswords.unlock(userName, current)) {
                                TokiAuthPasswords.remove(userName)
                                onDismiss()
                            } else {
                                error = R.string.tokiauth_unlock_wrong
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.tokiauth_password_remove))
                    }
                }
            }
        },
        confirmText = stringResource(R.string.generic_confirm),
        onConfirm = {
            when {
                hasPassword && !TokiAuthPasswords.unlock(userName, current) -> error = R.string.tokiauth_unlock_wrong
                !TokiAuthPasswords.isValidPassword(password) -> error = R.string.tokiauth_password_short
                password != confirm -> error = R.string.tokiauth_password_mismatch
                TokiAuthPasswords.set(userName, password) -> onDismiss()
                else -> error = R.string.tokiauth_password_save_failed
            }
        },
        onCancel = onDismiss,
        onDismissRequest = onDismiss
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = { onValueChange(it.take(TokiAuthPasswords.MAX_LENGTH)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        label = { Text(text = label) }
    )
}
