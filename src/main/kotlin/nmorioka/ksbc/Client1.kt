package nmorioka.ksbc

import nmorioka.ksbc.core.ClientCore
import sun.misc.Signal
import sun.misc.SignalHandler


fun main(args: Array<String>) {
    var loopEnable = true
    val sc = ClientCore("localhost", 50100, "localhost", 50082)

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
