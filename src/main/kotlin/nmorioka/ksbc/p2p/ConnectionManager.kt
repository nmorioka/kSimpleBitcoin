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
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.scheduler.Schedulers
import java.util.*
import kotlin.concurrent.timer

class ConnectionManager(val host: String, val port: Int, val callback: (Request, Peer?) -> Unit) {

    private val messageManager = MessageManager()

    private val coreNodeList = NodeList()
    private val edgeNodeList = NodeList()

    private var myConnectionHost: String? = null
    private var myConnectionPost: Int? = null

    private var server: Server? = null
    private var pingTimer: Timer? = null

    init {
        addPeer(Peer(host, port))
    }

    fun start() {
        server = Server(host, port)
        server?.start {
            handleMessage(it)
        }
        pingTimer = timer("pingTimer", false, 0, 30000) {
            checkPeersConnection()
            checkEdgesConnection()
        }
    }

    fun stop() {
        server?.shutdown()


        let2(this.myConnectionHost, this.myConnectionPost) { h, p ->
            val message = buildMessage(MsgType.REMOVE)
            sendMsg(Peer(h, p), message)
        }

        pingTimer?.cancel()
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

    private fun addEdgeNode(peer: Peer) {
        edgeNodeList.add(peer)
    }

    private fun removeEdgeNode(peer: Peer) {
        edgeNodeList.remove(peer)
    }

    private fun checkSelf(host: String, port: Int): Boolean {
        return this.host == host && this.port == port
    }

    private fun buildMessage(type: MsgType, payload: String? = null): String {
        return messageManager.build(type, this.host, this.port, payload)
    }

    private fun handleMessage(request: Request) {
        println("handle message : [${request}]")

        val pair = Pair(request.result, request.code)
        when (Pair(request.result, request.code)) {
            Pair("error", MsgResponseCode.ERR_PROTOCOL_UNMATCH) -> {
                println("Error: Protocol name is not matched")
            }
            Pair("error", MsgResponseCode.ERR_VERSION_UNMATCH) -> {
                println("Error: Protocol version is not matched")
            }
            Pair("ok", MsgResponseCode.OK_WITHOUT_PAYLOAD) -> {
                let2(request.host, request.port) { h, p ->
                    when (request.type) {
                        MsgType.ADD -> {
                            println("ADD node request was received!!")
                            addPeer(Peer(h, p))
                            if (checkSelf(h, p)) {
                                return@let2
                            } else {
                                val message = buildMessage(MsgType.CORE_LIST, coreNodeList.dump())
                                sendMsgToAllPeer(message)
                                sendMsgToAllEdge(message)
                            }
                        }
                        MsgType.REMOVE -> {
                            println("REMOVE request was received!! from ${h} ${p}")
                            removePeer(Peer(h, p))

                            val message = buildMessage(MsgType.CORE_LIST, coreNodeList.dump())
                            sendMsgToAllPeer(message)
                            sendMsgToAllEdge(message)
                        }
                        MsgType.PING -> return@let2
                        MsgType.REQUEST_CORE_LIST -> {
                            println("List for Core nodes was requested!!")
                            val message = buildMessage(MsgType.CORE_LIST, coreNodeList.dump())
                            sendMsg(Peer(h, p), message)
                        }
                        MsgType.ADD_AS_EDGE -> {
                            val edge = Peer(h, p)
                            addEdgeNode(edge)
                            val message = buildMessage(MsgType.CORE_LIST, coreNodeList.dump())
                            sendMsg(edge, message)
                        }
                        MsgType.REMOVE_EDGE -> {
                            val edge = Peer(h, p)
                            println("REMOVE_EDGE request was received!! from ${edge} ")
                            removeEdgeNode(edge)
                        }
                        else -> {
                            println("callback..")
                            callback(request, Peer(h, p))
                        }
                    }
                }
            }
            Pair("ok", MsgResponseCode.OK_WITH_PAYLOAD) -> {
                when (request.type) {
                    MsgType.CORE_LIST -> {
                        // TODO: 受信したリストをただ上書きしてしまうのは本来セキュリティ的には宜しくない。
                        // 信頼できるノードの鍵とかをセットしとく必要があるかも
                        println("Refresh the core node list...")
                        request.payload?.let {
                            coreNodeList.overwrite(it)
                        }
                    }
                    else -> {
                        let2(request.host, request.port) { h, p ->
                            println("callback..")
                            callback(request, Peer(h, p))
                        }
                    }
                }
            } else -> {
                println("recieved unknown command ${request.type}")

            }
        }
    }

    fun sendMsg(peer: Peer, message: String) {
        println("send to ${peer} ${message}")
        val client = Client(peer.host, peer.port)
        client.send(message).subscribe( {
            // do nothing
        }, {
            println("Connection failed for peer : ${peer}")
            removePeer(peer)
        })
    }

    fun sendMsgToAllPeer(message: String) {
        println("send_msg_to_all_peer was called!")
        coreNodeList.getSet().filter { peer -> !checkSelf(peer.host, peer.port) }
                .forEach { peer ->
                    sendMsg(peer, message)
                }
    }

    fun sendMsgToAllEdge(message: String) {
        println("send_msg_to_all_edge was called! ")

        edgeNodeList.getSet().forEach { peer ->
            sendMsg(peer, message)
        }
    }

    /**
     * 指定したメッセージ種別のプロトコルメッセージを作成して返却する
     *
     * @param type 作成したいメッセージの種別をMessageManagerの規定に従い指定
     * @param payload メッセージにデータを格納したい場合に指定する
     * @return MessageManagerのbuild_messageによって生成されたJSON形式のメッセージ
     */
    fun getMsgText(type: MsgType, payload: String? = null): String {
        val message = messageManager.build(type, this.host, this.port, payload)
        println("generated_msg: [${message}]")
        return message
    }

    /**
     * 接続されているCoreノード全ての生存確認を行う。クラスの外からは利用しない想定
     * この確認処理は定期的に実行される
     */
    private fun checkPeersConnection() {
        println("check_peers_connection was called")

        coreNodeList.getSet().iterator().toFlux().filter { it ->
            !checkSelf(it.host, it.port)
        }.flatMap { peer ->
            isAlive(peer)
        }.reduce(mutableSetOf<Peer>()) { deleteSet, pair ->
            if (pair.second == false) {
                deleteSet.add(pair.first)
            }
            deleteSet
        }.subscribe { deleteSet ->
            if (deleteSet.isNotEmpty()) {
                println("Removing peer ${deleteSet}")
                deleteSet.forEach {
                    coreNodeList.getSet().remove(it)
                }

                val message = buildMessage(MsgType.CORE_LIST, coreNodeList.dump())
                sendMsgToAllPeer(message)
                sendMsgToAllEdge(message)
            }
            println("current core node list: ${coreNodeList.getSet()}")
        }
    }

    /**
     * 有効ノード確認メッセージの送信
     */
    private fun isAlive(peer: Peer): Mono<Pair<Peer, Boolean>> {
        val client = Client(peer.host, peer.port)
        val message = buildMessage(MsgType.PING)

        return client.send(message).flatMap { t ->
            Mono.just(Pair(peer, true))
        }.onErrorResume {
            Mono.just(Pair(peer, false))
        }
    }

    /**
     * 接続されているEdgeノード全ての生存確認を行う。クラスの外からは利用しない想定
     * この確認処理は定期的に実行される
     */
    private fun checkEdgesConnection() {
        println("check_edges_connection was called")

        edgeNodeList.getSet().iterator().toFlux().flatMap { peer ->
            isAlive(peer)
        }.reduce(mutableSetOf<Peer>()) { deleteSet, pair ->
            if (pair.second == false) {
                deleteSet.add(pair.first)
            }
            deleteSet
        }.subscribe { deleteSet ->
            if (deleteSet.isNotEmpty()) {
                println("Removing Edges ${deleteSet}")
                deleteSet.forEach {
                    edgeNodeList.getSet().remove(it)
                }

                print("current edge node list: ${edgeNodeList.getSet()}")
            }
            println("current core node list: ${coreNodeList.getSet()}")
        }
    }

}

class ConnectionManager4Edge(val host: String, val port: Int, var myCoreHost: String, var myCorePort: Int, val callback: (Request) -> Unit) {
    private val messageManager = MessageManager()

