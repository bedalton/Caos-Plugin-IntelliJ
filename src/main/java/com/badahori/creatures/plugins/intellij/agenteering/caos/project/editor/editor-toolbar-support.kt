package com.badahori.creatures.plugins.intellij.agenteering.caos.project.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.caos2
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.BUNDLE_DEFINITIONS_FOLDER
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.DisposablePsiTreChangeListener
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

/**
 * Opens up the docs for the given variant
 */
internal fun openDocs(project: Project, variant: CaosVariant): Boolean {
    // Get path to documents
    val docRelativePath = "$BUNDLE_DEFINITIONS_FOLDER/${variant.code}-Lib.caosdef"
    // Load document virtual file
    val virtualFile = CaosVirtualFileSystem.instance.findFileByPath(docRelativePath)
        ?: CaosFileUtil.getPluginResourceFile(docRelativePath)

    // Fetch psi file from virtual file
    val file = virtualFile?.getPsiFile(project)
        ?: getProjectPsiFileByName(
            project,
            "${variant.code}-Lib.caosdef",
            GlobalSearchScope.allScope(project)
        )
        ?: return false

    // If failed to find variant docs, disable button and return

    // Navigate to Docs.
    file.navigate(true)
    return true
}


internal fun CaosScriptFile.addCaos2ChangeListener(listener: ((isCaos2: String?) -> Unit)?): DisposablePsiTreChangeListener? {
    if (!this.isValid)
        return null
    val project = this.project
    if (project.isDisposed) {
        return null
    }
    val pointer = SmartPointerManager.createPointer(this)
    return CaosFileCaos2ChangedListener(project, pointer, listener)
}

private class CaosFileCaos2ChangedListener(
    var project: Project?,
    private var pointer: SmartPsiElementPointer<CaosScriptFile>?,
    private inline var isCaos2ChangeHandler: ((isCaos2: String?) -> Unit)?
) : DisposablePsiTreChangeListener {

    private var caos2: String? = "::NULL::"
    init {
        onChange(null)
    }
    override fun beforeChildAddition(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {
        onChange(event.child)
    }

    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildMovement(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildrenChange(event: PsiTreeChangeEvent) {
    }

    override fun beforePropertyChange(event: PsiTreeChangeEvent) {
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
        onChange(event.child)
    }

    override fun childRemoved(event: PsiTreeChangeEvent) {
        onChange(event.child)
    }

    override fun childReplaced(event: PsiTreeChangeEvent) {
        onChange(event.child)
    }

    override fun childrenChanged(event: PsiTreeChangeEvent) {
        onChange(event.child)
    }

    override fun childMoved(event: PsiTreeChangeEvent) {
        onChange(null)
    }

    override fun propertyChanged(event: PsiTreeChangeEvent) {
    }

    override fun dispose() {
        val theProject = project
            ?: return
        project = null
        pointer = null
        caos2 = null
        isCaos2ChangeHandler = null
        if (!theProject.isDisposed) {
            PsiManager.getInstance(theProject).removePsiTreeChangeListener(this)
        }
    }

    private fun onChange(child: PsiElement?) {
        try {
            val associatedFile = pointer?.element
                ?: return dispose()

            if (!associatedFile.isValid) {
                return dispose()
            }
            try {
                if (!child?.containingFile?.isEquivalentTo(associatedFile).orTrue()) {
                    return
                }
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
                caos2 = null
                return
            }
            val newIsCaos2 = associatedFile.caos2
            if (newIsCaos2 == caos2) {
                return
            }
            caos2 = newIsCaos2
            invokeLater {
                runWriteAction {
                    isCaos2ChangeHandler?.let { it(newIsCaos2) }
                }
            }
        } catch (e: PsiInvalidElementAccessException) {
            LOGGER.severe("Invalid PSI access. ${e.message}")
            e.printStackTrace()
        }
    }
}

