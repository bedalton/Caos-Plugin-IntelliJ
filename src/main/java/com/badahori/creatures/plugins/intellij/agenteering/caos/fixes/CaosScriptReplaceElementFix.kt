package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.utils.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager

class CaosScriptReplaceElementFix(
    element: PsiElement,
    private val replacement: String,
    private val fixText: String = "Replace with '$replacement'",
    private val trimSpaces: Boolean = false
)  : IntentionAction, LocalQuickFix {

    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CAOSScript

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return pointer.element != null
    }

    override fun getText(): String = fixText

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val element = pointer.element
                ?: return
        applyFix(element)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(descriptor.psiElement)
    }

    private fun applyFix(element:PsiElement) {
        val document = element.document
            ?: return
        val startOffset = element.startOffset
        var endOffset = element.endOffset
        if (trimSpaces) {
            var next = element.next
            while (next != null && next.text.trim(' ').isEmpty()) {
                endOffset = next.endOffset
                next = next.next
            }
        }
        runUndoTransparentWriteAction {
            EditorUtil.replaceText(document, TextRange(startOffset, endOffset), replacement)
        }
    }


}
