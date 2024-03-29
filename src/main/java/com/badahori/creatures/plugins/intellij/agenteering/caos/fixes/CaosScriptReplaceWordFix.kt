package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager

class CaosScriptReplaceWordFix (private val word:String, element:CaosScriptIsCommandToken)  : IntentionAction, LocalQuickFix {

    private val currentCommand = element.commandString.uppercase()
    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CAOSScript

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return pointer.element != null && file is CaosScriptFile
    }

    override fun getText(): String = CaosBundle.message("caos.fixes.replace-command-with", currentCommand, word.uppercase())

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val element = pointer.element
                ?: return
        applyFix(project, element)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(project, descriptor.psiElement)
    }

    private fun applyFix(project:Project, element:PsiElement) {
        CaosScriptPsiElementFactory.createCommandTokenElement(project, word)?.let { newToken ->
            element.replace(newToken)
        }
    }


}
