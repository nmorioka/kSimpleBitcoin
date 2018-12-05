package nmorioka.ksbc.core

import nmorioka.ksbc.p2p.*

class ClientCore(val myHost: String, val myPort:Int, val coreNodeHost: String, val coreNodePort: Int) {
    var clientState: ClientState

    val cm: ConnectionManager4Edge

    val mpm = MyProtocolMessageHandler()
    val myProtocolMessageStore = mutableListOf<String>()

    init {
        clientState = ClientState.INIT
        println("Initializing client core...")
        cm = ConnectionManager4Edge(myHost, myPort, coreNodeHost, coreNodePort) { request ->
            handleMessage(request)
        }
    }

    /**
     * Coreノードとしての待受を開始する（上位UI層向け)
     */
    fun start() {
        cm.start()
        clientState = ClientState.ACTIVE
        cm.connectToCoreNode()
    }

    fun shutdown() {
        println("Shutdown edge node...")

        clientState = ClientState.SHUTING_DOWN
        cm.stop()
    }

    fun getMyCurrentState(): ClientState {
        return clientState
    }

    /**
     * ConnectionManager4Edgeに引き渡すコールバックの中身
     */
    private fun handleMessage(requet: Request) {
        println(requet)

        if (requet.type == MsgType.RSP_FULL_CHAIN) {
            // TODO: ブロックチェーン送信要求に応じて返却されたブロックチェーンを検証する処理を呼び出す
        } else if (requet.type == MsgType.ENHANCED) {
            // P2P Network を単なるトランスポートして使っているアプリケーションが独自拡張したメッセージはここで処理する。
            // SimpleBitcoin としてはこの種別は使わない
            requet.payload?.let {
                mpm.handleMessage(it) { s1, s2 ->
                    clientApi(s1, s2)
                }
            }
        }
    }

    /**
     * 拡張メッセージとして送信されてきたメッセージを格納しているリストを取得する
     * （現状未整備で特に意図した利用用途なし）
     */
    fun getMyProtocolMessage(): MutableList<String> {
        return this.myProtocolMessageStore
    }

    /***
     * 接続中のCoreノードに対してメッセージを送付する（上位UI層向け
     * @param type MessageManagerで規定のメッセージ種別を指定する
     * @param message メッセージ本文。文字列化されたJSONを想定
     */
    fun sendMessageToMyCoreNode(type: MsgType, message: String) {
        val newMessage = cm.getMsgText(type, message)
        cm.sendMsg(Peer(coreNodeHost, coreNodePort), newMessage)
    }

    /**
     * MyProtocolMessageHandlerで呼び出すための拡張関数群（現状未整備）
     * @param request MyProtocolMessageHandlerから呼び出されるコマンドの種別
     * @param message コマンド実行時に利用するために引き渡されるメッセージ
     */
    private fun clientApi(request: String, message: String?): String? {
        if (request == "pass_message_to_client_application" && message != null) {
            myProtocolMessageStore.add(message)
        } else if (request == "api_type") {
            return "client_core_api"
        } else {
            println("not implemented api was used")
        }

        return null
    }
}

enum class ClientState(val rawValue: Int) {
    INIT(0),
    ACTIVE(1),
    SHUTING_DOWN(2)
}

