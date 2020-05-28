package com.openc2e.plugins.intellij.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.project.CaosScriptProjectSettings
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptEnumNextStatement
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptScriptBodyElement
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptVarToken
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptPsiElementFactory
import com.openc2e.plugins.intellij.caos.utils.EditorUtil
import com.openc2e.plugins.intellij.caos.utils.document
import com.openc2e.plugins.intellij.caos.utils.editor
import com.openc2e.plugins.intellij.caos.utils.matchCase

class CaosScriptEtchOnC1QuickFix (element:CaosScriptEnumNextStatement) : LocalQuickFix, IntentionAction {

    private val pointer = SmartPointerManager.createPointer(element)
    override fun getFamilyName(): String = CaosBundle.message("caos.fixes.family-name")
    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        pointer.element?.let {
            return shouldProcess(it)
        }
        return false
    }

    private fun shouldProcess(enumNext:CaosScriptEnumNextStatement) : Boolean {
        val variant = enumNext.containingCaosFile.variant
        return variant == "C1"
                && enumNext.enumHeaderCommand.commandToken.kEtch != null
                && enumNext.enumHeaderCommand.classifier?.species != null
                && getVarNumberToUse(enumNext) != null
    }

    override fun getText(): String = CaosBundle.message("caos.intention.etch-to-c1")

    override fun invoke(project: Project, editorIn: Editor?, file: PsiFile?) {
        val editor = editorIn
                ?: return
        val element = pointer.element
                ?: return
        applyFix(editor.document, editor, element)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? CaosScriptEnumNextStatement
                ?: return
        if (!shouldProcess(element))
            return
        element.editor?.let {
            applyFix(it.document, it, element)
            return
        }
        element.document?.let {
            applyFix(it, null, element)
            return
        }
    }

    private fun getVarNumberToUse(element:CaosScriptEnumNextStatement) : Int? {
        val containingScript = element.getParentOfType(CaosScriptScriptBodyElement::class.java)
                ?: return null
        val varsUsed = PsiTreeUtil.collectElementsOfType(containingScript, CaosScriptVarToken::class.java)
        val varNumbersUsed = varsUsed
                .filter {
                    // Only use elements not used before this block
                    it.varX != null && it.endOffset < element.endOffset
                }
                .mapNotNull {
                    it.varIndex
                }.toSet()
        var varNumber:Int?
        for(i in 0..9) {
            if (i !in varNumbersUsed) {
                return i
            }
        }
        return null
    }

    private fun applyFix(document:Document, editor: Editor?, element:CaosScriptEnumNextStatement) {
        val etch = element.enumHeaderCommand.commandToken.kEtch
                ?: return
        val varNumber = getVarNumberToUse(element)
                ?: return
        val insertionPoint = element.enumHeaderCommand.endOffset
        val varText = "var$varNumber"
        val etchText = etch.text
        val doif = "DOIF".matchCase(etchText)
        val endi = "ENDI".matchCase(etchText)
        val enum = "ENUM".matchCase(etchText)
        val touch = "TOUC".matchCase(etchText)
        val targ = "TARG".matchCase(etchText)
        val gt = "GT".matchCase(etchText)
        val replacement = CaosScriptPsiElementFactory.createCommandTokenElement(element.project, enum)
                ?: return
        element.enumHeaderCommand.commandToken.replace(replacement)
        val text = "$doif $touch $varText $targ $gt 0 " + element.codeBlock?.text + " $endi"
        element.codeBlock?.textRange?.let {
            EditorUtil.deleteText(document, it)
        }
        val project = element.project
        if (editor != null) {
            EditorUtil.insertText(editor, text, insertionPoint, false)
        } else {
            EditorUtil.insertText(project, document, text, insertionPoint)
        }

        PsiDocumentManager.getInstance(project).commitDocument(document)
        CodeStyleManager.getInstance(project).reformat(element, false)
    }
}