package com.openc2e.plugins.intellij.agenteering.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.lineNumber
import com.openc2e.plugins.intellij.agenteering.caos.utils.document

object CaosScriptExpandCommasIntentionAction : IntentionAction {
    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")


    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (file == null)
            return false
        val hasCommas = file.text.orEmpty().contains(",")
        val isSingleLine = file.text.length > 5 && file.lastChild.lineNumber == 0
        if (hasCommas)
            return true
        else if (isSingleLine) {
            return true
        }
        return false
    }

    override fun getText(): String = CaosBundle.message("caos.intentions.commands-on-new-line")

    override fun invoke(project: Project, editor: Editor?, fileIn: PsiFile?) {
        val file = fileIn ?: return
        val document = PsiDocumentManager.getInstance(project).getCachedDocument(file) ?: fileIn.document
        if (document != null) {
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        val newLines = PsiTreeUtil.collectElementsOfType(file, CaosScriptSpaceLikeOrNewline::class.java)
        for (newLine in newLines) {
            replaceIfBlankOrComma(newLine)
        }
        CodeStyleManager.getInstance(project).reformat(fileIn, true)
    }

    private fun replaceIfBlankOrComma(next:PsiElement?) {
        if (next == null)
            return
        if (next.text.trim(' ', '\n', ',').isEmpty() || next.text == ",")
            next.replace(CaosScriptPsiElementFactory.newLine(next.project))
    }

}