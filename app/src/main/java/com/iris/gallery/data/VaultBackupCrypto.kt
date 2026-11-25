package com.iris.gallery.data

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private val VAULT_ARCHIVE_MAGIC = byteArrayOf(
    'I'.code.toByte(),
    'R'.code.toByte(),
    'I'.code.toByte(),
    'S'.code.toByte(),
    'V'.code.toByte(),
    'L'.code.toByte(),
    'T'.code.toByte(),
    '1'.code.toByte(),
)
private const val VAULT_ARCHIVE_VERSION: Byte = 1
private const val VAULT_KDF_ITERATIONS = 200_000
private const val VAULT_SALT_BYTES = 16
private const val VAULT_NONCE_BYTES = 12
private const val VAULT_GCM_TAG_BITS = 128
private const val VAULT_GCM_TAG_BYTES = 16
private const val VAULT_CHUNK_SIZE = 65_536
private const val VAULT_HEADER_SIZE = 8 + 1 + 4 + VAULT_SALT_BYTES + VAULT_NONCE_BYTES + 4 // 45 bytes
private const val CHUNK_FLAG_INTERMEDIATE: Byte = 0
private const val CHUNK_FLAG_FINAL: Byte = 1

class VaultWrongPasswordException(cause: Throwable? = null) :
    IOException("Incorrect vault backup password", cause)

class VaultInvalidArchiveException(message: String, cause: Throwable? = null) :
    IOException(message, cause)

object VaultBackupCrypto {

    fun isVaultBackupHeader(input: InputStream): Boolean {
        val header = ByteArray(VAULT_ARCHIVE_MAGIC.size)
        var offset = 0
        while (offset < header.size) {
            val read = input.read(header, offset, header.size - offset)
            if (read <= 0) return false
            offset += read
        }
        return header.contentEquals(VAULT_ARCHIVE_MAGIC)
    }

    internal fun deriveKeyBytes(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    internal fun buildHeader(salt: ByteArray, baseNonce: ByteArray, iterations: Int, chunkSize: Int): ByteArray {
        val buf = ByteBuffer.allocate(VAULT_HEADER_SIZE)
        buf.put(VAULT_ARCHIVE_MAGIC)
        buf.put(VAULT_ARCHIVE_VERSION)
        buf.putInt(iterations)
        buf.put(salt)
        buf.put(baseNonce)
        buf.putInt(chunkSize)
        return buf.array()
    }

    internal fun chunkNonce(baseNonce: ByteArray, chunkIndex: Long): ByteArray {
        val nonce = baseNonce.copyOf()
        for (i in 0 until 8) {
            val shift = (7 - i) * 8
            val idxByte = ((chunkIndex ushr shift) and 0xFFL).toInt()
            nonce[4 + i] = (nonce[4 + i].toInt() xor idxByte).toByte()
        }
        return nonce
    }

    internal fun chunkAad(headerBytes: ByteArray, chunkIndex: Long, isFinal: Boolean): ByteArray {
        val buf = ByteBuffer.allocate(headerBytes.size + 8 + 1)
        buf.put(headerBytes)
        buf.putLong(chunkIndex)
        buf.put(if (isFinal) CHUNK_FLAG_FINAL else CHUNK_FLAG_INTERMEDIATE)
        return buf.array()
    }
}

/**
 * Streaming authenticated encryption (chunked AES-256-GCM) with PBKDF2-HMAC-SHA256 key derivation.
 * Each 64 KiB chunk uses a unique IV derived from [baseNonce] XOR chunk index and binds the
 * header, chunk index, and final-chunk flag via AAD to prevent tampering, reordering, or truncation.
 */
class ChunkedGcmOutputStream(
    rawOut: OutputStream,
    password: CharArray,
) : OutputStream() {
    private val out = DataOutputStream(rawOut)
    private val keyBytes: ByteArray
    private val keySpec: SecretKeySpec
    private val baseNonce = ByteArray(VAULT_NONCE_BYTES)
    private val headerBytes: ByteArray
    private val buffer = ByteArray(VAULT_CHUNK_SIZE)
    private var buffered = 0
    private var chunkIndex = 0L
    private var closed = false

    init {
        val rng = SecureRandom()
        val salt = ByteArray(VAULT_SALT_BYTES).also { rng.nextBytes(it) }
        rng.nextBytes(baseNonce)
        keyBytes = VaultBackupCrypto.deriveKeyBytes(password, salt, VAULT_KDF_ITERATIONS)
        keySpec = SecretKeySpec(keyBytes, "AES")
        headerBytes = VaultBackupCrypto.buildHeader(salt, baseNonce, VAULT_KDF_ITERATIONS, VAULT_CHUNK_SIZE)
        out.write(headerBytes)
    }

    override fun write(b: Int) {
        checkOpen()
        buffer[buffered++] = b.toByte()
        if (buffered == buffer.size) {
            writeChunk(isFinal = false)
        }
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        checkOpen()
        var offset = off
        var remaining = len
        while (remaining > 0) {
            val space = buffer.size - buffered
            val toCopy = minOf(space, remaining)
            System.arraycopy(b, offset, buffer, buffered, toCopy)
            buffered += toCopy
            offset += toCopy
            remaining -= toCopy
            if (buffered == buffer.size) {
                writeChunk(isFinal = false)
            }
        }
    }

    override fun flush() {
        if (!closed) {
            out.flush()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            writeChunk(isFinal = true)
            out.flush()
        } finally {
            keyBytes.fill(0)
            buffer.fill(0)
            runCatching { out.close() }
        }
    }

    private fun checkOpen() {
        if (closed) throw IOException("Stream already closed")
    }

    private fun writeChunk(isFinal: Boolean) {
        val nonce = VaultBackupCrypto.chunkNonce(baseNonce, chunkIndex)
        val aad = VaultBackupCrypto.chunkAad(headerBytes, chunkIndex, isFinal)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(VAULT_GCM_TAG_BITS, nonce))
        cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(buffer, 0, buffered)
        out.writeByte(if (isFinal) CHUNK_FLAG_FINAL.toInt() else CHUNK_FLAG_INTERMEDIATE.toInt())
        out.writeInt(ciphertext.size)
        out.write(ciphertext)
        buffered = 0
        chunkIndex++
    }
}

