package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.refactoring.suggested.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.EditorUtil

class CaosScriptInsertBeforeFix(private val fixText:String, private val wordText:String, element:PsiElement) : IntentionAction {

    private val element = SmartPointerManager.createPointer(element)

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun getText(): String = fixText

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return element.element != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null)
            return
        val element = element.element
                ?: return
        EditorUtil.insertText(editor, "$wordText ", element.startOffset, false)
    }
}