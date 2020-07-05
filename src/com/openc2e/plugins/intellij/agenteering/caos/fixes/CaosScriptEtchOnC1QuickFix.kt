package com.openc2e.plugins.intellij.agenteering.caos.fixes

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
import com.intellij.refactoring.suggested.startOffset
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.openc2e.plugins.intellij.agenteering.caos.lang.variant
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptEnumNextStatement
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptBodyElement
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.openc2e.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.openc2e.plugins.intellij.agenteering.caos.utils.EditorUtil
import com.openc2e.plugins.intellij.agenteering.caos.utils.document
import com.openc2e.plugins.intellij.agenteering.caos.utils.editor
import com.openc2e.plugins.intellij.agenteering.caos.utils.matchCase

class CaosScriptEtchOnC1QuickFix (element:CaosScriptEnumNextStatement) : LocalQuickFix, IntentionAction {

    private val pointer = SmartPointerManager.createPointer(element)
    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")
    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        pointer.element?.let {
            return shouldProcess(it)
        }
        return false
    }

    private fun shouldProcess(enumNext:CaosScriptEnumNextStatement) : Boolean {
        val variant = enumNext.containingCaosFile.variant
        return variant == CaosVariant.C1
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
        for(i in 0..9) {
            if (i !in varNumbersUsed) {
                return i
            }
        }
        return null
    }

    private fun applyFix(document:Document, editor: Editor?, element:CaosScriptEnumNextStatement) {
        val project = element.project
        val etch = element.enumHeaderCommand.commandToken.kEtch
                ?: return
        val varNumber = getVarNumberToUse(element)
                ?: return
        val insertionPoint = element.enumHeaderCommand.endOffset
        val etchText = etch.text
        val varText = "var$varNumber".matchCase(etchText)
        val doif = "DOIF".matchCase(etchText)
        val endi = "ENDI".matchCase(etchText)
        val enum = "ENUM".matchCase(etchText)
        val touch = "TOUC".matchCase(etchText)
        val targ = "TARG".matchCase(etchText)
        val gt = "GT".matchCase(etchText)
        val replacement = CaosScriptPsiElementFactory.createCommandTokenElement(element.project, enum)
                ?: return
        PsiDocumentManager.getInstance(project).commitDocument(document)
        element.enumHeaderCommand.commandToken.replace(replacement)
        val lineDelim = if (element.text.contains("\n"))
            "\n"
        else
            " "
        val text = "$lineDelim$doif $touch $varText $targ $gt 0$lineDelim" + element.codeBlock?.text + "$lineDelim$endi"
        element.codeBlock?.textRange?.let {
            PsiDocumentManager.getInstance(project).commitDocument(document)
            EditorUtil.deleteText(document, it)
        }
        val setv = "SETV".matchCase(etchText) + " $varText $targ$lineDelim"
        if (editor != null) {
            PsiDocumentManager.getInstance(project).commitDocument(document)
            EditorUtil.insertText(editor, setv, element.startOffset,false)
            PsiDocumentManager.getInstance(project).commitDocument(document)
            EditorUtil.insertText(editor, text, insertionPoint+setv.length, false)
        } else {
            PsiDocumentManager.getInstance(project).commitDocument(document)
            EditorUtil.insertText(document, setv, element.startOffset)
            PsiDocumentManager.getInstance(project).commitDocument(document)
            EditorUtil.insertText(project, document, text, insertionPoint+setv.length)
        }
        PsiDocumentManager.getInstance(project).commitDocument(document)
        CodeStyleManager.getInstance(project).reformat(element.parent, false)
    }
}