/**
 * Streaming authenticated decryptor matching [ChunkedGcmOutputStream].
 * Rejects wrong passwords immediately on chunk 0 and rejects truncated or reordered streams.
 */
class ChunkedGcmInputStream(
    rawIn: InputStream,
    password: CharArray,
) : InputStream() {
    private val input = DataInputStream(rawIn)
    private val headerBytes = ByteArray(VAULT_HEADER_SIZE)
    private val keyBytes: ByteArray
    private val keySpec: SecretKeySpec
    private val baseNonce = ByteArray(VAULT_NONCE_BYTES)
    private val maxChunkSize: Int

    private var plainChunk = ByteArray(0)
    private var plainPos = 0
    private var chunkIndex = 0L
    private var reachedFinal = false
    private var closed = false

    init {
        try {
            input.readFully(headerBytes)
        } catch (err: EOFException) {
            throw VaultInvalidArchiveException("File is not a valid Iris Vault backup", err)
        }
        val buf = ByteBuffer.wrap(headerBytes)
        val magic = ByteArray(VAULT_ARCHIVE_MAGIC.size)
        buf.get(magic)
        if (!magic.contentEquals(VAULT_ARCHIVE_MAGIC)) {
            throw VaultInvalidArchiveException("File is not a valid Iris Vault backup")
        }
        val version = buf.get()
        if (version != VAULT_ARCHIVE_VERSION) {
            throw VaultInvalidArchiveException("Unsupported vault backup version: $version")
        }
        val iterations = buf.int
        if (iterations !in 10_000..2_000_000) {
            throw VaultInvalidArchiveException("Invalid key derivation parameters")
        }
        val salt = ByteArray(VAULT_SALT_BYTES)
        buf.get(salt)
        buf.get(baseNonce)
        maxChunkSize = buf.int
        if (maxChunkSize !in 4_096..1_048_576) {
            throw VaultInvalidArchiveException("Invalid chunk size")
        }
        keyBytes = VaultBackupCrypto.deriveKeyBytes(password, salt, iterations)
        keySpec = SecretKeySpec(keyBytes, "AES")
    }

    override fun read(): Int {
        val one = ByteArray(1)
        val n = read(one, 0, 1)
        return if (n < 0) -1 else (one[0].toInt() and 0xFF)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (closed) throw IOException("Stream closed")
        if (len == 0) return 0
        while (plainPos >= plainChunk.size) {
            if (reachedFinal) return -1
            readNextChunk()
        }
        val available = plainChunk.size - plainPos
        val toCopy = minOf(available, len)
        System.arraycopy(plainChunk, plainPos, b, off, toCopy)
        plainPos += toCopy
        return toCopy
    }

    /**
     * Drains any trailing bytes until the authenticated final chunk is verified so truncated
     * archives cannot slip past callers that stop reading early (e.g. ZipInputStream EOF).
     */
    fun verifyEndOfStream() {
        while (!reachedFinal) {
            readNextChunk()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        keyBytes.fill(0)
        plainChunk.fill(0)
        runCatching { input.close() }
    }

    private fun readNextChunk() {
        val flagByte = try {
            input.readByte()
        } catch (err: EOFException) {
            throw VaultInvalidArchiveException("Vault backup is truncated or incomplete", err)
        }
        val isFinal = when (flagByte) {
            CHUNK_FLAG_INTERMEDIATE -> false
            CHUNK_FLAG_FINAL -> true
            else -> throw VaultInvalidArchiveException("Corrupt vault chunk flag")
        }
        val cipherLen = try {
            input.readInt()
        } catch (err: EOFException) {
            throw VaultInvalidArchiveException("Vault backup is truncated", err)
        }
        val maxCipherLen = maxChunkSize + VAULT_GCM_TAG_BYTES
        if (cipherLen < VAULT_GCM_TAG_BYTES || cipherLen > maxCipherLen) {
            throw VaultInvalidArchiveException("Corrupt vault chunk length: $cipherLen")
        }
        val cipherBytes = ByteArray(cipherLen)
        try {
            input.readFully(cipherBytes)
        } catch (err: EOFException) {
            throw VaultInvalidArchiveException("Vault backup is truncated", err)
        }

        val nonce = VaultBackupCrypto.chunkNonce(baseNonce, chunkIndex)
        val aad = VaultBackupCrypto.chunkAad(headerBytes, chunkIndex, isFinal)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(VAULT_GCM_TAG_BITS, nonce))
        cipher.updateAAD(aad)
        val decrypted = try {
            cipher.doFinal(cipherBytes)
        } catch (err: AEADBadTagException) {
            if (chunkIndex == 0L) {
                throw VaultWrongPasswordException(err)
            } else {
                throw VaultInvalidArchiveException("Vault backup is corrupted or tampered with", err)
            }
        }
        plainChunk = decrypted
        plainPos = 0
        chunkIndex++
        if (isFinal) {
            reachedFinal = true
        }
    }
}
