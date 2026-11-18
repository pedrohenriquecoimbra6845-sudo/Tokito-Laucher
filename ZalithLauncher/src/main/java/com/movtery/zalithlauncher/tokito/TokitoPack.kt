/*
 * Tokito Launcher
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

package com.movtery.zalithlauncher.tokito

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.StringRes
import com.movtery.layer_controller.layout.loadLayoutFromString
import com.movtery.layer_controller.utils.newRandomFileName
import com.movtery.layer_controller.utils.saveToFile
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.control.ControlManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.control.mouse.arrowPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.crossHairPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.iBeamPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.linkPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.notAllowedPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.resizeAllPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.resizeEWPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.resizeNSPointerFile
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.Normalizer
import java.util.zip.ZipInputStream

private const val TAG = "TokitoPack"

/**
 * The mouse pointers of the game. Each one is a single image file of the launcher
 * (the same files that Settings > Control > Mouse pointer replaces one by one).
 *
 * @param words whole words of a file name that point to this pointer
 * @param parts pieces of a file name (letters and digits only, no spaces) that point to this pointer
 */
enum class PointerSlot(
    @StringRes val nameRes: Int,
    private val words: List<String>,
    private val parts: List<String>
) {
    //the order matters: the most specific pointers are tested first and the plain arrow is the last one
    NOT_ALLOWED(
        R.string.tokito_pointer_not_allowed,
        words = listOf("no", "block", "blocked", "deny", "denied"),
        parts = listOf("notallowed", "forbidden", "proibido", "unavailable")
    ),
    RESIZE_ALL(
        R.string.tokito_pointer_resize_all,
        words = listOf("all", "move", "mover", "drag"),
        parts = listOf("resizeall", "sizeall", "allscroll", "fleur")
    ),
    RESIZE_NS(
        R.string.tokito_pointer_resize_ns,
        words = listOf("ns", "ud", "vert"),
        parts = listOf("resizens", "nsresize", "sizens", "rowresize", "updown", "vertical")
    ),
    RESIZE_EW(
        R.string.tokito_pointer_resize_ew,
        words = listOf("ew", "lr", "horiz"),
        parts = listOf("resizeew", "ewresize", "sizeew", "colresize", "leftright", "horizontal")
    ),
    CROSSHAIR(
        R.string.tokito_pointer_crosshair,
        words = listOf("cross", "mira", "plus"),
        parts = listOf("crosshair", "precision")
    ),
    IBEAM(
        R.string.tokito_pointer_ibeam,
        words = listOf("text", "texto", "beam", "caret"),
        parts = listOf("ibeam")
    ),
    LINK(
        R.string.tokito_pointer_link,
        words = listOf("link", "hand", "mao", "dedo", "finger"),
        parts = listOf("pointing", "linkselect")
    ),
    ARROW(
        R.string.tokito_pointer_arrow,
        words = listOf("arrow", "seta", "default", "normal", "pointer", "cursor", "mouse"),
        parts = emptyList()
    );

    /** The launcher file that holds this pointer */
    fun file(): File = when (this) {
        ARROW -> arrowPointerFile
        LINK -> linkPointerFile
        IBEAM -> iBeamPointerFile
        CROSSHAIR -> crossHairPointerFile
        RESIZE_NS -> resizeNSPointerFile
        RESIZE_EW -> resizeEWPointerFile
        RESIZE_ALL -> resizeAllPointerFile
        NOT_ALLOWED -> notAllowedPointerFile
    }

    companion object {
        /** Finds the pointer that a file name talks about (arrow.png, resize_ns.png, "Mão.png"...) */
        fun match(fileName: String): PointerSlot? {
            val nameWords = splitWords(fileName.substringBeforeLast('.'))
            val joined = nameWords.joinToString("")
            return entries.firstOrNull { slot ->
                slot.words.any { it in nameWords } || slot.parts.any { joined.contains(it) }
            }
        }
    }
}

/** What an imported ZIP brought */
data class PackImportResult(
    val pointers: List<PointerSlot>,
    val wallpapers: Int,
    val huds: Int,
    /** HUD files made by a newer version of the controls editor than this launcher understands */
    val hudsTooNew: Int,
    val ignored: Int
) {
    val isEmpty: Boolean
        get() = pointers.isEmpty() && wallpapers == 0 && huds == 0
}

/**
 * ZIP packs of Tokito Launcher: one `.zip` with mouse pointers, wallpapers and a control HUD.
 *
 * Folders and file names are understood loosely:
 *  - pointers: images inside a folder called `pointers`, `cursors`, `mouse` or `setas`, or images named
 *    like the pointer (`arrow.png`, `link.png`, `ibeam.png`, `crosshair.png`, `resize_ns.png`, `resize_ew.png`,
 *    `resize_all.png`, `not_allowed.png`)
 *  - wallpapers: images inside a folder called `wallpapers`, `backgrounds` or `fundos` (or any big image)
 *  - HUD: any `.json` made by the control editor of the launcher
 *
 * Nothing is extracted by its own path: every file is read, checked and then written to a path chosen here.
 */
