package utils

inline fun <K, V> Map<out K, V>.first(predicate: (Map.Entry<K, V>) -> Boolean): V? {
    for (element in this) {
        val result = predicate(element)
        if (result) {
            return element.value
        }
    }
    return null
}