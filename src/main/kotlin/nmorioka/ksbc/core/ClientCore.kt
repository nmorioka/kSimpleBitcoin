package nmorioka.ksbc.core

import nmorioka.ksbc.p2p.ConnectionManager4Edge

class ClientCore(val myHost: String, val myPort:Int, val coreNodeHost: String, val coreNodePort: Int) {
    var clientState: ClientState

    val cm: ConnectionManager4Edge

    init {
        clientState = ClientState.INIT
        println("Initializing client core...")
        cm = ConnectionManager4Edge(myHost, myPort, coreNodeHost, coreNodePort)
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
}

enum class ClientState(val rawValue: Int) {
    INIT(0),
    ACTIVE(1),
    SHUTING_DOWN(2)
}

