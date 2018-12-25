package nmorioka.ksbc.blockchain

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import nmorioka.ksbc.getDoubleSha256
import nmorioka.ksbc.hash256
import nmorioka.ksbc.let2


private val moshi = Moshi.Builder().build()
private val listType = Types.newParameterizedType(List::class.java, Map::class.java, String::class.java, String::class.java)
private val listAdapter: JsonAdapter<List<Map<String, String>>> = moshi.adapter(listType)
private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
private val mapAdapter: JsonAdapter<Map<String, String>> = moshi.adapter(mapType)


fun nonceIsProof(message: String, nonce: String, difficulty: Int = 4): Boolean {
    val numbers = (1..difficulty).map{ "0" }.reduce {acc, s -> acc + s}
    val digit = getDoubleSha256(message + nonce)
    return digit.endsWith(numbers)
}

/**
 * 正当性確認に使うためブロックのハッシュ値を取る
 * @param block Block
 */
fun getHash(block: Block): String {
    return getDoubleSha256(toJson(block))
}

fun getStr(block: Block): String {
    return nmorioka.ksbc.getHash(block.toDict(true))
}

fun toJson(block: Block): String {
    return mapAdapter.toJson(block.toDict(true))
}

fun fromJson(str: String): Block? {
    return mapAdapter.fromJson(str)?.let { fromDict(it) }
}

fun fromDict(dict: Map<String, String>): Block? {
    val transactionData = dict["transactions"] ?: return null

    return let2(listAdapter.fromJson(transactionData), dict["timestamp"]) { transactions, timestamp ->
        Block(transactions = transactions, previousHash = dict["previous_block"], timestamp = timestamp.toLong(), _nonce = dict["nonce"])
    }

    return null
}


/**
 * transaction: ブロック内にセットされるトランザクション
 * previous_block_hash: 直前のブロックのハッシュ値
 */
open class Block internal constructor(val transactions: List<Map<String, String>>,
                                      val previousHash: String?,
                                      val timestamp: Long = System.currentTimeMillis(),
                                      _nonce: String? = null) {
    val nonce: String

    init {
        val initmap = previousHash?.let {
            mapOf<String, String>(
                    "timestamp" to timestamp.toString(),
                    "transactions" to listAdapter.toJson(transactions),
                    "previous_block" to previousHash)

        } ?: mapOf<String, String>(
                    "timestamp" to timestamp.toString(),
                    "transactions" to listAdapter.toJson(transactions))


        if (_nonce != null && nonceIsProof(mapAdapter.toJson(initmap), _nonce)) {
            nonce = _nonce
        } else {
            val initTime = System.currentTimeMillis()
            nonce = computeNonceForPow(toMessage(false))

            val diffTime = System.currentTimeMillis() - initTime
            println("init block time[${diffTime}]")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Block
        return toDict(true).equals(other.toDict(true))
    }

    open fun toMessage(includeNonce: Boolean): String {
        return mapAdapter.toJson(toDict(includeNonce))
    }

    open fun toDict(includeNonce: Boolean): Map<String, String> {
        if (includeNonce) {
            previousHash?.let {
                return  mapOf<String, String>(
                        "timestamp" to timestamp.toString(),
                        "transactions" to listAdapter.toJson(transactions),
                        "previous_block" to previousHash,
                        "nonce" to nonce)
            }
            return  mapOf<String, String>(
                        "timestamp" to timestamp.toString(),
                        "transactions" to listAdapter.toJson(transactions),
                        "nonce" to nonce)
        } else {
            previousHash?.let {
                return  mapOf<String, String>(
                        "timestamp" to timestamp.toString(),
                        "transactions" to listAdapter.toJson(transactions),
                        "previous_block" to previousHash)

            }
            return  mapOf<String, String>(
                    "timestamp" to timestamp.toString(),
                    "transactions" to listAdapter.toJson(transactions))
        }
    }

    override fun toString(): String {
        return getHash(this)
    }

    /**
     * block生成の計算処理
     * difficultyの数字を増やせば増やすほど、末尾で揃えなければならない桁数が増える
     */
    private fun computeNonceForPow(message: String, difficulty: Int = 4): String {
        val numbers = (1..difficulty).map{ "0" }.reduce {acc, s -> acc + s}
        var i = 1
        while(true) {
            val nonce = i.toString()
            if (nonceIsProof(message, nonce)) {
                return nonce
            }
            i++
        }
    }

}

class BlockBuilder() {
    /**
     * 前方にブロックを持たないブロックチェーンの始原となるブロック。
     * transaction にセットしているのは「{"message":"this_is_simple_bitcoin_genesis_block"}」をSHA256でハッシュしたもの。深い意味はない
     */
    fun generateGeneisBlock(): Block {
        return Block(transactions = listOf(mapOf<String, String>("message" to "this_is_simple_bitcoin_genesis_block")), previousHash = null, timestamp = 0)
    }

    fun generateNewBlock(transactions: List<Map<String, String>>, previousBlockHash: String): Block {
        return Block(transactions, previousBlockHash)
    }
}
