/*
 * Tokito Laucher
 * Based on Zalith Launcher 2 (Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors).
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

package com.movtery.zalithlauncher.game.rust

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.path.DOWNLOAD_OKHTTP_CLIENT
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.path.createRequestBuilder
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.fetchStringFromUrl
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest

private const val TAG = "RustEdition"

/** What the Rust Edition download is doing right now */
sealed interface RustEditionState {
    /** Nothing is running */
    data object Idle : RustEditionState

    /** Looking for / downloading the app. [progress] goes from 0 to 1, or is -1 when not known yet */
    data class Working(val progress: Float) : RustEditionState
}

/**
 * "Minecraft Rust Edition": a native port of Minecraft: Java Edition written in Rust
 * (Starling, formerly Horizon Android, MIT license, https://github.com/JuanLuci/starling).
 *
 * It is a separate Android app, so Tokito Laucher cannot embed it. What the launcher does:
 * - detects it and opens it when it is installed;
 * - downloads the latest APK straight from the official site of the project (its GitHub releases)
 *   into the Tokito folder (`rust_edition`) and opens the Android installer.
 *
 * The APK itself is never part of the Tokito source code or of the Tokito APK.
 */
object RustEdition {
    /** Release page of the app (the official site) */
    const val RELEASES_URL = "https://github.com/JuanLuci/starling/releases/latest"

    private const val LATEST_RELEASE_API = "https://api.github.com/repos/JuanLuci/starling/releases/latest"

    /** The files of the app can only come from here */
    private const val OFFICIAL_DOWNLOAD_PREFIX = "https://github.com/JuanLuci/starling/releases/download/"

    private const val TASK_ID = "tokito_rust_edition_download"
    private const val APK_MIME = "application/vnd.android.package-archive"

    /** Package ids used by the app (stable, preview, dev and the legacy Horizon Android id) */
    private val PACKAGES = listOf(
        "io.github.juanluci.starling",
        "io.github.juanluci.starling.preview",
        "io.github.juanluci.starling.dev",
        "dev.horizon.android"
    )

    /** What the download is doing now (the Rust Edition card shows it) */
    var state: RustEditionState by mutableStateOf(RustEditionState.Idle)
        private set

    /** Folder of Tokito where the app is downloaded to */
    fun folder(): File = File(PathManager.DIR_FILES_EXTERNAL, "rust_edition")

    /** The newest APK that is already in the Tokito folder, or null */
    fun downloadedApk(): File? = folder()
        .listFiles { file -> file.isFile && file.extension.equals("apk", ignoreCase = true) }
        ?.maxByOrNull { it.lastModified() }

