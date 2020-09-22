package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.AgentBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.AuthorBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.FileBlock.SoundBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.FileBlock.SpriteBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobFileData
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobToDataObjectDecompiler
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sprites.toPngByteArray
import com.badahori.creatures.plugins.intellij.agenteering.utils.littleEndian
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.apache.commons.io.FilenameUtils
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger


class CobFileTreeNode(
        private val nonNullProject: Project,
        private val file: VirtualFile
) : AbstractTreeNode<VirtualFile>(nonNullProject, file), SortableTreeElement {

    val cobData = CobToDataObjectDecompiler.decompile(ByteBuffer.wrap(file.contentsToByteArray()).littleEndian())

    private val cobVirtualFile: CaosVirtualFile by lazy {
        getCobVirtualFileDirectory(file)
    }

    override fun expandOnDoubleClick(): Boolean {
        return true
    }

    override fun navigate(requestFocus: Boolean) {
    }

    override fun canNavigate(): Boolean = false

    override fun canNavigateToSource(): Boolean = false

    override fun getChildren(): List<AbstractTreeNode<*>> = when (cobData) {
        is CobFileData.C1CobData -> getChildren(cobData)
        is CobFileData.C2CobData -> getChildren(cobData)
        else -> emptyList()
    }

    private fun getChildren(data: CobFileData.C1CobData): List<AbstractTreeNode<*>> {
        return getChildren(data.cobBlock, CaosVariant.C1)
    }

    private fun getChildren(data: CobFileData.C2CobData): List<AbstractTreeNode<*>> {
        return data.let { it.agentBlocks + it.authorBlocks + it.soundFileBlocks + it.spriteFileBlocks }.flatMap { getChildren(it, CaosVariant.C2) }
    }

    private fun getChildren(block: CobBlock, variant: CaosVariant): List<AbstractTreeNode<*>> {
        return when (block) {
            is SpriteBlock -> listOf(CobSpriteFileTreeNode(nonNullProject, cobVirtualFile, block))
            is SoundBlock -> listOf(SoundFileTreeNode(nonNullProject, cobVirtualFile, block))
            is AuthorBlock -> listOf(AuthorTreeNode(nonNullProject, block))
            is AgentBlock -> {
                val scripts = listOfNotNull(
                        block.installScript?.let { CaosScriptFileTreeNode(it.toCaosFile(nonNullProject, cobVirtualFile, variant), 0, "Install Script") },
                        block.removalScript?.let { CaosScriptFileTreeNode(it.toCaosFile(nonNullProject, cobVirtualFile, variant), 0, "Install Script") }
                )
                val previews:List<AbstractTreeNode<*>> = listOfNotNull(block.image?.let {
                    SpriteImageTreeNode(
                            nonNullProject,
                            cobVirtualFile,
                            "Thumbnail",
                                it.toPngByteArray()
                            )
                })
                previews + scripts + block.eventScripts.mapIndexed { index, script ->
                    CaosScriptFileTreeNode(script.toCaosFile(nonNullProject, cobVirtualFile, variant), index)
                }
            }
            is CobBlock.UnknownCobBlock -> emptyList()
        }
    }


    override fun getAlphaSortKey(): String {
        return file.name.toLowerCase()
    }

    override fun update(presentationData: PresentationData) {
        presentationData.setIcon(null)
        presentationData.presentableText = file.name
        presentationData.locationString = null
    }
}

internal class AuthorTreeNode(project: Project, block: AuthorBlock)
    : AbstractTreeNode<AuthorBlock>(project, block), SortableTreeElement {
    override fun getChildren(): List<AbstractTreeNode<*>> = emptyList()
    override fun navigate(p0: Boolean) {}
    override fun canNavigate(): Boolean = false
    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = "Author"
        presentationData.locationString = null
        presentationData.setIcon(null)
    }


    override fun getAlphaSortKey(): String {
        return "$weight"
    }

    override fun getWeight(): Int = 0
}

internal class CobSpriteFileTreeNode(
        project: Project,
        enclosingCob:CaosVirtualFile,
        private val block: SpriteBlock
) : AbstractTreeNode<VirtualFile>(project, wrapFileBlock(enclosingCob, block)), SortableTreeElement {
    override fun getVirtualFile(): VirtualFile = value

    private val spritesVirtualFileContainer:CaosVirtualFile by lazy {
        enclosingCob.createChildDirectory(null, "${block.fileName}.sprites") as CaosVirtualFile
    }

    private val myChildren:List<SpriteImageTreeNode> by lazy {
        val fileNameBase = FilenameUtils.getBaseName(block.fileName) +"_"
        val images = block.sprite.images
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
        presentationData.presentableText = block.fileName
        presentationData.locationString = null
        presentationData.setIcon(null)
    }

    override fun getWeight(): Int = 1

    override fun getAlphaSortKey(): String {
        return "$weight"
    }
}

private fun wrapFileBlock(enclosingCob:CaosVirtualFile, block:CobBlock.FileBlock) : VirtualFile {
    return CaosVirtualFile(block.fileName, block.contents, false).apply {
        enclosingCob.addChild(this)
    }
}

internal class SoundFileTreeNode(project: Project, private val enclosingCob: CaosVirtualFile, private val block: SoundBlock)
    : AbstractTreeNode<SoundBlock>(project, block) , SortableTreeElement{

    private val virtualFile  by lazy {
        CaosVirtualFile(block.fileName, block.contents, false).apply {
            enclosingCob.addChild(this)
        }
    }
    override fun getChildren(): List<AbstractTreeNode<*>> = emptyList()
    override fun navigate(focus: Boolean) {
        PsiManager.getInstance(project!!).findFile(virtualFile)?.navigate(focus)
    }
    override fun canNavigate(): Boolean = PsiManager.getInstance(project!!).findFile(virtualFile)?.canNavigate().orFalse()
    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = block.fileName
        presentationData.locationString = "(decompiled)"
        presentationData.setIcon(null)
    }


    override fun getAlphaSortKey(): String {
        return "$weight"
    }
    override fun getWeight(): Int = 2

}

private val decompiledId = AtomicInteger(0)

internal fun getCobVirtualFileDirectory(file: VirtualFile) : CaosVirtualFile {
    val path = "Decompiled (${decompiledId.incrementAndGet()})/${file.name}"
    return CaosVirtualFileSystem.instance.getDirectory(path, true)!!.apply {
        isWritable = false
    }
}