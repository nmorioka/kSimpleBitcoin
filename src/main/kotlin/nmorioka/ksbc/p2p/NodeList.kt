package nmorioka.ksbc.p2p

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class NodeList {
    private val lockObject = Object()
    private val nodeSet = mutableSetOf<Peer>()

    private val moshi = Moshi.Builder().build()
    private val type = Types.newParameterizedType(MutableSet::class.java,Peer::class.java)
    private val adapter: JsonAdapter<MutableSet<Peer>> = moshi.adapter(type)


    /**
     * Coreノードをリストに追加する。
     * @param peer  Coreノードとして格納されるノードの接続情報（IPアドレスとポート番号）
     */
    fun add(peer: Peer) {
        synchronized(lockObject) {
            println("Adding peer: ${peer}")
            nodeSet.add(peer)
            println("Adding end: ${nodeSet}")
        }
    }

    /**
     * 離脱したと判断されるCoreノードをリストから削除する
     * @param peer 削除するノードの接続先情報（IPアドレスとポート番号）
     */
    fun remove(peer: Peer) {
        synchronized(lockObject) {
            if (nodeSet.contains(peer)) {
                println("Removing peer: ${peer}")
                nodeSet.remove(peer)
                println("Current Core list: ${nodeSet}")
            }
        }
    }

    /**
     * 複数のpeerの生存確認を行った後で一括での上書き処理をしたいような場合
     */
    fun overwrite(newSet: MutableSet<Peer>) {
        synchronized(lockObject) {
            println("core node list will be going to overwrite")
            nodeSet.clear()
            newSet?.forEach {it -> nodeSet.add(it)}
            println("Current Core list: ${nodeSet}")
        }
    }

    fun overwrite(jsonObject: String) {
        val newSet = adapter.fromJson(jsonObject)

        newSet?.let{
            overwrite(newSet)
        }
    }

    /**
     * 現在接続状態にあるPeerの一覧を返却する
     */
    fun getSet(): MutableSet<Peer> {
        return this.nodeSet
    }


    fun getLength(): Int {
        return nodeSet.size
    }

    /**
     * リストのトップにあるPeerを返却する
     */
    fun getNodeInfo(): Peer {
        return nodeSet.first()
    }

    fun dump(): String {
        return adapter.toJson(nodeSet)
    }

}