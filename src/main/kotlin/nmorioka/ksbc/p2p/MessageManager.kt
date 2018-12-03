package nmorioka.ksbc.p2p

import com.github.zafarkhaja.semver.Version
import com.squareup.moshi.Moshi


class MessageManager {
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(Message::class.java)

    init {
        println("Initializing MessageManager...")
    }

    fun build(msgType: MsgType, myHost:String, myPort: Int, payload: String? = null): String {
        val message = if (payload != null) {
            Message(msg_type = msgType.rawValue, my_host = myHost, my_port = myPort, payload = payload)
        } else {
            Message(msg_type = msgType.rawValue, my_host = myHost, my_port = myPort)
        }
        return adapter.toJson(message)
    }

    fun parse(json: String): Response? {
        val message = adapter.fromJson(json)
        if (message == null) {
            return null
        }

        return if (message.protocol != Message.PROTOCOL_NAME) {
            Response(result = "error", code = MsgResponseCode.ERR_PROTOCOL_UNMATCH)
        } else if (Version.valueOf(message.version).greaterThan(Version.valueOf(Message.MY_VERSION))) {
            Response(result = "error", code = MsgResponseCode.ERR_VERSION_UNMATCH)
        } else if (message.msg_type == MsgType.CORE_LIST.rawValue) {
            Response(result = "ok", code = MsgResponseCode.OK_WITH_PAYLOAD, type = MsgType.fromRawValue(message.msg_type), host = message.my_host, port = message.my_port, payload = message.payload)
        } else {
            Response(result = "ok", code = MsgResponseCode.OK_WITHOUT_PAYLOAD, host = message.my_host, port = message.my_port, type = MsgType.fromRawValue(message.msg_type))
        }
    }
}

data class Message(
        val protocol: String = PROTOCOL_NAME,
        val version: String = MY_VERSION,
        val msg_type: Int,
        val my_host: String,
        val my_port: Int,
        val payload: String? = null
) {
    companion object {
        val PROTOCOL_NAME = "simple_bitcoin_protocol"
        val MY_VERSION = "0.1.0"
    }
}

data class Response(
        val result: String,
        val code: MsgResponseCode,
        val type: MsgType? = null,
        val host: String? = null,
        val port: Int? = null,
        val payload: String? = null
)

enum class MsgType(val rawValue: Int) {
    NONE(-1),
    ADD(0),
    REMOVE(1),
    CORE_LIST(2),
    REQUEST_CORE_LIST(3),
    PING(4),
    ADD_AS_EDGE(5),
    REMOVE_EDGE(6);

    companion object {
        fun fromRawValue(rawValue: Int): MsgType {
            return values().firstOrNull { it.rawValue == rawValue } ?: NONE
        }
    }
}

enum class MsgResponseCode(val rawValue: Int) {
    ERR_PROTOCOL_UNMATCH(0),
    ERR_VERSION_UNMATCH(1),
    OK_WITH_PAYLOAD(2),
    OK_WITHOUT_PAYLOAD(3)
}