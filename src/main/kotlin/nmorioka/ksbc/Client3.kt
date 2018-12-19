package nmorioka.ksbc

import nmorioka.ksbc.core.ClientCore
import nmorioka.ksbc.p2p.MsgType
import nmorioka.ksbc.transaction.dumpTransaction
import sun.misc.Signal
import sun.misc.SignalHandler


fun main(args: Array<String>) {
    var loopEnable = true
    val cc = ClientCore("localhost", 50095, "localhost", 50082)

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

    val transaction = mapOf(
        "sender" to "test4",
        "recipient" to "test5",
        "value" to 3
    )

    dumpTransaction(transaction)?.let {
        cc.sendMessageToMyCoreNode(MsgType.NEW_TRANSACTION, it)
    }

    val transaction2 = mapOf(
            "sender" to "test6",
            "recipient" to "test7",
            "value"  to 2
    )

    dumpTransaction(transaction2)?.let {
        cc.sendMessageToMyCoreNode(MsgType.NEW_TRANSACTION, it)
    }

    Thread.sleep(10000)

    val transaction3 = mapOf(
        "sender" to "test8",
        "recipient" to "test9",
        "value" to 10
    )

    dumpTransaction(transaction3)?.let {
        cc.sendMessageToMyCoreNode(MsgType.NEW_TRANSACTION, it)
    }

    while (loopEnable) {
        Thread.sleep(3000)
        // for daemon
    }
}
