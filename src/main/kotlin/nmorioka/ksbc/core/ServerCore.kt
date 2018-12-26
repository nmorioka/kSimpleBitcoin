package nmorioka.ksbc.core

import nmorioka.ksbc.blockchain.BlockchainManager
import nmorioka.ksbc.blockchain.fromJson
import nmorioka.ksbc.blockchain.toJson
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
    private val blockchainManager: BlockchainManager

    private var isBBRunning = false
    private var flagStopBlockBuild = false

    private var transactionTimer: Timer? = null

    init {
        serverState = ServerState.INIT
        println("Initializing server...")
        cm = ConnectionManager(myHost, myPort) { request, isCore, peer ->
            handleMessage(request, isCore, peer)
        }

        blockchainManager = BlockchainManager()
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
        transactionTimer?.cancel()
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

    fun getAllChainsForResolveConflict() {
        println("get_all_chains_for_resolve_conflict called")
        val message = cm.getMsgText(MsgType.REQUEST_FULL_CHAIN)
        cm.sendMsgToAllPeer(message)
    }

    private fun generateBlockWithTp() {
        while (!flagStopBlockBuild) {
            this.isBBRunning = true
            val result = transactionPool.getStoredTransactions()
            if (result.isNotEmpty()) {
                val newTransactions = blockchainManager.removeUselessTransaction(result)
                transactionPool.renewMyTransactions(newTransactions)
                if (newTransactions.isEmpty()) {
                    break
                }
                val newBlock = blockchainManager.generateNewBlock(newTransactions)
                blockchainManager.setNewBlock(newBlock)
                val message = cm.getMsgText(MsgType.NEW_BLOCK, toJson(newBlock))
                cm.sendMsgToAllPeer(message)
                transactionPool.clearMyTransactions(result.size)
            } else {
                println("Transaction Pool is empty ...'")
            }
            break

        }
        println("Current Blockchain size ... ${blockchainManager.getMyChainLength()}")

        this.flagStopBlockBuild = false
        this.isBBRunning = false
    }

    /**
     * ConnectionManagerに引き渡すコールバックの中身
     */
    private fun handleMessage(request: Request, isCore: Boolean, peer: Peer?) {
        if (peer != null) {
            when (request.type) {
                MsgType.REQUEST_FULL_CHAIN -> {
                    println("Send our latest blockchain for reply to : ${peer}")
                    val myChain = blockchainManager.getMyChainDump()
                    println("request mychain [${myChain}]")
                    val message = cm.getMsgText(MsgType.RSP_FULL_CHAIN, myChain)
                    cm.sendMsg(peer, message)
                    return
                }
            }
        } else {
            when (request.type) {
                MsgType.NEW_TRANSACTION -> {
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
                    if (isCore == false) {
                        println("block received from unknown")
                        return
                    }
                    // 新規ブロックを検証し、正当なものであればブロックチェーンに追加する
                    request.payload?.let {
                        val newBlock  = fromJson(it)
                        if (newBlock != null && blockchainManager.isValidNewBlock(newBlock)) {
                            // ブロック生成が行われていたら、いったん停止してあげる
                            // (threadingなのでキレイに止まらない可能性あり)
                            if (this.isBBRunning) {
                                this.flagStopBlockBuild = true
                            }
                            blockchainManager.setNewBlock(newBlock)
                        } else {
                            // ブロックとして不正ではないがVerifyにコケる場合は自分がorphanブロックを生成している
                            // 可能性がある
                            getAllChainsForResolveConflict()
                        }
                    }
                }
                MsgType.RSP_FULL_CHAIN -> {
                    if (isCore == false) {
                        println("block received from unknown")
                        return
                    }
                    // ブロックチェーン送信要求に応じて返却されたブロックチェーンを検証し、有効なものか検証した上で
                    // 自分の持つチェインと比較し優位な方を今後のブロックチェーンとして有効化する

                    request.payload?.let {
                        val newChain = blockchainManager.convertChain(request.payload)
                        println("rsp_full_chain [${newChain}]")
                        if (newChain == null) {
                            return
                        }

                        val result = blockchainManager.resolveConflicts(newChain)
                        println("blockchain received")
                        if (result.second.size > 0) {
                            // orphanブロック群の中にあった未処理扱いになるTransactionをTransactionPoolに戻す
                            val newTransactions = blockchainManager.getTransactionsFromOrphanBlocks(result.second)
                            for (newTransaction in newTransactions) {
                                transactionPool.setNewTransaction(newTransaction)
                            }
                        }

                    }
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

