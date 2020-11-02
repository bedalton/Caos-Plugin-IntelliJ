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
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptArgument
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCAssignment
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*

class CaosScriptCls2ToClasFix(element: CaosScriptCAssignment) : IntentionAction, LocalQuickFix {

    private val element = SmartPointerManager.createPointer(element)

    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean
            = element.element?.lvalue?.commandStringUpper == "CLS2"

    override fun getText(): String = CaosBundle.message("caos.intention.cls2-to-clas")

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
        if (!element.commandToken?.commandString?.equalsIgnoreCase("cls2").orFalse())
            return null;
        val familyAndGenus = element.lvalue?.getChildrenOfType(CaosScriptArgument::class.java)
                ?: return null
        if (familyAndGenus.size != 2)
            return null

        val clasText = "CLAS".matchCase(element.commandString)
        val clas:Int
        val setv = "setv".matchCase(element.commandString)
        return try {
            val family = familyAndGenus[0].text.toInt()
            val genus = familyAndGenus[1].text.toInt()
            val species = element.getChildrenOfType(CaosScriptArgument::class.java).last().text.toInt()
            clas = CaosAgentClassUtils.toClas(family, genus, species)
            "$setv $clasText $clas"
        } catch (e:Exception) {
            null
        }
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