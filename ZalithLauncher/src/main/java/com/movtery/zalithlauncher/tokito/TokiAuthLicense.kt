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

package com.movtery.zalithlauncher.tokito

import android.util.Base64
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import org.json.JSONObject
import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Locale

private const val TAG = "TokiAuthLicense"

/**
 * Tokito Launcher: the license of the "TokiAuth Original" account.
 *
 * TokiAuth Original is the verified account of the Tokito Launcher. It is **not** a Mojang / Microsoft
 * license: servers that check the login online (premium) still do not accept it.
 *
 * A license is a text key issued by the admin of the launcher after the payment ([PRICE_BRL] reais, one time):
 *
 *     TKA1.<base64url(payload)>.<base64url(signature)>
 *
 * - the payload is a small JSON `{"n": "<player name in lower case>", "k": "std" | "admin", "i": <unix seconds>}`;
 * - the signature is an ECDSA P-256 / SHA-256 signature (DER) of the payload bytes. Only the admin has the private
 *   key (it lives in a GitHub secret of the workflow `tokiauth-license.yml`), the launcher only has the public key;
 * - a license works for one player name only, so a key that was shared does not work for another name.
 *
 * The check is done on the device (no server). Anyone can change an open source app, so this protects against
 * sharing and casual use, not against someone who edits the launcher; a stronger protection needs a server
 * that checks the account on the game server side.
 *
 * The player of the admin ([ADMIN_NAME]) uses it for free: the admin issues an `admin` license for that name
 * with the same workflow. The name itself never unlocks anything (anyone could type it).
 */
object TokiAuthLicense {
    const val PRICE_BRL = 3
    const val PREFIX = "TKA1"
    const val KIND_STANDARD = "std"
    const val KIND_ADMIN = "admin"

    /** The admin of the Tokito Launcher; this player does not pay (the license for it is issued by the admin) */
    const val ADMIN_NAME = "OPedzGoat"

    /** Where a player pays / asks for the key (the admin hands the key out on Discord) */
    const val PAY_URL = TokitoLinks.DISCORD

    /** Public key (X.509 SubjectPublicKeyInfo, base64) of the ECDSA P-256 key that signs the licenses */
    private const val PUBLIC_KEY_B64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAENx7CRxvNzVEzmHFhGBb5Emv0J+Q/CiaTJAVQBgKbsWL7FDuYaLVyMm88GCtuL1g0kp72ZqI41jSCqtjZfGxsag=="

    /** What a license says (only trustworthy after the signature was checked) */
    data class Info(val name: String, val kind: String, val issuedAt: Long) {
        val isAdmin: Boolean get() = kind == KIND_ADMIN
    }

    private val file: File
        get() = File(PathManager.DIR_FILES_PRIVATE, "tokito_licenses.json")

    private val publicKey by lazy {
        KeyFactory.getInstance("EC").generatePublic(
            X509EncodedKeySpec(Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT))
        )
    }

    /** token -> checked result, so the chat can check many messages without repeating the math */
    private val checked = HashMap<String, Info?>()

    fun normalize(name: String): String = name.trim().lowercase(Locale.ROOT)

    private fun decode(part: String): ByteArray {
        val padded = part + "=".repeat((4 - part.length % 4) % 4)
        return Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /** Removes spaces and line breaks that appear when a key is copied from a chat */
    fun clean(token: String): String = token.filterNot { it.isWhitespace() }

    /**
     * Checks the signature of [token] and returns what it says, or null when it is not a real license.
     */
    @Synchronized
    fun check(token: String?): Info? {
        if (token.isNullOrBlank()) return null
        val key = clean(token)
        if (checked.containsKey(key)) return checked[key]
        val result = try {
            val parts = key.split('.')
            if (parts.size != 3 || parts[0] != PREFIX) {
                null
            } else {
                val payload = decode(parts[1])
                val signature = decode(parts[2])
                val verifier = Signature.getInstance("SHA256withECDSA")
                verifier.initVerify(publicKey)
                verifier.update(payload)
                if (!verifier.verify(signature)) {
                    null
                } else {
                    val json = JSONObject(String(payload, Charsets.UTF_8))
                    Info(
                        name = json.getString("n"),
                        kind = json.optString("k", KIND_STANDARD),
                        issuedAt = json.optLong("i", 0L)
                    )
                }
            }
        } catch (e: Exception) {
            Logger.warning(TAG, "Could not check a license", e)
            null
        }
        if (checked.size > 512) checked.clear()
        checked[key] = result
        return result
    }

    /** True when [token] is a real license for the player [name] */
    fun verify(name: String, token: String?): Boolean {
        val info = check(token) ?: return false
        return info.name == normalize(name)
    }

    /** The file is read again only when it changed (the launcher and the game are different processes) */
    private var cachedStamp = -1L
    private var cached = JSONObject()

    @Synchronized
    private fun read(): JSONObject {
        val stamp = if (file.exists()) file.lastModified() else 0L
        if (stamp != cachedStamp) {
            cached = try {
                if (stamp != 0L) JSONObject(file.readText()) else JSONObject()
            } catch (e: Exception) {
                Logger.warning(TAG, "Could not read the license file", e)
                JSONObject()
            }
            cachedStamp = stamp
        }
        return cached
    }

    @Synchronized
    private fun write(json: JSONObject): Boolean = try {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(json.toString())
        val ok = if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        } else true
        cached = json
        cachedStamp = if (file.exists()) file.lastModified() else 0L
        ok
    } catch (e: Exception) {
        Logger.warning(TAG, "Could not save the license file", e)
        false
    }

    /** The key that was saved for [name] (it may be invalid if the file was edited) */
    fun tokenFor(name: String): String? {
        val saved = read().optString(normalize(name), "")
        return saved.takeIf { verify(name, it) }
    }

    /** What the saved license of [name] says, or null when there is none */
    fun infoFor(name: String): Info? = check(tokenFor(name))

    fun isLicensed(name: String): Boolean = name.isNotBlank() && tokenFor(name) != null

    fun isAdmin(name: String): Boolean = infoFor(name)?.isAdmin == true

    /** Saves [token] for [name] when it is a real license for that name */
    @Synchronized
    fun activate(name: String, token: String): Boolean {
        val key = clean(token)
        if (!verify(name, key)) return false
        val json = read()
        json.put(normalize(name), key)
        return write(json)
    }

    @Synchronized
    fun remove(name: String) {
        val json = read()
        if (json.has(normalize(name))) {
            json.remove(normalize(name))
            write(json)
        }
    }
}
