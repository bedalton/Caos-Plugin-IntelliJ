package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.tree

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.AgentBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.AuthorBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.FileBlock.SoundBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.FileBlock.SpriteBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobFileData
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobToDataObjectDecompiler
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.littleEndian
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import java.nio.ByteBuffer
import java.util.*


class CobFileTreeNode(
        private val nonNullProject: Project,
        private val file: VirtualFile
) : AbstractTreeNode<VirtualFile>(nonNullProject, file) {

    val cobData = CobToDataObjectDecompiler.decompile(ByteBuffer.wrap(file.contentsToByteArray()).littleEndian())

    private val cobVirtualFile: CaosVirtualFile by lazy {
        val parentFolder = CaosVirtualFile(UUID.randomUUID().toString(), null, true)
        CaosVirtualFileSystem.instance.addFile(parentFolder)
        CaosVirtualFile(file.name, null, true).apply { parentFolder.addChild(this) }
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
        return getChildren(file.name, data.cobBlock, CaosVariant.C1)
    }

    private fun getChildren(data: CobFileData.C2CobData): List<AbstractTreeNode<*>> {
        val cobFile = file.name
        return data.let { it.agentBlocks + it.authorBlocks + it.soundFileBlocks + it.spriteFileBlocks }.flatMap { getChildren(cobFile, it, CaosVariant.C2) }
    }

    private fun getChildren(cobFile: String, block: CobBlock, variant: CaosVariant): List<AbstractTreeNode<*>> {
        return when (block) {
            is SpriteBlock -> listOf(SpriteFileTreeNode(nonNullProject, cobFile, block))
            is SoundBlock -> listOf(SoundFileTreeNode(nonNullProject, cobFile, block))
            is AuthorBlock -> listOf(AuthorTreeNode(nonNullProject, cobFile, block))
            is AgentBlock -> {
                val scripts = listOfNotNull(
                        block.installScript?.let { CobScriptFileTreeNode(it.toCaosFile(nonNullProject, cobVirtualFile, variant), cobFile, 0, "Install Script") },
                        block.removalScript?.let { CobScriptFileTreeNode(it.toCaosFile(nonNullProject, cobVirtualFile, variant), cobFile, 0, "Install Script") }
                )
                scripts + block.eventScripts.mapIndexed { index, script ->
                    CobScriptFileTreeNode(script.toCaosFile(nonNullProject, cobVirtualFile, variant), cobFile, index)
                }
            }
            is CobBlock.UnknownCobBlock -> emptyList()
        }
    }

    override fun update(presentationData: PresentationData) {
        presentationData.setIcon(null)
        presentationData.presentableText = file.name
        presentationData.locationString = null
    }
}

class AuthorTreeNode(project: Project, private val enclosingCobFileName: String, block: AuthorBlock)
    : AbstractTreeNode<AuthorBlock>(project, block) {
    override fun getChildren(): List<AbstractTreeNode<*>> = emptyList()
    override fun navigate(p0: Boolean) {}
    override fun canNavigate(): Boolean = false
    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = "Author"
        presentationData.locationString = enclosingCobFileName
        presentationData.setIcon(null)
    }
}

class SpriteFileTreeNode(
        project: Project,
        private val enclosingCobFileName: String,
        private val block: SpriteBlock
) : AbstractTreeNode<SpriteBlock>(project, block) {
    override fun getChildren(): List<AbstractTreeNode<*>> = emptyList()
    override fun navigate(p0: Boolean) {}
    override fun canNavigate(): Boolean = false
    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = block.fileName
        presentationData.locationString = enclosingCobFileName
        presentationData.setIcon(null)
    }
}

class SoundFileTreeNode(project: Project, private val enclosingCobFileName: String, private val block: SoundBlock)
    : AbstractTreeNode<SoundBlock>(project, block) {
    override fun getChildren(): List<AbstractTreeNode<*>> = emptyList()
    override fun navigate(p0: Boolean) {}
    override fun canNavigate(): Boolean = false
    override fun canNavigateToSource(): Boolean = false
    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = block.fileName
        presentationData.locationString = enclosingCobFileName
        presentationData.setIcon(null)
    }

}

