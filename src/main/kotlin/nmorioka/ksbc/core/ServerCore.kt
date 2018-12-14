package nmorioka.ksbc.core

import nmorioka.ksbc.blockchain.BlockBuilder
import nmorioka.ksbc.blockchain.BlockchainManager
import nmorioka.ksbc.getHash
import nmorioka.ksbc.p2p.*
import nmorioka.ksbc.transaction.TransactionPool
import nmorioka.ksbc.transaction.loadTransaction
import java.util.*
import kotlin.concurrent.timer

class ServerCore(val myHost: String, val myPort:Int, val coreNodeHost: String? = null, val coreNodePort: Int? = null) {
    private var serverState: ServerState

    private val cm: ConnectionManager
    private val mpm = MyProtocolMessageHandler()
    private val myProtocolMessageStore = mutableListOf<String>()

    private val transactionPool = TransactionPool()
    private val blockBuilder = BlockBuilder()
    private val blockchainManager: BlockchainManager
    private var prevBlockHash: String

    private var transactionTimer: Timer? = null

    init {
        serverState = ServerState.INIT
        println("Initializing server...")
        cm = ConnectionManager(myHost, myPort) { request, isCore, peer ->
            handleMessage(request, isCore, peer)
        }

        val genesisBlock = blockBuilder.generateGeneisBlock()
        blockchainManager = BlockchainManager(genesisBlock)
        prevBlockHash = getHash(genesisBlock)
    }

    /**
     * Coreノードとしての待受を開始する（上位UI層向け)
     */
    fun start() {
        cm.start()

        transactionTimer = timer("transactionTimer", false, 0, 10000) {
            generateBlockWithTp()
        }

        serverState = ServerState.STANDBY
    }

    fun shutdown() {
        println("Shutdown server...")

        serverState = ServerState.SHUTING_DOWN
        cm.stop()
    }

    fun getMyCurrentState(): ServerState {
        return serverState
    }

    /**
     * 事前に取得した情報に従い拠り所となる他のCoreノードに接続する（上位UI層向け)
     */
    fun joinNetwork() {
        if (coreNodeHost != null && coreNodePort != null) {
            serverState = ServerState.CONNECTED_TO_NETWORK
            cm.joinNetwork(coreNodeHost, coreNodePort)
        } else {
            println("This server is runnning as Genesis Core Node...")
        }
    }

    private fun generateBlockWithTp() {
        val result = transactionPool.getStoredTransactions()
        if (result.isNotEmpty()) {
            val newBlock = blockBuilder.generateNewBlock(result, this.prevBlockHash)
            blockchainManager.setNewBlock(newBlock)
            transactionPool.clearMyTransactions(result.size)
            this.prevBlockHash = getHash(newBlock)
        } else {
            println("Transaction Pool is empty ...'")
        }

        println("Current Blockchain is ... ${blockchainManager.chain}")
        println("Current prev_block_hash is ... ${this.prevBlockHash}")
    }




    /**
     * ConnectionManagerに引き渡すコールバックの中身
     */
    private fun handleMessage(request: Request, isCore: Boolean, peer: Peer?) {
        if (peer != null) {
            when (request.type) {
                MsgType.RSP_FULL_CHAIN -> {
                    // TODO: 現状はREQUEST_FULL_CHAINの時にしかこの処理に入らないけど、
                    //  まだブロックチェーンを作るところまで行ってないのでとりあえず口だけ作っておく
                    println("Send our latest blockchain for reply to : ${peer}")
                    return
                }
            }
        } else {
            when (request.type) {
                MsgType.NEW_TRANSACTION -> {
                    // TODO: 新規transactionを登録する処理を呼び出す
                    request.payload?.let {
                        val transaction = loadTransaction(request.payload)
                        println("received new_transaction ${transaction}")

                        val currentTransactions = transactionPool.getStoredTransactions()
                        if (transaction == null) {
                            return
                        }
                        if (currentTransactions.contains(transaction)) {
                            println("this is already pooled transaction: ${transaction}")
                            return
                        }

                        transactionPool.setNewTransaction(transaction)
                        if (isCore == false) {
                            val newMessage = cm.getMsgText(MsgType.NEW_TRANSACTION, request.payload)
                            cm.sendMsgToAllPeer(newMessage)
                        }
                    }
                }
                MsgType.NEW_BLOCK -> {
                    // TODO: 新規ブロックを検証する処理を呼び出す
                }
                MsgType.RSP_FULL_CHAIN -> {
                    // TODO: ブロックチェーン送信要求に応じて返却されたブロックチェーンを検証する処理を呼び出す
                }
                MsgType.ENHANCED -> {
                    // P2P Network を単なるトランスポートして使っているアプリケーションが独自拡張したメッセージはここで処理する。
                    // SimpleBitcoin としてはこの種別は使わない

                    // あらかじめ重複チェック（ポリシーによる。別にこの処理しなくてもいいかも
                    println("received enhanced message [${request.payload}]")
                    request.payload?.let {
                        if (myProtocolMessageStore.contains(it) == false) {
                            myProtocolMessageStore.add(it)
                            mpm.handleMessage(it) { s1, s2 ->
                                coreApi(s1, s2)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * MyProtocolMessageHandlerで呼び出すための拡張関数群（現状未整備）
     * @param request MyProtocolMessageHandlerから呼び出されるコマンドの種別
     * @param message コマンド実行時に利用するために引き渡されるメッセージ
     */
    private fun coreApi(request: String, message: String?): String? {
        val type = MsgType.ENHANCED
        val apiProtocol = ApiProtocol.fromRawValue(request)

        when(apiProtocol) {
            ApiProtocol.SEND_TO_ALL_PEER -> {
                val newMessage = cm.getMsgText(type, message)
                cm.sendMsgToAllPeer(newMessage)
                return "ok"
            }
            ApiProtocol.SEND_TO_ALL_EDGE -> {
                val newMessage= cm.getMsgText(type, message)
                cm.sendMsgToAllEdge(newMessage)
                return "ok"
            }
            ApiProtocol.API_TYPE -> {
                return "server_core_api"
            }
        }

        return null
    }
}

enum class ServerState(val rawValue: Int) {
    INIT(0),
    STANDBY(1),
    CONNECTED_TO_NETWORK(2),
    SHUTING_DOWN(3)
}






