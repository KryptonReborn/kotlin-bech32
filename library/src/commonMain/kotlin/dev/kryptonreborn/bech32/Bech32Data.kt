package dev.kryptonreborn.bech32

/**
 * Bech32 data in 5-bit byte format and human-readable part (HRP) information.
 */
data class Bech32Data(
    val encoding: Bech32Encoding,
    val hrp: String,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Bech32Data

        if (encoding != other.encoding) return false
        if (hrp != other.hrp) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encoding.hashCode()
        result = 31 * result + hrp.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Return the data, fully-decoded with 8-bits per byte.
 *
 * @return The data, fully-decoded as a byte array.
 */
fun Bech32Data.decode5to8(): ByteArray = Bech32.convertBits(data, 0, data.size, 5, 8, false)
