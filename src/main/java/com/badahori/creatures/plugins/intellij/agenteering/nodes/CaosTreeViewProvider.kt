package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.bedalton.creatures.sprite.parsers.PhotoAlbum
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
            settings: ViewSettings
    ): MutableCollection<AbstractTreeNode<out Any>> {
        val project = parent.project
                ?: return children
        if (project.isDisposed) {
            return children
        }
        return children.map { child ->
            virtualFileFromNode(child)?.toNode(project, settings, child) ?: child
        }.toMutableList()
    }

    private fun virtualFileFromNode(node: AbstractTreeNode<*>) : VirtualFile? {
        return when (val value = node.value) {
            is VirtualFile -> value
            is File -> VfsUtil.findFileByIoFile(value, true)
            is PsiFile -> value.virtualFile
            else -> null
        }.let {
            if (it?.isValid == true)
                it
            else
                null
        }
    }

    override fun getData(selected: MutableCollection<AbstractTreeNode<*>>, dataId: String): Any? = null
}


private fun VirtualFile.toNode(project:Project, viewSettings: ViewSettings, originalNode: AbstractTreeNode<*>) : AbstractTreeNode<*>? {
    if (project.isDisposed || !isValid) {
        return null
    }
    return when (extension?.lowercase()) {
        "cob", "rcb" -> {
            if (originalNode is CobFileTreeNode) {
                originalNode
            } else {
                CobFileTreeNode(project, this, viewSettings)
            }
        }
        "cos" -> {
            if (originalNode is ProjectCaosScriptFileTreeNode) {
                originalNode
            } else {
                (getPsiFile(project) as? CaosScriptFile)?.let { psiFile ->
                    ProjectCaosScriptFileTreeNode(project, psiFile, viewSettings)
                }
            }
        }
        "agents", "agent" -> {
            if (originalNode is PrayFileTreeNode) {
                originalNode
            } else {
                PrayFileTreeNode(
                    project,
                    this,
                    viewSettings
                )
            }
        }
        "photo album" -> {
            if (originalNode.presentation.locationString.isNullOrBlank()) {
                PhotoAlbum.creaturesHexMonikerToToken(this.nameWithoutExtension)?.let {
                    originalNode.presentation.locationString = it
                }
            }
            originalNode
        }
        //in VALID_SPRITE_EXTENSIONS -> SpriteFileTreeNode(project, this)
        "sfc" -> SfcFileTreeNode(project, this, viewSettings)
        else -> null
    }
}


internal const val SORT_WEIGHT = 20