object TokitoPack {
    /** Prefix of the id saved in the settings when the background is an image of the library */
    const val ID_PREFIX = "pack:"

    private const val MAX_ENTRIES = 600
    private const val MAX_IMAGE_BYTES = 24L * 1024 * 1024
    private const val MAX_JSON_BYTES = 4L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 200L * 1024 * 1024
    private const val MAX_LIBRARY = 40

    /** An image this big or bigger (in pixels) is taken as a wallpaper when nothing else says what it is */
    private const val BIG_IMAGE_PX = 600

    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "bmp")
    private val INFO_FILES = setOf("pack.json", "manifest.json", "tokito.json", "info.json", "package.json")

    /** Text files that packs carry along (read me, license): skipped without being counted */
    private val NOTE_EXTENSIONS = setOf("txt", "md", "nfo", "url", "html")
    private val NOTE_NAMES = setOf("license", "licence", "readme", "notice", "credits")
    private val POINTER_FOLDERS = setOf(
        "pointer", "pointers", "cursor", "cursors", "mouse", "seta", "setas", "ponteiro", "ponteiros"
    )
    private val WALLPAPER_FOLDERS = setOf(
        "wallpaper", "wallpapers", "background", "backgrounds", "fundo", "fundos", "bg", "wp"
    )

    /** Folder of the wallpapers that came in ZIP packs (the launcher background is a copy of one of them) */
    val wallpaperDir: File
        get() = File(PathManager.DIR_FILES_PRIVATE, "tokito_wallpapers")

    private val tempDir: File
        get() = File(PathManager.DIR_CACHE, "tokito_pack_import")

    fun listWallpapers(): List<File> =
        wallpaperDir.listFiles { file -> file.isFile && file.extension.lowercase() in IMAGE_EXTENSIONS }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    fun wallpaperId(file: File): String = ID_PREFIX + file.name

    fun deleteWallpaper(file: File) {
        FileUtils.deleteQuietly(file)
    }

    /**
     * Reads the ZIP and puts what it brings where the launcher keeps it:
     * pointers over the pointer files, wallpapers in the library and HUD files in the control layouts
     * (the last HUD is selected and the controls stop following the launcher style, otherwise the style
     * would repaint the HUD that was just imported).
     *
     * @throws IOException when the file cannot be read as a ZIP
     */
    suspend fun import(context: Context, uri: Uri): PackImportResult = withContext(Dispatchers.IO) {
        val temp = tempDir
        FileUtils.deleteQuietly(temp)
        temp.mkdirs()
        try {
            val extracted = extract(context, uri, temp)
            applyItems(extracted.items, extracted.skipped)
        } finally {
            FileUtils.deleteQuietly(temp)
        }
    }

    private class Item(
        val path: String,
        val base: String,
        val ext: String,
        val file: File
    ) {
        val parents: List<String> get() = path.split('/').dropLast(1)
    }

    private class Extracted(
        val items: List<Item>,
        val skipped: Int
    )

    private fun extract(context: Context, uri: Uri, temp: File): Extracted {
        val items = ArrayList<Item>()
        var skipped = 0
        var total = 0L
        var seen = 0

        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open the selected file")
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                if (++seen > MAX_ENTRIES) break

                val path = entry.name.replace('\\', '/').trimStart('/')
                if (path.isEmpty() || path.startsWith("__MACOSX/")) continue
                val base = path.substringAfterLast('/')
                //hidden files and the "._name" copies that macOS adds
                if (base.isEmpty() || base.startsWith(".")) continue
                if (base.lowercase() in INFO_FILES) continue

                val ext = base.substringAfterLast('.', "").lowercase()
                val isJson = ext == "json"
                if (!isJson && ext !in IMAGE_EXTENSIONS) {
                    val isNote = ext in NOTE_EXTENSIONS || base.lowercase() in NOTE_NAMES
                    if (!isNote) skipped++
                    continue
                }

                val out = File(temp, "${items.size}.tmp")
                val size = copyLimited(zip, out, if (isJson) MAX_JSON_BYTES else MAX_IMAGE_BYTES)
                if (size < 0 || total + size > MAX_TOTAL_BYTES) {
                    FileUtils.deleteQuietly(out)
                    skipped++
                    continue
                }
                total += size
                items += Item(path = path, base = base, ext = ext, file = out)
            }
        }
        return Extracted(items, skipped)
    }

    /** Copies at most [limit] bytes. Returns the number of bytes or -1 when the data is bigger than the limit */
    private fun copyLimited(input: InputStream, out: File, limit: Long): Long {
        var total = 0L
        val buffer = ByteArray(16 * 1024)
        out.outputStream().use { stream ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > limit) return -1
                stream.write(buffer, 0, read)
            }
        }
        return total
    }

    private suspend fun applyItems(items: List<Item>, skippedWhileReading: Int): PackImportResult {
        var ignored = skippedWhileReading
        val sorted = items.sortedBy { it.path.lowercase() }

        //HUD of the controls
        var huds = 0
        var hudsTooNew = 0
        var lastHud: File? = null
        for (item in sorted.filter { it.ext == "json" }) {
            try {
                val layout = loadLayoutFromString(item.file.readText())
                val target = File(PathManager.DIR_CONTROL_LAYOUTS, "${newRandomFileName()}.json")
                layout.saveToFile(target)
                huds++
                lastHud = target
            } catch (e: Exception) {
                if (e.message?.contains("not supported", ignoreCase = true) == true) hudsTooNew++ else ignored++
                Logger.warning(TAG, "Skipping ${item.path}, it is not a usable control layout", e)
            }
        }

        //images: pointer or wallpaper
        val pointerCandidates = ArrayList<Item>()
        val wallpaperCandidates = ArrayList<Item>()
        for (item in sorted.filter { it.ext != "json" }) {
            val size = imageSize(item.file)
            if (size == null) {
                ignored++
                continue
            }
            when (folderKind(item.parents)) {
                FolderKind.POINTER -> pointerCandidates += item
                FolderKind.WALLPAPER -> wallpaperCandidates += item
                null -> when {
                    maxOf(size.first, size.second) >= BIG_IMAGE_PX -> wallpaperCandidates += item
                    PointerSlot.match(item.base) != null -> pointerCandidates += item
                    else -> ignored++
                }
            }
        }

        //pointers
        val assigned = LinkedHashMap<PointerSlot, Item>()
        val unnamed = ArrayList<Item>()
        for (item in pointerCandidates) {
            val slot = PointerSlot.match(item.base)
            when {
                slot == null -> unnamed += item
                slot in assigned -> ignored++
                else -> assigned[slot] = item
            }
        }
        //a pack with one single pointer image without a recognizable name: it is the arrow
        if (assigned.isEmpty() && unnamed.size == 1) {
            assigned[PointerSlot.ARROW] = unnamed.removeAt(0)
        }
        ignored += unnamed.size

        val pointers = ArrayList<PointerSlot>()
        for ((slot, item) in assigned) {
            try {
                val target = slot.file()
                target.parentFile?.mkdirs()
                item.file.copyTo(target, overwrite = true)
                pointers += slot
            } catch (e: Exception) {
                ignored++
                Logger.warning(TAG, "Failed to use ${item.path} as the ${slot.name} pointer", e)
            }
        }

        //wallpapers
        var wallpapers = 0
        if (wallpaperCandidates.isNotEmpty()) {
            val dir = wallpaperDir
            dir.mkdirs()
            val used = HashSet<String>()
            var room = MAX_LIBRARY - listWallpapers().size
            for (item in wallpaperCandidates) {
                if (room <= 0) {
                    ignored++
                    continue
                }
                val name = uniqueLibraryName(item, used)
                try {
                    item.file.copyTo(File(dir, name), overwrite = true)
                    wallpapers++
                    room--
                } catch (e: Exception) {
                    ignored++
                    Logger.warning(TAG, "Failed to add ${item.path} to the wallpaper library", e)
                }
            }
        }

        lastHud?.let { file ->
            AllSettings.controlLayout.save(file.name)
            AllSettings.controlsFollowLauncherStyle.save(false)
            ControlManager.refresh()
        }

        return PackImportResult(
            pointers = pointers,
            wallpapers = wallpapers,
            huds = huds,
            hudsTooNew = hudsTooNew,
            ignored = ignored
        )
    }

    private enum class FolderKind { POINTER, WALLPAPER }

    /** What the nearest folder with a known name says the image is */
    private fun folderKind(parents: List<String>): FolderKind? {
        for (folder in parents.asReversed()) {
            val words = splitWords(folder)
            if (words.any { it in POINTER_FOLDERS }) return FolderKind.POINTER
            if (words.any { it in WALLPAPER_FOLDERS }) return FolderKind.WALLPAPER
        }
        return null
    }

    private fun imageSize(file: File): Pair<Int, Int>? = try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
    } catch (e: Exception) {
        Logger.warning(TAG, "Could not read the size of ${file.name}", e)
        null
    }

    /** A safe file name for the library, different from the other ones of this import */
    private fun uniqueLibraryName(item: Item, used: MutableSet<String>): String {
        val stem = item.base.substringBeforeLast('.')
            .map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_' }
            .joinToString("")
            .trim('_')
            .take(40)
            .ifEmpty { "wallpaper" }
        var name = "$stem.${item.ext}"
        var n = 2
        while (!used.add(name.lowercase())) {
            name = "${stem}_$n.${item.ext}"
            n++
        }
        return name
    }
}

/** "Resize_NS-pointer", "resizeNS" and "Mão" become lower case words without accents: [resize, ns, pointer], ... */
private fun splitWords(text: String): List<String> =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.isNotEmpty() }
