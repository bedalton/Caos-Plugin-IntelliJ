package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID
import org.jetbrains.annotations.NotNull

object FileIndexUtil {

    fun <K,V> getKeysAndValues(indexId: ID<K, V>, project: Project, scope: GlobalSearchScope): List<Pair<K, V>> {
        val index = FileBasedIndex.getInstance()
        return index
            .getAllKeys(indexId, project)
            .flatMap {

            }
    }


    private fun <K, V> mapKeyToValues(index: ID<K,V>, project: Project, scope: GlobalSearchScope, key: K): Pair<K, V> {
        return
    }

}