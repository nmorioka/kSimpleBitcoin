package nmorioka.ksbc.p2p

import io.rsocket.AbstractRSocket
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.transport.netty.server.NettyContextCloseable
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.util.DefaultPayload
import nmorioka.ksbc.let2
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

class ConnectionManager(val host: String, val port: Int) {

    private val messageManager = MessageManager()

    private val coreNodeList = CoreNodeList()

    private var myConnectionHost: String? = null
    private var myConnectionPost: Int? = null

    private var server: Server? = null

    init {
        addPeer(Peer(host, port))
    }

    fun start() {
        server = Server(host, port)
        server?.start {
            handleMessage(it)
        }
    }

    fun stop() {
        server?.shutdown()

        // TODO 離脱要求の送信
        /*
        self.ping_timer_p.cancel()
        self.ping_timer_e.cancel()
        if self.my_c_host is not None:
        msg = self.mm.build(MSG_REMOVE, self.port)
        self.send_msg((self.my_c_host, self.my_c_port), msg)
        */
    }

    fun joinNetwork(host: String ,port: Int) {
        this.myConnectionHost = host
        this.myConnectionPost = port
        connectToP2PNW(host, port)
    }

    private fun connectToP2PNW(host: String, port: Int) {
        val message = buildMessage(MsgType.ADD)
        sendMsg(Peer(host, port), message)
    }

    private fun addPeer(peer: Peer) {
        coreNodeList.add(peer)
    }

    private fun removePeer(peer: Peer) {
        coreNodeList.remove(peer)
    }

    private fun checkSelf(host: String, port: Int): Boolean {
        return this.host == host && this.port == port
    }

    private fun buildMessage(type: MsgType, payload: String? = null): String {
        return messageManager.build(type, this.host, this.port, payload)
    }

    private fun handleMessage(response: Response) {
        val pair = Pair(response.result, response.code)
        when (Pair(response.result, response.code)) {
            Pair("error", MsgResponseCode.ERR_PROTOCOL_UNMATCH) -> {
                println("Error: Protocol name is not matched")
            }
            Pair("error", MsgResponseCode.ERR_VERSION_UNMATCH) -> {
                println("Error: Protocol version is not matched")
            }
            Pair("ok", MsgResponseCode.OK_WITHOUT_PAYLOAD) -> {
                let2(response.host, response.port) { h, p ->
                    when (response.type) {
                        MsgType.ADD -> {
                            println("ADD node request was received!!")
                            addPeer(Peer(h, p))
                            if (checkSelf(h, p)) {
                                return@let2
                            } else {
                                val message = buildMessage(MsgType.CORE_LIST, coreNodeList.dump())
                                sendMsgToAllPeer(message)
                            }
                        }
                        MsgType.REMOVE -> {
                            println("REMOVE request was received!! from ${h} ${p}")
                            removePeer(Peer(h, p))

                            val message = buildMessage(MsgType.CORE_LIST, coreNodeList.dump())
                            sendMsgToAllPeer(message)
                        }
                        MsgType.PING -> return@let2
                        MsgType.REQUEST_CORE_LIST -> {
                            println("List for Core nodes was requested!!")
                            val message = buildMessage(MsgType.CORE_LIST, coreNodeList.dump())
                            sendMsg(Peer(h, p), message)
                        }
                        else -> {
                            println("recieved unknown command ${response.type}")
                        }
                    }
                }
            }
            Pair("ok", MsgResponseCode.OK_WITH_PAYLOAD) -> {
                when (response.type) {
                    MsgType.CORE_LIST -> {
                        // TODO: 受信したリストをただ上書きしてしまうのは本来セキュリティ的には宜しくない。
                        // 信頼できるノードの鍵とかをセットしとく必要があるかも
                        println("Refresh the core node list...")
                        response.payload?.let {
                            coreNodeList.overwrite(it)
                        }
                    }
                    else -> {
                        // TODO
//                        self.callback((result, reason, cmd, peer_port, payload), None)
//                        return
                        println("else..")
                    }
                }
            }
        }
    }

