package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNumber
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import kotlin.math.ceil
import kotlin.math.floor

class CaosScriptRoundNumberFix(element: CaosScriptNumber, float:Float, val roundDown:Boolean) : IntentionAction {

    private val pointer = SmartPointerManager.createPointer(element)

    private val newValue = if (roundDown)
        floor(float).toInt()
    else
        ceil(float).toInt()

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun isAvailable(p0: Project, p1: Editor?, p2: PsiFile?): Boolean {
        return pointer.element != null
    }

    override fun getText(): String {
        return if (roundDown) {
            CaosBundle.message("caos.fixes.round-number", "down", newValue)
        } else {
            CaosBundle.message("caos.fixes.round-number", "up", newValue)
        }
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val element = pointer.element
                ?: return
        val newElement = CaosScriptPsiElementFactory.createNumber(project, newValue)
                .number
                ?: return
        element.replace(newElement)
    }


}