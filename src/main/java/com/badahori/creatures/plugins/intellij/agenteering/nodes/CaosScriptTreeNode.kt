package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.runWriteAction
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.tree.LeafState
import icons.CaosScriptIcons


internal class ProjectCaosScriptFileTreeNode(
    private val caosFile: CaosScriptFile
) : VirtualFileBasedNode<VirtualFile>(caosFile.project, caosFile.virtualFile) {

    private val scripts by lazy {
        PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java)
    }

    private val possibleScripts: Int by lazy {
        var count = 0;
        SCRIPT_HEADER_REGEX.findAll(caosFile.text).iterator().forEach { _ ->
            count++
        }
        count
    }

    override fun isAlwaysLeaf(): Boolean {
        return possibleScripts < 2
    }

    override fun expandOnDoubleClick(): Boolean {
        return false
    }


    override fun navigate(requestFocus: Boolean) {
        caosFile.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return caosFile.canNavigateToSource()
    }

    override fun canNavigateToSource(): Boolean {
        return caosFile.canNavigateToSource()
    }

    private fun getPresentableText(): String {
        return caosFile.name
    }

    override fun getChildren(): List<AbstractTreeNode<*>> {
        if (scripts.size != 1) {
            // Macro indices
            var macroIndex = 0
            val hasMacros = scripts.filterIsInstance<CaosScriptMacro>().size > 1
            // Install Script indices
            var installScriptIndex = 0
            val hasInstallScripts = scripts.filterIsInstance<CaosScriptInstallScript>().size > 1
            // Removal Script indices
            var removalScriptIndex = 0
            val hasRemovalScripts = scripts.filterIsInstance<CaosScriptRemovalScript>().size > 1
            return scripts.mapNotNull { script ->
                when (script) {
                    is CaosScriptMacro -> {
                        if (PsiTreeUtil.collectElementsOfType(script, CaosScriptIsCommandToken::class.java).isEmpty())
                            null
                        else
                            SubScriptLeafNode(
                                script,
                                if (hasMacros) ++macroIndex else null
                            )
                    }
                    is CaosScriptInstallScript -> SubScriptLeafNode(
                        script,
                        if (hasInstallScripts) ++installScriptIndex else null
                    )
                    is CaosScriptRemovalScript -> SubScriptLeafNode(
                        script,
                        if (hasRemovalScripts) ++removalScriptIndex else null
                    )
                    else -> SubScriptLeafNode(script, null)
                }
            }
        }
        return emptyList()
    }

    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = getPresentableText()
        presentationData.setIcon(CaosScriptIcons.CAOS_FILE_ICON)
    }

    override fun getLeafState(): LeafState {
        if (isAlwaysLeaf)
            return LeafState.ALWAYS
        else
            return LeafState.ASYNC
    }

    companion object {
        private val SCRIPT_HEADER_REGEX = "(scrp|iscr|rscr)".toRegex()
    }

}


