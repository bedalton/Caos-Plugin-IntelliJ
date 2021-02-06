package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode


class CaosFileTreeStructureProvider : TreeStructureProvider {
    override fun modify(
        parent: AbstractTreeNode<*>,
        children: MutableCollection<AbstractTreeNode<*>>,
        settings: ViewSettings?
    ): MutableCollection<AbstractTreeNode<*>> {
        val project = parent.project
            ?: return children
        val nodes = mutableListOf<AbstractTreeNode<*>>()
        for (child in children) {
            if (child !is PsiFileNode) {
                nodes.add(child)
                continue
            }
            val virtualFile = child.virtualFile
            if (virtualFile == null) {
                nodes.add(child)
                continue
            }
            val psiFile = virtualFile.getPsiFile(project)
            if (psiFile != null) {
                if (psiFile is CaosScriptFile) {
                    nodes.add(ProjectCaosScriptFileTreeNode(psiFile))
                    continue
                }
            }
            nodes.add(child)
        }
        return nodes
    }
}