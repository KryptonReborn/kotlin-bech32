package dev.kryptonreborn.bech32

import kotlinx.io.Buffer
import kotlinx.io.readByteArray

/**
 * Implementation of the Bech32 encoding.
 *
 * Based on the implementation in bitcoinj
 * @see <a href="https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/base/Bech32.java">Bech32</a>
 */
open class Bech32 {
    companion object : Bech32()

    /**
     * The Bech32 character set for encoding.
     */
    private val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    /**
     * The Bech32 character set for decoding.
     */
    private val CHARSET_REV = byteArrayOf(
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        15, -1, 10, 17, 21, 20, 26, 30, 7, 5, -1, -1, -1, -1, -1, -1,
        -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
        1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1,
        -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
        1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1
    )

    /**
     * Encode a byte array to a Bech32 string
     *
     * @param encoding Desired encoding Bech32 or Bech32m
     * @param hrp human-readable part to use for encoding
     * @param bytes Arbitrary binary data (8-bits per byte)
     *
     * @return A Bech32 string
     */
    fun encodeBytes(encoding: Bech32Encoding, hrp: String, bytes: ByteArray): String {
        return encode(encoding, hrp, convertBits(bytes, fromBits = 8, toBits = 5, pad = true))
    }

    /**
     * Encode a Bech32 string.
     *
     * @param bech32 Contains 5-bits/byte data, desired encoding and human-readable part
     *
     * @return A string containing the Bech32-encoded data
     */
    fun encode(bech32: Bech32Data): String {
        return encode(bech32.encoding, bech32.hrp, bech32.data)
    }

    /**
     * Encode a Bech32 string.
     *
     * @param encoding The requested encoding
     * @param hrp The requested human-readable part
     * @param values Binary data in 5-bit per byte format
     *
     * @return A string containing the Bech32-encoded data
     */
    fun encode(encoding: Bech32Encoding, hrp: String, values: ByteArray): String {
        require(hrp.isNotEmpty()) { "human-readable part is too short: " + hrp.length }
        require(hrp.length <= 83) { "human-readable part is too long: " + hrp.length }

        val lcHrp = hrp.lowercase()
        val checksum = createChecksum(encoding, lcHrp, values)
        val combined = ByteArray(values.size + checksum.size).apply {
            values.copyInto(this)
            checksum.copyInto(this, values.size)
        }
        return buildString(lcHrp.length + 1 + combined.size) {
            append(lcHrp)
            append(1)
            for (b in combined) {
                append(CHARSET[b.toInt()])
            }
        }
    }

    /**
     * Decode a Bech32 string.
     *
     * To get the fully-decoded data, call [Bech32Data.decode5to8] on the returned `Bech32Data`.
     *
     * @param str A string containing Bech32-encoded data
     *
     * @return An object with the detected encoding, hrp, and decoded data (in 5-bit per byte format)
     */
    fun decode(str: String): Bech32Data {
        require(str.length >= 8) { Bech32Exception("Input too short: " + str.length) }
        require(str.length <= 90) { Bech32Exception("Input too long: " + str.length) }

        var lower = false
        var upper = false
        for (i in str.indices) {
            val c = str[i]
            if (c.code < 33 || c.code > 126) throw Bech32Exception("Invalid character '$c' at position $i")
            if (c in 'a'..'z') {
                if (upper) throw Bech32Exception("Invalid character '$c' at position $i")
                lower = true
            }
            if (c in 'A'..'Z') {
                if (lower) throw Bech32Exception("Invalid character '$c' at position $i")
                upper = true
            }
        }
        val pos = str.lastIndexOf('1')
        if (pos < 1) throw Bech32Exception("Missing human-readable part")
        val dataPartLength = str.length - 1 - pos
        if (dataPartLength < 6) throw Bech32Exception("Data part too short: $dataPartLength")
        val values = ByteArray(dataPartLength)
        for (i in 0 until dataPartLength) {
            val c = str[i + pos + 1]
            if (CHARSET_REV[c.code].toInt() == -1) throw Bech32Exception("Invalid character '$c' at position ${i + pos + 1}")
            values[i] = CHARSET_REV[c.code]
        }
        val hrp = str.substring(0, pos).lowercase()
        val encoding = verifyChecksum(hrp, values) ?: throw Bech32Exception("Checksum does not validate")
        return Bech32Data(encoding, hrp, values.copyOfRange(0, values.size - 6))
    }

