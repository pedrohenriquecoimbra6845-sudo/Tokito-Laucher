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

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.wardrobe.SkinModelType
import com.movtery.zalithlauncher.game.account.wardrobe.SkinType
import com.movtery.zalithlauncher.game.account.wardrobe.getLocalUUIDWithSkinModel
import com.movtery.zalithlauncher.game.account.wardrobe.isSlimModel
import com.movtery.zalithlauncher.game.account.wardrobe.validateSkinFile
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

private const val TAG = "TokitoSkins"

/**
 * What happened when skins were added to the library.
 *
 * @param added skins that went into the library
 * @param oldFormat old skins (64x32), which the launcher does not use
 * @param invalid files that are not a valid skin
 * @param full skins left out because the library is full
 * @param failed the file could not be read at all
 */
data class SkinImportResult(
    val added: Int = 0,
    val oldFormat: Int = 0,
    val invalid: Int = 0,
    val full: Int = 0,
    val failed: Boolean = false
)

/**
 * Tokito Launcher: the Skins category, a library of skin images (PNG, 64x64) that can be put on the
 * Offline and TokiAuth accounts. The skins of online accounts come from their own service.
 */
object TokitoSkins {
    private const val MAX_SKINS = 60
    private const val MAX_ZIP_ENTRIES = 400
    private const val MAX_PNG_BYTES = 2L * 1024 * 1024

    /** Folder of the library */
    val dir: File
        get() = File(PathManager.DIR_FILES_PRIVATE, "tokito_skins")

    private val tempDir: File
        get() = File(PathManager.DIR_CACHE, "tokito_skin_import")

