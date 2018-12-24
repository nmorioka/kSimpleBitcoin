package nmorioka.ksbc.blockchain

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types


class BlockchainManager() {
    private val moshi = Moshi.Builder().build()
    private val chainType = Types.newParameterizedType(List::class.java, Block::class.java)
    private val chainAdapter: JsonAdapter<List<Block>> = moshi.adapter(chainType)

    private val chain = mutableListOf<Block>()
    private val lock = Object()

    private var prevBlockHash: String

    private val blockBuilder = BlockBuilder()

    init {
        println("Initializing BlockchainManager...")
        val genesisBlock = blockBuilder.generateGeneisBlock()
        chain.add(genesisBlock)
        prevBlockHash = getHash(genesisBlock)
    }

    fun generateNewBlock(transactions: List<Map<String, String>>): Block {
        return Block(transactions, prevBlockHash)
    }

    fun setNewBlock(block: Block) {
        synchronized(lock) {
            chain.add(block)
            prevBlockHash = getHash(block)
            println("Current prev_block_hash is ... ${this.prevBlockHash}")
        }
    }

    fun getMyBlockchain(): List<Block> {
        if (chain.isEmpty()) {
            return listOf()
        } else {
            return chain.toList()
        }
    }

    fun getMyChainLength(): Int {
        return chain.size
    }

    fun getMyChainDump(): String {
        return chainAdapter.toJson(chain)
    }

    fun convertChain(json: String): List<Block>? {
        return chainAdapter.fromJson(json)
    }

    fun isValidChain(): Boolean {
        val head = getHash(chain[0])
        chain.fold(head) { previous, block ->
            val hash = getHash(block)
            println("previous[${block.previousHash}] to [${hash}]")
            if (block.previousHash != null && previous != block.previousHash) {
                return false
            }
            hash
        }

        return true
    }

    fun isValidBlock(block: Block): Boolean {
        val message = block.toMessage(false)
        val nonce = block.nonce

        if (block.previousHash != prevBlockHash) {
            println("Invalid block (bad previous_block) [${block.previousHash}] [${prevBlockHash}]")
            println("Invalid block (bad previous_block)")
            return false
        }

        if (nonceIsProof(message, nonce)) {
            println("OK, this seems valid block")
            return true
        } else {
            println("Invalid block (bad nonce) [${nonce}]")
            return false
        }
    }

    /**
     * 与えられた Transaction のリストの中ですでに自分が管理するブロックチェーン内に含まれたTransactionがある場合、それを削除したものを返却する
     */
    fun removeUselessTransaction(transactions:  List<Map<String, Any>>): List<Map<String, Any>> {
        val mutableTransactions = transactions.toMutableList()
        if (transactions.isNotEmpty()) {
            var cunnrentIndex = 1
            while (cunnrentIndex < chain.size) {
                val block = chain[cunnrentIndex]
                val newTransactions = block.transactions
                for (t in newTransactions) {
                    for (t2 in transactions){
                        if (t == t2) {
                            mutableTransactions.remove(t2)
                        }
                    }
                }
                cunnrentIndex++
            }
            return mutableTransactions
        } else {
            println("no transaction to be removed...")
            return listOf()
        }
    }

}