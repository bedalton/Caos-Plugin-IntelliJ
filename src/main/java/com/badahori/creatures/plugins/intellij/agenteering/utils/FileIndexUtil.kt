package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID

object FileIndexUtil {

    fun <K,V> getKeysAndValues(indexId: ID<K, V>, scope: GlobalSearchScope): List<Pair<K, V>> {
        val out = mutableListOf<Pair<K,V>>()
        val index = FileBasedIndex.getInstance()
        index.processAllKeys(indexId,  process@{ key ->
            if (key != null) {
                out.addAll(mapKeyToValues(index, indexId, scope, key))
            }
            return@process true
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
        return keys.associateWith { out.filter { it.first == it }.map { it.second } }
    }


    private fun <K, V> mapKeyToValues(index: FileBasedIndex, indexId: ID<K, V>, scope: GlobalSearchScope, key: K): List<Pair<K, V>> {
        if (key == null) {
            return emptyList()
        }
        val containingFiles = index.getContainingFiles(indexId, key, scope)
        return if (containingFiles.isEmpty() || containingFiles.none { scope.accept(it) }) {
            emptyList()
        } else {
            index.getValues(indexId, key, scope)
                .map { value ->
                    Pair(key, value)
                }
        }
    }

}