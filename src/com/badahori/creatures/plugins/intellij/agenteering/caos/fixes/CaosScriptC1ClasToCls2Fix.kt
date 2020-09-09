package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCAssignment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpectsValueOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.badahori.creatures.plugins.intellij.agenteering.utils.editor
import com.badahori.creatures.plugins.intellij.agenteering.utils.matchCase

class CaosScriptC1ClasToCls2Fix(element: CaosScriptCAssignment) : IntentionAction, LocalQuickFix {

    private val element = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean
            = element.element?.lvalue?.commandStringUpper == "CLAS"

    override fun getText(): String = CaosBundle.message("caos.intentions.clas-to-cls2")

    override fun invoke(project: Project, editorIn: Editor?, file: PsiFile?) {
        val element = element.element
                ?: return
        applyFix(editorIn, element)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
                ?: return
        element.editor?.let {
            applyFix(it, element)
            return
        }
        element.document?.let {
            applyFix(it, element)
        }
    }

    private fun getScriptText() : String? {
        val element = element.element
                ?: return null
        val clasInt = (element.arguments.getOrNull(1) as? CaosScriptExpectsValueOfType)
                ?.rvalue
                ?.expression
                ?.intValue
                ?: return null
        val clas = CaosAgentClassUtils.parseClas(clasInt)
                ?: return null
        val setv = "setv".matchCase(element.commandString)
        val clasText = "CLS2".matchCase(element.commandString)
        return "$setv $clasText ${clas.family} ${clas.genus} ${clas.species}"
    }

    private fun applyFix(documentIn: Document?, element: PsiElement) {
        val document = documentIn ?: return
        val scriptText = getScriptText()
                ?: return
        val range = element.textRange
        EditorUtil.replaceText(document, range, scriptText)
    }

    private fun applyFix(editorIn: Editor?, element:PsiElement) {
        val editor = editorIn ?: return
        val scriptText = getScriptText()
                ?: return
        val range = element.textRange
        EditorUtil.deleteText(editor.document, range)
        EditorUtil.insertText(editor, scriptText, range.startOffset, true)
    }
}