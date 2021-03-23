package com.badahori.creatures.plugins.intellij.agenteering.bundles.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager

class Caos2CobRemoveFileFix(element: PsiElement, private val fixText:String? = null) : IntentionAction,
    LocalQuickFix {

    private val fileName = (element.text.trim('\n','\r','\t', ' ', '"'))
    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("cob.caos2cob.inspections.group")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return pointer.element != null && file is CaosScriptFile
    }

    override fun getText(): String = fixText ?: CaosBundle.message("cob.caos2cob.fixes.delete-error-file", fileName)

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val element = pointer.element
            ?: return
        applyFix(element)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(descriptor.psiElement)
    }

    private fun applyFix(element: PsiElement) {
        val nextPointer = (element.next)?.let {
            SmartPointerManager.createPointer(it)
        }
        element.delete()
        val next = nextPointer?.element
            ?: return
        if (next.text.isBlank() && next.next?.text?.isBlank().orFalse()) {
            next.delete()
        }
    }


}