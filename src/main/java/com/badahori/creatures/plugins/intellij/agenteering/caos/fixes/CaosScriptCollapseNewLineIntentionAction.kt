package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Block
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlockLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptComment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSpaceLikeOrNewline
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosInjectorNotifications
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
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

        // Comma is not a valid collapse char in C2e
        if (collapseChar == CollapseChar.COMMA) {
            // Ensure a variant is set for this file
            // so that we can validate this option
            val variant = (file as? CaosScriptFile)?.variant
                ?: return false
            // Return if variant is new variant
            if (variant.isNotOld)
                return false
        }
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
        val COLLAPSE_WITH_COMMA = CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA)
        val COLLAPSE_WITH_SPACE = CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE)

        private val whitespaceOrComma = "(\\s|,)+".toRegex()
        private val trailingText = "[ \n,\t]+".toRegex()

        fun collapseLinesInCopy(elementIn: PsiElement, collapseChar: CollapseChar = CollapseChar.COMMA): PsiElement? {
            val project = elementIn.project
            val document = PsiDocumentManager.getInstance(project).getCachedDocument(elementIn.containingFile) ?: elementIn.document
            if (document != null) {
                PsiDocumentManager.getInstance(project).commitDocument(document)
            }
            val element = elementIn.copy()
            return collapseLines(element, collapseChar)
        }

        fun collapseLines(element: PsiElement, collapseChar: CollapseChar): PsiElement? {
            val project = element.project
            PsiTreeUtil.collectElementsOfType(element, CaosScriptCaos2Block::class.java).firstOrNull()?.let {caos2Block ->
                val next = caos2Block.next
                val spacePointer = if (next != null && next.elementType in CaosScriptTokenSets.WHITESPACES)
                    SmartPointerManager.createPointer(next)
                else
                    null
                caos2Block.delete()
                spacePointer?.element?.delete()
            }

            val comments = PsiTreeUtil.collectElementsOfType(element, CaosScriptComment::class.java)
            var didDeleteComments = true
            for (comment in comments) {
                didDeleteComments = if (comment.isValid) {
                    comment.getParentOfType(CaosScriptCodeBlockLine::class.java)?.apply {
                        delete()
                    } != null && didDeleteComments
                } else {
                    false
                }
            }
            if (!didDeleteComments) {
                CaosNotifications.showError(project, "CAOS Formatting Error", "Failed to remove comments from document for flattening")
                return null
            }
            element.document?.let {
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(it)
                CodeStyleManager.getInstance(project).reformat(element, false)
            }
            val newLines = PsiTreeUtil.collectElementsOfType(element, CaosScriptSpaceLikeOrNewline::class.java)
                .map {
                    SmartPointerManager.createPointer(it)
                }
            var didReplaceAll = true
            for (newLinePointer in newLines) {
                val newLine = newLinePointer.element
                    ?: continue
                if (newLine.isValid) {
                    replaceWithSpaceOrComma(newLine, collapseChar)
                } else {
                    didReplaceAll = false
                }
            }
            if (!didReplaceAll) {
                CaosInjectorNotifications.showError(project, "CAOS Formatting Error", "Failed to replace new lines with ${if (collapseChar == CollapseChar.SPACE) "spaces" else "commas"}")
                return null
            }
            while (trailingText.matches(element.firstChild.text))
                element.firstChild.delete()
            while (trailingText.matches(element.lastChild.text))
                element.lastChild.delete()
            runWriteAction {
                element.document?.let {
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(it)
                    CodeStyleManager.getInstance(project).reformat(element, true)
                }
            }
            return element
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