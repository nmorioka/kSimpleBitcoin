package nmorioka.ksbc.blockchain

import nmorioka.ksbc.getHash


class BlockchainManager(val genesisBlock: GenesisBlock) {
    val chain = mutableListOf<Block>()
    val lock = Object()


    init {
        println("Initializing BlockchainManager...")
        chain.add(genesisBlock)
    }

    fun setNewBlock(block: Block) {
        synchronized(lock) {
            chain.add(block)
        }
    }

    fun isValid(): Boolean {
        val head = getHash(chain[0])
        chain.fold(head) { previous, block ->
            val hash = getHash(block)
            println("previous[${block.getPreviousHash()}] to [${hash}]")
            if (block.getPreviousHash() != null && previous != block.getPreviousHash()) {
                return false
            }
            hash
        }

        return true
    }

}