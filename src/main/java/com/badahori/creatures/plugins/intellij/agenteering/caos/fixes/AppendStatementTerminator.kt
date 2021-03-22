package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.intellij.codeInspection.IntentionAndQuickFixAction
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.badahori.creatures.plugins.intellij.agenteering.utils.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.lineNumber
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.badahori.creatures.plugins.intellij.agenteering.utils.editor
import com.badahori.creatures.plugins.intellij.agenteering.utils.matchCase

class AppendStatementTerminator(elementIn: PsiElement, private val replacementText:String) : IntentionAndQuickFixAction() {

    private val pointer = SmartPointerManager.createPointer(elementIn)

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun applyFix(project: Project, file: PsiFile?, editor: Editor?) {
        val element = pointer.element
                ?: return
        applyFix(project, element, replacementText)
    }

    override fun startInWriteAction(): Boolean = true

    override fun getName(): String = "Close statement with ${replacementText.toUpperCase()}"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
                ?: return
        val replacementText = getReplacementText(element)
                ?: return
        applyFix(project, element, replacementText)
    }

    private fun applyFix(project: Project, element:PsiElement, replacementText: String) {
        val other = element.previous ?: element.next ?: element
        val singleLine = !other.textContains('\n')
        var separatorToken = if (!singleLine) "\n" else if (other.textContains(',')) "," else " "
        val text = replacementText.matchCase(element.lastChild.text, element.variant ?: CaosVariant.C1)
        var replacement = "$separatorToken$text"
        var insertAt:Int = element.lastChild.endOffset
        var next = element.next
        while (next != null && next.text.isBlank()) {
            next = next.next
        }
        if (next != null) {
            insertAt = next.previous?.endOffset ?: next.startOffset
            if (next.textContains(','))
                insertAt += 1
            separatorToken = if (next.lineNumber != element.lastChild.lineNumber) "\n" else if (next.textContains(',')) "," else " "
                replacement = "$text$separatorToken"
        }
        element.editor?.let {
            EditorUtil.insertText(it, replacement, insertAt, true)
            return
        }
        element.document?.let {
            EditorUtil.insertText(project, it, replacementText, element.lastChild.endOffset)
        }
    }

    private fun getReplacementText(element:PsiElement) : String? {
        val replacement = when (element) {
            is CaosScriptDoifStatement -> "ENDI"
            is CaosScriptEnumNextStatement -> "NEXT"
            is CaosScriptEnumSceneryStatement -> "NSCN"
            is CaosScriptRepeatStatement -> "REPE"
            is CaosScriptSubroutine -> "RETN"
            is CaosScriptFile -> "ENDM"
            else -> null
        }
        return if (replacement == null || element.lastChild.text.toUpperCase().endsWith(replacement))
            null
        else
            replacement
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is CaosScriptFile
    }
}