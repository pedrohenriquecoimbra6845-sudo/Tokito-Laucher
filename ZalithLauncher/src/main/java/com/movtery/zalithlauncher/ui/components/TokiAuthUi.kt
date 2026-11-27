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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.tokito.TokiAuthLicense
import com.movtery.zalithlauncher.tokito.TokiAuthPasswords
import com.movtery.zalithlauncher.tokito.TokitoLinks

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

/**
 * Tokito Launcher: buys the license of "TokiAuth Original" for the player [userName].
 *
 * The player pays on the shop of the Tokito (Base44). When the payment ends the shop sends the player back to the
 * launcher with the signed license in the link (`tokito://tokiauth/activate?license=...`), the signature is checked
 * on the device, the account becomes Original by itself and this dialog finishes ([onActivated]).
 * Pasting a key stays only as a fallback, for a phone whose browser did not open the link.
 */
@Composable
fun TokiAuthLicenseDialog(
    userName: String,
    onActivated: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var key by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    var showKey by remember { mutableStateOf(false) }
    var shopFailed by remember { mutableStateOf(false) }

    //The license arrives from the shop (or from the typed key): the dialog finishes by itself
    val changes by TokiAuthLicense.changes.collectAsStateWithLifecycle()
    LaunchedEffect(changes, userName) {
        if (TokiAuthLicense.isLicensed(userName)) onActivated()
    }

    fun activate() {
        val cleaned = TokiAuthLicense.clean(key)
        error = when {
            !TokiAuthLicense.verify(userName, cleaned) -> R.string.tokiauth_license_invalid
            !TokiAuthLicense.activate(userName, cleaned) -> R.string.tokiauth_license_save_failed
            else -> null
        }
    }

    SimpleAlertDialog(
        title = stringResource(R.string.tokiauth_license_title),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.tokiauth_license_text, TokiAuthLicense.PRICE_BRL),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.tokiauth_license_for, userName),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(R.string.tokiauth_license_buy_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (shopFailed) {
                    Text(
                        text = stringResource(R.string.tokiauth_license_shop_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(onClick = { showKey = !showKey }) {
                    Text(text = stringResource(R.string.tokiauth_license_have_key))
                }
                if (showKey) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = key,
                        onValueChange = {
                            key = it.take(700)
                            error = null
                        },
                        maxLines = 3,
                        isError = error != null,
                        label = { Text(text = stringResource(R.string.tokiauth_license_key_label)) },
                        placeholder = { Text(text = stringResource(R.string.tokiauth_license_key_hint)) },
                        supportingText = {
                            error?.let { Text(text = stringResource(it, userName)) }
                        }
                    )
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = key.isNotBlank(),
                        onClick = { activate() }
                    ) {
                        Text(text = stringResource(R.string.tokiauth_license_activate))
                    }
                }
            }
        },
        confirmText = stringResource(R.string.tokiauth_license_buy, TokiAuthLicense.PRICE_BRL),
        onConfirm = {
            shopFailed = !TokitoLinks.open(context, TokiAuthLicense.buyUrl(userName))
        },
        onCancel = onCancel,
        onDismissRequest = onCancel
    )
}

/**
 * Tokito Launcher: one line that tells if [userName] has the TokiAuth Original license
 * and opens the activation when it does not.
 */
@Composable
fun TokiAuthLicenseRow(
    userName: String,
    licensed: Boolean,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val admin = licensed && TokiAuthLicense.isAdmin(userName)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (licensed) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = when {
                    admin -> stringResource(R.string.tokiauth_license_active_admin)
                    licensed -> stringResource(R.string.tokiauth_license_active)
                    else -> stringResource(R.string.tokiauth_license_needed, TokiAuthLicense.PRICE_BRL)
                },
                style = MaterialTheme.typography.labelLarge
            )
            if (!licensed) {
                TextButton(
                    enabled = userName.isNotBlank(),
                    onClick = onActivate
                ) {
                    Text(text = stringResource(R.string.tokiauth_license_get))
                }
            }
        }
    }
}
