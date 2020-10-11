package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase

class CaseInsensitiveHashMap<V> : MutableMap<String,V> {
    private val map = mutableMapOf<String,V>()

    override fun containsKey(key: String): Boolean {
        return map.keys.any {
            it.equalsIgnoreCase(key)
        }
    }

    override fun containsValue(value: V): Boolean = map.containsValue(value)

    override fun get(key: String): V? = existingKey(key)?.let { map[key] }

    override fun isEmpty(): Boolean = map.isEmpty()

    override fun clear() {
        map.clear()
    }

    override fun put(key: String, value: V): V? {
        val previous = remove(key)
        map[key] = value
        return previous
    }

    override fun putAll(from: Map<out String, V>) {
        from.forEach {
            put(it.key, it.value)
        }
    }

    override fun remove(key: String): V?
            = map.remove(existingKey(key))

    private fun existingKey(key:String) : String? {
        return map.keys.firstOrNull { it.equalsIgnoreCase(key)}
    }

    override val size: Int
        get() = map.size
    override val entries: MutableSet<MutableMap.MutableEntry<String, V>>
        get() = map.entries
    override val keys: MutableSet<String>
        get() = map.keys
    override val values: MutableCollection<V>
        get() = map.values
}