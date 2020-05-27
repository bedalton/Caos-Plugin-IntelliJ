package com.openc2e.plugins.intellij.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCompositeElement

class CaosScriptClasToCls2Fix(element:CaosScriptCompositeElement) : IntentionAction {

    private val element = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = "CaosScript"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean
            = element.element?.text?.toLowerCase() == "cls2"

    override fun getText(): String = "Convert C1 CLAS statement to C2"

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        TODO("Not yet implemented")
    }
}