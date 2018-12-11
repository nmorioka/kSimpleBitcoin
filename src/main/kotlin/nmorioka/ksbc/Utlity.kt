package nmorioka.ksbc

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import nmorioka.ksbc.blockchain.Block
import java.security.MessageDigest

fun <T1 : Any, T2 : Any, R> let2(a: T1?, b: T2?, callback: (T1, T2) -> R): R? =
        if (a != null && b != null)
            callback(a, b)
        else
            null

private val moshi = Moshi.Builder().build()
private val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
private val adapter: JsonAdapter<Map<String, Any>> = moshi.adapter(type)

fun hash256(message: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(message.toByteArray())
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}

fun getDoubleSha256(message: String): String {
    return hash256(hash256(message))
}

/**
 * 正当性確認に使うためブロックのハッシュ値を取る
 * @param block Block
 */
fun getHash(block: Block): String {
    return getHash(block.toDict())
}

fun getHash(map: Map<String, Any>): String {
    val str = adapter.toJson(map)
    return getDoubleSha256(str)
}
