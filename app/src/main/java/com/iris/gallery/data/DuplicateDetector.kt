package com.iris.gallery.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

data class DuplicateGroup(
    val items: List<MediaImage>,
    val exact: Boolean,
) {
    val reclaimableBytes: Long get() = items.drop(1).sumOf { it.sizeBytes }
}

class DuplicateDetector(private val context: Context) {
    suspend fun scan(
        media: List<MediaImage>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val photos = media.filter { !it.isVideo && it.sizeBytes > 0 }
        val exactGroups = mutableListOf<DuplicateGroup>()
        val exactIds = mutableSetOf<Long>()

        // Hash only plausible exact candidates. This avoids reading every original.
        photos.groupBy { Triple(it.sizeBytes, it.width, it.height) }.values
            .filter { it.size > 1 }
            .forEach { candidates ->
                candidates.mapNotNull { item -> checksum(item)?.let { it to item } }
                    .groupBy({ it.first }, { it.second }).values
                    .filter { it.size > 1 }
                    .forEach { group ->
                        exactGroups += DuplicateGroup(group.sortedByDescending { it.dateTaken }, exact = true)
                        exactIds += group.map { it.id }
                    }
            }

        val remaining = photos.filterNot { it.id in exactIds }
        val fingerprints = ArrayList<Pair<MediaImage, Long>>(remaining.size)
        remaining.forEachIndexed { index, item ->
            coroutineContext.ensureActive()
            fingerprint(item)?.let { fingerprints += item to it }
            onProgress(index + 1, remaining.size)
        }

        // Union visually close fingerprints. A strict threshold limits false positives.
        val parent = IntArray(fingerprints.size) { it }
        fun root(value: Int): Int {
            var i = value
            while (parent[i] != i) { parent[i] = parent[parent[i]]; i = parent[i] }
            return i
        }
        fun union(a: Int, b: Int) { val ra = root(a); val rb = root(b); if (ra != rb) parent[rb] = ra }
        // Six independent bands guarantee that fingerprints within five bit
        // changes share at least one band (pigeonhole principle), avoiding O(n²).
        val bands = HashMap<Long, MutableList<Int>>()
        for (i in fingerprints.indices) {
            val candidates = hashSetOf<Int>()
            for (band in 0 until 6) {
                val start = band * 11
                val width = if (band == 5) 9 else 11
                val mask = (1L shl width) - 1
                val key = (band.toLong() shl 56) or ((fingerprints[i].second ushr start) and mask)
                bands[key]?.let(candidates::addAll)
            }
            for (j in candidates) {
                val a = fingerprints[i].first
                val b = fingerprints[j].first
                val sameShape = kotlin.math.abs(a.width.toFloat() / a.height.coerceAtLeast(1) -
                    b.width.toFloat() / b.height.coerceAtLeast(1)) < 0.035f
                if (sameShape && java.lang.Long.bitCount(fingerprints[i].second xor fingerprints[j].second) <= 5) {
                    union(i, j)
                }
            }
            for (band in 0 until 6) {
                val start = band * 11
                val width = if (band == 5) 9 else 11
                val mask = (1L shl width) - 1
                val key = (band.toLong() shl 56) or ((fingerprints[i].second ushr start) and mask)
                bands.getOrPut(key) { mutableListOf() }.add(i)
            }
        }
        val similarGroups = fingerprints.indices.groupBy { root(it) }.values
            .filter { it.size > 1 }
            .map { indexes ->
                DuplicateGroup(indexes.map { fingerprints[it].first }
                    .sortedWith(compareByDescending<MediaImage> { it.width.toLong() * it.height }.thenByDescending { it.dateTaken }), exact = false)
            }
        (exactGroups + similarGroups).sortedByDescending { it.reclaimableBytes }
    }

    private fun checksum(item: MediaImage): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(item.uri)?.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        } ?: return null
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    private fun fingerprint(item: MediaImage): Long? = runCatching {
        val bitmap = context.contentResolver.loadThumbnail(item.uri, Size(9, 8), null)
        val scaled = if (bitmap.width == 9 && bitmap.height == 8) bitmap
            else Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash = 0L
        var bit = 0
        for (y in 0 until 8) for (x in 0 until 8) {
            val left = scaled.getPixel(x, y)
            val right = scaled.getPixel(x + 1, y)
            fun luma(c: Int) = ((c shr 16 and 255) * 299 + (c shr 8 and 255) * 587 + (c and 255) * 114)
            if (luma(left) > luma(right)) hash = hash or (1L shl bit)
            bit++
        }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        hash
    }.getOrNull()
}
