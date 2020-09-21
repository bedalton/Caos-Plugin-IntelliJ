package com.badahori.creatures.plugins.intellij.agenteering.sfc.tree

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode

class SfcTreeStructureProvider : TreeStructureProvider{
    override fun modify(node: AbstractTreeNode<*>, children: MutableCollection<AbstractTreeNode<Any>>, settings: ViewSettings?): MutableCollection<AbstractTreeNode<Any>> {
        return children.map { child ->
            when (child.value) {
                else -> child
            }
        }.toMutableList()
    }

}