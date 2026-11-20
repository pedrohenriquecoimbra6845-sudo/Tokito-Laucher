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
import com.movtery.zalithlauncher.game.account.wardrobe.SkinType
import com.movtery.zalithlauncher.game.account.wardrobe.validateSkinFile
import com.movtery.zalithlauncher.path.DOWNLOAD_OKHTTP_CLIENT
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.path.createRequestBuilder
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

private const val TAG = "TokitoSkinSearch"

/** Result of the search of a skin */
sealed interface SkinSearchResult {
    /** @param file the downloaded skin (a temporary file; add it to the library to keep it) */
    data class Found(val name: String, val file: File) : SkinSearchResult

    /** There is no player with this name */
    data object NotFound : SkinSearchResult

    /** The player has an old skin (64x32), which the launcher does not use */
    data object OldFormat : SkinSearchResult

    /** The name is not a valid Minecraft name */
    data object BadName : SkinSearchResult

    /** No internet, or the service did not answer */
    data object Failed : SkinSearchResult
}

/**
 * Tokito Launcher: search of skins in the Skins category. It looks for the public skin of a Minecraft Java
 * player by name, in the profile service of Mojang (the same service that the game uses): the name gives
 * the player id, the id gives the address of the skin image, and only that image is downloaded.
 * Nothing about the player of this launcher is sent: only the name that was typed.
 */
object TokitoSkinSearch {
    private const val PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/"
    private const val SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/"
    private const val MAX_PNG_BYTES = 256 * 1024
    private val NAME_REGEX = Regex("^[A-Za-z0-9_]{3,16}$")

    suspend fun search(rawName: String): SkinSearchResult = withContext(Dispatchers.IO) {
        val name = rawName.trim()
        if (!NAME_REGEX.matches(name)) return@withContext SkinSearchResult.BadName
        try {
            //1. name -> id
            val profileText = DOWNLOAD_OKHTTP_CLIENT.newCall(createRequestBuilder(PROFILE_URL + name).build())
                .execute().use { response ->
                    if (response.code == 404 || response.code == 204) return@withContext SkinSearchResult.NotFound
                    if (!response.isSuccessful) return@withContext SkinSearchResult.Failed
                    response.body?.string()
                } ?: return@withContext SkinSearchResult.Failed
            val profile = JSONObject(profileText)
            val id = profile.optString("id")
            val exactName = profile.optString("name", name)
            if (!Regex("^[0-9a-fA-F]{32}$").matches(id)) return@withContext SkinSearchResult.NotFound

            //2. id -> address of the skin
            val sessionText = DOWNLOAD_OKHTTP_CLIENT.newCall(createRequestBuilder(SESSION_URL + id).build())
                .execute().use { response ->
                    if (!response.isSuccessful) return@withContext SkinSearchResult.Failed
                    response.body?.string()
                } ?: return@withContext SkinSearchResult.Failed
            val properties = JSONObject(sessionText).optJSONArray("properties")
                ?: return@withContext SkinSearchResult.Failed
            var skinUrl: String? = null
            for (i in 0 until properties.length()) {
                val property = properties.getJSONObject(i)
                if (property.optString("name") != "textures") continue
                val decoded = String(Base64.decode(property.optString("value"), Base64.DEFAULT), Charsets.UTF_8)
                skinUrl = JSONObject(decoded).optJSONObject("textures")?.optJSONObject("SKIN")?.optString("url")
            }
            //a player without a skin of their own uses a default one, which has no address here
            if (skinUrl.isNullOrBlank()) return@withContext SkinSearchResult.NotFound
            //only the texture server of Minecraft, over a secure connection
            val host = skinUrl.substringAfter("://", "").substringBefore('/')
            if (!skinUrl.startsWith("https://") || !(host == "textures.minecraft.net" || host.endsWith(".minecraft.net"))) {
                return@withContext SkinSearchResult.Failed
            }

            //3. download the image
            val dir = File(PathManager.DIR_CACHE, "tokito_skin_search").also { it.mkdirs() }
            val target = File(dir, "$exactName.png")
            val bytes = DOWNLOAD_OKHTTP_CLIENT.newCall(createRequestBuilder(skinUrl).build())
                .execute().use { response ->
                    if (!response.isSuccessful) return@withContext SkinSearchResult.Failed
                    val body = response.body ?: return@withContext SkinSearchResult.Failed
                    val data = body.byteStream().use { stream ->
                        val out = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            val read = stream.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            if (out.size() > MAX_PNG_BYTES) return@withContext SkinSearchResult.Failed
                        }
                        out.toByteArray()
                    }
                    data
                }
            target.writeBytes(bytes)

            when (validateSkinFile(target)) {
                SkinType.DualLayer -> SkinSearchResult.Found(exactName, target)
                SkinType.Classic -> SkinSearchResult.OldFormat
                SkinType.Invalid -> SkinSearchResult.Failed
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.warning(TAG, "Skin search failed", e)
            SkinSearchResult.Failed
        }
    }
}
