package nmorioka.ksbc.blockchain

import nmorioka.ksbc.getHash

/**
 * transaction: ブロック内にセットされるトランザクション
 * previous_block_hash: 直前のブロックのハッシュ値
 */
open class Block internal constructor(val transactions: Map<String, Any>, val previousBlock: String?) {
    val timestamp = System.currentTimeMillis()

    open fun toDict(): Map<String, Any> {
        previousBlock?.let {
            return mapOf<String, Any>(
                    "timestamp" to timestamp,
                    "transactions" to getHash(transactions),
                    "previous_block" to previousBlock
            )
        }

        return mapOf<String, Any>(
                "timestamp" to timestamp,
                "transactions" to getHash(transactions)
        )
    }


}

/**
 * 前方にブロックを持たないブロックチェーンの始原となるブロック。
 * transaction にセットしているのは「{"message":"this_is_simple_bitcoin_genesis_block"}」をSHA256でハッシュしたもの。深い意味はない
 */
class GenesisBlock : Block(mapOf<String, Any>("message" to "this_is_simple_bitcoin_genesis_block"), null) {
    override fun toDict(): Map<String, Any> {
        return mapOf<String, Any>(
                "transactions" to getHash(transactions),
                "genesis_block" to true
        )
    }
}


class BlockBuilder() {
    fun generateGeneisBlock(): GenesisBlock {
        return GenesisBlock()
    }

    fun generateNewBlock(transactions: Map<String, Any>, previousBlockHash: String): Block {
        return Block(transactions, previousBlockHash)
    }

}
