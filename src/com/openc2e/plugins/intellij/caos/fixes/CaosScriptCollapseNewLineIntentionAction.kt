package com.openc2e.plugins.intellij.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptSpaceLikeOrNewline
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptPsiElementFactory
import com.openc2e.plugins.intellij.caos.psi.util.lineNumber
import com.openc2e.plugins.intellij.caos.utils.document

class CaosScriptCollapseNewLineIntentionAction(private val collapseChar: CollapseChar) : IntentionAction {
    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")


    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (file == null)
            return false
        val hasNewlines = file.text.orEmpty().contains("\n")
        val isMultiline = file.text.length > 5 && file.lastChild.lineNumber != 0
        return hasNewlines || isMultiline
    }

    override fun getText(): String = collapseChar.text

    override fun invoke(project: Project, editor: Editor?, fileIn: PsiFile?) {
        val file = fileIn ?: return
        val document = PsiDocumentManager.getInstance(project).getCachedDocument(file) ?: fileIn.document
        if (document != null) {
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        val newLines = PsiTreeUtil.collectElementsOfType(file, CaosScriptSpaceLikeOrNewline::class.java)
        for (newLine in newLines) {
            replaceWithSpaceOrComma(newLine)
        }
        runWriteAction {
            CodeStyleManager.getInstance(project).reformat(fileIn, true)
        }
    }

    private fun replaceWithSpaceOrComma(next: PsiElement?) {
        if (next == null)
            return
        if ((next.text != "\n" && next.text.contains('\n')) || next.text == collapseChar.char)
            return
        val element = if (collapseChar == CollapseChar.SPACE)
            CaosScriptPsiElementFactory.spaceLikeOrNewlineSpace(next.project)
        else
            CaosScriptPsiElementFactory.comma(next.project)
        next.replace(element)

    }

}

enum class CollapseChar(internal val text: String, internal val char: String) {
    SPACE(CaosBundle.message("caos.intentions.collapse-lines-with", "spaces"), " "),
    COMMA(CaosBundle.message("caos.intentions.collapse-lines-with", "commas"), ",")
}