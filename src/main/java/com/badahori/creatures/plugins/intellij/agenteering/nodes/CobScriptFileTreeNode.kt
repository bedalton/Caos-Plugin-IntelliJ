package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.*
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.AgentBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.AuthorBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.FileBlock.SoundBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.FileBlock.SpriteBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sprites.toPngByteArray
import com.badahori.creatures.plugins.intellij.agenteering.utils.FileNameUtils
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.badahori.creatures.plugins.intellij.agenteering.utils.littleEndian
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.tree.LeafState
import icons.CaosScriptIcons
import java.awt.Color
import java.nio.ByteBuffer


internal class CobFileTreeNode(
    private val nonNullProject: Project,
    private val file: VirtualFile,
    private val viewSettings: ViewSettings?
) : VirtualFileBasedNode<VirtualFile>(nonNullProject, file, viewSettings) {

    private val cobNameWithoutExtension = file.nameWithoutExtension
    private val cobData by lazy {
        try {
            CobToDataObjectDecompiler.decompile(
                ByteBuffer.wrap(file.contentsToByteArray()).littleEndian(),
                file.nameWithoutExtension
            )
        } catch (e: Exception) {
            CobFileData.InvalidCobData("Failed to parse COB. ${e.message}")
        }
    }

    private val cobVirtualFile: CaosVirtualFile by lazy {
        CobVirtualFileUtil.getOrCreateCobVirtualFileDirectory(file)
    }

    override fun expandOnDoubleClick(): Boolean {
        return true
    }

    override fun navigate(requestFocus: Boolean) {
    }

    override fun canNavigate(): Boolean = false

    override fun canNavigateToSource(): Boolean = false

    override fun getChildren(): List<AbstractTreeNode<*>> {
        val children = when (val data = cobData) {
            is CobFileData.C1CobData -> getChildren(data)
            is CobFileData.C2CobData -> getChildren(data)
            else -> emptyList()
        }
        children.forEach {
            it.parent = this
        }
        return children
    }

    private fun getChildren(data: CobFileData.C1CobData): List<AbstractTreeNode<*>> {
        return getChildren(data.cobBlock, CaosVariant.C1)
    }

    private fun getChildren(data: CobFileData.C2CobData): List<AbstractTreeNode<*>> {
        return data.let { it.agentBlocks + it.authorBlocks + it.soundFileBlocks + it.spriteFileBlocks }
            .flatMap { getChildren(it, CaosVariant.C2) }
    }

    private fun getChildren(block: CobBlock, variant: CaosVariant): List<AbstractTreeNode<*>> {
        return when (block) {
            is SpriteBlock -> listOf(CobSpriteFileTreeNode(nonNullProject, cobVirtualFile, block, viewSettings))
            is SoundBlock -> listOf(SoundFileTreeNode(nonNullProject, cobVirtualFile, block))
            is AuthorBlock -> listOf(AuthorTreeNode(nonNullProject, block))
            is AgentBlock -> {
                val needsInstallScriptIdentifier = block.installScripts.size > 2
                val installScripts = block.installScripts.mapIndexed { i, installScript ->
                    ChildCaosScriptFileTreeNode(
                        cobNameWithoutExtension,
                        installScript.toCaosFile(nonNullProject, cobVirtualFile, variant),
                        0,
                        file.nameWithoutExtension + " Install Script" + (if (needsInstallScriptIdentifier)  " ($i)" else ""),
                        viewSettings
                    )
                }

                val scripts = installScripts + listOfNotNull(
                    block.removalScript?.let {
                        ChildCaosScriptFileTreeNode(
                            parentName = cobNameWithoutExtension,
                            caosFile = it.toCaosFile(nonNullProject, cobVirtualFile, variant),
                            scriptIndex = 0,
                            presentableTextIn = file.nameWithoutExtension + " Removal Script",
                            viewSettings
                        )
                    }
                )
                val previews: List<AbstractTreeNode<*>> = listOfNotNull(block.image?.let {
                    SpriteImageTreeNode(
                        nonNullProject,
                        cobVirtualFile,
                        "Thumbnail",
                        it.toPngByteArray(),
                        viewSettings
                    )
                })
                previews + scripts + block.eventScripts.mapIndexed { index, script ->
                    ChildCaosScriptFileTreeNode(
                        cobNameWithoutExtension,
                        script.toCaosFile(nonNullProject, cobVirtualFile, variant),
                        index,
                        viewSettings = viewSettings
                    )
                }
            }
            is CobBlock.UnknownCobBlock -> emptyList()
        }
    }

    override fun update(presentationData: PresentationData) {
        val icon = when {
            cobVirtualFile.extension like "rcb" -> CaosScriptIcons.RCB_FILE_ICON
            cobData.variant == CaosVariant.C1 -> CaosScriptIcons.C1_COB_FILE_ICON
            cobData.variant == CaosVariant.C2 -> CaosScriptIcons.C2_COB_FILE_ICON
            else -> CaosScriptIcons.COB_FILE_ICON
        }
        presentationData.setIcon(icon)
        presentationData.presentableText = file.name
        presentationData.locationString = null
        if (cobData is CobFileData.InvalidCobData) {
            presentationData.setAttributesKey(ERROR_COB_TEXT_ATTRIBUTES)
            presentationData.tooltip = "$name is invalid"
        }
    }

    override fun contains(file: VirtualFile): Boolean {
        return myVirtualFile == file || file in myVirtualFile.children
    }

    override fun getLeafState(): LeafState {
        return LeafState.ASYNC
    }
}

