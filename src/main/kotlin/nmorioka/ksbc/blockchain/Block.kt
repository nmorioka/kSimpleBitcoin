package nmorioka.ksbc.blockchain

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types


private val moshi = Moshi.Builder().build()
private val type = Types.newParameterizedType(List::class.java, Map::class.java, String::class.java, Any::class.java)
private val adapter: JsonAdapter<List<Map<String, Any>>> = moshi.adapter(type)


/**
 * transaction: ブロック内にセットされるトランザクション
 * previous_block_hash: 直前のブロックのハッシュ値
 */
open class Block internal constructor(val transactions: List<Map<String, Any>>, val previousBlock: String?) {
    val timestamp = System.currentTimeMillis()

    open fun toDict(): Map<String, Any> {
        previousBlock?.let {
            return mapOf<String, Any>(
                    "timestamp" to timestamp,
                    "transactions" to adapter.toJson(transactions),
                    "previous_block" to previousBlock
            )
        }

        return mapOf<String, Any>(
                "timestamp" to timestamp,
                "transactions" to adapter.toJson(transactions)
        )
    }
}

/**
 * 前方にブロックを持たないブロックチェーンの始原となるブロック。
 * transaction にセットしているのは「{"message":"this_is_simple_bitcoin_genesis_block"}」をSHA256でハッシュしたもの。深い意味はない
 */
class GenesisBlock : Block(listOf(mapOf<String, Any>("message" to "this_is_simple_bitcoin_genesis_block")), null) {
    override fun toDict(): Map<String, Any> {
        return mapOf<String, Any>(
                "transactions" to adapter.toJson(transactions),
                "genesis_block" to true
        )
    }
}


class BlockBuilder() {
    fun generateGeneisBlock(): GenesisBlock {
        return GenesisBlock()
    }

    fun generateNewBlock(transactions: List<Map<String, Any>>, previousBlockHash: String): Block {
        return Block(transactions, previousBlockHash)
    }

}
