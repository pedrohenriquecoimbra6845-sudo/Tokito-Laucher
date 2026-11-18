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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "TokitoPopups"

/**
 * Tokito Launcher: a pop-up of the home screen, a small floating frame with a photo or a video.
 *
 * @param file photo or video, inside [TokitoPopups.dir]
 * @param thumb still frame of a video (shown while the video is not playing)
 * @param aspect width divided by height
 * @param size 0 small, 1 medium, 2 large
 * @param x position in the free space of the screen, from 0 (start) to 1 (end)
 * @param y same as [x], from the top to the bottom
 */
data class TokitoPopup(
    val id: String,
    val isVideo: Boolean,
    val file: String,
    val thumb: String?,
    val aspect: Float,
    val size: Int,
    val x: Float,
    val y: Float
) {
    val mediaFile: File get() = File(TokitoPopups.dir, file)

    /** Image to show while there is no video playing (the photo itself for photos) */
    val previewFile: File get() = File(TokitoPopups.dir, thumb ?: file)
}

/** What happened when a photo or a video was added */
enum class PopupImportResult {
    ADDED,
    FULL,
    TOO_BIG,
    FAILED
}

/**
 * Tokito Launcher: the pop-ups (floating photos and videos) of the home screen.
 *
 * Photos are scaled down to 1280 pixels and saved again, so they weigh little. Videos are kept as they are
 * (up to 60 MB) with a small still frame next to them: the launcher plays them muted, in a loop, only
 * while the home screen is open, at most two at the same time, and shows the still frame otherwise.
 */
object TokitoPopups {
    const val MAX_POPUPS = 6

    /** Videos that start by themselves at the same time */
    const val MAX_PLAYING_VIDEOS = 2

    private const val MAX_VIDEO_BYTES = 60L * 1024 * 1024
    private const val MAX_IMAGE_SIDE = 1280
    private const val MAX_THUMB_SIDE = 480

    val dir: File
        get() = File(PathManager.DIR_FILES_PRIVATE, "tokito_popups")

    private val indexFile: File
        get() = File(dir, "popups.json")

    /** The pop-ups, in the order they were added */
    val popups = mutableStateListOf<TokitoPopup>()

    /** Pop-ups closed with the X during this run: they come back the next time the app starts */
    val dismissed = mutableStateListOf<String>()

    @Volatile
    private var loaded = false
    private val loadMutex = Mutex()

