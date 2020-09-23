package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.io.File

class SfcTreeStructureProvider : TreeStructureProvider {
    override fun modify(
            parent: AbstractTreeNode<*>,
            children: MutableCollection<AbstractTreeNode<*>>,
            settings: ViewSettings?
    ): MutableCollection<AbstractTreeNode<out Any>> {
        val project = parent.project
                ?: return children
        return children.map { child ->
            virtualFileFromNode(child)?.toNode(project) ?: child
        }.toMutableList()
    }

    private fun virtualFileFromNode(node: AbstractTreeNode<*>): VirtualFile? {
        return when (val value = node.value) {
            is VirtualFile -> value
            is File -> VfsUtil.findFileByIoFile(value, true)
            is PsiFile -> value.virtualFile
            else -> null
        }
    }
}


private fun VirtualFile.toNode(project: Project): AbstractTreeNode<*>? {
    return if (extension?.toLowerCase() != "sfc")
        null
    else
        SfcFileTreeNode(project, this)
}