    /**
     * Decode a Bech32 string to a byte array.
     *
     * @param bech32 A Bech32 format string
     * @param expectedHrp Expected value for the human-readable part
     * @param expectedEncoding Expected encoding
     *
     * @return Decoded value as byte array (8-bits per byte)
     */
    fun decodeBytes(bech32: String, expectedHrp: String, expectedEncoding: Bech32Encoding): ByteArray {
        val decoded = decode(bech32)
        if (decoded.hrp != expectedHrp || decoded.encoding !== expectedEncoding) {
            throw Bech32Exception("Unexpected hrp or encoding")
        }
        return decoded.decode5to8()
    }

    /** Create a checksum.  */
    private fun createChecksum(encoding: Bech32Encoding, hrp: String, values: ByteArray): ByteArray {
        val hrpExpanded = expandHrp(hrp)
        val enc = ByteArray(hrpExpanded.size + values.size + 6).apply {
            hrpExpanded.copyInto(this)
            values.copyInto(this, hrpExpanded.size)
        }
        val mod = polymod(enc) xor encoding.checksum
        val ret = ByteArray(6)
        for (i in 0..5) {
            ret[i] = ((mod ushr (5 * (5 - i))) and 31).toByte()
        }
        return ret
    }

    /** Verify a checksum.  */
    private fun verifyChecksum(hrp: String, values: ByteArray): Bech32Encoding? {
        val hrpExpanded = expandHrp(hrp)
        val combined = ByteArray(hrpExpanded.size + values.size).apply {
            hrpExpanded.copyInto(this)
            values.copyInto(this, hrpExpanded.size)
        }
        return when (polymod(combined)) {
            Bech32Encoding.BECH32.checksum -> Bech32Encoding.BECH32
            Bech32Encoding.BECH32M.checksum -> Bech32Encoding.BECH32M
            else -> null
        }
    }

    /** Expand a HRP for use in checksum computation.  */
    private fun expandHrp(hrp: String): ByteArray {
        val hrpLength = hrp.length
        val ret = ByteArray(hrpLength * 2 + 1)
        for (i in 0 until hrpLength) {
            val c = hrp[i].code and 0x7f // Limit to standard 7-bit ASCII
            ret[i] = ((c ushr 5) and 0x07).toByte()
            ret[i + hrpLength + 1] = (c and 0x1f).toByte()
        }
        ret[hrpLength] = 0
        return ret
    }

    /** Find the polynomial with value coefficients mod the generator as 30-bit.  */
    private fun polymod(values: ByteArray): Int {
        var c = 1
        for (v_i in values) {
            val c0 = (c ushr 25) and 0xff
            c = ((c and 0x1ffffff) shl 5) xor (v_i.toInt() and 0xff)
            if ((c0 and 1) != 0) c = c xor 0x3b6a57b2
            if ((c0 and 2) != 0) c = c xor 0x26508e6d
            if ((c0 and 4) != 0) c = c xor 0x1ea119fa
            if ((c0 and 8) != 0) c = c xor 0x3d4233dd
            if ((c0 and 16) != 0) c = c xor 0x2a1462b3
        }
        return c
    }

    /**
     * Helper for re-arranging bits into groups.
     */
    internal fun convertBits(
        `in`: ByteArray,
        inStart: Int = 0,
        inLen: Int = `in`.size,
        fromBits: Int,
        toBits: Int,
        pad: Boolean,
    ): ByteArray {
        var acc = 0
        var bits = 0
        val out = Buffer()
        val maxv = (1 shl toBits) - 1
        val max_acc = (1 shl (fromBits + toBits - 1)) - 1
        for (i in 0 until inLen) {
            val value = `in`[i + inStart].toInt() and 0xff
            if ((value ushr fromBits) != 0) {
                throw Bech32Exception("Input value '$value' exceeds '$fromBits' bit size")
            }
            acc = ((acc shl fromBits) or value) and max_acc
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                out.writeByte(((acc ushr bits) and maxv).toByte())
            }
        }
        if (pad) {
            if (bits > 0) out.writeByte(((acc shl (toBits - bits)) and maxv).toByte())
        } else if (bits >= fromBits || ((acc shl (toBits - bits)) and maxv) != 0) {
            throw Bech32Exception("Could not convert bits, invalid padding")
        }
        return out.readByteArray()
    }
}
