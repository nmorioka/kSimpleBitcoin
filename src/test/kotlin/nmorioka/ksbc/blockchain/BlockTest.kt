package nmorioka.ksbc.blockchain

import nmorioka.ksbc.getDoubleSha256
import nmorioka.ksbc.hash256
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.asserter


class BlockTest {

    @Test
    fun test_getHash () {
        val transaction = mapOf(
                "sender" to "test8",
                "recipient" to "test9",
                "value" to 10.toString()
        )
        val block = Block(listOf(transaction), "hoge")
        assertEquals(64, getHash(block).length)
    }

    @Test
    fun fromJson_toJson() {
        val transaction = mapOf(
                "sender" to "test8",
                "recipient" to "test9",
                "value" to 10.toString()
        )
        val block = Block(listOf(transaction), "hoge")

        val json = toJson(block)
        println("json ${json}")

        val generated = fromJson(json)
        assertNotNull(generated)

        assertEquals(block, generated)
    }

}