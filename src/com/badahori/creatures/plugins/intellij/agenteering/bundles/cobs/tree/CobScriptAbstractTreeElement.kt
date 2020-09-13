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
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import java.nio.ByteBuffer
import java.util.*
import javax.swing.Icon


class CobFileViewTreeElement(
        private val file:VirtualFile,
        private val project:Project
) : StructureViewTreeElement {

    val cobData = CobToDataObjectDecompiler.decompile(ByteBuffer.wrap(file.contentsToByteArray()).littleEndian())

    private val cobVirtualFile:CaosVirtualFile by lazy {
        val parentFolder = CaosVirtualFile(UUID.randomUUID().toString(), null, true)
        CaosVirtualFile(file.name, null, true).apply { parent = parentFolder }
    }

    override fun getValue(): Any {
        return file
    }

    override fun navigate(requestFocus: Boolean) {
    }

    override fun canNavigate(): Boolean = false

    override fun canNavigateToSource(): Boolean = false

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String? = file.name

            override fun getLocationString(): String? = null

            override fun getIcon(p0: Boolean): Icon? = null
        }
    }

    override fun getChildren(): Array<TreeElement> = when (cobData) {
        is CobFileData.C1CobData -> getChildren(cobData).toTypedArray()
        is CobFileData.C2CobData -> getChildren(cobData).toTypedArray()
        else -> emptyArray()
    }

    private fun getChildren(data:CobFileData.C1CobData) :List<TreeElement> {
        return getChildren(cobVirtualFile, data.cobBlock, CaosVariant.C1)
    }

    private fun getChildren(data:CobFileData.C2CobData) : List<TreeElement> {
        return data.let { it.agentBlocks + it.authorBlocks + it.soundFileBlocks + it.spriteFileBlocks }.flatMap { getChildren(cobVirtualFile, it, CaosVariant.C2) }
    }

    private fun getChildren(cobFile:CaosVirtualFile, block:CobBlock, variant:CaosVariant) : List<TreeElement> {
        return when(block) {
            is SpriteBlock -> listOf(SpriteFileTreeElement(cobFile.name, block))
            is SoundBlock -> listOf(SoundFileTreeElement(cobFile.name, block))
            is AuthorBlock -> listOf(AuthorTreeElement(cobFile.name, block))
            is AgentBlock -> {
                val scripts = listOfNotNull(
                        block.installScript?.let { CobScriptFileViewTreeElement(it.toCaosFile(project, cobFile, variant), cobFile.name, 0, "Install Script") },
                        block.removalScript?.let { CobScriptFileViewTreeElement(it.toCaosFile(project, cobFile, variant), cobFile.name, 0, "Install Script") }
                )
                scripts + block.eventScripts.mapIndexed { index, script ->
                    CobScriptFileViewTreeElement(script.toCaosFile(project, cobFile, variant), cobFile.name, index)
                }
            }
            is CobBlock.UnknownCobBlock -> emptyList()
        }
    }
}

class AuthorTreeElement(private val cobFile:String, private val block: AuthorBlock):StructureViewTreeElement {
    override fun getPresentation(): ItemPresentation {
        return object:ItemPresentation {
            override fun getPresentableText(): String? = "Author"
            override fun getLocationString(): String? = cobFile
            override fun getIcon(p0: Boolean): Icon? = null
        }
    }
    override fun getChildren(): Array<TreeElement> = emptyArray()
    override fun navigate(p0: Boolean) {}
    override fun canNavigate(): Boolean = false
    override fun canNavigateToSource(): Boolean = false
    override fun getValue(): Any = block
}

class SpriteFileTreeElement(private val cobFile:String, private val block: SpriteBlock):StructureViewTreeElement {
    override fun getPresentation(): ItemPresentation {
        return object:ItemPresentation {
            override fun getPresentableText(): String? = block.fileName
            override fun getLocationString(): String? = cobFile
            override fun getIcon(p0: Boolean): Icon? = null
        }
    }
    override fun getChildren(): Array<TreeElement> = emptyArray()
    override fun navigate(p0: Boolean) {}
    override fun canNavigate(): Boolean = false
    override fun canNavigateToSource(): Boolean = false
    override fun getValue(): Any = block.sprite
}

