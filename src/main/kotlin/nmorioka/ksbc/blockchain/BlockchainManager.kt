package nmorioka.ksbc.blockchain

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types


fun isValidBlock(block: Block, previousHash: String?): Boolean {
    val message = block.toMessage(false)
    val nonce = block.nonce

    if (block.previousHash != previousHash) {
        println("Invalid block (bad previous_block) [${block.previousHash}] [${previousHash}]")
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

fun isValidChain(chain: List<Block>): Boolean {
    val head = chain[0].previousHash
    chain.fold(head) { previous, block ->
        val hash = getHash(block)
        println("previous[${block.previousHash}] to [${hash}]")
        if (isValidBlock(block, previous) == false) {
            return false
        }
        hash
    }

    return true
}

class BlockchainManager() {
    private val moshi = Moshi.Builder().build()
    private val chainType = Types.newParameterizedType(List::class.java, Block::class.java)
    private val chainAdapter: JsonAdapter<List<Block>> = moshi.adapter(chainType)

    private val myChain = mutableListOf<Block>()
    private val lock = Object()

    private var prevBlockHash: String

    private val blockBuilder = BlockBuilder()

    init {
        println("Initializing BlockchainManager...")
        val genesisBlock = blockBuilder.generateGeneisBlock()
        myChain.add(genesisBlock)
        prevBlockHash = getHash(genesisBlock)
    }

    fun generateNewBlock(transactions: List<Map<String, String>>): Block {
        return Block(transactions, prevBlockHash)
    }

    fun setNewBlock(block: Block) {
        synchronized(lock) {
            myChain.add(block)
            prevBlockHash = getHash(block)
            println("Current prev_block_hash is ... ${this.prevBlockHash}")
        }
    }

    fun getMyBlockchain(): List<Block> {
        if (myChain.isEmpty()) {
            return listOf()
        } else {
            return myChain.toList()
        }
    }

    fun getMyChainLength(): Int {
        return myChain.size
    }

    fun getMyChainDump(): String {
        return chainAdapter.toJson(myChain)
    }

    fun convertChain(json: String): List<Block>? {
        return chainAdapter.fromJson(json)
    }

    fun isValidNewBlock(block: Block): Boolean {
        return isValidBlock(block, this.prevBlockHash)
    }

    /**
     * ブロックチェーン自体を更新し、それによって変更されるはずの最新のprev_block_hashを計算して返却する
     */
    fun renewMyBlockChain(chain: List<Block>): String? {
        synchronized(lock) {
            if (isValidChain(chain)) {
                myChain.clear()
                myChain.addAll(chain)
                this.prevBlockHash =  getHash(myChain.last())
                return this.prevBlockHash
            } else {
                println("invalid myChain cannot be set...")
                return null
            }
        }
    }

    /**
     * 自分のブロックチェーンと比較して、長い方を有効とする。有効性検証自体はrenew_my_blockchainで実施
     */
    fun resolveConflicts(chain: List<Block>):Pair<String?, List<Block>> {
        val myChainLen = getMyChainLength()
        val newChainLen = chain.size

        val pool4OrphanBlocks = getMyBlockchain().toMutableList()

        // 自分のチェーンの中でだけ処理済みとなっているTransactionを救出する。現在のチェーンに含まれていない
        // ブロックを全て取り出す。時系列を考えての有効無効判定などはしないかなり簡易な処理。
        if (newChainLen <= myChainLen) {
            println("ivalid myChain cannot be set...")
            return Pair(null, listOf())
        }

        for (b in pool4OrphanBlocks) {
            for (b2 in chain) {
                if (b == b2) {
                    pool4OrphanBlocks.remove(b)
                }
            }
        }

        return renewMyBlockChain(chain).let {
            Pair(it, pool4OrphanBlocks)
        } ?: Pair(null, listOf())
    }

    fun getTransactionsFromOrphanBlocks(orphanBlocks: List<Block>): List<Map<String, String>> {
        val currentIndex = 0
        val newTransactions = mutableListOf<Map<String, String>>()

        while (currentIndex < orphanBlocks.size) {
            val transactions = orphanBlocks[currentIndex].transactions
            val target = removeUselessTransaction(transactions)
            newTransactions.addAll(target)
        }

        return newTransactions.toList()
    }

    /**
     * 与えられた Transaction のリストの中ですでに自分が管理するブロックチェーン内に含まれたTransactionがある場合、それを削除したものを返却する
     */
    fun removeUselessTransaction(transactions: List<Map<String, String>>): List<Map<String, String>> {
        val mutableTransactions = transactions.toMutableList()
        if (transactions.isNotEmpty()) {
            var cunnrentIndex = 1
            while (cunnrentIndex < myChain.size) {
                val block = myChain[cunnrentIndex]
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