package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.editor
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager

class CaosScriptInsertBeforeFix(
    private val fixDescription: String,
    private val textToInsert: String,
    element: PsiElement,
    private val appendChar: Char? = ' ',
    private val offsetInNewText: Int? = null,
    private val after: ((editor: Editor) -> Unit)? = null
) : IntentionAction, LocalQuickFix {

    private val element = SmartPointerManager.createPointer(element)

    override fun getFamilyName(): String = CAOSScript

    override fun applyFix(p0: Project, descriptor: ProblemDescriptor) {
        val element = element.element
            ?: return
        val editor = element.editor
            ?: return
        invoke(editor, element)
    }

    override fun getText(): String = fixDescription

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return element.element != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val element = element.element
            ?: return
        invoke(editor, element)
    }

    private fun invoke(editor: Editor?, element:PsiElement) {
        if (editor == null)
            return
        val appendText = if (appendChar == null)
            ""
        else
            "$appendChar"
        EditorUtil.insertText(editor, "$textToInsert$appendText", element.startOffset, false)
        if (offsetInNewText != null) {
            EditorUtil.offsetCaret(editor, element.startOffset + offsetInNewText)
        }
        after?.invoke(editor)
    }
}