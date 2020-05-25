package com.openc2e.plugins.intellij.caos.fixes

import com.intellij.codeInspection.IntentionAndQuickFixAction
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptStringLike
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptPsiElementFactory
import com.openc2e.plugins.intellij.caos.psi.util.getSelfOrParentOfType

class CaosScriptFixQuoteType(element:PsiElement, val quoteStart:Char, val quoteEnd:Char = quoteStart) : IntentionAndQuickFixAction() {
    private val pointer = SmartPointerManager.createPointer(element)

    override fun getName(): String = "Fix quotation type"
    override fun getFamilyName(): String = "CaosScript"

    override fun applyFix(project: Project, element: PsiFile?, editor: Editor?) {
        pointer.element?.let {
            applyFix(project, it)
        }
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(project, descriptor.psiElement)
    }

    private fun applyFix(project:Project, element:PsiElement) {
        val expression = element.getSelfOrParentOfType(CaosScriptStringLike::class.java)
                ?: return
        val newElement = CaosScriptPsiElementFactory.createStringRValue(
                project,
                expression.stringValue,
                quoteStart,
                quoteEnd
        )
        newElement.expression?.let {
            expression.replace(it)
        }
    }
}