class CobScriptFileTreeNode(
        private val caosFile: CaosScriptFile,
        private val enclosingCobFileName: String,
        private val scriptIndex: Int,
        private val presentableTextIn: String? = null,
        private val alwaysLeaf: Boolean = PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java).size < 2
) : AbstractTreeNode<CaosScriptFile>(caosFile.project, caosFile) {

    override fun isAlwaysLeaf(): Boolean {
        return alwaysLeaf
    }

    override fun navigate(requestFocus: Boolean) {
        quickFormat(caosFile)
        caosFile.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return caosFile.canNavigateToSource()
    }

    override fun canNavigateToSource(): Boolean {
        return caosFile.canNavigateToSource()
    }

    private fun getPresentableText(): String {
        this@CobScriptFileTreeNode.presentableTextIn?.let {
            return it
        }
        val scripts = PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java)
        if (scripts.size != 1 || scripts.filter { it !is CaosScriptMacro }.size != 1) {
            return "Script $scriptIndex"
        }
        return when (val script = scripts.firstOrNull { it !is CaosScriptMacro }) {
            is CaosScriptEventScript -> "${script.family} ${script.genus} ${script.species} ${script.eventNumber}"
            is CaosScriptInstallScript -> "Install Script"
            is CaosScriptRemovalScript -> "Removal Script"
            else -> "Script $scriptIndex"
        }
    }

    override fun getChildren(): List<AbstractTreeNode<*>> {
        val scripts = PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java)
        if (scripts.size != 1 || scripts.filter { it !is CaosScriptMacro }.size != 1) {
            return scripts.map {
                SubScriptLeafNode(it, enclosingCobFileName)
            }
        }
        return emptyList()
    }

    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = getPresentableText()
        presentationData.locationString = "(Decompiled)"
    }
}


internal class SubScriptLeafNode(
        private val script: CaosScriptScriptElement,
        private val enclosingCobFileName: String
) : AbstractTreeNode<CaosScriptScriptElement>(script.project, script), SortableTreeElement {

    private val text by lazy {
        when (script) {
            is CaosScriptEventScript -> "${script.family} ${script.genus} ${script.species} ${script.eventNumber}"
            is CaosScriptInstallScript -> "Install Script"
            is CaosScriptRemovalScript -> "Removal Script"
            else -> "Body Script"
        }
    }

    override fun getChildren(): List<AbstractTreeNode<*>> {
        return emptyList()
    }

    override fun navigate(requestFocus: Boolean) {
        (script as? Navigatable)?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = (script as? Navigatable)?.canNavigate().orFalse()

    override fun canNavigateToSource(): Boolean = (script as? Navigatable)?.canNavigateToSource().orFalse()

    override fun getAlphaSortKey(): String {
        return when (script) {
            is CaosScriptEventScript -> "x" + listOf(script.family, script.genus, script.species, script.eventNumber)
                    .joinToString("|") { it.toString().padStart(8, '0') }
            is CaosScriptInstallScript -> "0"
            is CaosScriptRemovalScript -> "1"
            else -> "z"
        }
    }

    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = text
        presentationData.locationString = enclosingCobFileName
    }
}

internal fun quickFormat(caosFile: CaosScriptFile) {
    val variant = caosFile.variant ?: CaosVariant.C1
    val command: WriteCommandAction<Void?> = object : WriteCommandAction.Simple<Void?>(caosFile.project, caosFile) {
        override fun run() {
            caosFile.virtualFile?.isWritable = true
            caosFile.variant = variant
            CaosScriptExpandCommasIntentionAction.invoke(project, caosFile)
            caosFile.variant = variant
            /*CodeStyleManager.getInstance(project).reformat(caosFile, false)
            caosFile.variant = variant*/
        }
    }
    command.execute()
}