package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID

object FileIndexUtil {

    fun <K,V> getKeysAndValues(indexId: ID<K, V>, scope: GlobalSearchScope): List<Pair<K, V>> {
        val out = mutableListOf<Pair<K,V>>()
        val index = FileBasedIndex.getInstance()
        index.processAllKeys(indexId,  { key ->
            out.addAll(mapKeyToValues(index, indexId, scope, key))
        }, scope, null)
        return out
    }

    fun <K,V> getKeysAndValuesAsMap(indexId: ID<K, V>, scope: GlobalSearchScope): Map<K, List<V>> {
        val out = mutableListOf<Pair<K,V>>()
        val index = FileBasedIndex.getInstance()
        index.processAllKeys(indexId,  { key ->
            out.addAll(mapKeyToValues(index, indexId, scope, key))
        }, scope, null)
        val keys = out.map { it.first }.distinct()
        return keys.associate {
            it to out.filter { it.first == it }.map { it.second }
        }
    }


    private fun <K, V> mapKeyToValues(index: FileBasedIndex, indexId: ID<K, V>, scope: GlobalSearchScope, key: K): List<Pair<K, V>> {
        if (key == null) {
            return emptyList()
        }
        index.getValues(indexId, key, scope)
        return index.getValues(indexId, key, scope)
            .map { value ->
                Pair(key, value)
            }
    }

}