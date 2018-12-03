package nmorioka.ksbc

import nmorioka.ksbc.core.ServerCore
import sun.misc.Signal
import sun.misc.SignalHandler


fun main(args: Array<String>) {
    var loopEnable = true
    val sc = ServerCore("localhost", 50082)

    val handleObject = object : SignalHandler {
        override fun handle(signal: Signal) {
            println("handle signal ${signal}")
            sc.shutdown()
            loopEnable = false
        }
    }
    val signal1 = Signal("TERM")
    val signal2 = Signal("INT")
    Signal.handle(signal1, handleObject)
    Signal.handle(signal2, handleObject)

    sc.start()

    while (loopEnable) {
        Thread.sleep(3000)
        // for daemon
    }
}

/*
    val server = SampleServer()
    server.start()
 */

/*
fun testMeessageManager() {
    val moshi = Moshi.Builder().build()
    val adapter = moshi.adapter(Message::class.java)

    val message1 =  Message(msg_type = MsgType.ADD.rawValue)
    println(adapter.toJson(message1))
    val message2 =  Message(msg_type = MsgType.CORE_LIST.rawValue, payload = "hogehoge")
    println(adapter.toJson(message2))

}
*/