    private val coreNodeList = NodeList()

    private var server: Server? = null
    private var pingTimer: Timer? = null

    fun start() {
        server = Server(host, port)
        server?.start {
            handleMessage(it)
        }
        pingTimer = timer("pingTimer", false, 0, 30000) {
            checkPeersConnection()
        }
    }

    fun stop() {
        server?.shutdown()

        pingTimer?.cancel()
    }

    fun connectToCoreNode() {
        connectToP2PNW(myCoreHost, myCorePort)
    }

    private fun connectToP2PNW(host: String, port: Int) {
        val message = buildMessage(MsgType.ADD_AS_EDGE)
        sendMsg(Peer(host, port), message)
    }

    private fun buildMessage(type: MsgType, payload: String? = null): String {
        return messageManager.build(type, this.host, this.port, payload)
    }

    private fun handleMessage(request: Request) {
        val pair = Pair(request.result, request.code)
        when (Pair(request.result, request.code)) {
            Pair("error", MsgResponseCode.ERR_PROTOCOL_UNMATCH) -> {
                println("Error: Protocol name is not matched")
            }
            Pair("error", MsgResponseCode.ERR_VERSION_UNMATCH) -> {
                println("Error: Protocol version is not matched")
            }
            Pair("ok", MsgResponseCode.OK_WITHOUT_PAYLOAD) -> {
                let2(request.host, request.port) { h, p ->
                    when (request.type) {
                        MsgType.PING -> return@let2
                        else -> {
                            // 接続情報以外のメッセージしかEdgeノードで処理することは想定していない
                            println("Edge node does not have functions for this message!")
                        }
                    }
                }
            }
            Pair("ok", MsgResponseCode.OK_WITH_PAYLOAD) -> {
                when (request.type) {
                    MsgType.CORE_LIST -> {
                        // Coreノードに依頼してCoreノードのリストを受け取る口だけはある
                        println("Refresh the core node list...")
                        request.payload?.let {
                            coreNodeList.overwrite(it)
                        }
                    }
                    else -> {
                        println("callback..")
                        this.callback(request)
                    }
                }
            }
        }
    }

