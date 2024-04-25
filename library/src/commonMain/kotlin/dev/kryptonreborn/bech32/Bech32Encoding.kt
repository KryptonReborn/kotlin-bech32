package dev.kryptonreborn.bech32

/**
 * Enumeration of known Bech32 encoding format types: Bech32 and Bech32m.
 */
enum class Bech32Encoding(val checksum: Int) {
    BECH32(1),
    BECH32M(0x2bc830a3)
}
