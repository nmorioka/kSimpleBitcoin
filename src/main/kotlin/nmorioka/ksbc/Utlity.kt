package nmorioka.ksbc

fun <T1 : Any, T2 : Any, R> let2(a: T1?, b: T2?, callback: (T1, T2) -> R): R? =
        if (a != null && b != null)
            callback(a, b)
        else
            null