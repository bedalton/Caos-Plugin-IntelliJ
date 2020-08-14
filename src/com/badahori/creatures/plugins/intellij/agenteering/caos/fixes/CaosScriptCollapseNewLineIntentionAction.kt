package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptComment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSpaceLikeOrNewline
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.lineNumber
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.previous
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.document
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptCollapseNewLineIntentionAction(private val collapseChar: CollapseChar) : IntentionAction, LocalQuickFix {
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

    override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
        val file = problemDescriptor.psiElement?.containingFile ?: return
        invoke(project, file)
    }

    override fun invoke(project: Project, editor: Editor, fileIn: PsiFile?) {
        val file = fileIn ?: return
        invoke(project, file)
    }

    private fun invoke(project: Project, file: PsiFile) {
        val document = PsiDocumentManager.getInstance(project).getCachedDocument(file) ?: file.document
        if (document != null) {
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        collapseLines(file, collapseChar)
    }


    companion object {

        private val whitespaceOrComma = "(\\s|,)+".toRegex()
        private val trailingText = "[ \n,\t]+".toRegex()

        fun collapseLinesInCopy(fileIn: PsiFile, collapseChar: CollapseChar = CollapseChar.COMMA): PsiFile {
            val project = fileIn.project
            val document = PsiDocumentManager.getInstance(project).getCachedDocument(fileIn) ?: fileIn.document
            if (document != null) {
                PsiDocumentManager.getInstance(project).commitDocument(document)
            }
            val file = fileIn.copy().let { it as? PsiFile ?: it.containingFile }
            return collapseLines(file, collapseChar)
        }

        fun collapseLines(file: PsiFile, collapseChar: CollapseChar): PsiFile {
            val project = file.project
            val comments = PsiTreeUtil.collectElementsOfType(file, CaosScriptComment::class.java)
            for (comment in comments) {
                if (comment.isValid) {
                    var replacement = CaosScriptPsiElementFactory.spaceLikeOrNewlineSpace(project)
                    replacement = comment.replace(replacement)
                    replacement.delete()
                }
            }
            file.document?.let {
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(it)
                CodeStyleManager.getInstance(project).reformat(file, true)
            }
            val newLines = PsiTreeUtil.collectElementsOfType(file, CaosScriptSpaceLikeOrNewline::class.java)
            for (newLine in newLines) {
                if (newLine.isValid)
                    replaceWithSpaceOrComma(newLine, collapseChar)
            }
            while (trailingText.matches(file.firstChild.text))
                file.firstChild.delete()
            while (trailingText.matches(file.lastChild.text))
                file.lastChild.delete()
            runWriteAction {
                file.document?.let {
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(it)
                    CodeStyleManager.getInstance(project).reformat(file, true)
                }
            }
            return file
        }

        private fun replaceWithSpaceOrComma(nextIn: PsiElement?, collapseChar: CollapseChar) {

            if (nextIn == null)
                return

            val pointer = SmartPointerManager.createPointer(nextIn)
            var previous = nextIn.previous
            while(previous is PsiWhiteSpace) {
                previous.delete()
                previous = previous.previous
            }
            val next = pointer.element
                    ?: return
            val previousIsNotCollapseChar = next.previous?.text != collapseChar.char
            if ((next.text != "\n" && !next.text.contains('\n')) || (next.text == collapseChar.char && previousIsNotCollapseChar))
                return
            var superNext = if (previousIsNotCollapseChar) {
                val element = if (collapseChar == CollapseChar.SPACE)
                    CaosScriptPsiElementFactory.spaceLikeOrNewlineSpace(next.project)
                else
                    CaosScriptPsiElementFactory.comma(next.project)
                val replaced = next.replace(element)
                replaced.next
            } else {
                next
            } ?: return
            while (whitespaceOrComma.matches(superNext.text)) {
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