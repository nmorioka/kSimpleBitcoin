package nmorioka.ksbc

import nmorioka.ksbc.core.ClientCore
import nmorioka.ksbc.p2p.MsgType
import sun.misc.Signal
import sun.misc.SignalHandler

fun main(args: Array<String>) {
    var loopEnable = true
    val cc = ClientCore("localhost", 50098, "localhost", 50082)

    val handleObject = object : SignalHandler {
        override fun handle(signal: Signal) {
            println("handle signal ${signal}")
            cc.shutdown()
            loopEnable = false
        }
    }
    val signal1 = Signal("TERM")
    val signal2 = Signal("INT")
    Signal.handle(signal1, handleObject)
    Signal.handle(signal2, handleObject)

    cc.start()

    Thread.sleep(5000)

    val message = """"
    {
        'from':'hoge',
        'to':'fuga',
        'message' :'test'
    }
    """

    // my_p2p_client.send_message_to_my_core_node(MSG_ENHANCED,json.dumps(message))
    cc.sendMessageToMyCoreNode(MsgType.ENHANCED, message)

    while (loopEnable) {
        Thread.sleep(3000)
        // for daemon
    }
}
