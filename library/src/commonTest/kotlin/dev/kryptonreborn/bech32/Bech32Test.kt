package dev.kryptonreborn.bech32

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class Bech32Test {
    @Test
    fun valid_bech32() {
        for (valid in validBech32) valid(valid)
    }

    @Test
    fun valid_bech32m() {
        for (valid in validBech32M) valid(valid)
    }

    private fun valid(valid: String) {
        val bechData = Bech32.decode(valid)
        var recode = Bech32.encode(bechData)
        assertEquals(
            valid.lowercase(),
            recode.lowercase(),
            "Failed to roundtrip '$valid' -> '$recode'",
        )
        // Test encoding with an uppercase HRP
        recode = Bech32.encode(bechData.encoding, bechData.hrp.uppercase(), bechData.data)
        assertEquals(
            valid.lowercase(),
            recode.lowercase(),
            "Failed to roundtrip '$valid' -> '$recode'",
        )
    }

    private val validBech32: Array<String> =
        arrayOf(
            "A12UEL5L",
            "a12uel5l",
            "an83characterlonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1tt5tgs",
            "abcdef1qpzry9x8gf2tvdw0s3jn54khce6mua7lmqqqxw",
            "11qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqc8247j",
            "split1checkupstagehandshakeupstreamerranterredcaperred2y9e3w",
            "?1ezyfcl",
        )
    private val validBech32M: Array<String> =
        arrayOf(
            "A1LQFN3A",
            "a1lqfn3a",
            "an83characterlonghumanreadablepartthatcontainsthetheexcludedcharactersbioandnumber11sg7hg6",
            "abcdef1l7aum6echk45nj3s0wdvt2fg8x9yrzpqzd3ryx",
            "11llllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllludsr8",
            "split1checkupstagehandshakeupstreamerranterredcaperredlc445v",
            "?1v759aa",
        )

    @Test
    fun invalid_bech32() {
        for (invalid in invalidBech32) invalid(invalid)
    }

    @Test
    fun invalid_bech32m() {
        for (invalid in invalidBech32M) invalid(invalid)
    }

    private fun invalid(invalid: String) {
        assertFails("Parsed an invalid code: '$invalid'") {
            Bech32.decode(invalid)
        }
    }

    private val invalidBech32: Array<String> =
        arrayOf(
            // HRP character out of range
            " 1nwldj5",
            // HRP character out of range
            charArrayOf(0x7f.toChar()).concatToString() + "1axkwrx",
            // HRP character out of range
            charArrayOf(0x80.toChar()).concatToString() + "1eym55h",
            // overall max length exceeded
            "an84characterslonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1569pvx",
            // No separator character
            "pzry9x0s0muk",
            // Empty HRP
            "1pzry9x0s0muk",
            // Invalid data character
            "x1b4n0q5v",
            // Too short checksum
            "li1dgmt3",
            // Invalid character in checksum
            "de1lg7wt" + charArrayOf(0xff.toChar()).concatToString(),
            // checksum calculated with uppercase form of HRP
            "A1G7SGD8",
            // empty HRP
            "10a06t8",
            // empty HRP
            "1qzzfhee",
        )

    private val invalidBech32M: Array<String> =
        arrayOf(
            // HRP character out of range
            " 1xj0phk",
            // HRP character out of range
            charArrayOf(0x7f.toChar()).concatToString() + "1g6xzxy",
            // HRP character out of range
            charArrayOf(0x80.toChar()).concatToString() + "1vctc34",
            // overall max length exceeded
            "an84characterslonghumanreadablepartthatcontainsthetheexcludedcharactersbioandnumber11d6pts4",
            // No separator character
            "qyrz8wqd2c9m",
            // Empty HRP
            "1qyrz8wqd2c9m",
            // Invalid data character
            "y1b0jsk6g",
            // Invalid data character
            "lt1igcx5c0",
            // Too short checksum
            "in1muywd",
            // Invalid character in checksum
            "mm1crxm3i",
            // Invalid character in checksum
            "au1s5cgom",
            // checksum calculated with uppercase form of HRP
            "M1VUXWEZ",
            // empty HRP
            "16plkw9",
            // empty HRP
            "1p2gdwpf",
        )

    @Test
    fun encodeBytes() {
        nip19Vectors().forEach { (hex, hrp, expectedBech32) ->
            val bech32 = Bech32.encodeBytes(Bech32Encoding.BECH32, hrp, hex.hexToByteArray())

            assertEquals(expectedBech32, bech32, "incorrect encoding")
        }
    }

    @Test
    fun decodeBytes() {
        nip19Vectors().forEach { (expectedHex, hrp, bech32) ->
            val decoded = Bech32.decode(bech32)
            val decodedData = decoded.decode5to8().toHexString()

            assertEquals(Bech32Encoding.BECH32, decoded.encoding, "incorrect encoding type")
            assertEquals(hrp, decoded.hrp, "incorrect hrp")
            assertEquals(expectedHex, decodedData, "incorrect decoded data")
        }
    }

    @Test
    fun decodeBytes2() {
        nip19Vectors().forEach { (expectedHex, hrp, bech32) ->
            val decoded = Bech32.decodeBytes(bech32, hrp, Bech32Encoding.BECH32)

            assertEquals(expectedHex, decoded.toHexString(), "incorrect decoded data")
        }
    }

    // These vectors are from NIP-19: https://github.com/nostr-protocol/nips/blob/master/19.md
    private fun nip19Vectors() =
        listOf(
            Triple(
                "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
                "npub",
                "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6",
            ),
            Triple(
                "7e7e9c42a91bfef19fa929e5fda1b72e0ebc1a4c1141673e2794234d86addf4e",
                "npub",
                "npub10elfcs4fr0l0r8af98jlmgdh9c8tcxjvz9qkw038js35mp4dma8qzvjptg",
            ),
            Triple(
                "67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa",
                "nsec",
                "nsec1vl029mgpspedva04g90vltkh6fvh240zqtv9k0t9af8935ke9laqsnlfe5",
            ),
        )

    @Test
    fun decode_invalidCharacter_notInAlphabet() {
        assertFailsWith(Bech32Exception::class) {
            Bech32.decode("A12OUEL5X")
        }
    }

    @Test
    fun decode_invalidCharacter_upperLowerMix() {
        assertFailsWith(Bech32Exception::class) {
            Bech32.decode("A12OUEL5X")
            Bech32.decode("A12UeL5X")
        }
    }

    @Test
    fun decode_invalidNetwork() {
        assertFailsWith(Bech32Exception::class) {
            Bech32.decode("A12UEL5X")
        }
    }

    @Test
    fun decode_invalidHrp() {
        assertFailsWith(Bech32Exception::class) {
            Bech32.decode("1pzry9x0s0muk")
        }
    }
}
