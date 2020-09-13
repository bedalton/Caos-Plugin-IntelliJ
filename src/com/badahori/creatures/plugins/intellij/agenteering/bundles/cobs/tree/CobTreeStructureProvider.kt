package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.tree

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.io.File

class CobNodeProjectViewProvider : TreeStructureProvider, DumbAware {
    override fun modify(
            parent: AbstractTreeNode<*>,
            children: MutableCollection<AbstractTreeNode<*>>,
            settings: ViewSettings?
    ): MutableCollection<AbstractTreeNode<out Any>> {
        LOGGER.info("Trying to create COB node project view")
        val project = parent.project
                ?: return children
        LOGGER.info("TREE NODE has project")
        return children.map { child ->
            val value = child.value
            val virtualFile = when (value) {
                is VirtualFile -> value
                is File -> VfsUtil.findFileByIoFile(value, true)
                is PsiFile -> value.virtualFile
                else -> null
            }
            if (virtualFile == null || !virtualFile.extension?.toLowerCase()?.endsWith("cob").orFalse()) {
                LOGGER.info("TREE NODE: ${child.value::class.java.canonicalName}(${child.value}) is not file or not COB file")
                child
            } else {
                LOGGER.info("TREE NODE: ${virtualFile.name} is COB file")
                CobFileTreeNode(project, virtualFile)
            }
        }.toMutableList()
    }

    override fun getData(selected: MutableCollection<AbstractTreeNode<*>>, dataId: String): Any? = null
}