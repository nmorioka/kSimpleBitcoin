package nmorioka.ksbc.p2p

class MyProtocolMessageHandler {

    /**
     * とりあえず受け取ったメッセージを自分がCoreノードならブロードキャスト、
     * Edgeならコンソールに出力することでメッセっぽいものをデモ
     * @param message 拡張プロトコルで送られてきたJSON形式のメッセージ
     * @param api ServerCore(or ClientCore）側で用意されているAPI呼び出しのためのコールバック
     *            api(param1, param2) という形で利用する
     */
    fun handleMessage(message:String, api: (String, String?) -> String?) {
        //     msg = json.loads(msg)
        val myApi = api("api_type", null)
        println("my_api: ${myApi}")

        if (myApi == "server_core_api") {
            println("Bloadcasting ... ${message}")
            // TODO: よく考えるとEdgeから受信した場合にしか他のCoreにブロードキャストしないようにすれば重複チェックもいらない…
            api(ApiProtocol.SEND_TO_ALL_PEER.rawValue, message)
            api(ApiProtocol.SEND_TO_ALL_EDGE.rawValue, message)
        } else {
            println("MyProtocolMessageHandler received ${message}")
            api(ApiProtocol.PASS_TO_CLIENT_APP.rawValue, message)
        }
    }

}

enum class ApiProtocol(val rawValue: String) {
    NONE("none"),

    API_TYPE("api_type"),

    SEND_TO_ALL_PEER("send_message_to_all_peer"),
    SEND_TO_ALL_EDGE("send_message_to_all_edge"),

    PASS_TO_CLIENT_APP("pass_message_to_client_application");

    companion object {
        fun fromRawValue(rawValue: String): ApiProtocol {
            return ApiProtocol.values().firstOrNull { it.rawValue == rawValue } ?: ApiProtocol.NONE
        }
    }
}