    fun sendMsg(peer: Peer, message: String) {
        println("send to ${peer} ${message}")
        val client = Client(peer.host, peer.port)
        client.send(message).subscribe( {
            // do nothing
        }, {
            println("Connection failed for peer : ${peer}")
            coreNodeList.remove(peer)
            println("Tring to connect into P2P network...")
            if (coreNodeList.getLength() > 0) {
                val new = coreNodeList.getNodeInfo()
                this.myCoreHost = new.host
                this.myCorePort = new.port
                connectToCoreNode()
                sendMsg(new, message)
            } else {
                println("No core node found in our list...")
                pingTimer?.cancel()
            }
        })
    }

    /**
     * 指定したメッセージ種別のプロトコルメッセージを作成して返却する
     *
     * @param type 作成したいメッセージの種別をMessageManagerの規定に従い指定
     * @param payload メッセージにデータを格納したい場合に指定する
     * @return MessageManagerのbuild_messageによって生成されたJSON形式のメッセージ
     */
    fun getMsgText(type: MsgType, payload: String? = null): String {
        val message = messageManager.build(type, this.host, this.port, payload)
        println("generated_msg: [${message}]")
        return message
    }

    /**
     * 生存確認メッセージの送信処理実体。中で確認処理は定期的に実行し続けられる
     * @param peer 送信確認メッセージの送り先となるノードの接続情報（IPアドレスとポート番号）
     */
    private fun checkPeersConnection() {
        println("check_peers_connection was called")

        val peer = Peer(myCoreHost, myCorePort)

        val client = Client(peer.host, peer.port)
        val message = buildMessage(MsgType.PING)

        client.send(message).subscribe({}, { error ->
            println("Connection failed for peer : ${peer}")
            coreNodeList.remove(peer)
            println("Tring to connect into P2P network...")
            if (coreNodeList.getLength() > 0) {
                val new = coreNodeList.getNodeInfo()
                this.myCoreHost = new.host
                this.myCorePort = new.port
                connectToCoreNode()
            } else {
                println("No core node found in our list...")
                pingTimer?.cancel()
            }
        })
    }

}


data class Peer(val host: String, val port: Int)

private class Server(val host: String, val port: Int) {
    var serverDisposable: Disposable? = null

    val messageManager = MessageManager()

    fun start(execute: (request: Request) -> Unit) {
        println("Waiting for the connection ...")
        serverDisposable = RSocketFactory.receive()
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
    }

    fun shutdown() {
        serverDisposable?.dispose()
    }

}

private class Client(val host: String, val port: Int) {

    fun send(message: String): Mono<Void> {
        return RSocketFactory.connect()
                .transport(TcpClientTransport.create(host, port))
                .start()
                .flatMap { t: RSocket? ->
                    t?.fireAndForget(DefaultPayload.create(message))
                }
    }

}
