package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Block
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*

typealias OnVariantChangeListener = (variant: CaosVariant?) -> Unit

internal interface DisposablePsiTreChangeListener: PsiTreeChangeListener, Disposable

internal class CaosFileTreeChangedListener(
    file: CaosScriptFile,
    private var runInWriteAction: Boolean = true,
    private var variantChangedListener: OnVariantChangeListener?
) : DisposablePsiTreChangeListener {


    private var nextCheck = -1L
    private var project: Project? = file.project
    private var currentVariant: CaosVariant? = file.variant ?: file.module?.variant
    private var pointer: SmartPsiElementPointer<CaosScriptFile>? = SmartPointerManager.createPointer(file)

    override fun beforeChildAddition(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {
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
    }

    override fun propertyChanged(event: PsiTreeChangeEvent) {
    }

    override fun dispose() {
        val theProject = project
            ?: return
        project = null
        pointer = null
        currentVariant = null
        variantChangedListener = null
        if (!theProject.isDisposed) {
            PsiManager.getInstance(theProject).removePsiTreeChangeListener(this)
        }
    }

    private fun onChange(child: PsiElement?) {
        val now = now()
        if (now < nextCheck) {
            return
        }
        nextCheck = now + DELAY
        try {
            val associatedFile = pointer?.element
                ?: return dispose()
            if (!child?.containingFile?.isEquivalentTo(associatedFile).orFalse())
                return
            val block = child?.getSelfOrParentOfType(CaosScriptCaos2Block::class.java)
                ?: return
            val newVariant = block.caos2Variant
            if (newVariant == currentVariant) {
                return
            }
            currentVariant = newVariant ?: associatedFile.variant
            if (runInWriteAction) {
                invokeLater {
                    runWriteAction {
                        variantChangedListener?.let { it(newVariant) }
                    }
                }
            } else {
                variantChangedListener?.let { it(newVariant) }
            }
        } catch (_: PsiInvalidElementAccessException) {

        }
    }
}

private const val DELAY = 8_000