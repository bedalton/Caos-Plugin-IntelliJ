package com.openc2e.plugins.intellij.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCRndv
import com.openc2e.plugins.intellij.caos.utils.EditorUtil
import com.openc2e.plugins.intellij.caos.utils.document
import com.openc2e.plugins.intellij.caos.utils.editor

class CaosScriptReorderRndvParameters(element: CaosScriptCRndv) : IntentionAction, LocalQuickFix {

    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun applyFix(p0: Project, p1: ProblemDescriptor) {
        TODO("Not yet implemented")
    }

    override fun isAvailable(p0: Project, p1: Editor?, p2: PsiFile?): Boolean {
        pointer.element?.let {
            return it.expectsIntList.size == 2
        }
        return false
    }

    override fun getText(): String = CaosBundle.message("caos.fixes.swap-rndv-values")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        pointer.element?.let {
            applyFix(it)
        }
    }

    private fun applyFix(element: CaosScriptCRndv) {
        val value1 = element.minElement
                ?: return

        val value2 = element.maxElement
                ?: return
        val range = TextRange.create(value1.startOffset, value2.endOffset)
        val text = "${value2.text} ${value1.text}"
        element.editor?.let {
            EditorUtil.replaceText(it, range, text, true)
            return
        }

        element.document?.let {
            EditorUtil.replaceText(it, range, text)
        }
    }
}