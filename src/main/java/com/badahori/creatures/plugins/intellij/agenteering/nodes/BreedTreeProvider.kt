package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VirtualFile

class BreedTreeProvider: TreeStructureProvider {
    override fun modify(
        parentNode: AbstractTreeNode<*>,
        childNodes: Collection<AbstractTreeNode<*>>,
        viewSettings: ViewSettings?,
    ): Collection<AbstractTreeNode<*>> {
        val out = mutableListOf<AbstractTreeNode<*>>()
        val groups: MutableMap<BreedPartKey, MutableList<VirtualFile>>  = mutableMapOf()
        for(childNode in childNodes) {
            val data = childNode.value
            if (data is VirtualFile && !data.isDirectory) {
                val key = BreedPartKey
                    .fromFileName(data.name)
                    ?.copyWithPart(null)
                if (key == null) {
                    out.add(childNode)
                    continue
                }
                if (!groups.containsKey(key)) {
                    groups[key] = mutableListOf(data)
                } else {
                    groups[key]!!.add(data)
                }
            } else {
                out.add(childNode)
            }
        }
        val breedNodes: List<AbstractTreeNode<*>> = groups.map { (key, children) ->
            BreedNode(
                parentNode.project,
                key,
                children,
                viewSettings
            )
        }
        return out + breedNodes
    }

}