internal class AuthorTreeNode(project: Project, block: AuthorBlock) : AbstractTreeNode<AuthorBlock>(project, block) {
    override fun getChildren(): List<AbstractTreeNode<*>> = emptyList()
    override fun navigate(p0: Boolean) {}
    override fun canNavigate(): Boolean = false
    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = "Author"
        presentationData.locationString = null
        presentationData.setIcon(null)
    }

    override fun getName() = virtualFile.name

}

internal class CobSpriteFileTreeNode(
    project: Project,
    enclosingCob: CaosVirtualFile,
    private val block: SpriteBlock,
    private val viewSettings: ViewSettings?
) : AbstractTreeNode<VirtualFile>(project, wrapFileBlock(enclosingCob, block)) {

    private val myVirtualFile by lazy {
        enclosingCob.createChildWithContent(block.fileName, block.contents, true)
    }

    override fun getVirtualFile(): VirtualFile = myVirtualFile

    private val spritesVirtualFileContainer: CaosVirtualFile by lazy {
        enclosingCob.createChildDirectory(null, "${block.fileName}.sprites") as CaosVirtualFile
    }

    private val myChildren: List<SpriteImageTreeNode> by lazy {
        val fileNameBase = FileNameUtils.getBaseName(block.fileName).orEmpty() + "_"
        val images = block.sprite.images
        val padLength = "${images.size}".length
        images.mapIndexed map@{ index, image ->
            image?.toPngByteArray()?.let {
                SpriteImageTreeNode(
                    project,
                    spritesVirtualFileContainer,
                    fileNameBase + "$index".padStart(padLength, '0'),
                    it,
                    viewSettings = viewSettings
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
        presentationData.presentableText = block.fileName
        presentationData.locationString = null
        presentationData.setIcon(null)
    }

    override fun getName() = virtualFile.name

    override fun getLeafState(): LeafState {
        return LeafState.ASYNC
    }
}

private fun wrapFileBlock(enclosingCob: CaosVirtualFile, block: CobBlock.FileBlock): VirtualFile {
    return CaosVirtualFile(block.fileName, block.contents, false).apply {
        enclosingCob.addChild(this)
    }
}

internal class SoundFileTreeNode(
    project: Project,
    private val enclosingCob: CaosVirtualFile,
    private val block: SoundBlock
) : AbstractTreeNode<SoundBlock>(project, block) {

    internal val virtualFile by lazy {
        CaosVirtualFile(block.fileName, block.contents, false).apply {
            enclosingCob.addChild(this)
        }
    }

    override fun getChildren(): List<AbstractTreeNode<*>> = emptyList()
    override fun navigate(focus: Boolean) {
        PsiManager.getInstance(project!!).findFile(virtualFile)?.navigate(focus)
    }

    override fun canNavigate(): Boolean =
        PsiManager.getInstance(project!!).findFile(virtualFile)?.canNavigate().orFalse()

    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = block.fileName
        presentationData.locationString = "(decompiled)"
        presentationData.setIcon(null)
    }


    override fun getName() = virtualFile.name
}

private val ERROR_COB_TEXT_ATTRIBUTES =
    TextAttributesKey.createTextAttributesKey("INVALID_COB", HighlighterColors.NO_HIGHLIGHTING).apply {
        defaultAttributes.effectColor = Color(223, 45, 45)
        defaultAttributes.effectType = EffectType.WAVE_UNDERSCORE
    }