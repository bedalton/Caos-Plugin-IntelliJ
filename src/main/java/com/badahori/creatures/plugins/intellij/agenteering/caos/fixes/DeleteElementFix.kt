package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.IntentionAndQuickFixAction
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager

class DeleteElementFix(private val message: String, elementIn: PsiElement) : IntentionAndQuickFixAction() {

    private val pointer = SmartPointerManager.createPointer(elementIn)

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun applyFix(project: Project, file: PsiFile?, editor: Editor?) {
        val element = pointer.element
            ?: return
        applyFix(project, element)
    }

    override fun startInWriteAction(): Boolean = true

    override fun getName(): String = message

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
            ?: return
        applyFix(project, element)
    }

    private fun applyFix(project: Project, element: PsiElement) {
        val range = getDeletionRange(element)
            ?: return
        element.document?.let { document ->
            WriteCommandAction.runWriteCommandAction(
                project,
                message,
                "CaosScript",
                {
                    EditorUtil.deleteText(document, range)
                }
            )
        }
    }

    /**
     * Get the deletion range which includes either its preceding or following whitespace
     */
    private fun getDeletionRange(element: PsiElement?): TextRange? {
        if (element == null)
            return null
        val originalRange = element.textRange
        val previous = getEarliestPrecedingWhitespace(element)?.textRange
        if (previous != null) {
            return TextRange(previous.startOffset, originalRange.endOffset)
        }
        val next = getFurthestFollowingWhitespace(element)
        if (next != null) {
            return TextRange(originalRange.startOffset, next.endOffset)
        }
        return originalRange
    }


    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return pointer.element != null
    }
}