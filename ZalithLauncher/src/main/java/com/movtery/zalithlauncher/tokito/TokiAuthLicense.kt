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

import android.net.Uri
import android.util.Base64
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.io.File
import java.security.KeyFactory
import java.security.PublicKey
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
 * A license is a text key, signed after the payment ([PRICE_BRL] reais, one time):
 *
 *     TKA1.<base64url(payload)>.<base64url(signature)>
 *
 * - the payload is a small JSON `{"n": "<player name in lower case>", "k": "std" | "admin", "i": <unix seconds>}`;
 * - the signature is an ECDSA P-256 / SHA-256 signature of the payload bytes, either in DER (what `openssl` makes)
 *   or as the raw 64 bytes `r || s` (what the WebCrypto of a browser / Deno makes);
 * - a license works for one player name only, so a key that was shared does not work for another name.
 *
 * How a player gets it (no key is handed out by hand):
 *
 * 1. the launcher opens the shop of the Tokito ([SHOP_URL], made on Base44) with the player name;
 * 2. the shop finishes the payment, signs the license on its backend and sends the player back to the game with
 *    `tokito://tokiauth/activate?license=TKA1...` ([returnUrl]);
 * 3. the launcher checks the signature on the device ([activateFromLink]) and the account becomes Original.
 *
 * Two public keys are accepted. The **shop key** is the one whose private part lives only on the backend of the
 * shop and it can issue standard licenses only. The **admin key** (the private part lives in the GitHub secret of
 * the workflow `tokiauth-license.yml`) can also issue the `admin` license of the player of the admin ([ADMIN_NAME]),
 * who uses it for free. The name itself never unlocks anything (anyone could type it).
 *
 * The check is done on the device (no server). Anyone can change an open source app, so this protects against
 * sharing and casual use, not against someone who edits the launcher; a stronger protection needs a server
 * that checks the account on the game server side.
 */
object TokiAuthLicense {
    const val PRICE_BRL = 3
    const val PREFIX = "TKA1"
    const val KIND_STANDARD = "std"
    const val KIND_ADMIN = "admin"

    /** The admin of the Tokito Launcher; this player does not pay (the license for it is issued by the admin) */
    const val ADMIN_NAME = "OPedzGoat"

    /** The shop of the Tokito (Base44): the player pays there and the site brings him back to the launcher */
    const val SHOP_URL = "https://tokitolauncher.base44.app"

    /** The shop brings the player back with `<scheme>://tokiauth/activate?license=TKA1...` */
    const val LINK_HOST = "tokiauth"
    const val LINK_PATH = "/activate"
    const val LINK_SCHEME_STABLE = "tokito"

    /** The Tokito Preview uses its own scheme, so a phone that has both apps does not ask which one to open */
    const val LINK_SCHEME_PREVIEW = "tokitopreview"

    val linkScheme: String
        get() = if (BuildConfig.APPLICATION_ID.endsWith(".preview")) LINK_SCHEME_PREVIEW else LINK_SCHEME_STABLE

    /** Where the shop must send the player after the payment (the launcher also tells it as the `return` parameter) */
    val returnUrl: String
        get() = "$linkScheme://$LINK_HOST$LINK_PATH"

    /** The page of the shop for [player]: `?player=<name>&return=<returnUrl>&price=<reais>` */
    fun buyUrl(player: String): String =
        "$SHOP_URL/?player=${Uri.encode(player.trim())}&return=${Uri.encode(returnUrl)}&price=$PRICE_BRL"

