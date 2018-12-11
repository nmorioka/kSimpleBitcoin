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
        tp.setNewTransaction("hoge")
        tp.setNewTransaction("fuga")
        val pooledTransactions = tp.getStoredTransactions()

        assertEquals(2, pooledTransactions.size)
        assertEquals("hoge", pooledTransactions[0])
        assertEquals("fuga", pooledTransactions[1])
    }

    @Test
    fun clear() {
        val tp = TransactionPool()
        tp.setNewTransaction("hoge")
        tp.setNewTransaction("fuga")
        val pooledTransactions = tp.getStoredTransactions()
        tp.setNewTransaction("piyo")
        tp.clearMyTransactions(pooledTransactions.size)

        val clearedTransactions = tp.getStoredTransactions()
        assertEquals(1, clearedTransactions.size)
        assertEquals("piyo", clearedTransactions[0])
    }
}