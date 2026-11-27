/*
 * Tokito Launcher
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com>, the Tokito Launcher creators and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.tokito

import com.movtery.zalithlauncher.path.DOWNLOAD_OKHTTP_CLIENT
import com.movtery.zalithlauncher.path.createRequestBuilder
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.time.Instant

private const val TAG = "TokitoUpdates"

/** A modpack of Modrinth, as it is shown on the home screen */
data class TokitoPackInfo(
    val projectId: String,
    val title: String,
    val author: String,
    val iconUrl: String?,
    /** The newest game version that the modpack supports (null when unknown) */
    val gameVersion: String?,
    /** When the modpack got its newest version (or, for the list of the latest ones, when it was changed) */
    val updatedMs: Long,
    /** Name of the newest version of the modpack, when it is known */
    val latestVersionName: String? = null
)

/** One of the installed versions of the player that looks like a modpack of Modrinth */
data class TokitoPackMatch(
    val installedName: String,
    val installedMs: Long,
    val pack: TokitoPackInfo,
    /** The modpack published a version after the player installed it */
    val hasUpdate: Boolean
)

/** An installed version to look for: the name that the player sees and its folder */
class TokitoInstalledPack(val name: String, val dir: File)

data class TokitoUpdatesState(
    val checking: Boolean = false,
    val failed: Boolean = false,
    /** The installed modpacks that were recognized */
    val mine: List<TokitoPackMatch> = emptyList(),
    /** The modpacks that were changed most recently (for everybody) */
    val recent: List<TokitoPackInfo> = emptyList(),
    val checkedAt: Long = 0L
) {
    /** How many of the installed modpacks have a newer version */
    val updateCount: Int get() = mine.count { it.hasUpdate }
}

/**
 * Tokito Launcher: finds out which modpacks of the player got a new version, to make updating them easy
 * (the "Modpacks atualizados" tile of the home screen).
 *
 * The launcher does not save where a modpack came from, so this looks for each installed version by name on
 * Modrinth. When the name matches a modpack, the date of its newest version is compared with the date when the
 * player installed it. It can be wrong (two modpacks with the same name, a pack that was renamed), so the screen
 * always says "possible update" and lets the player look at the modpack before doing anything.
 *
 * Only Modrinth is searched: CurseForge needs a key. Nothing is sent about the player except the names of the
 * versions, as search words, and the result is kept for an hour.
 */
object TokitoUpdates {
    private const val SEARCH_URL = "https://api.modrinth.com/v2/search"
    private const val PROJECT_URL = "https://api.modrinth.com/v2/project/"
    private const val CACHE_MS = 60L * 60L * 1000L
    private const val MAX_INSTALLED = 10
    private const val MAX_CHARS = 512 * 1024
    private const val RECENT_COUNT = 10

    private val _state = MutableStateFlow(TokitoUpdatesState())
    val state: StateFlow<TokitoUpdatesState> = _state.asStateFlow()

    /** A key that stops two checks from running at the same time */
    private val lock = Any()

    @Volatile
    private var running = false

    /**
     * Checks the installed versions and reads the list of the latest modpacks.
     * Does nothing when a check ran in the last hour (unless [force]) or one is already running.
     */
    suspend fun refresh(installed: List<TokitoInstalledPack>, force: Boolean = false) {
        val now = System.currentTimeMillis()
        val old = _state.value
        if (!force && old.checkedAt in 1..now && now - old.checkedAt < CACHE_MS) return
        synchronized(lock) {
            if (running) return
            running = true
        }
        _state.update { it.copy(checking = true) }

        try {
            val result = withContext(Dispatchers.IO) { check(installed) }
            _state.value = result.copy(checking = false, checkedAt = System.currentTimeMillis())
        } catch (e: CancellationException) {
            _state.update { it.copy(checking = false) }
            throw e
        } catch (e: Throwable) {
            Logger.warning(TAG, "Could not check for updated modpacks", e)
            _state.update { it.copy(checking = false, failed = true) }
        } finally {
            running = false
        }
    }

