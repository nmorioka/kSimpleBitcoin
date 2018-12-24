package nmorioka.ksbc.transaction

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

private val moshi = Moshi.Builder().build()
private val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
private val adapter: JsonAdapter<Map<String, String>> = moshi.adapter(type)


fun loadTransaction(str: String): Map<String, String>? {
    return adapter.fromJson(str)
}

fun dumpTransaction(transaction: Map<String, String>): String? {
    return adapter.toJson(transaction)
}

class TransactionPool {
    private val transactions = mutableListOf<Map<String, String>>()
    private val lock = Object()

    init {
        println("Initializing TransactionPool...")
    }

    fun setNewTransaction(transaction: Map<String, String>) {
        synchronized(lock) {
            println("set_new_transaction is called [${transaction}]")
            transactions.add(transaction)
        }
    }

    fun clearMyTransactions(index: Int) {
        synchronized(lock) {
            if (index <= transactions.size) {
                transactions.subList(0, index).clear()
            }
            println("transaction is now refreshed ... ${transactions}")
        }

    }

    fun getStoredTransactions(): List<Map<String, String>> {
        return transactions.toList()
    }

    fun size(): Int {
        return transactions.size
    }

}

