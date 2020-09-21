package com.badahori.creatures.plugins.intellij.agenteering.sfc.tree

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VirtualFile

class SfcTreeStructureProvider : TreeStructureProvider{
    override fun modify(node: AbstractTreeNode<*>, children: MutableCollection<AbstractTreeNode<Any>>, settings: ViewSettings?): MutableCollection<AbstractTreeNode<Any>> {
        return children.map { child ->
            LOGGER.info("Tree Node: ${child.value}")
            when (val value = child.value) {
                is VirtualFile -> {
                    LOGGER.info("Node is virtual file: ${value.name}")
                    child
                }
                else -> child
            }
        }.toMutableList()
    }

}