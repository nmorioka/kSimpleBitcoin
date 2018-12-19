package nmorioka.ksbc.blockchain

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import nmorioka.ksbc.getDoubleSha256


private val moshi = Moshi.Builder().build()
private val type = Types.newParameterizedType(List::class.java, Map::class.java, String::class.java, Any::class.java)
private val adapter: JsonAdapter<List<Map<String, Any>>> = moshi.adapter(type)



interface Block {
    fun getPreviousHash(): String?
    fun toDict(): Map<String, Any>
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
        nonce = computeNonceForPow(adapter.toJson(transactions))

        val diffTime = System.currentTimeMillis() - initTime
        println("init block time[${diffTime}]")

    }

    override fun getPreviousHash(): String? {
        return previousBlock
    }

    override fun toDict(): Map<String, Any> {
        return mapOf<String, Any>(
                "timestamp" to timestamp,
                "transactions" to adapter.toJson(transactions),
                "previous_block" to previousBlock)
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

    fun generateNewBlock(transactions: List<Map<String, Any>>, previousBlockHash: String): BasicBlock {
        return BasicBlock(transactions, previousBlockHash)
    }

}
