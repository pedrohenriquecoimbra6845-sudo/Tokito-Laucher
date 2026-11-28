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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "TokitoHomeMedia"

/**
 * Tokito Launcher: the pictures and the video that the player chooses for the home screen
 * (the background of the small PLAY card and the Mini Wallpaper).
 *
 * Every chosen file is copied into the folder of the launcher, so it keeps working when the original is moved or
 * deleted. A picture is scaled down while it is copied (a big photo of the camera would only waste memory), and
 * a video is limited in size. Every choice gets a new file name, so the screen never shows an old, cached picture.
 */
object TokitoHomeMedia {
    /** Slot of the background of the PLAY card */
    const val SLOT_HERO = "hero"

    /** Slot of the horizontal Mini Wallpaper (the one with the greeting) */
    const val SLOT_WALL = "wall"

    /** Slot of the vertical Mini Wallpaper ("wallv_" never matches the files of "wall_") */
    const val SLOT_WALL_TALL = "wallv"

    /** Biggest video that is accepted, in megabytes */
    const val MAX_VIDEO_MB = 60

    /** Biggest GIF that is accepted, in megabytes */
    const val MAX_GIF_MB = 30

    private const val MAX_VIDEO_BYTES = MAX_VIDEO_MB * 1024L * 1024L
    private const val MAX_GIF_BYTES = MAX_GIF_MB * 1024L * 1024L
    private const val HERO_MAX_SIDE = 1280
    private const val WALL_MAX_SIDE = 1600

    enum class Status { OK, TOO_BIG, FAILED }

    /** The result of a choice: [name] is the new file name when [status] is [Status.OK] */
    class Imported(val status: Status, val name: String? = null)

    val dir: File
        get() = File(PathManager.DIR_FILES_PRIVATE, "tokito_home")

    /** The file with this name (null when the name is empty, tries to leave the folder or the file is gone) */
    fun fileOf(name: String): File? {
        if (name.isBlank() || name.contains('/') || name.contains("..")) return null
        return File(dir, name).takeIf { it.isFile }
    }

    /** Deletes what the slot had (the pictures and the videos that came from it) */
    fun clear(slot: String) {
        dir.listFiles()?.forEach { file ->
            if (file.name.startsWith("${slot}_")) file.delete()
        }
    }

    /** Copies a photo that the player picked, scaled down, and returns its new name */
    suspend fun importPhoto(context: Context, uri: Uri, slot: String): Imported = withContext(Dispatchers.IO) {
        try {
            dir.mkdirs()
            val maxSide = if (slot == SLOT_HERO) HERO_MAX_SIDE else WALL_MAX_SIDE
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val longest = max(bounds.outWidth, bounds.outHeight)
            if (longest <= 0) return@withContext Imported(Status.FAILED)

            //decode at a size close to the final one, so a big photo does not fill the memory
            var sample = 1
            while (longest / (sample * 2) >= maxSide) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@withContext Imported(Status.FAILED)

            val rotation = readRotation(context, uri)
            val rotated = if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            } else {
                decoded
            }
            val scaled = rotated.scaledToFit(maxSide)

            val withAlpha = scaled.hasAlpha()
            val name = "${slot}_${System.currentTimeMillis()}.${if (withAlpha) "png" else "jpg"}"
            File(dir, name).outputStream().use { sink ->
                scaled.compress(
                    if (withAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                    90,
                    sink
                )
            }
            //identity set: the same bitmap is never recycled twice
            setOf(decoded, rotated, scaled).forEach { if (!it.isRecycled) it.recycle() }

            removeOld(slot, keep = name)
            Imported(Status.OK, name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Logger.warning(TAG, "Failed to import the photo", e)
            Imported(Status.FAILED)
        }
    }

    /** Copies a video that the player picked (up to [MAX_VIDEO_MB] MB) and returns its new name */
    suspend fun importVideo(context: Context, uri: Uri, slot: String): Imported = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val name = "${slot}_${System.currentTimeMillis()}.mp4"
        val out = File(dir, name)
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext Imported(Status.FAILED)
            var tooBig = false
            input.use { source ->
                out.outputStream().use { sink ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_VIDEO_BYTES) {
                            tooBig = true
                            break
                        }
                        sink.write(buffer, 0, read)
                    }
                }
            }
            if (tooBig) {
                out.delete()
                return@withContext Imported(Status.TOO_BIG)
            }

            //a file that is not a video must not be kept
            val retriever = MediaMetadataRetriever()
            val isVideo = try {
                retriever.setDataSource(out.absolutePath)
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) != null
            } finally {
                retriever.release()
            }
            if (!isVideo) {
                out.delete()
                return@withContext Imported(Status.FAILED)
            }

            removeOld(slot, keep = name)
            Imported(Status.OK, name)
        } catch (e: CancellationException) {
            out.delete()
            throw e
        } catch (e: Throwable) {
            out.delete()
            Logger.warning(TAG, "Failed to import the video", e)
            Imported(Status.FAILED)
        }
    }

    /** Copies a GIF that the player picked (up to [MAX_GIF_MB] MB) and returns its new name */
    suspend fun importGif(context: Context, uri: Uri, slot: String): Imported = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val name = "${slot}_${System.currentTimeMillis()}.gif"
        val out = File(dir, name)
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext Imported(Status.FAILED)
            var tooBig = false
            input.use { source ->
                out.outputStream().use { sink ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_GIF_BYTES) {
                            tooBig = true
                            break
                        }
                        sink.write(buffer, 0, read)
                    }
                }
            }
            if (tooBig) {
                out.delete()
                return@withContext Imported(Status.TOO_BIG)
            }

            //a file that is not a GIF must not be kept: every GIF starts with GIF87a or GIF89a
            val header = ByteArray(6)
            val read = out.inputStream().use { it.read(header) }
            val magic = if (read == header.size) String(header, Charsets.US_ASCII) else ""
            if (magic != "GIF87a" && magic != "GIF89a") {
                out.delete()
                return@withContext Imported(Status.FAILED)
            }

            removeOld(slot, keep = name)
            Imported(Status.OK, name)
        } catch (e: CancellationException) {
            out.delete()
            throw e
        } catch (e: Throwable) {
            out.delete()
            Logger.warning(TAG, "Failed to import the GIF", e)
            Imported(Status.FAILED)
        }
    }

    /** Copies what the player picked, whatever it is: a GIF, a video or a photo */
    suspend fun importAny(context: Context, uri: Uri, slot: String): Imported {
        val type = context.contentResolver.getType(uri).orEmpty().lowercase()
        val path = uri.lastPathSegment.orEmpty().lowercase()
        return when {
            type == "image/gif" || path.endsWith(".gif") -> importGif(context, uri, slot)
            type.startsWith("video/") || path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mkv") ->
                importVideo(context, uri, slot)
            else -> importPhoto(context, uri, slot)
        }
    }

    /** Deletes the older files of a slot (the player now uses the new one) */
    private fun removeOld(slot: String, keep: String) {
        dir.listFiles()?.forEach { file ->
            if (file.name.startsWith("${slot}_") && file.name != keep) file.delete()
        }
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
}
