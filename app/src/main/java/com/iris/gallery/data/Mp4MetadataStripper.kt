package com.iris.gallery.data

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Removes user data and creation timestamps from MP4/MOV files without touching
 * the media samples. Everything is done at the box level, so it stays codec
 * agnostic and works for HEVC, AV1, H.264 and so on.
 *
 * "Strip all" drops the `udta` and `meta` boxes and zeroes the creation and
 * modification times in `mvhd`, `tkhd` and `mdhd`. The location-only mode just
 * drops the `©xyz` and `loci` boxes inside `udta`.
 */
internal object Mp4MetadataStripper {

    private const val MAX_MOOV_BYTES = 64L * 1024L * 1024L

    private val CONTAINER_BOXES = setOf(
        "moov", "trak", "mdia", "minf", "stbl", "dinf", "edts", "mvex", "moof", "traf", "mfra"
    )
    private val TIME_BOXES = setOf("mvhd", "tkhd", "mdhd")
    private val LOCATION_BOXES = setOf("\u00A9xyz", "loci", "\u00A9lat", "\u00A9lon")

    private class Header(val totalSize: Long, val headerSize: Int, val type: String)

    /**
     * Rewrites [input] into [output]. Returns false when the file does not look
     * like a valid MP4/MOV or the rewrite hits an unexpected box layout.
     */
    fun strip(input: InputStream, output: OutputStream, stripAll: Boolean): Boolean {
        return runCatching {
            DataInputStream(BufferedInputStream(input, 64 * 1024)).use { source ->
                DataOutputStream(BufferedOutputStream(output, 64 * 1024)).use { target ->
                    while (true) {
                        val header = readHeader(source) ?: break
                        val payloadSize = payloadSizeOf(header)
                        if (header.type == "moov") {
                            val payload = readPayload(source, payloadSize)
                            writeBox(target, "moov", rewriteContainer(payload, stripAll))
                        } else {
                            writeHeader(target, header)
                            copyPayload(source, target, payloadSize)
                        }
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun rewriteContainer(payload: ByteArray, stripAll: Boolean): ByteArray {
        val source = DataInputStream(ByteArrayInputStream(payload))
        val output = ByteArrayOutputStream(payload.size)
        val target = DataOutputStream(output)

        while (true) {
            val header = readHeader(source) ?: break
            val payloadSize = payloadSizeOf(header)
            if (payloadSize < 0 || payloadSize > source.available()) {
                throw IOException("invalid child box ${header.type}")
            }
            val child = ByteArray(payloadSize.toInt())
            source.readFully(child)

            when {
                stripAll && (header.type == "udta" || header.type == "meta") -> Unit
                stripAll && header.type in LOCATION_BOXES -> Unit
                !stripAll && header.type == "udta" ->
                    writeBox(target, "udta", rewriteContainer(child, stripAll = false))
                !stripAll && header.type in LOCATION_BOXES -> Unit
                stripAll && header.type in TIME_BOXES ->
                    writeBox(target, header.type, zeroTimes(child))
                header.type in CONTAINER_BOXES ->
                    writeBox(target, header.type, rewriteContainer(child, stripAll))
                else -> writeBox(target, header.type, child)
            }
        }

        target.flush()
        return output.toByteArray()
    }

    private fun zeroTimes(payload: ByteArray): ByteArray {
        if (payload.size < 12) return payload
        val copy = payload.copyOf()
        val version = copy[0].toInt() and 0xFF
        if (version == 1) {
            for (index in 4 until minOf(20, copy.size)) copy[index] = 0
        } else {
            for (index in 4 until minOf(12, copy.size)) copy[index] = 0
        }
        return copy
    }

    private fun readHeader(input: DataInputStream): Header? {
        val first = input.read()
        if (first < 0) return null
        val size32 = (first shl 24) or
            (input.readUnsignedByte() shl 16) or
            (input.readUnsignedByte() shl 8) or
            input.readUnsignedByte()
        val typeBytes = ByteArray(4)
        input.readFully(typeBytes)
        val type = String(typeBytes, Charsets.ISO_8859_1)

        return when (size32) {
            0 -> Header(-1L, 8, type)
            1 -> Header(input.readLong(), 16, type)
            else -> Header(size32.toLong() and 0xFFFFFFFFL, 8, type)
        }
    }

    private fun payloadSizeOf(header: Header): Long {
        if (header.totalSize < 0) return -1L
        val size = header.totalSize - header.headerSize
        if (size < 0) throw IOException("invalid box size")
        return size
    }

    private fun readPayload(input: InputStream, payloadSize: Long): ByteArray {
        if (payloadSize < 0) return input.readBytes()
        if (payloadSize > MAX_MOOV_BYTES) throw IOException("moov box too large")
        val bytes = ByteArray(payloadSize.toInt())
        var offset = 0
        while (offset < bytes.size) {
            val read = input.read(bytes, offset, bytes.size - offset)
            if (read < 0) throw EOFException()
            offset += read
        }
        return bytes
    }

    private fun copyPayload(input: InputStream, output: OutputStream, payloadSize: Long) {
        val buffer = ByteArray(64 * 1024)
        if (payloadSize < 0) {
            while (true) {
                val read = input.read(buffer)
                if (read < 0) return
                output.write(buffer, 0, read)
            }
        }
        var remaining = payloadSize
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
            if (read < 0) throw EOFException()
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun writeHeader(output: DataOutputStream, header: Header) {
        when {
            header.headerSize == 16 -> {
                output.writeInt(1)
                writeType(output, header.type)
                output.writeLong(header.totalSize)
            }
            header.totalSize < 0 -> {
                output.writeInt(0)
                writeType(output, header.type)
            }
            else -> {
                output.writeInt(header.totalSize.toInt())
                writeType(output, header.type)
            }
        }
    }

    private fun writeBox(output: DataOutputStream, type: String, payload: ByteArray) {
        output.writeInt(payload.size + 8)
        writeType(output, type)
        output.write(payload)
    }

    private fun writeType(output: DataOutputStream, type: String) {
        output.write(type.toByteArray(Charsets.ISO_8859_1))
    }
}
