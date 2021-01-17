package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.util.TextRange

class CaosScriptReplaceElementFix(
    element: PsiElement,
    private val replacement: String,
    private val fixText: String = "Replace '${element.text}' with '$replacement'",
    private val trimSpaces: Boolean = false
)  : IntentionAction, LocalQuickFix {

    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return pointer.element != null && file is CaosScriptFile
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
        val spaces =
        runUndoTransparentWriteAction {
            EditorUtil.replaceText(document, TextRange(startOffset, endOffset), replacement)
        }
    }


}
