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

        assertEquals("494926c7a06228da9043a665de687b72a76687a11d2a107079a13c9b9c10a22b", genesisBlockHash)

        val transaction = mapOf<String, Any>(
                "sender" to "test1",
                "recipient" to "test2",
                "value" to 3
        )
        val newBlock = builder.generateNewBlock(transaction, genesisBlockHash)
        manager.setNewBlock(newBlock)
        val newBlockHash = getHash(newBlock)

        val transaction2 = mapOf<String, Any>(
                "sender" to "test1",
                "recipient" to "test3",
                "value" to 2
        )
        val newBlock2 = builder.generateNewBlock(transaction2, newBlockHash)
        manager.setNewBlock(newBlock2)
        val newBlock2Hash = getHash(newBlock2)

        assertTrue { manager.isValid() }
    }
}