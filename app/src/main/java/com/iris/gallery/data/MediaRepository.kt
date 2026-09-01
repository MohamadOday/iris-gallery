package com.iris.gallery.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.AtomicFile
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

class MediaRepository(private val context: Context) {
    private val snapshot = AtomicFile(File(context.filesDir, "media_snapshot.bin"))

    fun loadSnapshot(): List<MediaImage> = runCatching {
        DataInputStream(snapshot.openRead().buffered()).use { input ->
            if (input.readInt() != 2) return@use emptyList()
            List(input.readInt().coerceIn(0, 100_000)) {
                val id = input.readLong(); val isVideo = input.readBoolean()
                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                MediaImage(id, ContentUris.withAppendedId(collection, id), input.readUTF(), input.readLong(),
                    input.readInt(), input.readInt(), input.readUTF(), input.readLong(), input.readUTF(),
                    isVideo, input.readLong(), input.readUTF(), input.readLong(), input.readInt(), input.readUTF())
            }
        }
    }.getOrDefault(emptyList())

    suspend fun loadImages(trashed: Boolean = false): List<MediaImage> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Video.VideoColumns.DURATION,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.MediaColumns.TITLE,
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                MediaStore.Images.Media.RELATIVE_PATH
            } else {
                MediaStore.Images.Media.DATA
            },
        )

        val result = buildList {
            val mediaSelection = buildString {
                append("(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)")
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    if (trashed) {
                        append(" AND ${MediaStore.MediaColumns.IS_TRASHED} = 1")
                    } else {
                        append(" AND ${MediaStore.MediaColumns.IS_TRASHED} = 0 AND ${MediaStore.MediaColumns.IS_PENDING} = 0")
                    }
                } else if (android.os.Build.VERSION.SDK_INT >= 29) {
                    append(" AND ${MediaStore.MediaColumns.IS_PENDING} = 0")
                }
            }
            val selectionArgs = buildList {
                add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
                add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            }.toTypedArray()
            val order = "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"
            val cursorResult = if (android.os.Build.VERSION.SDK_INT >= 30) {
                context.contentResolver.query(collection, projection, Bundle().apply {
                    putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, mediaSelection)
                    putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, order)
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED,
                        if (trashed) MediaStore.MATCH_ONLY else MediaStore.MATCH_EXCLUDE)
                }, null)
            } else {
                context.contentResolver.query(collection, projection, mediaSelection, selectionArgs, order)
            }
            cursorResult?.use { cursor ->
                val id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val taken = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val added = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val width = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val height = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val bucketId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val mediaType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val duration = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)
                val mimeType = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val size = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val orientation = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
                val title = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE)
                val path = cursor.getColumnIndexOrThrow(
                    if (android.os.Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.RELATIVE_PATH
                    else MediaStore.Images.Media.DATA,
                )
                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(id)
                    val isVid = cursor.getInt(mediaType) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    val filePath = if (android.os.Build.VERSION.SDK_INT >= 29) {
                        "/storage/emulated/0/${cursor.getString(path).orEmpty()}${cursor.getString(name).orEmpty()}"
                    } else {
                        cursor.getString(path).orEmpty()
                    }
                    val mediaUri = if (isVid) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaId)
                    } else {
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)
                    }
                    if (filePath.isNotBlank()) {
                        val file = File(filePath)
                        if (!file.exists()) {
                            android.media.MediaScannerConnection.scanFile(context, arrayOf(filePath), null, null)
                            if (android.os.Build.VERSION.SDK_INT <= 28) {
                                runCatching { context.contentResolver.delete(mediaUri, null, null) }
                            }
                            continue
                        }
                    }
                    add(
                        MediaImage(
                            id = mediaId,
                            uri = mediaUri,
                            name = cursor.getString(name).orEmpty(),
                            dateTaken = cursor.getLong(taken).takeIf { it > 0 }
                                ?: cursor.getLong(added) * 1_000,
                            width = cursor.getInt(width),
                            height = cursor.getInt(height),
                            path = filePath,
                            bucketId = cursor.getLong(bucketId),
                            bucketName = cursor.getString(bucketName).orEmpty().ifBlank { "Other" },
                            isVideo = isVid,
                            durationMs = cursor.getLong(duration),
                            mimeType = cursor.getString(mimeType).orEmpty(),
                            sizeBytes = cursor.getLong(size),
                            orientation = cursor.getInt(orientation),
                            title = cursor.getString(title).orEmpty(),
                        ),
                    )
                }
            }
        }
        val sorted = result.sortedWith(
            compareByDescending<MediaImage> { it.dateTaken }
                .thenByDescending { it.id }
        )
        if (!trashed) saveSnapshot(sorted)
        sorted
    }

    private fun saveSnapshot(media: List<MediaImage>) {
        var stream: java.io.FileOutputStream? = null
        runCatching {
            stream = snapshot.startWrite()
            val output = DataOutputStream(stream!!.buffered())
            output.writeInt(2); output.writeInt(media.size)
            media.forEach { item ->
                output.writeLong(item.id); output.writeBoolean(item.isVideo); output.writeUTF(item.name.take(8_000))
                output.writeLong(item.dateTaken); output.writeInt(item.width); output.writeInt(item.height)
                output.writeUTF(item.path.take(16_000)); output.writeLong(item.bucketId); output.writeUTF(item.bucketName.take(8_000))
                output.writeLong(item.durationMs); output.writeUTF(item.mimeType.take(1_000)); output.writeLong(item.sizeBytes)
                output.writeInt(item.orientation); output.writeUTF(item.title.take(8_000))
            }
            output.flush()
            snapshot.finishWrite(stream)
        }.onFailure { snapshot.failWrite(stream) }
    }
}
