package nmorioka.ksbc.p2p

import nmorioka.ksbc.p2p.MsgType
import org.junit.Test
import kotlin.test.assertEquals


class MessageManagerTest {

    @Test
    fun build_without_payload () {
        val manager = MessageManager();

        assertEquals("{\"msg_type\":0,\"my_host\":\"localhost\",\"my_port\":200,\"protocol\":\"simple_bitcoin_protocol\",\"version\":\"0.1.0\"}", manager.build(MsgType.ADD, "localhost", 200))
    }

    @Test
    fun build_with_payload () {
        val manager = MessageManager();

        assertEquals("{\"msg_type\":2,\"my_host\":\"localhost\",\"my_port\":100,\"payload\":\"hogehoge\",\"protocol\":\"simple_bitcoin_protocol\",\"version\":\"0.1.0\"}", manager.build(MsgType.CORE_LIST, "localhost", 100,"hogehoge"))
    }

    @Test
    fun parse_without_payload() {
        val manager = MessageManager();
        val response = manager.parse("{\"msg_type\":0,\"my_port\":100,\"protocol\":\"simple_bitcoin_protocol\",\"version\":\"0.1.0\"}")

        assertEquals("ok", response?.result)
        assertEquals(MsgResponseCode.OK_WITHOUT_PAYLOAD, response?.code)
        assertEquals(MsgType.ADD, response?.type)
        assertEquals(100, response?.port)
        assertEquals(null, response?.payload)
    }

    @Test
    fun parse_with_payload() {
        val manager = MessageManager();
        val response = manager.parse("{\"msg_type\":2,\"my_port\":100,\"payload\":\"hogehoge\",\"protocol\":\"simple_bitcoin_protocol\",\"version\":\"0.1.0\"}")

        assertEquals("ok", response?.result)
        assertEquals(MsgResponseCode.OK_WITH_PAYLOAD, response?.code)
        assertEquals(MsgType.CORE_LIST, response?.type)
        assertEquals(100, response?.port)
        assertEquals("hogehoge", response?.payload)
    }

    @Test
    fun parse_with_error_version() {
        val manager = MessageManager();
        val response = manager.parse("{\"msg_type\":2,\"my_port\":100,\"payload\":\"hogehoge\",\"protocol\":\"simple_bitcoin_protocol\",\"version\":\"1.1.0\"}")

        assertEquals("error", response?.result)
        assertEquals(MsgResponseCode.ERR_VERSION_UNMATCH, response?.code)
        assertEquals(null, response?.type)
        assertEquals(null, response?.payload)
    }

    @Test
    fun parse_with_error_protocol() {
        val manager = MessageManager();
        val response = manager.parse("{\"msg_type\":2,\"my_port\":100,\"payload\":\"hogehoge\",\"protocol\":\"simple_bitcoin_protocol2\",\"version\":\"1.1.0\"}")

        assertEquals("error", response?.result)
        assertEquals(MsgResponseCode.ERR_PROTOCOL_UNMATCH, response?.code)
        assertEquals(null, response?.type)
        assertEquals(null, response?.payload)
    }

    @Test
    fun parse_with_error_emptyjson() {
        val manager = MessageManager();
        val response = manager.parse("{}")

        assertEquals("error", response?.result)
        assertEquals(MsgResponseCode.ERR_PROTOCOL_UNMATCH, response?.code)
        assertEquals(null, response?.type)
        assertEquals(null, response?.payload)
    }

}