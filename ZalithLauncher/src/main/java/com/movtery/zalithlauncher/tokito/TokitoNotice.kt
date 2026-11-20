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

import com.movtery.zalithlauncher.path.DOWNLOAD_OKHTTP_CLIENT
import com.movtery.zalithlauncher.path.createRequestBuilder
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

private const val TAG = "TokitoNotice"

/**
 * Tokito Launcher: an announcement for all the players (a new version, a warning, a message of the creators).
 *
 * @param id number of the announcement: every player sees each number once
 * @param versionCode version code of the newest launcher (0 when the announcement is not about a version)
 */
data class TokitoNotice(
    val id: Int,
    val title: String,
    val message: String,
    val url: String?,
    val versionCode: Int,
    val versionName: String?
)

/**
 * Tokito Launcher: reads the announcement that the creators publish in the repository
 * (the file `tokito-update.json`, see the README).
 *
 * There is no server: the launcher only downloads that small file, at most once every 6 hours,
 * and shows the announcement when its number is bigger than the last one the player saw.
 * It only reaches the players that already have a version with this feature (0.11.4 or newer),
 * and only while the repository is public.
 */
object TokitoNotices {
    /** Tokito Preview (version name "t0.0.1") reads the file of its own branch, so it never gets the notices of the stable version */
    private val isPreview: Boolean = com.movtery.zalithlauncher.BuildConfig.VERSION_NAME.startsWith("t")

    val NOTICE_URL: String =
        "https://raw.githubusercontent.com/pedrohenriquecoimbra6845-sudo/Tokito-Laucher/" +
            (if (isPreview) "preview" else "main") + "/tokito-update.json"

    private const val CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L
    private const val MAX_CHARS = 64 * 1024

    /**
     * @return the announcement when one is published, null when there is none, the check is not due yet
     * or there is no internet
     */
    suspend fun check(): TokitoNotice? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val last = AllSettings.tokitoLastNoticeCheck.getValue()
        if (last in 1..now && now - last < CHECK_INTERVAL_MS) return@withContext null

        try {
            val request = createRequestBuilder(NOTICE_URL)
                .header("Cache-Control", "no-cache")
                .build()
            DOWNLOAD_OKHTTP_CLIENT.newCall(request).execute().use { response ->
                AllSettings.tokitoLastNoticeCheck.save(now)
                if (!response.isSuccessful) return@use null
                val text = response.body?.string()?.take(MAX_CHARS) ?: return@use null
                parse(text)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            //no internet, a private repository, a broken file: nothing to show
            Logger.warning(TAG, "Could not check for an announcement", e)
            null
        }
    }

    private fun parse(text: String): TokitoNotice? {
        val json = JSONObject(text)
        val id = json.optInt("id", 0)
        if (id <= 0) return null
        val message = localized(json, "message") ?: return null
        return TokitoNotice(
            id = id,
            title = localized(json, "title").orEmpty(),
            message = message,
            //only secure links are opened
            url = json.optString("url").takeIf { it.startsWith("https://") },
            versionCode = json.optInt("versionCode", 0),
            versionName = json.optString("versionName").takeIf { it.isNotBlank() }
        )
    }

    /** A text that is a plain string or an object with one text for each language ("pt", "en"...) */
    private fun localized(json: JSONObject, key: String): String? {
        val value = json.opt(key) ?: return null
        if (value is String) return value.takeIf { it.isNotBlank() }
        if (value !is JSONObject) return null

        val language = Locale.getDefault().language
        for (code in listOf(language, "en", "pt")) {
            val text = value.optString(code)
            if (text.isNotBlank()) return text
        }
        val keys = value.keys()
        while (keys.hasNext()) {
            val text = value.optString(keys.next())
            if (text.isNotBlank()) return text
        }
        return null
    }
}
