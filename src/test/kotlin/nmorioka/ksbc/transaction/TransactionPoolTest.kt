package nmorioka.ksbc.transaction

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionPoolTest {
    @Test
    fun empty_pool () {
        val tp = TransactionPool()
        assertTrue { tp.getStoredTransactions().isEmpty() }
    }

    @Test
    fun add() {
        val tp = TransactionPool()
        tp.setNewTransaction(mapOf("hoge" to 1.toString()))
        tp.setNewTransaction(mapOf("fuga" to 2.toString()))
        val pooledTransactions = tp.getStoredTransactions()

        assertEquals(2, pooledTransactions.size)
        assertEquals(mapOf("hoge" to 1.toString()), pooledTransactions[0])
        assertEquals(mapOf("fuga" to 2.toString()), pooledTransactions[1])
    }

    @Test
    fun clear() {
        val tp = TransactionPool()
        tp.setNewTransaction(mapOf("hgoe" to 1.toString()))
        tp.setNewTransaction(mapOf("fuga" to 2.toString()))
        val pooledTransactions = tp.getStoredTransactions()
        tp.setNewTransaction(mapOf("piyo" to 3.toString()))
        tp.clearMyTransactions(pooledTransactions.size)

        val clearedTransactions = tp.getStoredTransactions()
        assertEquals(1, clearedTransactions.size)
        assertEquals(mapOf("piyo" to 3.toString()), clearedTransactions[0])
    }

    @Test
    fun renewMyTransactions() {
        val tp = TransactionPool()
        tp.setNewTransaction(mapOf("hgoe" to 1.toString()))
        tp.setNewTransaction(mapOf("fuga" to 2.toString()))

        val replace = mutableListOf<Map<String,String>>()
        replace.add(mapOf("piyo" to 3.toString()))
        replace.add(mapOf("hoge2" to 4.toString()))
        replace.add(mapOf("fuga2" to 5.toString()))

        tp.renewMyTransactions(replace)

        assertEquals(3, tp.size())
        assertEquals("3", tp.getStoredTransactions()[0]["piyo"])
    }

}