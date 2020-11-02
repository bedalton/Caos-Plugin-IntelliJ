package com.badahori.creatures.plugins.intellij.agenteering.caos.libs


/**
 * Interface marking a class as having an get operator
 */
interface HasGetter<K,V> {
    operator fun get(key: K): V
}