internal class ChildCaosScriptFileTreeNode(
    private val parentName: String,
    private val caosFile: CaosScriptFile,
    private val scriptIndex: Int,
    private val presentableTextIn: String? = null
) : VirtualFileBasedNode<VirtualFile>(caosFile.project, caosFile.virtualFile), SortableTreeElement {

    private val scripts by lazy {
        PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java)
    }

    override fun isAlwaysLeaf(): Boolean {
        return scripts.size < 2
    }

    override fun navigate(requestFocus: Boolean) {
        runWriteAction {
            caosFile.quickFormat()
            caosFile.navigate(requestFocus)
            caosFile.virtualFile?.isWritable = false
        }
    }

    override fun canNavigate(): Boolean {
        return caosFile.canNavigateToSource()
    }

    override fun canNavigateToSource(): Boolean {
        return caosFile.canNavigateToSource()
    }

    private fun getPresentableText(): String {
        this@ChildCaosScriptFileTreeNode.presentableTextIn?.let {
            return it
        }
        val scripts = PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java)
        if (scripts.size != 1 || scripts.filter { it !is CaosScriptMacro }.size != 1) {
            return "Script $scriptIndex"
        }
        return when (val script = scripts.firstOrNull { it !is CaosScriptMacro }) {
            is CaosScriptEventScript -> "${script.family} ${script.genus} ${script.species} ${script.eventNumber}"
            is CaosScriptInstallScript -> parentName.nullIfEmpty()?.let { "$it " }.orEmpty() + "- Install Script"
            is CaosScriptRemovalScript -> parentName.nullIfEmpty()?.let { "$it " }.orEmpty() + " - Removal Script"
            else -> parentName.nullIfEmpty()?.let { "$it - " }.orEmpty() + "Script $scriptIndex"
        }
    }

    override fun getChildren(): List<AbstractTreeNode<*>> {
        if (scripts.size != 1) {
            // Macro indices
            var macroIndex = 0
            val hasMacros = scripts.filterIsInstance<CaosScriptMacro>().size > 1
            // Install Script indices
            var installScriptIndex = 0
            val hasInstallScripts = scripts.filterIsInstance<CaosScriptInstallScript>().size > 1
            // Removal Script indices
            var removalScriptIndex = 0
            val hasRemovalScripts = scripts.filterIsInstance<CaosScriptRemovalScript>().size > 1
            return scripts.map { script ->
                when (script) {
                    is CaosScriptMacro -> SubScriptLeafNode(
                        script,
                        if (hasMacros) ++macroIndex else null,
                        script.containingFile.name
                    )
                    is CaosScriptInstallScript -> SubScriptLeafNode(
                        script,
                        if (hasInstallScripts) ++installScriptIndex else null,
                        script.containingFile.name
                    )
                    is CaosScriptRemovalScript -> SubScriptLeafNode(
                        script,
                        if (hasRemovalScripts) ++removalScriptIndex else null,
                        script.containingFile.name
                    )
                    else -> SubScriptLeafNode(script, null, script.containingFile.name)
                }
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

    override fun getLeafState(): LeafState {
        return if (isAlwaysLeaf)
            LeafState.ALWAYS
        else
            LeafState.ASYNC
    }
}


internal class SubScriptLeafNode(
    private val script: CaosScriptScriptElement,
    private val index: Int? = null,
    private val enclosingCobFileName: String? = null
) : AbstractTreeNode<CaosScriptScriptElement>(script.project, script), SortableTreeElement {

    override fun isAlwaysLeaf(): Boolean {
        return true
    }

    private val text by lazy {
        val indexText = if (index != null)
            " ($index)"
        else
            ""
        when (script) {
            is CaosScriptEventScript -> "${script.family} ${script.genus} ${script.species} ${script.eventNumber}"
            is CaosScriptInstallScript -> "Install Script$indexText"
            is CaosScriptRemovalScript -> "Removal Script$indexText"
            else -> "Macro$indexText"
        }
    }

    override fun getChildren(): List<AbstractTreeNode<*>> {
        return emptyList()
    }

    private val navigationNode: Navigatable
        get() {
            return PsiTreeUtil.collectElementsOfType(script, CaosScriptIsCommandToken::class.java).firstOrNull()
                ?: script
        }

    override fun navigate(requestFocus: Boolean) {
        navigationNode.navigate(requestFocus)
    }

    override fun getVirtualFile(): VirtualFile? {
        return (script.containingFile ?: script.originalElement.containingFile).virtualFile
    }

    override fun canNavigate(): Boolean = navigationNode.canNavigate()

    override fun canNavigateToSource(): Boolean = navigationNode.canNavigateToSource()

    override fun getAlphaSortKey(): String {
        return when (script) {
            is CaosScriptEventScript -> listOf(script.family, script.genus, script.species, script.eventNumber)
                .joinToString("|") { it.toString().padStart(6, '0') }
            is CaosScriptInstallScript -> "w"
            is CaosScriptRemovalScript -> "x"
            is CaosScriptMacro -> "y"
            else -> "z"
        } + text
    }

    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = text
        if (enclosingCobFileName != null)
            presentationData.locationString = enclosingCobFileName
        presentationData.setIcon(
            when (script) {
                is CaosScriptMacro -> CaosScriptIcons.MACRO
                is CaosScriptInstallScript -> CaosScriptIcons.INSTALL_SCRIPT
                is CaosScriptRemovalScript -> CaosScriptIcons.REMOVAL_SCRIPT
                is CaosScriptEventScript -> CaosScriptIcons.EVENT_SCRIPT
                else -> null
            }
        )
    }

    override fun getWeight(): Int = when (script) {
        is CaosScriptInstallScript -> 7
        is CaosScriptRemovalScript -> 8
        is CaosScriptMacro -> 9
        else -> 6
    }

    override fun getLeafState(): LeafState {
        return LeafState.ALWAYS
    }
}
