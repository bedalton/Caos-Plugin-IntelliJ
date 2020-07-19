package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptComment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSpaceLikeOrNewline
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.lineNumber
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.document
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

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

    override fun invoke(project: Project, editor: Editor, fileIn: PsiFile?) {
        val file = fileIn ?: return
        val document = PsiDocumentManager.getInstance(project).getCachedDocument(file) ?: fileIn.document
        if (document != null) {
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        val comments = PsiTreeUtil.collectElementsOfType(file, CaosScriptComment::class.java)
        for(comment in comments) {
            comment.delete()
        }
        val newLines = PsiTreeUtil.collectElementsOfType(file, CaosScriptSpaceLikeOrNewline::class.java)
        for (newLine in newLines) {
            if (newLine.isValid)
                replaceWithSpaceOrComma(newLine, collapseChar)
        }
        runWriteAction {
            CodeStyleManager.getInstance(project).reformat(fileIn, true)
        }
    }


    companion object {
        fun collapseLinesInCopy(fileIn: PsiFile, collapseChar:CollapseChar = CollapseChar.COMMA) : PsiFile {
            val project = fileIn.project
            var document = PsiDocumentManager.getInstance(project).getCachedDocument(fileIn) ?: fileIn.document
            if (document != null) {
                PsiDocumentManager.getInstance(project).commitDocument(document)
            }
            val file = fileIn.copy().let { it as? PsiFile ?: it.containingFile }
            val comments = PsiTreeUtil.collectElementsOfType(file, CaosScriptComment::class.java)
            for(comment in comments) {
                comment.delete()
            }
            val newLines = PsiTreeUtil.collectElementsOfType(file, CaosScriptSpaceLikeOrNewline::class.java)
            for (newLine in newLines) {
                if (newLine.isValid)
                    replaceWithSpaceOrComma(newLine, collapseChar)
            }
            document = PsiDocumentManager.getInstance(project).getCachedDocument(file) ?: file.document
            if (document != null) {
                PsiDocumentManager.getInstance(project).commitDocument(document)
            }
            return file
        }

        private fun replaceWithSpaceOrComma(next: PsiElement?, collapseChar: CollapseChar) {
            if (next == null)
                return
            if ((next.text != "\n" && !next.text.contains('\n')) || next.text == collapseChar.char)
                return
            val element = if (collapseChar == CollapseChar.SPACE)
                CaosScriptPsiElementFactory.spaceLikeOrNewlineSpace(next.project)
            else
                CaosScriptPsiElementFactory.comma(next.project)
            next.replace(element)
            var superNext = next.next
                    ?: return
            while(superNext.text.contains("\n") || superNext.text.contains(collapseChar.char)) {
                superNext.delete()
                superNext = superNext.next ?: return
            }
        }
    }

}

enum class CollapseChar(internal val text: String, internal val char: String) {
    SPACE(CaosBundle.message("caos.intentions.collapse-lines-with", "spaces"), " "),
    COMMA(CaosBundle.message("caos.intentions.collapse-lines-with", "commas"), ",")
}