    fun list(): List<File> =
        dir.listFiles { file -> file.isFile && file.extension.equals("png", ignoreCase = true) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    /** Name shown for a skin of the library */
    fun nameOf(file: File): String = file.nameWithoutExtension

    fun delete(file: File) {
        file.delete()
    }

    /**
     * Adds what the player picked: one PNG or a ZIP with several PNG files.
     * Only 64x64 skins go into the library.
     */
    suspend fun import(context: Context, uri: Uri): SkinImportResult = withContext(Dispatchers.IO) {
        val temp = tempDir
        try {
            temp.deleteRecursively()
            temp.mkdirs()

            val header = ByteArray(4)
            val readHeader = context.contentResolver.openInputStream(uri)?.use { stream ->
                var filled = 0
                while (filled < header.size) {
                    val count = stream.read(header, filled, header.size - filled)
                    if (count < 0) break
                    filled += count
                }
                filled == header.size
            } ?: false
            if (!readHeader) return@withContext SkinImportResult(failed = true)

            val isZip = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
            val isPng = header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
                    header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()

            when {
                isPng -> {
                    val file = File(temp, "single.png")
                    val copied = context.contentResolver.openInputStream(uri)?.use { copyLimited(it, file) } ?: false
                    if (!copied) return@withContext SkinImportResult(invalid = 1)
                    val base = sanitize(pickedName(context, uri)?.removeSuffix(".png")?.removeSuffix(".PNG"))
                    count(SkinImportResult(), addValidated(file, base))
                }

                isZip -> {
                    var result = SkinImportResult()
                    context.contentResolver.openInputStream(uri)?.use { raw ->
                        ZipInputStream(raw.buffered()).use { zip ->
                            var entries = 0
                            while (entries < MAX_ZIP_ENTRIES) {
                                val entry = zip.nextEntry ?: break
                                entries++
                                if (entry.isDirectory || !entry.name.lowercase().endsWith(".png")) {
                                    zip.closeEntry()
                                    continue
                                }
                                val file = File(temp, "entry_$entries.png")
                                if (!copyLimited(zip, file)) {
                                    result = result.copy(invalid = result.invalid + 1)
                                    zip.closeEntry()
                                    continue
                                }
                                val base = sanitize(entry.name.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.'))
                                result = count(result, addValidated(file, base))
                                zip.closeEntry()
                            }
                        }
                    } ?: return@withContext SkinImportResult(failed = true)
                    result
                }

                else -> SkinImportResult(invalid = 1)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.warning(TAG, "Failed to import skins", e)
            SkinImportResult(failed = true)
        } finally {
            temp.deleteRecursively()
        }
    }

    /**
     * Puts a skin of the library on an Offline or TokiAuth account, the same way the account screen does it
     * when a skin is picked.
     *
     * @return false when the account does not take local skins or the skin is not valid
     */
    suspend fun apply(account: Account, skin: File): Boolean = withContext(Dispatchers.IO) {
        if (!account.isLocalAccount()) return@withContext false
        if (validateSkinFile(skin) != SkinType.DualLayer) return@withContext false
        try {
            val model = if (skin.isSlimModel()) SkinModelType.ALEX else SkinModelType.STEVE
            val target = account.getSkinFile()
            target.parentFile?.mkdirs()
            skin.copyTo(target, overwrite = true)
            account.skinModelType = model
            account.profileId = getLocalUUIDWithSkinModel(account.username, model)
            AccountsManager.suspendSaveAccount(account)
            AccountsManager.refreshWardrobe()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.warning(TAG, "Failed to apply the skin", e)
            false
        }
    }

    /**
     * Saves the skin the account uses now into the library.
     */
    suspend fun saveCurrent(account: Account): Boolean = withContext(Dispatchers.IO) {
        val source = account.getSkinFile()
        if (!source.isFile || validateSkinFile(source) != SkinType.DualLayer) return@withContext false
        if (list().size >= MAX_SKINS) return@withContext false
        try {
            dir.mkdirs()
            source.copyTo(uniqueFile(sanitize(account.username)), overwrite = false)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.warning(TAG, "Failed to save the current skin", e)
            false
        }
    }

    /**
     * Adds a skin file that was downloaded (the search of skins). The file is copied into the library.
     * @return the file in the library, or null when it is not a valid 64x64 skin or the library is full
     */
    suspend fun addDownloaded(file: File, name: String): File? = withContext(Dispatchers.IO) {
        if (list().size >= MAX_SKINS) return@withContext null
        if (validateSkinFile(file) != SkinType.DualLayer) return@withContext null
        dir.mkdirs()
        file.copyTo(uniqueFile(sanitize(name)), overwrite = false)
    }

    private const val ADDED = 1
    private const val OLD_FORMAT = 2
    private const val INVALID = 3
    private const val FULL = 4

    private fun count(current: SkinImportResult, outcome: Int): SkinImportResult = when (outcome) {
        ADDED -> current.copy(added = current.added + 1)
        OLD_FORMAT -> current.copy(oldFormat = current.oldFormat + 1)
        FULL -> current.copy(full = current.full + 1)
        else -> current.copy(invalid = current.invalid + 1)
    }

    private suspend fun addValidated(file: File, base: String): Int {
        if (list().size >= MAX_SKINS) return FULL
        return when (validateSkinFile(file)) {
            SkinType.DualLayer -> {
                dir.mkdirs()
                file.copyTo(uniqueFile(base), overwrite = false)
                ADDED
            }
            SkinType.Classic -> OLD_FORMAT
            SkinType.Invalid -> INVALID
        }
    }

    /** Copies the stream into the file; false when it is bigger than a skin could ever be */
    private fun copyLimited(input: InputStream, target: File): Boolean {
        var total = 0L
        target.outputStream().use { sink ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_PNG_BYTES) return false
                sink.write(buffer, 0, read)
            }
        }
        return true
    }

    private fun pickedName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }

    /** A name that is safe for a file: letters, numbers, spaces, dots, dashes and underscores */
    private fun sanitize(name: String?): String {
        val clean = name.orEmpty()
            .map { if (it.isLetterOrDigit() || it == ' ' || it == '.' || it == '-' || it == '_') it else '_' }
            .joinToString("")
            .trim()
            .trim('.')
            .take(40)
        return clean.ifBlank { "skin" }
    }

    private fun uniqueFile(base: String): File {
        var file = File(dir, "$base.png")
        var number = 2
        while (file.exists()) {
            file = File(dir, "$base-$number.png")
            number++
        }
        return file
    }
}
