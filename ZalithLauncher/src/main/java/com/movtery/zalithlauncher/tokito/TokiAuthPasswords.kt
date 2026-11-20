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
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val TAG = "TokiAuthPasswords"

/**
 * Tokito Launcher: the password of the TokiAuth accounts.
 *
 * The password itself is never saved: only a salted hash (PBKDF2 with HMAC-SHA256, 120 000 rounds, a random
 * salt for each account) in a private file of the launcher. It is asked once each time the launcher process
 * starts, before the game is opened with a TokiAuth account that has a password.
 *
 * This protects the account on this device. It is not a login on a server: the account stays an offline kind
 * of account for the game servers. The site of TokiAuth is [SITE_URL].
 */
object TokiAuthPasswords {
    const val SITE_URL = "https://tokitolauncher.base44.app"
    const val MIN_LENGTH = 6
    const val MAX_LENGTH = 64

    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    private val file: File
        get() = File(PathManager.DIR_FILES_PRIVATE, "tokito_tokiauth.json")

    /** Passwords typed in the creation window, waiting for the account to be created */
    private var pending: Pair<String, String>? = null

    /** Accounts that were unlocked since the launcher started */
    private val unlocked = HashSet<String>()

    private fun key(userName: String) = userName.trim().lowercase(Locale.ROOT)

    private fun read(): JSONObject = try {
        if (file.exists()) JSONObject(file.readText()) else JSONObject()
    } catch (e: Exception) {
        Logger.warning(TAG, "Could not read the password file", e)
        JSONObject()
    }

    @Synchronized
    private fun write(json: JSONObject): Boolean = try {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(json.toString())
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        } else true
    } catch (e: Exception) {
        Logger.warning(TAG, "Could not save the password file", e)
        false
    }

    private fun hash(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun isValidPassword(password: String): Boolean = password.length in MIN_LENGTH..MAX_LENGTH

    /** The player typed this password for the account that is being created (it is saved when the account is created) */
    @Synchronized
    fun stage(userName: String, password: String) {
        pending = key(userName) to password
    }

    /** Saves the staged password for [userName], if there is one */
    @Synchronized
    fun commitStaged(userName: String): Boolean {
        val staged = pending ?: return false
        pending = null
        if (staged.first != key(userName)) return false
        return set(userName, staged.second)
    }

    @Synchronized
    fun set(userName: String, password: String): Boolean {
        if (!isValidPassword(password)) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val digest = hash(password, salt, ITERATIONS)
        val json = read()
        json.put(
            key(userName),
            JSONObject()
                .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .put("hash", Base64.encodeToString(digest, Base64.NO_WRAP))
                .put("rounds", ITERATIONS)
        )
        unlocked.add(key(userName))
        return write(json)
    }

    fun has(userName: String): Boolean = read().has(key(userName))

    @Synchronized
    fun remove(userName: String) {
        val json = read()
        if (json.has(key(userName))) {
            json.remove(key(userName))
            write(json)
        }
        unlocked.remove(key(userName))
    }

    /** True when the account has a password that was not typed yet since the launcher started */
    @Synchronized
    fun needsUnlock(userName: String): Boolean = has(userName) && key(userName) !in unlocked

    /** Checks the password; when it is right the account stays unlocked until the launcher is closed */
    @Synchronized
    fun unlock(userName: String, password: String): Boolean {
        val entry = read().optJSONObject(key(userName)) ?: return true
        return try {
            val salt = Base64.decode(entry.getString("salt"), Base64.NO_WRAP)
            val expected = Base64.decode(entry.getString("hash"), Base64.NO_WRAP)
            val actual = hash(password, salt, entry.optInt("rounds", ITERATIONS))
            val ok = MessageDigest.isEqual(expected, actual)
            if (ok) unlocked.add(key(userName))
            ok
        } catch (e: Exception) {
            Logger.warning(TAG, "Could not check the password", e)
            false
        }
    }
}
