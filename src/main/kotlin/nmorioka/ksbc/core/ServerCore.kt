package nmorioka.ksbc.core

import nmorioka.ksbc.p2p.ConnectionManager

class ServerCore(val myHost: String, val myPort:Int, val coreNodeHost: String? = null, val coreNodePort: Int? = null) {
    var serverState: ServerState

    val cm: ConnectionManager

    init {
        serverState = ServerState.INIT
        println("Initializing server...")
        cm = ConnectionManager(myHost, myPort)
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
}

enum class ServerState(val rawValue: Int) {
    INIT(0),
    STANDBY(1),
    CONNECTED_TO_NETWORK(2),
    SHUTING_DOWN(3)
}






