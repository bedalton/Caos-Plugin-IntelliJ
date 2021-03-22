package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.editor
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor

class CaosScriptInsertBeforeFix(
    private val fixText: String,
    private val wordText: String,
    element: PsiElement,
    private val appendChar: Char = ' '
) : IntentionAction, LocalQuickFix {

    private val element = SmartPointerManager.createPointer(element)

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun applyFix(p0: Project, descriptor: ProblemDescriptor) {
        val element = element.element
            ?: return
        val editor = element.editor
            ?: return
        invoke(editor, element)
    }

    override fun getText(): String = fixText

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
        EditorUtil.insertText(editor, "$wordText$appendChar", element.startOffset, false)
    }
}