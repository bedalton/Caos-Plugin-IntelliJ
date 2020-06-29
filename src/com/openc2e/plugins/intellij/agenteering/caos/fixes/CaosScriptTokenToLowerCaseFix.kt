package com.openc2e.plugins.intellij.agenteering.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptShouldBeLowerCase
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.openc2e.plugins.intellij.agenteering.caos.utils.EditorUtil
import com.openc2e.plugins.intellij.agenteering.caos.utils.document

class CaosScriptTokenToLowerCaseFix(element: CaosScriptShouldBeLowerCase) : LocalQuickFix, IntentionAction {

    private val element = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return true
    }

    override fun getText(): String = CaosBundle.message("caos.annotator.command-annotator.invalid-command-case.text")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val element = element.element
                ?: return
        applyFix(project, element)

    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? CaosScriptShouldBeLowerCase
                ?: return
        applyFix(project, element)
    }

    private fun applyFix(project: Project, element: CaosScriptShouldBeLowerCase) {
        val document = element.document
                ?: return
        try {
            PsiDocumentManager.getInstance(project).commitDocument(document)
        } catch (e:Exception) {}
        EditorUtil.replaceText(document, element.textRange, element.text.toLowerCase())
    }

}