    /** Intent that opens the installed app, or null when it is not installed */
    fun launchIntent(context: Context): Intent? {
        val manager = context.packageManager
        for (packageName in PACKAGES) {
            val intent = runCatching { manager.getLaunchIntentForPackage(packageName) }.getOrNull()
            if (intent != null) return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return null
    }

    fun isInstalled(context: Context): Boolean = launchIntent(context) != null

    /** Opens the app. Returns false when it is not installed (or could not be opened) */
    fun open(context: Context): Boolean {
        val intent = launchIntent(context) ?: return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    /** The app only exists for 64-bit ARM */
    fun isDeviceSupported(): Boolean = Build.SUPPORTED_64_BIT_ABIS.contains("arm64-v8a")

    /**
     * Downloads the latest APK from the official site into the Tokito folder, then opens the installer.
     * The progress shows in the task menu and in [state].
     */
    fun downloadAndInstall(context: Context) {
        if (state is RustEditionState.Working) return
        val appContext = context.applicationContext

        if (!isDeviceSupported()) {
            toast(appContext, appContext.getString(R.string.tokito_rust_unsupported_abi))
            return
        }

        state = RustEditionState.Working(-1f)
        TaskSystem.submitTask(
            Task.runTask(
                id = TASK_ID,
                dispatcher = Dispatchers.IO,
                task = { task ->
                    task.updateMessage(androidText(R.string.tokito_rust_task_checking))
                    val release = fetchLatestRelease()

                    val target = File(folder(), release.fileName)
                    target.parentFile?.mkdirs()

                    if (!isAlreadyDownloaded(target, release)) {
                        task.updateMessage(androidText(R.string.tokito_rust_task_downloading, release.fileName))
                        cleanFolder(except = null)
                        download(release, target) { progress ->
                            task.updateProgress(progress)
                            state = RustEditionState.Working(progress)
                        }
                    }
                    //only the newest file stays in the folder
                    cleanFolder(except = target)

                    task.updateProgress(1f)
                    task.updateMessage(androidText(R.string.tokito_rust_task_installing))
                    withContext(Dispatchers.Main) {
                        toast(appContext, appContext.getString(R.string.tokito_rust_downloaded))
                        install(appContext, target)
                    }
                },
                onError = { th ->
                    Logger.warning(TAG, "Failed to get Rust Edition!", th)
                    withContext(Dispatchers.Main) {
                        toast(
                            appContext,
                            appContext.getString(R.string.tokito_rust_error, th.getMessageOrToString())
                        )
                        //the official site can always be opened in the browser
                        openLink(appContext, RELEASES_URL)
                    }
                },
                onFinally = {
                    state = RustEditionState.Idle
                }
            )
        )
    }

    /**
     * Opens the Android installer for the APK that is already in the Tokito folder.
     * @return false when there is no APK
     */
    fun installDownloaded(context: Context): Boolean {
        val apk = downloadedApk() ?: return false
        install(context.applicationContext, apk)
        return true
    }

    /**
     * Opens the Android installer. If Tokito is not allowed to install apps yet, opens the page
     * where the user allows it: the APK stays in the folder and can be installed afterwards.
     */
    private fun install(context: Context, apk: File) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                toast(context, context.getString(R.string.tokito_rust_need_permission))
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apk)
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, APK_MIME)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            Logger.warning(TAG, "Failed to open the installer!", it)
            toast(context, context.getString(R.string.tokito_rust_error, it.getMessageOrToString()))
        }
    }

    private fun openLink(context: Context, url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun toast(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    /** The release and the APK file that goes with it */
    private class Release(
        val tag: String,
        val fileName: String,
        val url: String,
        val size: Long,
        val sha256: String?
    )

    /** Asks the official site (GitHub releases of the project) for the latest release */
    private suspend fun fetchLatestRelease(): Release {
        val json = JSONObject(fetchStringFromUrl(LATEST_RELEASE_API))
        val assets = json.optJSONArray("assets") ?: throw IOException("The release has no files")

        var best: JSONObject? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            //the 64-bit ARM build is the one that runs on phones
            val isArm64 = name.contains("arm64", ignoreCase = true)
            val bestIsArm64 = best?.optString("name")?.contains("arm64", ignoreCase = true) == true
            if (best == null || (isArm64 && !bestIsArm64)) best = asset
        }
        val asset = best ?: throw IOException("The release has no APK")

        val name = asset.optString("name")
        if (!Regex("[A-Za-z0-9._-]+").matches(name)) throw IOException("Unexpected file name: $name")

        val url = asset.optString("browser_download_url")
        if (!url.startsWith(OFFICIAL_DOWNLOAD_PREFIX)) throw IOException("The file is not from the official site")

        val digest = asset.optString("digest")
        return Release(
            tag = json.optString("tag_name"),
            fileName = name,
            url = url,
            size = asset.optLong("size", -1L),
            sha256 = digest.takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")?.lowercase()
        )
    }

    private fun isAlreadyDownloaded(target: File, release: Release): Boolean {
        if (!target.isFile) return false
        if (release.size > 0 && target.length() != release.size) return false
        val expected = release.sha256 ?: return release.size > 0
        return runCatching { sha256Of(target) == expected }.getOrDefault(false)
    }

    /** Removes the other APKs and unfinished downloads from the Tokito folder */
    private fun cleanFolder(except: File?) {
        folder().listFiles()?.forEach { file ->
            if (file.isFile && file != except && (file.extension.equals("apk", true) || file.extension == "part")) {
                file.delete()
            }
        }
    }

    private suspend fun download(
        release: Release,
        target: File,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val partial = File(target.parentFile, target.name + ".part")
        val digest = MessageDigest.getInstance("SHA-256")

        try {
            DOWNLOAD_OKHTTP_CLIENT.newCall(createRequestBuilder(release.url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code} - ${response.message}")

                val body = response.body
                val total = body.contentLength().takeIf { it > 0 } ?: release.size

                body.byteStream().use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var done = 0L
                        var lastReport = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            done += read
                            //report about 40 times in the whole download
                            if (total > 0 && done - lastReport >= total / 40 + 1) {
                                lastReport = done
                                onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }

            if (release.size > 0 && partial.length() != release.size) {
                throw IOException("The download is incomplete (${partial.length()} of ${release.size} bytes)")
            }
            val actual = digest.digest().toHex()
            if (release.sha256 != null && actual != release.sha256) {
                throw IOException("The downloaded file does not match the official checksum")
            }

            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) throw IOException("Could not save ${target.name}")
        } catch (th: Throwable) {
            partial.delete()
            throw th
        }
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