    private fun sendMsg(peer: Peer, message: String) {
        println("send to ${peer} ${message}")
        val client = Client(peer.host, peer.port)
        // TODO error func
        client.sendOnce(message)
    }

    private fun sendMsgToAllPeer(message: String) {
        println("send_msg_to_all_peer was called!")
        coreNodeList.getSet().filter { peer -> !checkSelf(peer.host, peer.port) }
                .forEach { peer ->
                    sendMsg(peer, message)
                }
    }


    /**
     * 接続されているCoreノード全ての生存確認を行う。クラスの外からは利用しない想定
     * この確認処理は定期的に実行される
     */
    private fun checkPeersConnection() {
        println("check_peers_connection was called")
        val deadList = coreNodeList.getSet().filter { peer ->
            !checkSelf(peer.host, peer.port) && !isAlive(peer)}.toList()
        if (deadList.isNotEmpty()) {
            println("Removing peer ${deadList}")
            deadList.forEach {
                coreNodeList.getSet().remove(it)
            }

            val message = buildMessage(MsgType.CORE_LIST, coreNodeList.dump())
            sendMsgToAllPeer(message)
        }

        println("current core node list: ${coreNodeList.getSet()}")

        /* TODO
        self.send_msg_to_all_edge(msg)
        self.ping_timer_p = threading.Timer(PING_INTERVAL, self.__check_peers_connection)
        self.ping_timer_p.start()
        */
    }

    /**
     * 有効ノード確認メッセージの送信
     */
    private fun isAlive(peer: Peer): Boolean {
        val client = Client(peer.host, peer.port)
        val message = buildMessage(MsgType.PING)
        // TODO error...
        client.sendOnce(message)

        return true
    }

}

data class Peer(val host: String, val port: Int)

private class Server(val host: String, val port: Int) {
    var serverChannel: NettyContextCloseable? = null

    val messageManager = MessageManager()

    fun start(execute: (response: Response) -> Unit) {
        println("Waiting for the connection ...")
        val server = RSocketFactory.receive()
                .acceptor { setupPayload, reactiveSocket ->
                    println("Connected by")
                    Mono.just<RSocket>(
                            object : AbstractRSocket() {
                                override fun fireAndForget(p: Payload?): Mono<Void> {
                                    p?.let {
                                        println("p.data ${p.dataUtf8}")
                                        val response = messageManager.parse(p.dataUtf8)
                                        response?.let(execute)
                                    }

                                    return Mono.empty()
                                }
                            })
                }
                .transport<NettyContextCloseable>(TcpServerTransport.create(host, port))
                .start()
                .subscribeOn(Schedulers.elastic())
                .subscribe()

        // go to subscribe
        // serverChannel = server.block()
    }

    fun shutdown() {
        // serverChannel?.dispose()
    }

}


private class Client(val host: String, val port: Int) {
    fun sendOnce(message: String) {
        RSocketFactory.connect()
                .transport(TcpClientTransport.create(host, port))
                .start()
                .subscribe { rsocket ->
                    println(rsocket)
                    rsocket.fireAndForget(DefaultPayload.create(message))
                            .subscribe()
                    println("fire gone")
                }
    }

    fun close() {
    }

}

private class Client2(val host: String, val port: Int) {
    val socket = RSocketFactory.connect()
            .transport(TcpClientTransport.create(host, port))
            .start()
            .block()

    fun sendOnce(message: String) {
        socket?.let {
            it.fireAndForget(DefaultPayload.create(message))
//                    .onErrorMap { e ->
//                        println(e)
//                        return e.message
//                    }
                    .doOnNext{ println(it) }
                    .block()
            it.dispose()
        }
    }

    fun close() {
        socket?.dispose()
    }

}