    private fun check(installed: List<TokitoInstalledPack>): TokitoUpdatesState {
        var failures = 0

        //1. the latest modpacks (for everybody)
        val recent = try {
            search(query = null, limit = RECENT_COUNT, index = "updated")
        } catch (e: Exception) {
            failures++
            Logger.warning(TAG, "Could not read the latest modpacks", e)
            emptyList()
        }

        //2. the installed versions that are modpacks of Modrinth
        val mine = mutableListOf<TokitoPackMatch>()
        val candidates = installed
            .filter { looksLikePack(it.name) }
            .map { it to installTime(it.dir) }
            .sortedByDescending { it.second }
            .take(MAX_INSTALLED)
        for ((pack, installedMs) in candidates) {
            try {
                val hit = search(query = pack.name, limit = 3, index = "relevance")
                    .firstOrNull { sameName(pack.name, it.title) }
                    ?: continue
                val newest = newestVersion(hit.projectId)
                val published = newest?.second ?: hit.updatedMs
                mine += TokitoPackMatch(
                    installedName = pack.name,
                    installedMs = installedMs,
                    pack = hit.copy(
                        updatedMs = published,
                        latestVersionName = newest?.first
                    ),
                    //one day of margin: the pack could be installed just after its author published it
                    hasUpdate = published > installedMs + DAY_MS
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failures++
                Logger.warning(TAG, "Could not look for ${pack.name}", e)
                if (failures >= 3) break
            }
        }

        return TokitoUpdatesState(
            failed = failures > 0 && recent.isEmpty() && mine.isEmpty(),
            mine = mine.sortedByDescending { it.hasUpdate },
            recent = recent
        )
    }

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    /** A plain game version ("1.20.1") or a very short name is not a modpack */
    private fun looksLikePack(name: String): Boolean {
        if (normalize(name).length < 5) return false
        if (name.matches(Regex("""[\d.]+"""))) return false
        return true
    }

    private fun normalize(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

    /** The names are the same, or one is inside the other ("Fabulously Optimized 1.20.1" and "Fabulously Optimized") */
    private fun sameName(installed: String, title: String): Boolean {
        val a = normalize(installed)
        val b = normalize(title)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        return b.length >= 5 && (a.contains(b) || (a.length >= 5 && b.contains(a)))
    }

    /** When the version was installed: the date of its json file (it does not change when the player plays) */
    private fun installTime(dir: File): Long {
        val json = File(dir, "${dir.name}.json").lastModified()
        return if (json > 0L) json else dir.lastModified()
    }

    private fun get(url: String): String? {
        val request = createRequestBuilder(url).build()
        DOWNLOAD_OKHTTP_CLIENT.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()?.take(MAX_CHARS)
        }
    }

    private fun search(query: String?, limit: Int, index: String): List<TokitoPackInfo> {
        val facets = URLEncoder.encode("""[["project_type:modpack"]]""", "UTF-8")
        val words = if (query != null) "&query=" + URLEncoder.encode(query, "UTF-8") else ""
        val url = "$SEARCH_URL?facets=$facets&index=$index&limit=$limit$words"
        val text = get(url) ?: throw IllegalStateException("Modrinth did not answer")
        val hits = JSONObject(text).optJSONArray("hits") ?: return emptyList()
        val list = ArrayList<TokitoPackInfo>(hits.length())
        for (i in 0 until hits.length()) {
            val hit = hits.optJSONObject(i) ?: continue
            val id = hit.optString("project_id")
            val title = hit.optString("title")
            if (id.isBlank() || title.isBlank()) continue
            list += TokitoPackInfo(
                projectId = id,
                title = title,
                author = hit.optString("author"),
                iconUrl = hit.optString("icon_url").takeIf { it.startsWith("https://") },
                gameVersion = hit.optJSONArray("versions")?.lastString(),
                updatedMs = parseDate(hit.optString("date_modified"))
            )
        }
        return list
    }

    /** The name and the date of the newest version of the project, or null when it cannot be read */
    private fun newestVersion(projectId: String): Pair<String, Long>? {
        val text = get("$PROJECT_URL$projectId/version") ?: return null
        val array = JSONArray(text)
        var best: Pair<String, Long>? = null
        //the list is ordered, but the newest date is chosen anyway
        for (i in 0 until array.length()) {
            val version = array.optJSONObject(i) ?: continue
            val date = parseDate(version.optString("date_published"))
            if (best == null || date > best.second) {
                val name = version.optString("version_number").ifBlank { version.optString("name") }
                best = name to date
            }
        }
        return best
    }

    private fun JSONArray.lastString(): String? =
        if (length() == 0) null else optString(length() - 1).takeIf { it.isNotBlank() }

    private fun parseDate(text: String): Long = try {
        Instant.parse(text).toEpochMilli()
    } catch (_: Exception) {
        0L
    }
}
