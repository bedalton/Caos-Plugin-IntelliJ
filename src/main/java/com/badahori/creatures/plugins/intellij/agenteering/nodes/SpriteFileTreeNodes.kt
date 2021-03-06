package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.toPngByteArray
import com.badahori.creatures.plugins.intellij.agenteering.utils.FileNameUtils
import com.badahori.creatures.plugins.intellij.agenteering.utils.orElse
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.tree.LeafState
import icons.CaosScriptIcons


internal class SpriteFileTreeNode(
    project: Project,
    spriteVirtualFile: VirtualFile,
    viewSettings: ViewSettings?
) : VirtualFileBasedNode<VirtualFile>(project, spriteVirtualFile, viewSettings) {
    override fun getVirtualFile(): VirtualFile = value

    private val spritesVirtualFileContainer: CaosVirtualFile by lazy {
        val modulePath = ModuleUtil.findModuleForFile(spriteVirtualFile, project)?.moduleFile?.path
        val originalPath = value.path.let { path ->
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

    private val myChildren: List<SpriteImageTreeNode> by lazy {
        val fileNameBase = FileNameUtils.getBaseName(value.name).orElse("_") + "."
        val images = sprite.images
        val padLength = "${images.size}".length
        images.mapIndexed map@{ index, image ->
            image?.toPngByteArray()?.let {
                SpriteImageTreeNode(
                    project,
                    spritesVirtualFileContainer,
                    fileNameBase + "$index".padStart(padLength, '0'),
                    it,
                    viewSettings
                )
            }
        }.filterNotNull()
    }

    override fun getChildren(): List<AbstractTreeNode<*>> = myChildren

    override fun navigate(focus: Boolean) {
        PsiManager.getInstance(project!!).findFile(virtualFile)?.navigate(focus)
    }

    override fun expandOnDoubleClick(): Boolean = false
    override fun canNavigate(): Boolean =
        PsiManager.getInstance(project!!).findFile(virtualFile)?.canNavigate().orFalse()

    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = value.name
        presentationData.locationString = null
        val icon = when (virtualFile.extension?.toLowerCase()) {
            "spr" -> CaosScriptIcons.SPR_FILE_ICON
            "s16" -> CaosScriptIcons.S16_FILE_ICON
            "c16" -> CaosScriptIcons.C16_FILE_ICON
            else -> null
        }
        presentationData.setIcon(icon)
    }

    override fun getLeafState(): LeafState {
        return LeafState.ASYNC
    }
}

internal class SpriteImageTreeNode(
    project: Project,
    enclosingImage: CaosVirtualFile,
    private val fileName: String,
    data: ByteArray,
    viewSettings: ViewSettings?
) : VirtualFileBasedNode<VirtualFile>(
    project,
    enclosingImage.createChildWithContent("$fileName.png", data),
    viewSettings
) {
    override fun getVirtualFile(): VirtualFile = myVirtualFile
    override fun getChildren(): List<AbstractTreeNode<*>> = emptyList()
    override fun navigate(focus: Boolean) {
        PsiManager.getInstance(project!!).findFile(virtualFile)?.navigate(focus)
    }

    override fun canNavigate(): Boolean =
        PsiManager.getInstance(project!!).findFile(virtualFile)?.canNavigate().orFalse()

    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = fileName
        presentationData.locationString = null
        presentationData.setIcon(CaosScriptIcons.IMAGE)
    }
}