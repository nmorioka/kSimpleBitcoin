package nmorioka.ksbc.blockchain

import nmorioka.ksbc.getHash
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockchainManagerTest {
    @Test
    fun blockchain_isvalid () {
        val builder = BlockBuilder()
        val genesisBlock = builder.generateGeneisBlock()

        val manager = BlockchainManager(genesisBlock)

        val genesisBlockHash = getHash(genesisBlock)

        assertEquals("b74bdc8c3b826aae773e15b7c21fca53e0e7f665abb5aa45eb3b45cb07975925", genesisBlockHash)

        val transaction = mapOf<String, Any>(
                "sender" to "test1",
                "recipient" to "test2",
                "value" to 3
        )
        val newBlock = builder.generateNewBlock(listOf(transaction), genesisBlockHash)
        manager.setNewBlock(newBlock)
        val newBlockHash = getHash(newBlock)

        val transaction2 = mapOf<String, Any>(
                "sender" to "test1",
                "recipient" to "test3",
                "value" to 2
        )
        val newBlock2 = builder.generateNewBlock(listOf(transaction2), newBlockHash)
        manager.setNewBlock(newBlock2)
        val newBlock2Hash = getHash(newBlock2)

        assertTrue { manager.isValid() }
    }
}