    /** A public key that is accepted to check licenses */
    private class SigningKey(base64: String, val mayIssueAdmin: Boolean) {
        val publicKey: PublicKey by lazy {
            KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.decode(base64, Base64.DEFAULT)))
        }
    }

    /** X.509 SubjectPublicKeyInfo (base64) of the ECDSA P-256 keys that sign the licenses */
    private val signingKeys = listOf(
        //admin key
        SigningKey(
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAENx7CRxvNzVEzmHFhGBb5Emv0J+Q/CiaTJAVQBgKbsWL7FDuYaLVyMm88GCtuL1g0kp72ZqI41jSCqtjZfGxsag==",
            mayIssueAdmin = true
        ),
        //shop key (standard licenses only)
        SigningKey(
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAECHV4CkUQs5yn3g2iXgoLUVIQMMbjOLvB2CqYVFMmiMM2EbSzfcO/NZPa6cvWNJv5RBW3qk7njXroFavYSeEC4Q==",
            mayIssueAdmin = false
        )
    )

    /** What a license says (only trustworthy after the signature was checked) */
    data class Info(val name: String, val kind: String, val issuedAt: Long) {
        val isAdmin: Boolean get() = kind == KIND_ADMIN
    }

    /** What happened with a link that the shop opened in the launcher */
    sealed interface LinkResult {
        data class Activated(val name: String) : LinkResult
        data object Invalid : LinkResult
        data object SaveFailed : LinkResult
    }

    private val file: File
        get() = File(PathManager.DIR_FILES_PRIVATE, "tokito_licenses.json")

    private val _changes = MutableStateFlow(0)

    /** Goes up every time a license is saved or removed, so the screens that show it refresh by themselves */
    val changes: StateFlow<Int> = _changes.asStateFlow()

    /** token -> checked result, so the chat can check many messages without repeating the math */
    private val checked = HashMap<String, Info?>()

    fun normalize(name: String): String = name.trim().lowercase(Locale.ROOT)

    private fun decode(part: String): ByteArray {
        val padded = part + "=".repeat((4 - part.length % 4) % 4)
        return Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /** Removes spaces and line breaks that appear when a key is copied from a chat */
    fun clean(token: String): String = token.filterNot { it.isWhitespace() }

    /** The raw signature `r || s` (what WebCrypto makes) as the DER sequence that [Signature] checks */
    private fun rawToDer(raw: ByteArray): ByteArray {
        fun integer(part: ByteArray): ByteArray {
            var start = 0
            while (start < part.size - 1 && part[start] == 0.toByte()) start++
            var body = part.copyOfRange(start, part.size)
            if (body[0] < 0) body = byteArrayOf(0) + body //keeps the number positive
            return byteArrayOf(0x02, body.size.toByte()) + body
        }

        val half = raw.size / 2
        val r = integer(raw.copyOfRange(0, half))
        val s = integer(raw.copyOfRange(half, raw.size))
        return byteArrayOf(0x30, (r.size + s.size).toByte()) + r + s
    }

    private fun signedBy(signer: SigningKey, payload: ByteArray, signature: ByteArray): Boolean {
        fun attempt(sig: ByteArray): Boolean = try {
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(signer.publicKey)
            verifier.update(payload)
            verifier.verify(sig)
        } catch (_: Exception) {
            false
        }
        return attempt(signature) || (signature.size == 64 && attempt(rawToDer(signature)))
    }

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
                val json = JSONObject(String(payload, Charsets.UTF_8))
                val kind = json.optString("k", KIND_STANDARD)
                //the signature protects the payload, so the kind that is read here is the one that was signed
                val signer = signingKeys.firstOrNull {
                    (it.mayIssueAdmin || kind != KIND_ADMIN) && signedBy(it, payload, signature)
                }
                if (signer == null) {
                    null
                } else {
                    Info(
                        name = normalize(json.getString("n")),
                        kind = kind,
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
        val saved = write(json)
        if (saved) _changes.update { it + 1 }
        return saved
    }

    /**
     * The shop opened `<scheme>://tokiauth/activate?license=TKA1...` in the launcher: checks the license
     * and, when it is real, saves it for the player that is written in it.
     */
    fun activateFromLink(uri: Uri): LinkResult {
        val token = uri.getQueryParameter("license") ?: uri.getQueryParameter("key") ?: return LinkResult.Invalid
        val info = check(token) ?: return LinkResult.Invalid
        return if (activate(info.name, token)) LinkResult.Activated(info.name) else LinkResult.SaveFailed
    }

    @Synchronized
    fun remove(name: String) {
        val json = read()
        if (json.has(normalize(name))) {
            json.remove(normalize(name))
            if (write(json)) _changes.update { it + 1 }
        }
    }
}