class SoundFileTreeElement(private val cobFile:String, private val block: SoundBlock):StructureViewTreeElement {
    override fun getPresentation(): ItemPresentation {
        return object:ItemPresentation {
            override fun getPresentableText(): String? = block.fileName
            override fun getLocationString(): String? = cobFile
            override fun getIcon(p0: Boolean): Icon? = null
        }
    }

    override fun getChildren(): Array<TreeElement> = emptyArray()
    override fun navigate(p0: Boolean) {}
    override fun canNavigate(): Boolean = false
    override fun canNavigateToSource(): Boolean = false
    override fun getValue(): Any = block

}

class CobScriptFileViewTreeElement(
        private val caosFile:CaosScriptFile,
        private val cobFile:String,
        private val scriptIndex:Int,
        private val presentableText:String? = null,
        private val alphaSortKey: String? = "",
        val isAlwaysLeaf:Boolean = PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java).size < 2
) : StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any {
        return caosFile
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

    override fun getAlphaSortKey(): String {
        return alphaSortKey ?: ""
    }

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String? {
                this@CobScriptFileViewTreeElement.presentableText?.let {
                    return it
                }
                val scripts = PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java)
                if(scripts.size != 1 || scripts.filter{ it !is CaosScriptMacro }.size != 1) {
                    return "Script $scriptIndex"
                }
                return when(val script = scripts.firstOrNull { it !is CaosScriptMacro }) {
                    is CaosScriptEventScript -> "${script.family} ${script.genus} ${script.species} ${script.eventNumber}"
                    is CaosScriptInstallScript -> "Install Script"
                    is CaosScriptRemovalScript -> "Removal Script"
                    else -> "Script $scriptIndex"
                }
            }

            override fun getLocationString(): String? = cobFile

            override fun getIcon(p0: Boolean): Icon? = null
        }
    }

    override fun getChildren(): Array<TreeElement> {
        val scripts = PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java)
        if(scripts.size != 1 || scripts.filter{ it !is CaosScriptMacro }.size != 1) {
            return scripts.map {
                SubScriptLeafElement(it, cobFile)
            }.toTypedArray()
        }
        return emptyArray()
    }
}


internal class SubScriptLeafElement(
        private val script:CaosScriptScriptElement,
        private val enclosingCob:String
) : StructureViewTreeElement, SortableTreeElement {
    override fun getPresentation(): ItemPresentation {
        return object:ItemPresentation {
            override fun getPresentableText(): String? = text
            override fun getLocationString(): String? = enclosingCob
            override fun getIcon(p0: Boolean): Icon? = null
        }
    }

    private val text by lazy {
        when(script) {
            is CaosScriptEventScript -> "${script.family} ${script.genus} ${script.species} ${script.eventNumber}"
            is CaosScriptInstallScript -> "Install Script"
            is CaosScriptRemovalScript -> "Removal Script"
            else -> "Body Script"
        }
    }

    override fun getChildren(): Array<TreeElement> {
        return emptyArray()
    }

    override fun navigate(requestFocus: Boolean) {
        (script as? Navigatable)?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = (script as? Navigatable)?.canNavigate().orFalse()

    override fun canNavigateToSource(): Boolean = (script as? Navigatable)?.canNavigateToSource().orFalse()

    override fun getValue(): Any = script

    override fun getAlphaSortKey(): String {
        return when(script) {
            is CaosScriptEventScript ->  "x" + listOf(script.family, script.genus, script.species, script.eventNumber)
                    .joinToString("|") { it.toString().padStart(8, '0') }
            is CaosScriptInstallScript -> "0"
            is CaosScriptRemovalScript -> "1"
            else -> "z"
        }
    }
}