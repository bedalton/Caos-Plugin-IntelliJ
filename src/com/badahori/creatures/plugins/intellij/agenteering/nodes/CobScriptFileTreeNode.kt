package com.badahori.creatures.plugins.intellij.agenteering.nodes

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
import com.badahori.creatures.plugins.intellij.agenteering.sprites.toPngByteArray
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
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
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
        return getChildren(file.name, data.cobBlock, CaosVariant.C1)
    }

    private fun getChildren(data: CobFileData.C2CobData): List<AbstractTreeNode<*>> {
        val cobFile = file.name
        return data.let { it.agentBlocks + it.authorBlocks + it.soundFileBlocks + it.spriteFileBlocks }.flatMap { getChildren(cobFile, it, CaosVariant.C2) }
    }

    private fun getChildren(cobFile: String, block: CobBlock, variant: CaosVariant): List<AbstractTreeNode<*>> {
        return when (block) {
            is SpriteBlock -> listOf(CobSpriteFileTreeNode(nonNullProject, cobVirtualFile, block))
            is SoundBlock -> listOf(SoundFileTreeNode(nonNullProject, cobVirtualFile, block))
            is AuthorBlock -> listOf(AuthorTreeNode(nonNullProject, cobFile, block))
            is AgentBlock -> {
                val scripts = listOfNotNull(
                        block.installScript?.let { CobScriptFileTreeNode(it.toCaosFile(nonNullProject, cobVirtualFile, variant), cobFile, 0, "Install Script") },
                        block.removalScript?.let { CobScriptFileTreeNode(it.toCaosFile(nonNullProject, cobVirtualFile, variant), cobFile, 0, "Install Script") }
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
                    CobScriptFileTreeNode(script.toCaosFile(nonNullProject, cobVirtualFile, variant), cobFile, index)
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

internal class AuthorTreeNode(project: Project, private val enclosingCobFileName: String, block: AuthorBlock)
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

internal class CobScriptFileTreeNode(
        private val caosFile: CaosScriptFile,
        private val enclosingCobFileName: String,
        private val scriptIndex: Int,
        private val presentableTextIn: String? = null,
        private val alwaysLeaf: Boolean = PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java).size < 2
) : AbstractTreeNode<CaosScriptFile>(caosFile.project, caosFile), SortableTreeElement  {

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

    override fun getAlphaSortKey(): String {
        return "$weight"
    }

    override fun getWeight(): Int {
        val script = PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java)
                .firstOrNull { it !is CaosScriptMacro }
        return when (script) {
            is CaosScriptInstallScript -> 7
            is CaosScriptRemovalScript -> 8
            else -> 9
        }
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
        virtualFile?.isWritable = false
        (script as? Navigatable)?.navigate(requestFocus)
    }

    override fun getVirtualFile(): VirtualFile? {
        return (script.containingFile ?: script.originalElement.containingFile).virtualFile
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

    override fun getWeight(): Int = when(script) {
        is CaosScriptInstallScript -> 7
        is CaosScriptRemovalScript -> 8
        else -> 9
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
            caosFile.virtualFile?.isWritable = false
        }
    }
    command.execute()
}

private val decompiledId = AtomicInteger(0)

internal fun getCobVirtualFileDirectory(file: VirtualFile) : CaosVirtualFile {
    val path = "Decompiled (${decompiledId.incrementAndGet()})/${file.name}"
    return CaosVirtualFileSystem.instance.getDirectory(path, true)!!.apply {
        isWritable = false
    }
}