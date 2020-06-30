package com.openc2e.plugins.intellij.agenteering.caos.fixes

import com.intellij.codeInspection.IntentionAndQuickFixAction
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringLike
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType

class CaosScriptFixQuoteType(element:PsiElement, private val quoteStart:Char, private val quoteEnd:Char = quoteStart) : IntentionAndQuickFixAction() {
    private val pointer = SmartPointerManager.createPointer(element)

    override fun getName(): String = CaosBundle.message("caos.fixes.fix-quote-type")

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

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