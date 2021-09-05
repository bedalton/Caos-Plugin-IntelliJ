package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class CaseInsensitiveHashMap<V> : MutableMap<String, V> {
    private val readWriteLock = ReentrantReadWriteLock()
    private val readLock = readWriteLock.readLock()
    private val writeLock = readWriteLock.writeLock()
    private val map = mutableMapOf<String, V>()

    override fun containsKey(key: String): Boolean {
        readLock.withLock {
            return map.keys.any {
                it.equalsIgnoreCase(key)
            }
        }
    }

    override fun containsValue(value: V): Boolean = readLock.withLock { map.containsValue(value) }

    override fun get(key: String): V? = readLock.withLock { existingKey(key)?.let { map[key] } }

    override fun isEmpty(): Boolean = readLock.withLock { map.isEmpty() }

    override fun clear() {
        writeLock.withLock {
            map.clear()
        }
    }

    override fun put(key: String, value: V): V? {
        writeLock.withLock {
            val previous = remove(key)
            map[key] = value
            return previous
        }
    }

    override fun putAll(from: Map<out String, V>) {
        writeLock.withLock {
            from.forEach {
                put(it.key, it.value)
            }
        }
    }

    override fun remove(key: String): V? = writeLock.withLock { map.remove(existingKey(key)) }

    private fun existingKey(key: String): String? {
        readLock.withLock {
            return map.keys.firstOrNull { it.equalsIgnoreCase(key) }
        }
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