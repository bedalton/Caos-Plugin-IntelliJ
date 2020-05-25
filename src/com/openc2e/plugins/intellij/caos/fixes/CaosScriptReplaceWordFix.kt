package com.openc2e.plugins.intellij.caos.fixes;

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCompositeElement
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptPsiElementFactory

class CaosScriptReplaceWordFix (private val word:String, element:CaosScriptCompositeElement)  : IntentionAction {

    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = "Caos Script"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return pointer.element != null && file is CaosScriptFile
    }

    override fun getText(): String = "Replace with ${word.toUpperCase()}"

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        pointer.element?.let { element ->
            CaosScriptPsiElementFactory.createCommandTokenElement(project, word)?.let { newToken ->
                element.replace(newToken)
            }
        }
    }


}
