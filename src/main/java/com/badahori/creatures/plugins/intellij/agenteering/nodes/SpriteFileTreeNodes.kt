@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.nodes

import bedalton.creatures.common.util.FileNameUtil
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.myModulePath
import com.badahori.creatures.plugins.intellij.agenteering.utils.orElse
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.toPngByteArray
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
import kotlinx.coroutines.runBlocking


internal class SpriteFileTreeNode(
    project: Project,
    private val spriteVirtualFile: VirtualFile,
    viewSettings: ViewSettings?
) : VirtualFileBasedNode<VirtualFile>(project, spriteVirtualFile, viewSettings) {

    override fun getVirtualFile(): VirtualFile = spriteVirtualFile

    private val spritesVirtualFileContainer: CaosVirtualFile by lazy {
        val modulePath = ModuleUtil.findModuleForFile(spriteVirtualFile, project)?.myModulePath
        val originalPath = value.path.let { path ->
            if (modulePath != null && path.startsWith(modulePath))
                path.substring(modulePath.length)
            else
                path
        }
        CaosVirtualFileSystem.instance.getDirectory(originalPath, true)!!
    }

    private val sprite by lazy {
        runBlocking { SpriteParser.parse(value) }
    }

    private val myChildren: List<SpriteImageTreeNode> by lazy {
        if (!isValid()) {
            return@lazy emptyList()
        }
        val fileNameBase = FileNameUtil.getFileNameWithoutExtension(value.name).orElse("_") + "."
        val images = sprite.images
        val padLength = "${images.size}".length
        images.mapIndexed map@{ index, image ->
            SpriteImageTreeNode(
                project,
                spritesVirtualFileContainer,
                fileNameBase + "$index".padStart(padLength, '0'),
                image.toPngByteArray(),
                viewSettings
            )
        }
    }

    override fun getChildren(): List<AbstractTreeNode<*>> = myChildren

    override fun navigate(requestFocus: Boolean) {
        if (!isValid()) {
            return
        }
        PsiManager.getInstance(project!!).findFile(virtualFile)?.navigate(requestFocus)
    }

    override fun expandOnDoubleClick(): Boolean = false
    override fun canNavigate(): Boolean =
        isValid() && PsiManager.getInstance(project!!).findFile(virtualFile)?.canNavigate().orFalse()

    override fun canNavigateToSource(): Boolean = false

    override fun update(presentationData: PresentationData) {
        if (!isValid()) {
            return
        }
        presentationData.presentableText = value.name
        presentationData.locationString = null
        val icon = when (virtualFile.extension?.lowercase()) {
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
    override fun navigate(requestFocus: Boolean) {
        PsiManager.getInstance(project!!).findFile(virtualFile)?.navigate(requestFocus)
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