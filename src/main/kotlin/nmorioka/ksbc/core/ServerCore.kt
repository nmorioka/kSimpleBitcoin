package nmorioka.ksbc.core

class ServerCore {
    var serverState: ServerState = ServerState.INIT

    fun start() {
        serverState = ServerState.STANDBY
    }

    fun joinNetwork() {
        serverState = ServerState.CONNECTED_TO_NETWORK
    }

    fun shutdown() {
        serverState = ServerState.SHUTING_DOWN
    }

    fun getMyCurrentState(): ServerState {
        return serverState
    }
}

enum class ServerState(val rawValue: Int) {
    INIT(0),
    STANDBY(1),
    CONNECTED_TO_NETWORK(2),
    SHUTING_DOWN(3)
}