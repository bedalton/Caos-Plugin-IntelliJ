package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.count
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.runWriteAction
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.tree.LeafState
import icons.CaosScriptIcons


internal open class ProjectCaosScriptFileTreeNode(
    file: CaosScriptFile,
    viewSettings: ViewSettings?
) : VirtualFileBasedNode<VirtualFile>(file.project, file.virtualFile, viewSettings) {

    private val caosFileName = file.name

    val pointer = SmartPointerManager.createPointer(file)

    val caosFile: CaosScriptFile? get() = pointer.element


    private val scripts by lazy {
        val caosFile = caosFile
            ?: return@lazy emptyList()
        if (caosFile.isValid)
            PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java)
        else {
            LOGGER.severe("Cannot get child scripts in ProjectCaosScriptFileTreeNode. CAOSFile invalid")
            emptyList()
        }
    }

    private val possibleScripts: Int by lazy {
        var count = 0
        SCRIPT_HEADER_REGEX.findAll(file.text).iterator().forEach { _ ->
            count++
        }
        count
    }

    private val possibleSubroutines = file.text.toLowerCase().count("subr ")


    private val subroutines by lazy {
        PsiTreeUtil.collectElementsOfType(file, CaosScriptSubroutine::class.java)
    }

    override fun isAlwaysLeaf(): Boolean {
        return possibleScripts < 2 && possibleSubroutines < 1
    }


    override fun expandOnDoubleClick(): Boolean {
        return false
    }

    override fun navigate(requestFocus: Boolean) {
        caosFile?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return caosFile?.canNavigateToSource() == true
    }

    override fun canNavigateToSource(): Boolean {
        return caosFile?.canNavigateToSource() == true
    }

    private fun getPresentableText(): String {
        return caosFileName
    }

    override fun getChildren(): List<AbstractTreeNode<*>> {
        return if (scripts.size != 1) {
            // Macro indices
            var macroIndex = 0
            val hasMacros = scripts.filterIsInstance<CaosScriptMacro>().size > 1
            // Install Script indices
            var installScriptIndex = 0
            val numInstallScripts = scripts.filterIsInstance<CaosScriptInstallScript>().size
            // Removal Script indices
            var removalScriptIndex = 0
            val numRemovalScripts = scripts.filterIsInstance<CaosScriptRemovalScript>().size
            scripts.mapNotNull { script ->
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
                        if (numInstallScripts > 1) ++installScriptIndex else null
                    )
                    is CaosScriptRemovalScript -> SubScriptLeafNode(
                        script,
                        if (numRemovalScripts > 1) ++removalScriptIndex else null
                    )
                    else -> SubScriptLeafNode(
                        script,
                        null
                    )
                }
            }
        } else {
            val project = myProject
                ?: return emptyList()
            getSubroutineNodes(project, subroutines)
        }
    }

    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = getPresentableText()
        presentationData.setIcon(CaosScriptIcons.CAOS_FILE_ICON)
    }

    override fun getLeafState(): LeafState {
        return if (isAlwaysLeaf)
            LeafState.ALWAYS
        else
            LeafState.ASYNC
    }

    companion object {
        private val SCRIPT_HEADER_REGEX = "(scrp|iscr|rscr)".toRegex()
    }

}


internal class ChildCaosScriptFileTreeNode(
    private val parentName: String,
    caosFile: CaosScriptFile,
    private val scriptIndex: Int,
    private val presentableTextIn: String? = null,
    viewSettings: ViewSettings?
) : ProjectCaosScriptFileTreeNode(
    caosFile,
    viewSettings
) {

    override fun navigate(requestFocus: Boolean) {
        val caosFile = caosFile
            ?: return
        if (ApplicationManager.getApplication().isDispatchThread) {
            caosFile.virtualFile?.isWritable = true
            runWriteAction {
                caosFile.quickFormat()
                caosFile.virtualFile?.isWritable = false
            }
        }

        caosFile.navigate(requestFocus)
        caosFile.virtualFile?.isWritable = false
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
            is CaosScriptInstallScript -> parentName.nullIfEmpty()?.let { "$it " }.orEmpty() + " - Install Script"
            is CaosScriptRemovalScript -> parentName.nullIfEmpty()?.let { "$it " }.orEmpty() + " - Removal Script"
            else -> parentName.nullIfEmpty()?.let { "$it - " }.orEmpty() + "Script $scriptIndex"
        }
    }

    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = getPresentableText()
        presentationData.locationString = "(Decompiled)"
    }

    override fun getWeight(): Int {
        val script = PsiTreeUtil.collectElementsOfType(caosFile, CaosScriptScriptElement::class.java)
            .firstOrNull { it !is CaosScriptMacro }
        return when (script) {
            is CaosScriptInstallScript -> 17
            is CaosScriptRemovalScript -> 18
            is CaosScriptMacro -> 19
            else -> 16
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
    private val enclosingCobFileName: String? = null,
) : AbstractTreeNode<CaosScriptScriptElement>(script.project, script) {

    private val possibleSubroutines = script.text.toLowerCase().count("subr ")

    override fun isAlwaysLeaf(): Boolean {
        return possibleSubroutines < 1
    }


    override fun expandOnDoubleClick(): Boolean {
        return false
    }

    private val subroutines by lazy {
        PsiTreeUtil.collectElementsOfType(script, CaosScriptSubroutine::class.java)
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
        return getSubroutineNodes(myProject, subroutines)
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
        ProgressIndicatorProvider.checkCanceled()
        if (!script.isValid)
            return null
        return try {
            if (!script.isValid)
                return null
            script.containingFile?.let {
                return if (it.isValid)
                    it.virtualFile
                else
                    null
            }
            script.originalElement.containingFile?.let {
                if (it.isValid)
                    it.virtualFile
                else
                    null
            }
        } catch (e: Exception) {
            if (e is ProcessCanceledException)
                return null
            LOGGER.severe("Error when getting virtual file in CaosScriptTreeNode")
            e.printStackTrace()
            null
        }
    }

    override fun canNavigate(): Boolean = navigationNode.canNavigate()

    override fun canNavigateToSource(): Boolean = navigationNode.canNavigateToSource()

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
        is CaosScriptInstallScript -> 17
        is CaosScriptRemovalScript -> 18
        is CaosScriptMacro -> 19
        else -> 16
    }

    override fun getLeafState(): LeafState {
        return if (isAlwaysLeaf)
            LeafState.ALWAYS
        else
            LeafState.ASYNC
    }
}


private fun getSubroutineNodes(
    project: Project,
    subroutines: Collection<CaosScriptSubroutine>
): List<GenericPsiNode<CaosScriptSubroutine>> {
    return subroutines.map { subroutine ->
        GenericPsiNode(
            project,
            subroutine,
            subroutine.name,
            CaosScriptIcons.SUBROUTINE,
            sortWeight = 50
        )
    }
}