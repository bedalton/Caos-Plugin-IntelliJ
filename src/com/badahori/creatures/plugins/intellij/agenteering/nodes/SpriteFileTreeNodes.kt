package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.sprites.toPngByteArray
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.apache.commons.io.FilenameUtils


internal class SpriteFileTreeNode(
        project: Project,
        spriteVirtualFile:VirtualFile
) : AbstractTreeNode<VirtualFile>(project, spriteVirtualFile), SortableTreeElement {
    override fun getVirtualFile(): VirtualFile = value

    private val spritesVirtualFileContainer:CaosVirtualFile by lazy {
        val modulePath = ModuleUtil.findModuleForFile(spriteVirtualFile, project)?.moduleFilePath
        val originalPath = value.path.let {path ->
            if (modulePath != null && path.startsWith(modulePath))
                path.substring(modulePath.length)
            else
                path
        }
        CaosVirtualFileSystem.instance.getDirectory(originalPath, true)!!
    }

    private val sprite by lazy {
        SpriteParser.parse(value)
    }

    private val myChildren:List<SpriteImageTreeNode> by lazy {
        val fileNameBase = FilenameUtils.getBaseName(value.name) +"_"
        val images = sprite.images
        val padLength = "${images.size}".length
        images.mapIndexed map@{index, image ->
            SpriteImageTreeNode(
                    project,
                    spritesVirtualFileContainer,
                    fileNameBase+"$index".padStart(padLength, '0'),
                    image?.toPngByteArray()
            )
        }
    }

    override fun getChildren(): List<AbstractTreeNode<*>> = myChildren

    override fun navigate(focus: Boolean) {
        PsiManager.getInstance(project!!).findFile(virtualFile)?.navigate(focus)
    }
    override fun expandOnDoubleClick(): Boolean = false
    override fun canNavigate(): Boolean = PsiManager.getInstance(project!!).findFile(virtualFile)?.canNavigate().orFalse()
    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = value.name
        presentationData.locationString = null
        presentationData.setIcon(null)
    }

    override fun getWeight(): Int = 1

    override fun getAlphaSortKey(): String {
        return "$weight"
    }
}

internal class SpriteImageTreeNode(
        project: Project,
        enclosingImage: CaosVirtualFile,
        private val fileName:String,
        data:ByteArray?
) : AbstractTreeNode<VirtualFile>(project, enclosingImage.createChildWithContent("$fileName.png", data)), SortableTreeElement {
    override fun getVirtualFile(): VirtualFile = value
    override fun getChildren(): List<AbstractTreeNode<*>> = emptyList()
    override fun navigate(focus: Boolean) {
        PsiManager.getInstance(project!!).findFile(virtualFile)?.navigate(focus)
    }
    override fun canNavigate(): Boolean = PsiManager.getInstance(project!!).findFile(virtualFile)?.canNavigate().orFalse()
    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = fileName
        presentationData.locationString = null
        presentationData.setIcon(null)
    }

    override fun getWeight(): Int = 1

    override fun getAlphaSortKey(): String {
        return "$weight"
    }
}