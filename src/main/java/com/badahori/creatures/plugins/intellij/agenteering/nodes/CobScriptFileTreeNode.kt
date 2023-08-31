package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.bedalton.common.util.PathUtil
import com.bedalton.log.*
import com.bedalton.common.util.toListOf
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.*
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.FileBlock.SoundBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.FileBlock.SpriteBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobFileData
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobToDataObjectDecompiler
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobVirtualFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.bedalton.common.util.formatted
import com.bedalton.io.bytes.internal.MemoryByteStreamReaderEx
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.ui.tree.LeafState
import icons.CaosScriptIcons
import kotlinx.coroutines.runBlocking
import java.awt.Color


internal class CobFileTreeNode(
    project: Project,
    private val file: VirtualFile,
    private val viewSettings: ViewSettings?,
) : VirtualFileBasedNode<VirtualFile>(project, file, viewSettings) {

    private val cobNameWithoutExtension = file.nameWithoutExtension
    private val cobData by lazy {
        if (!isValid()) {
            return@lazy CobFileData.InvalidCobData("Cob file state invalid")
        }
        runBlocking {
            try {
                CobToDataObjectDecompiler.decompile(
                    MemoryByteStreamReaderEx(file.contentsToByteArray()),
                    file.nameWithoutExtension
                )
            } catch (e: Exception) {
                Log.e { "Failed to parse COB. ${e.formatted(true)}" }
                CobFileData.InvalidCobData("Failed to parse COB. ${e.message}")
            }
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
        if (!isValid()) {
            return emptyList()
        }
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
        return getChildren(data.cobBlock, CaosVariant.C1, true)
    }

    private fun getChildren(data: CobFileData.C2CobData): List<AbstractTreeNode<*>> {
        val blocks = data.let { it.agentBlocks + it.authorBlocks + it.soundFileBlocks + it.spriteFileBlocks }
        val solo = data.agentBlocks.size == 1
        return blocks.flatMap { getChildren(it, CaosVariant.C2, solo) }
    }

    private fun getChildren(block: CobBlock, variant: CaosVariant, solo: Boolean): List<AbstractTreeNode<*>> {
        if (!isValid()) {
            return emptyList()
        }
        return when (block) {
            is SpriteBlock -> listOf(CobSpriteFileTreeNode(nonNullProject, cobVirtualFile, block, viewSettings))
            is SoundBlock -> listOf(SoundFileTreeNode(nonNullProject, cobVirtualFile, block))
            is AuthorBlock -> listOf(AuthorTreeNode(nonNullProject, cobVirtualFile, block, viewSettings))
            is AgentBlock -> {
                if (solo) {
                    flattenedAgent(block, variant)
                } else {
                    CobAgentTreeNode(
                        nonNullProject,
                        cobVirtualFile,
                        block,
                        variant,
                        viewSettings
                    ).toListOf()
                }
            }

            is UnknownCobBlock -> emptyList()
        }
    }

    private fun flattenedAgent(block: AgentBlock, variant: CaosVariant): List<AbstractTreeNode<*>> {
        val needsInstallScriptIdentifier = block.installScripts.size > 2
        val installScripts = block.installScripts.mapIndexed { i, installScript ->
            ChildCaosScriptFileTreeNode(
                nonNullProject,
                cobNameWithoutExtension,
                installScript.toCaosFile(nonNullProject, cobVirtualFile, variant),
                0,
                file.nameWithoutExtension + " Install Script" + (if (needsInstallScriptIdentifier) " ($i)" else ""),
                viewSettings
            )
        }

        val scripts = installScripts + listOfNotNull(
            block.removalScript?.nullIfEmpty()?.let {
                ChildCaosScriptFileTreeNode(
                    nonNullProject,
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
        return previews + scripts + block.eventScripts.mapIndexed { index, script ->
            ChildCaosScriptFileTreeNode(
                nonNullProject,
                cobNameWithoutExtension,
                script.toCaosFile(nonNullProject, cobVirtualFile, variant),
                index,
                viewSettings = viewSettings
            )
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
        return myVirtualFile.isValid && (myVirtualFile == file || file in myVirtualFile.children)
    }

    override fun getLeafState(): LeafState {
        return LeafState.ASYNC
    }
}

internal class CobAgentTreeNode(
    private val nonNullProject: Project,
    private val parentCobFile: VirtualFile,
    private val agentBlock: AgentBlock,
    private val variant: CaosVariant,
    private val viewSettings: ViewSettings?,
) : AbstractTreeNode<AgentBlock>(nonNullProject, agentBlock) {

    private val agentName get() = StringUtil.toTitleCase(agentBlock.name)

    private val cobVirtualFile: CaosVirtualFile by lazy {
        CobVirtualFileUtil.getOrCreateCobVirtualFileDirectory(parentCobFile).let { parent ->
            if (parent.hasChild(agentName)) {
                var filename: String
                do {
                    filename = agentName + '_' + randomString(4)
                } while (parent.hasChild(filename))
                parent.createChildDirectory(filename)
            } else {
                parent.createChildDirectory(agentName)
            }
        }
    }

    override fun expandOnDoubleClick(): Boolean {
        return true
    }

    override fun navigate(requestFocus: Boolean) {
    }

    override fun canNavigate(): Boolean = false

    override fun canNavigateToSource(): Boolean = false

    override fun getChildren(): List<AbstractTreeNode<*>> {
        val children = getChildren(agentBlock, CaosVariant.C2)
        children.forEach {
            it.parent = this
        }
        return children
    }

    private fun getChildren(block: AgentBlock, variant: CaosVariant): List<AbstractTreeNode<*>> {
        val needsInstallScriptIdentifier = block.installScripts.size > 2
        val installScripts = block.installScripts.mapIndexed { i, installScript ->
            ChildCaosScriptFileTreeNode(
                nonNullProject,
                agentName,
                installScript.toCaosFile(nonNullProject, cobVirtualFile, variant),
                0,
                agentName + " Install Script" + (if (needsInstallScriptIdentifier) " ($i)" else ""),
                viewSettings
            )
        }

        val scripts = installScripts + listOfNotNull(
            block.removalScript?.nullIfEmpty()?.let {
                ChildCaosScriptFileTreeNode(
                    nonNullProject,
                    parentName = agentName,
                    caosFile = it.toCaosFile(nonNullProject, cobVirtualFile, variant),
                    scriptIndex = 0,
                    presentableTextIn = "$agentName Removal Script",
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
        return previews + scripts + block.eventScripts.mapIndexed { index, script ->
            ChildCaosScriptFileTreeNode(
                nonNullProject,
                agentName,
                script.toCaosFile(nonNullProject, cobVirtualFile, variant),
                index,
                viewSettings = viewSettings
            )
        }
    }

    override fun getWeight(): Int {
        return 10
    }

    override fun update(presentationData: PresentationData) {
        val icon = when (variant) {
            CaosVariant.C1 -> CaosScriptIcons.C1_COB_AGENT_ICON
            CaosVariant.C2 -> CaosScriptIcons.C2_COB_AGENT_ICON
            else -> CaosScriptIcons.C1_COB_AGENT_ICON
        }
        presentationData.setIcon(icon)
        presentationData.presentableText = agentName
        presentationData.locationString = null
    }

    override fun getLeafState(): LeafState {
        return LeafState.ASYNC
    }
}

internal class AuthorTreeNode(
    project: Project,
    parent: CaosVirtualFile,
    block: AuthorBlock,
    viewSettings: ViewSettings?,
) : VirtualFileBasedNode<VirtualFile>(project, blockToVirtualFile(parent, block), viewSettings) {

    private val projectNotNull = project
    override fun getChildren(): List<AbstractTreeNode<*>> = emptyList()
    override fun navigate(requestFocus: Boolean) {
        if (projectNotNull.isDisposed) {
            return
        }
        virtualFile.getPsiFile(projectNotNull)?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = virtualFile.contentsToByteArray().size > 10
    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = "Author"
        presentationData.locationString = null
        presentationData.setIcon(null)
    }

    override fun getName() = virtualFile.name

    override fun getWeight(): Int {
        return super.getWeight() * 3
    }

    companion object {
        private fun blockToVirtualFile(parent: CaosVirtualFile, authorBlock: AuthorBlock): VirtualFile {
            val pairs = mutableListOf(
                "# Author" to "",
                "Name" to authorBlock.authorName,
                "Website" to authorBlock.authorUrl,
                "Email" to authorBlock.authorEmail,
                "Comments" to authorBlock.authorComments,
                "# Agent Information" to "",
                "Version" to authorBlock.version.toString(),
                "Revision" to authorBlock.revision.toString()
            )
            val text: String = if (pairs.all { it.second.isNullOrBlank() }) {
                ""
            } else {
                val out = StringBuilder()
                val padding = (pairs.maxOfOrNull { if (it.second.isNullOrBlank()) 0 else it.first.length } ?: 0) + 6
                for ((field, value) in pairs) {
                    if (value == null) {
                        continue
                    }
                    if (value.isBlank()) {
                        out.appendLine(field)
                    } else {
                        out.append(field)
                            .append(' ')
                            .append(".".repeat(padding - field.length))
                            .append(' ')
                            .append(value)
                            .appendLine()
                    }
                }
                out.toString()
            }
            return CobVirtualFileUtil.createChildTextFile(parent, "Author", text)
        }
    }

}

internal class CobSpriteFileTreeNode(
    private val nonNullProject: Project,
    enclosingCob: CaosVirtualFile,
    private val block: SpriteBlock,
    private val viewSettings: ViewSettings?,
) : AbstractTreeNode<VirtualFile>(nonNullProject, wrapFileBlock(enclosingCob, block)) {

    fun isValid(): Boolean {
        return !nonNullProject.isDisposed && myVirtualFile.isValid
    }

    private val myVirtualFile by lazy {
        enclosingCob.createChildWithContent(block.fileName, block.contents, true)
    }

    override fun getVirtualFile(): VirtualFile = myVirtualFile

    private val spritesVirtualFileContainer: CaosVirtualFile by lazy {
        enclosingCob["${block.fileName}.sprites"]?.let {
            enclosingCob.delete(it)
        }
        enclosingCob.createChildDirectory(null, "${block.fileName}.sprites") as CaosVirtualFile
    }

    private val myChildren: List<SpriteImageTreeNode> by lazy {
        if (!isValid()) {
            return@lazy emptyList()
        }
        val fileNameBase = PathUtil.getFileNameWithoutExtension(block.fileName).orEmpty() + "_"
        val images = block.sprite.images
        val padLength = "${images.size}".length
        images.mapIndexed map@{ index, image ->
            SpriteImageTreeNode(
                nonNullProject,
                spritesVirtualFileContainer,
                fileNameBase + "$index".padStart(padLength, '0'),
                image.toPngByteArray(),
                viewSettings = viewSettings
            )
        }
    }

    override fun getChildren(): List<AbstractTreeNode<*>> = myChildren

    override fun navigate(focus: Boolean) {
        if (!isValid())
            return
        PsiManager.getInstance(project!!).findFile(virtualFile)?.navigate(focus)
    }

    override fun expandOnDoubleClick(): Boolean = false

    override fun canNavigate(): Boolean =
        isValid() && PsiManager.getInstance(project!!).findFile(virtualFile)?.canNavigate().orFalse()

    override fun canNavigateToSource(): Boolean = false

    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = block.fileName
        presentationData.locationString = null
        presentationData.setIcon(getFileIcon(block.fileName))
    }

    override fun getName() = virtualFile.name

    override fun getLeafState(): LeafState {
        return LeafState.ASYNC
    }
}

private fun wrapFileBlock(enclosingCob: CaosVirtualFile, block: FileBlock): VirtualFile {
    return CaosVirtualFile(block.fileName, block.contents, false).apply {
        enclosingCob.addChild(this)
    }
}

internal class SoundFileTreeNode(
    project: Project,
    private val enclosingCob: CaosVirtualFile,
    private val block: SoundBlock,
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
        presentationData.setIcon(getFileIcon(virtualFile.name))
    }


    override fun getName() = virtualFile.name
}

private val ERROR_COB_TEXT_ATTRIBUTES =
    TextAttributesKey.createTextAttributesKey("INVALID_COB", HighlighterColors.NO_HIGHLIGHTING).apply {
        defaultAttributes.effectColor = JBColor(
            Color(223, 45, 45),
            Color(218, 30, 35)
        )
        defaultAttributes.effectType = EffectType.WAVE_UNDERSCORE
    }