    /** Reads the saved pop-ups (only the first call does it, the others wait for it) */
    suspend fun load() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            val list = withContext(Dispatchers.IO) { readIndex() }
            popups.clear()
            popups.addAll(list)
            loaded = true
        }
    }

    suspend fun remove(id: String) {
        val popup = popups.firstOrNull { it.id == id } ?: return
        popups.remove(popup)
        dismissed.remove(id)
        withContext(Dispatchers.IO) {
            File(dir, popup.file).delete()
            popup.thumb?.let { File(dir, it).delete() }
            writeIndex()
        }
    }

    suspend fun setSize(id: String, size: Int) {
        update(id) { it.copy(size = size.coerceIn(0, 2)) }
    }

    suspend fun move(id: String, x: Float, y: Float) {
        update(id) { it.copy(x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f)) }
    }

    private suspend fun update(id: String, change: (TokitoPopup) -> TokitoPopup) {
        val index = popups.indexOfFirst { it.id == id }
        if (index < 0) return
        popups[index] = change(popups[index])
        withContext(Dispatchers.IO) { writeIndex() }
    }

    /**
     * Adds a photo or a video picked by the player.
     */
    suspend fun import(context: Context, uri: Uri, video: Boolean): PopupImportResult {
        //the saved list has to be read first, or it would be replaced by the new pop-up alone
        load()
        return importNow(context, uri, video)
    }

    private suspend fun importNow(context: Context, uri: Uri, video: Boolean): PopupImportResult = withContext(Dispatchers.IO) {
        if (popups.size >= MAX_POPUPS) return@withContext PopupImportResult.FULL
        dir.mkdirs()
        val id = newId()
        val created = mutableListOf<File>()
        try {
            val popup = if (video) {
                importVideo(context, uri, id, created)
            } else {
                importImage(context, uri, id, created)
            }
            popups.add(popup)
            writeIndex()
            PopupImportResult.ADDED
        } catch (e: TooBigException) {
            created.forEach { it.delete() }
            PopupImportResult.TOO_BIG
        } catch (e: CancellationException) {
            created.forEach { it.delete() }
            throw e
        } catch (e: Exception) {
            Logger.warning(TAG, "Failed to import the pop-up", e)
            created.forEach { it.delete() }
            PopupImportResult.FAILED
        }
    }

    private class TooBigException : Exception()

    private fun importImage(context: Context, uri: Uri, id: String, created: MutableList<File>): TokitoPopup {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = max(bounds.outWidth, bounds.outHeight)
        require(longest > 0) { "This is not an image" }

        //decode at a size close to the final one, so a big photo does not fill the memory
        var sample = 1
        while (longest / (sample * 2) >= MAX_IMAGE_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Could not read the image")

        var bitmap = decoded
        val rotation = readRotation(context, uri)
        if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        bitmap = bitmap.scaledToFit(MAX_IMAGE_SIDE)

        val withAlpha = bitmap.hasAlpha()
        val name = "$id.${if (withAlpha) "png" else "jpg"}"
        val out = File(dir, name).also { created += it }
        out.outputStream().use { sink ->
            bitmap.compress(
                if (withAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                88,
                sink
            )
        }
        val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        if (bitmap !== decoded) decoded.recycle()
        bitmap.recycle()

        return newPopup(id = id, isVideo = false, file = name, thumb = null, aspect = aspect)
    }

    private fun importVideo(context: Context, uri: Uri, id: String, created: MutableList<File>): TokitoPopup {
        val name = "$id.mp4"
        val out = File(dir, name).also { created += it }

        val input = context.contentResolver.openInputStream(uri) ?: error("Could not open the video")
        input.use { source ->
            out.outputStream().use { sink ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_VIDEO_BYTES) throw TooBigException()
                    sink.write(buffer, 0, read)
                }
            }
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(out.absolutePath)
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: error("Could not read a frame of the video")

            //the frame already comes turned the way the video is shown
            val aspect = frame.width.toFloat() / frame.height.toFloat()
            val thumbName = "${id}_thumb.jpg"
            val thumbFile = File(dir, thumbName).also { created += it }
            val small = frame.scaledToFit(MAX_THUMB_SIDE)
            thumbFile.outputStream().use { sink -> small.compress(Bitmap.CompressFormat.JPEG, 80, sink) }
            if (small !== frame) small.recycle()
            frame.recycle()

            return newPopup(id = id, isVideo = true, file = name, thumb = thumbName, aspect = aspect)
        } finally {
            retriever.release()
        }
    }

    private fun newPopup(id: String, isVideo: Boolean, file: String, thumb: String?, aspect: Float): TokitoPopup {
        //new pop-ups start along the right side, one below the other
        val slot = popups.size % 4
        return TokitoPopup(
            id = id,
            isVideo = isVideo,
            file = file,
            thumb = thumb,
            aspect = aspect.coerceIn(0.2f, 5f),
            size = 1,
            x = 0.94f,
            y = 0.08f + slot * 0.27f
        )
    }

    private fun readRotation(context: Context, uri: Uri): Float = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    } catch (_: Exception) {
        0f
    }

    private fun Bitmap.scaledToFit(maxSide: Int): Bitmap {
        val longest = max(width, height)
        if (longest <= maxSide) return this
        val ratio = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            this,
            (width * ratio).roundToInt().coerceAtLeast(1),
            (height * ratio).roundToInt().coerceAtLeast(1),
            true
        )
    }

    private fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

    private fun readIndex(): List<TokitoPopup> {
        val file = indexFile
        if (!file.isFile) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            val list = mutableListOf<TokitoPopup>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val name = item.optString("file")
                //the file is gone: forget the pop-up
                if (name.isBlank() || !File(dir, name).isFile) continue
                list += TokitoPopup(
                    id = item.optString("id").ifBlank { newId() },
                    isVideo = item.optBoolean("video"),
                    file = name,
                    thumb = item.optString("thumb").takeIf { it.isNotBlank() },
                    aspect = item.optDouble("aspect", 1.0).toFloat().coerceIn(0.2f, 5f),
                    size = item.optInt("size", 1).coerceIn(0, 2),
                    x = item.optDouble("x", 0.94).toFloat().coerceIn(0f, 1f),
                    y = item.optDouble("y", 0.2).toFloat().coerceIn(0f, 1f)
                )
            }
            list.take(MAX_POPUPS)
        } catch (e: Exception) {
            Logger.warning(TAG, "Failed to read the pop-ups", e)
            emptyList()
        }
    }

    @Synchronized
    private fun writeIndex() {
        try {
            val array = JSONArray()
            popups.toList().forEach { popup ->
                array.put(
                    JSONObject().apply {
                        put("id", popup.id)
                        put("video", popup.isVideo)
                        put("file", popup.file)
                        put("thumb", popup.thumb ?: "")
                        put("aspect", popup.aspect.toDouble())
                        put("size", popup.size)
                        put("x", popup.x.toDouble())
                        put("y", popup.y.toDouble())
                    }
                )
            }
            dir.mkdirs()
            //written to a temporary file first, so a crash in the middle never leaves a broken list
            val temp = File(dir, "popups.json.tmp")
            temp.writeText(array.toString())
            if (!temp.renameTo(indexFile)) {
                indexFile.writeText(array.toString())
                temp.delete()
            }
        } catch (e: Exception) {
            Logger.warning(TAG, "Failed to save the pop-ups", e)
        }
    }
}
