package nmorioka.ksbc.blockchain

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import nmorioka.ksbc.getDoubleSha256


private val moshi = Moshi.Builder().build()
private val listType = Types.newParameterizedType(List::class.java, Map::class.java, String::class.java, Any::class.java)
private val listAdapter: JsonAdapter<List<Map<String, Any>>> = moshi.adapter(listType)
private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
private val mapAdapter: JsonAdapter<Map<String, Any>> = moshi.adapter(mapType)



interface Block {
    fun getPreviousHash(): String?
    fun toDict(includeNonce: Boolean): Map<String, Any>


}

/**
 * transaction: ブロック内にセットされるトランザクション
 * previous_block_hash: 直前のブロックのハッシュ値
 */
class BasicBlock internal constructor(val transactions: List<Map<String, Any>>, private val previousBlock: String) : Block {
    val timestamp = System.currentTimeMillis()
    val nonce: String

    init {
        val initTime = System.currentTimeMillis()
        nonce = computeNonceForPow(mapAdapter.toJson(toDict(false)))

        val diffTime = System.currentTimeMillis() - initTime
        println("init block time[${diffTime}]")

    }

    override fun getPreviousHash(): String? {
        return previousBlock
    }

    override fun toDict(includeNonce: Boolean): Map<String, Any> {
        if (includeNonce) {
            return  mapOf<String, Any>(
                    "timestamp" to timestamp,
                    "transactions" to listAdapter.toJson(transactions),
                    "previous_block" to previousBlock,
                    "nonce" to nonce)
        } else {
            return  mapOf<String, Any>(
                    "timestamp" to timestamp,
                    "transactions" to listAdapter.toJson(transactions),
                    "previous_block" to previousBlock)
        }
    }

    override fun toString(): String {
        return mapAdapter.toJson(toDict(true))
    }

    /**
     * block生成の計算処理
     * difficultyの数字を増やせば増やすほど、末尾で揃えなければならない桁数が増える
     */
    private fun computeNonceForPow(message: String, difficulty: Int = 4): String {
        val numbers = (1..difficulty).map{ "0" }.reduce {acc, s -> acc + s}
        var i = 1
        while(true) {
            val digit = getDoubleSha256(message + i)
            if (digit.endsWith(numbers)) {
                return i.toString()
            }
            i++
        }
    }

}

/**
 * 前方にブロックを持たないブロックチェーンの始原となるブロック。
 * transaction にセットしているのは「{"message":"this_is_simple_bitcoin_genesis_block"}」をSHA256でハッシュしたもの。深い意味はない
 */
class GenesisBlock(val transactions: List<Map<String, Any>> = listOf(mapOf<String, Any>("message" to "this_is_simple_bitcoin_genesis_block"))) : Block {
    override fun getPreviousHash(): String? {
        return null
    }

    override fun toDict(includeNonce: Boolean): Map<String, Any> {
        return mapOf<String, Any>(
                "transactions" to listAdapter.toJson(transactions),
                "genesis_block" to true
        )
    }

    override fun toString(): String {
        return mapAdapter.toJson(toDict(true))
    }
}


class BlockBuilder() {
    fun generateGeneisBlock(): GenesisBlock {
        return GenesisBlock()
    }

    fun generateNewBlock(transactions: List<Map<String, Any>>, previousBlockHash: String): BasicBlock {
        return BasicBlock(transactions, previousBlockHash)
    }

}
