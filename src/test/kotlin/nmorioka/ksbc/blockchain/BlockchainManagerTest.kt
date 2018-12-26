package nmorioka.ksbc.blockchain

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockchainManagerTest {
    @Test
    fun blockchain_isvalid () {
        val builder = BlockBuilder()
        val genesisBlock = builder.generateGeneisBlock()

        val manager = BlockchainManager()

        val genesisBlockHash = getHash(genesisBlock)

        assertEquals("df287102c1ea3a345ec33b7cbd806d282dd6a7e562d5b4d30c7861e32746eda9", genesisBlockHash)

        val transaction = mapOf(
                "sender" to "test1",
                "recipient" to "test2",
                "value" to 3.toString()
        )
        val newBlock = builder.generateNewBlock(listOf(transaction), genesisBlockHash)
        manager.setNewBlock(newBlock)
        val newBlockHash = getHash(newBlock)

        val transaction2 = mapOf(
                "sender" to "test1",
                "recipient" to "test3",
                "value" to 2.toString()
        )
        val newBlock2 = builder.generateNewBlock(listOf(transaction2), newBlockHash)
        manager.setNewBlock(newBlock2)

        assertTrue { isValidChain(manager.getMyBlockchain()) }
    }

    @Test
    fun blockchain_dump () {
        val builder = BlockBuilder()
        val genesisBlock = builder.generateGeneisBlock()
        val genesisBlockHash = getHash(genesisBlock)

        val manager = BlockchainManager()

        val transaction = mapOf(
                "sender" to "test1",
                "recipient" to "test2",
                "value" to 3.toString()
        )
        val newBlock = builder.generateNewBlock(listOf(transaction), genesisBlockHash)
        manager.setNewBlock(newBlock)
        val newBlockHash = getHash(newBlock)

        val transaction2 = mapOf(
                "sender" to "test1",
                "recipient" to "test3",
                "value" to 2.toString()
        )
        val newBlock2 = builder.generateNewBlock(listOf(transaction2), newBlockHash)
        manager.setNewBlock(newBlock2)

        val dump = manager.getMyChainDump()

        assertEquals(manager.getMyBlockchain(), manager.convertChain(dump))
    }

    @Test
    fun renewMyBlockChain_success() {
        val builder = BlockBuilder()
        val genesisBlock = builder.generateGeneisBlock()
        val genesisBlockHash = getHash(genesisBlock)

        val manager = BlockchainManager()

        val transaction = mapOf(
                "sender" to "test1",
                "recipient" to "test2",
                "value" to 3.toString()
        )
        val newBlock = builder.generateNewBlock(listOf(transaction), genesisBlockHash)
        manager.setNewBlock(newBlock)

        val manager2 = BlockchainManager()
        manager2.setNewBlock(newBlock)
        val newBlockHash = getHash(newBlock)
        val transaction2 = mapOf(
                "sender" to "test1",
                "recipient" to "test3",
                "value" to 2.toString()
        )
        val newBlock2 = builder.generateNewBlock(listOf(transaction2), newBlockHash)
        manager2.setNewBlock(newBlock2)

        val chain2 = manager2.getMyBlockchain()

        assertNotNull(manager.renewMyBlockChain(chain2))
    }


    @Test
    fun renewMyBlockChain_fail() {
        val builder = BlockBuilder()
        val genesisBlock = builder.generateGeneisBlock()
        val genesisBlockHash = getHash(genesisBlock)

        val manager = BlockchainManager()

        val transaction = mapOf(
                "sender" to "test1",
                "recipient" to "test2",
                "value" to 3.toString()
        )
        val newBlock = builder.generateNewBlock(listOf(transaction), genesisBlockHash)
        manager.setNewBlock(newBlock)

        val manager2 = BlockchainManager()
        manager2.setNewBlock(newBlock)
        val newBlockHash = getHash(newBlock)
        val transaction2 = mapOf(
                "sender" to "test1",
                "recipient" to "test3",
                "value" to 2.toString()
        )
        val newBlock2 = builder.generateNewBlock(listOf(transaction2), newBlockHash + "a")
        manager2.setNewBlock(newBlock2)

        val chain2 = manager2.getMyBlockchain()

        assertNull(manager.renewMyBlockChain(chain2))
    }


}