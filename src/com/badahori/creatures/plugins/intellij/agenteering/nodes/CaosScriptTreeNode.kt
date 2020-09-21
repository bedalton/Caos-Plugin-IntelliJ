package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.FileContentUtilCore


internal class CaosScriptFileTreeNode(
        private val caosFile: CaosScriptFile,
        private val scriptIndex: Int,
        private val presentableTextIn: String? = null,
        private val alwaysLeaf: Boolean = PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java).size < 2
) : AbstractTreeNode<CaosScriptFile>(caosFile.project, caosFile), SortableTreeElement {

    override fun isAlwaysLeaf(): Boolean {
        return alwaysLeaf
    }

    override fun navigate(requestFocus: Boolean) {
        //quickFormat(caosFile)
        runWriteAction {
            FileContentUtilCore.reparseFiles()
            DaemonCodeAnalyzer.getInstance(project).restart(caosFile)
        }
        invokeLater {
            caosFile.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean {
        return caosFile.canNavigateToSource()
    }

    override fun canNavigateToSource(): Boolean {
        return caosFile.canNavigateToSource()
    }

    private fun getPresentableText(): String {
        this@CaosScriptFileTreeNode.presentableTextIn?.let {
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
                SubScriptLeafNode(it, it.containingFile.name)
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

    override fun getWeight(): Int = when (script) {
        is CaosScriptInstallScript -> 7
        is CaosScriptRemovalScript -> 8
        else -> 9
    }
}

internal fun quickFormat(caosFile: CaosScriptFile) {
    val command: WriteCommandAction<Void?> = object : WriteCommandAction.Simple<Void?>(caosFile.project, caosFile) {
        override fun run() {
            caosFile.virtualFile?.isWritable = true
            CaosScriptExpandCommasIntentionAction.invoke(project, caosFile)
            caosFile.virtualFile?.isWritable = false
        }
    }
    command.execute()
}
