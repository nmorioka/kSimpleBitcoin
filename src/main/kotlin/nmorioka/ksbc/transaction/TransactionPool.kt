package nmorioka.ksbc.transaction

class TransactionPool {
    private val transactions = mutableListOf<String>()
    private val lock = Object()

    init {
        println("Initializing TransactionPool...")
    }

    fun setNewTransaction(transaction: String) {
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

    fun getStoredTransactions(): List<String> {
        return transactions.toList()
    }
}