package nmorioka.ksbc.core

import nmorioka.ksbc.p2p.*

class ServerCore(val myHost: String, val myPort:Int, val coreNodeHost: String? = null, val coreNodePort: Int? = null) {
    var serverState: ServerState

    val cm: ConnectionManager

    val mpm = MyProtocolMessageHandler()
    val myProtocolMessageStore = mutableListOf<String>()

    init {
        serverState = ServerState.INIT
        println("Initializing server...")
        cm = ConnectionManager(myHost, myPort) { request, peer ->
            handleMessage(request, peer)
        }
    }

    /**
     * Coreノードとしての待受を開始する（上位UI層向け)
     */
    fun start() {
        cm.start()
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

    /**
     * ConnectionManagerに引き渡すコールバックの中身
     */
    private fun handleMessage(request: Request, peer: Peer?) {
        if (peer == null) {
            // TODO: 現状はREQUEST_FULL_CHAINの時にしかこの処理に入らないけど、
            //  まだブロックチェーンを作るところまで行ってないのでとりあえず口だけ作っておく
            println("Send our latest blockchain for reply to : ${peer}")
        } else {
            when (request.type) {
                MsgType.NEW_TRANSACTION -> {
                    // TODO: 新規transactionを登録する処理を呼び出す
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

        // msg_type = MSG_ENHANCED

        if (request == "send_message_to_all_peer") {
            // new_message = self.cm.get_message_text(msg_type, message)
            // self.cm.send_msg_to_all_peer(new_message)
            return "ok"
        } else if (request == "send_message_to_all_edge") {
            // ew_message = self.cm.get_message_text(msg_type, message)
            // self.cm.send_msg_to_all_edge(new_message)
            return "ok"
        } else if (request == "api_type") {
            return "server_core_api"
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






