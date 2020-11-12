package com.badahori.creatures.plugins.intellij.agenteering.caos.generator


/**
 * Interface marking a class as having an get operator
 */
internal interface HasGetter<K,V> {
    operator fun get(key: K): V
}

internal class HasGetterImpl<K,V>(private val getter:(key:K) -> V) : HasGetter<K, V> {
    override operator fun get(key:K):V = getter(key)
}