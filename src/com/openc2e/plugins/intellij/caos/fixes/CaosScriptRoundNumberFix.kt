package com.openc2e.plugins.intellij.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptNumber
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptPsiElementFactory
import kotlin.math.ceil
import kotlin.math.floor

class CaosScriptRoundNumberFix(element:CaosScriptNumber, float:Float, val roundDown:Boolean) : IntentionAction {

    private val pointer = SmartPointerManager.createPointer(element)

    private val newValue = if (roundDown)
        floor(float).toInt()
    else
        ceil(float).toInt()

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = "CaosScript"

    override fun isAvailable(p0: Project, p1: Editor?, p2: PsiFile?): Boolean {
        return pointer.element != null
    }

    override fun getText(): String {
        return if (roundDown) {
            "Round number down to $newValue"
        } else {
            "Round number up to $newValue"
        }
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val newElement = CaosScriptPsiElementFactory.createNumber(project, newValue)
                .number
                ?: return
        pointer.element?.let { element->
            element.replace(newElement)
        }
    }


}