package nmorioka.ksbc

import org.junit.Test
import kotlin.test.assertEquals


class UtilityTest {

    @Test
    fun test_hash256 () {
        assertEquals("ecb666d778725ec97307044d642bf4d160aabb76f56c0069c71ea25b1e926825", hash256("hoge"))
    }

    @Test
    fun test_getDoubleSha256 () {
        assertEquals("882905aaf61d120761a612e09967123a7c1b3f7b5ea8cfc7939cd8db799faa31", getDoubleSha256("hoge"))
    }

    @Test
    fun test_getHash () {
        val transaction = mapOf(
                "sender" to "test8",
                "recipient" to "test9",
                "value" to 10.toString()
        )
        assertEquals("590869ee2f3468d29e890b2eb19746f449b74f50164f90e84eecbd59625b754b", getHash(transaction))
    }

}