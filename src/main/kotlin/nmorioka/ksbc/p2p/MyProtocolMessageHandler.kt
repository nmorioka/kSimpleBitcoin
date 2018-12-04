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
            // println("Bloadcasting ... json.dumps(msg)")
            // TODO: よく考えるとEdgeから受信した場合にしか他のCoreにブロードキャストしないようにすれば重複チェックもいらない…
            // api(SEND_TO_ALL_PEER, json.dumps(msg))
            // api(SEND_TO_ALL_EDGE, json.dumps(msg))
        } else {
            println("MyProtocolMessageHandler received ${message}")
            // api(PASS_TO_CLIENT_APP, msg)
        }
    }

}