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

class CaosScriptInsertSpaceFix(element:PsiElement) : IntentionAction, LocalQuickFix {

    private val insertBeforeElement = SmartPointerManager.createPointer(element)

    override fun getFamilyName(): String = CAOSScript

    override fun getText(): String = "Insert space"

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return insertBeforeElement.element != null
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
            ?: return
        val editor = element.editor
            ?: return
        invoke(editor, element)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {

        val element = insertBeforeElement.element
            ?: return

        if (editor == null)
            return

        invoke(editor, element)
    }

    fun invoke(editor: Editor, element: PsiElement) {
        EditorUtil.insertText(editor, " ", element.startOffset, true)
    }
}