package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.io.File

class CaosTreeViewProvider : TreeStructureProvider{
    override fun modify(
            parent: AbstractTreeNode<*>,
            children: MutableCollection<AbstractTreeNode<*>>,
            settings: ViewSettings?
    ): MutableCollection<AbstractTreeNode<out Any>> {
        val project = parent.project
                ?: return children
        return children.map { child ->
            virtualFileFromNode(child)?.toNode(project, settings) ?: child
        }.toMutableList()
    }

    private fun virtualFileFromNode(node:AbstractTreeNode<*>) : VirtualFile? {
        return when (val value = node.value) {
            is VirtualFile -> value
            is File -> VfsUtil.findFileByIoFile(value, true)
            is PsiFile -> value.virtualFile
            else -> null
        }
    }

    override fun getData(selected: MutableCollection<AbstractTreeNode<*>>, dataId: String): Any? = null
}


private fun VirtualFile.toNode(project:Project, viewSettings: ViewSettings?) : AbstractTreeNode<*>? {
    return when (extension?.toLowerCase()) {
        "cob", "rcb" -> CobFileTreeNode(project, this, viewSettings)
        "cos" -> (getPsiFile(project) as? CaosScriptFile)?.let { psiFile ->
            ProjectCaosScriptFileTreeNode(psiFile, viewSettings)
        }
        //in VALID_SPRITE_EXTENSIONS -> SpriteFileTreeNode(project, this)
        "sfc" -> SfcFileTreeNode(project, this, viewSettings)
        else -> null
    }
}


internal const val SORT_WEIGHT = 20