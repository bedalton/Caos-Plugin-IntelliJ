package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
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

class CaosScriptInsertAfterFix(private val fixText:String, private val wordText:String, element:PsiElement, val offsetInElement:Int = element.text.length) : LocalQuickFix, IntentionAction {

    private val element = SmartPointerManager.createPointer(element)

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")


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

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
            ?: return
        invoke(element.editor, element)
    }

    private fun invoke(editor: Editor?, element:PsiElement) {
        if (editor == null)
            return
        EditorUtil.insertText(editor, "$wordText ", element.startOffset + offsetInElement, true)
    }
}