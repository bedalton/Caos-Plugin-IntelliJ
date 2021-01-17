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

class CaosScriptInsertSpaceFix(element:PsiElement) : IntentionAction {

    private val insertBeforeElement = SmartPointerManager.createPointer(element)

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun getText(): String = "insert space"

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return insertBeforeElement.element != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null)
            return
        val element = insertBeforeElement.element
                ?: return
        EditorUtil.insertText(editor, " ", element.startOffset, true)
    }
}