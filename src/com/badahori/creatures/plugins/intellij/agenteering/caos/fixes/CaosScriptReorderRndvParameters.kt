package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCRndv
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.document
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.editor

class CaosScriptReorderRndvParameters(element: CaosScriptCRndv) : IntentionAction, LocalQuickFix {

    private val pointer = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
        (problemDescriptor.psiElement as? CaosScriptCRndv)?.let {element->
            applyFix(element)
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        pointer.element?.let {
            return it.expectsIntList.size == 2
        }
        return false
    }

    override fun getText(): String = CaosBundle.message("caos.fixes.swap-rndv-values")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        pointer.element?.let {
            applyFix(it)
        }
    }

    private fun applyFix(element: CaosScriptCRndv) {
        val value1 = element.minElement
                ?: return

        val value2 = element.maxElement
                ?: return
        val range = TextRange.create(value1.startOffset, value2.endOffset)
        val text = "${value2.text} ${value1.text}"
        element.editor?.let {
            EditorUtil.replaceText(it, range, text, true)
            return
        }

        element.document?.let {
            EditorUtil.replaceText(it, range, text)
        }
    }
}