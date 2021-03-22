package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.lineNumber
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.previous
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosInjectorNotifications
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.badahori.creatures.plugins.intellij.agenteering.utils.elementType
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptCollapseNewLineIntentionAction(private val collapseChar: CollapseChar) : IntentionAction,
    LocalQuickFix {
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
            val document = PsiDocumentManager.getInstance(project).getCachedDocument(elementIn.containingFile)
                ?: elementIn.document
            if (document != null) {
                PsiDocumentManager.getInstance(project).commitDocument(document)
            }
            val element = elementIn.copy()
            return collapseLines(element, collapseChar)
        }

        fun collapseLines(elementIn: PsiElement, collapseChar: CollapseChar): PsiElement? {
            val project = elementIn.project
            val pointer = SmartPointerManager.createPointer(elementIn)
            var element = pointer.element!!
            PsiTreeUtil.collectElementsOfType(element, CaosScriptCaos2Block::class.java).firstOrNull()
                ?.let { caos2Block ->
                    val next = caos2Block.next
                    val spacePointer = if (next != null && next.elementType in CaosScriptTokenSets.WHITESPACES)
                        SmartPointerManager.createPointer(next)
                    else
                        null
                    caos2Block.delete()
                    spacePointer?.element?.delete()
                }

            var didDeleteComments = PsiTreeUtil.collectElementsOfType(element, CaosScriptCommentBlock::class.java).map {
                SmartPointerManager.createPointer(it)
            }.all { commentBlockPointer ->
                commentBlockPointer.element?.let {
                    it.delete()
                    true
                } ?: false
            }

            val comments = PsiTreeUtil.collectElementsOfType(element, CaosScriptComment::class.java).map {
                SmartPointerManager.createPointer(it)
            }
            for (commentPointer in comments) {
                val comment = commentPointer.element
                didDeleteComments = if (comment != null && comment.isValid) {
                    comment.getParentOfType(CaosScriptCodeBlockLine::class.java)?.apply {
                        var next = this.next?.next
                            ?.let {
                                SmartPointerManager.createPointer(it)
                            }
                        delete()
                        while(next != null) {
                            val toDelete = next.element
                                ?: break
                            if (toDelete.text.isNotBlank())
                                break
                            val nextNext = toDelete.next
                            next = if (nextNext != null && nextNext.text.trim().isBlank())
                                SmartPointerManager.createPointer(nextNext)
                            else
                                null
                            toDelete.delete()
                        }
                    } != null && didDeleteComments
                } else {
                    false
                }
            }
            if (!didDeleteComments) {
                CaosNotifications.showError(
                    project,
                    "CAOS Formatting Error",
                    "Failed to remove comments from document for flattening"
                )
                return null
            }

            pointer.element?.document?.let {
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(it)
                CodeStyleManager.getInstance(project).reformat(element, false)
            }
           element = pointer.element!!
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
                CaosInjectorNotifications.showError(
                    project,
                    "CAOS Formatting Error",
                    "Failed to replace new lines with ${if (collapseChar == CollapseChar.SPACE) "spaces" else "commas"}"
                )
                return null
            }
            element = pointer.element!!
            PsiTreeUtil.collectElementsOfType(element, PsiWhiteSpace::class.java)
                .filter { it.elementType == TokenType.WHITE_SPACE }
                .map { SmartPointerManager.createPointer(it) }
                .forEach {
                    it.element?.delete()
                }
            element = pointer.element!!

            while (trailingText.matches(element.firstChild?.text ?: "x;x;x"))
                element.firstChild.delete()
            while (trailingText.matches(element.lastChild?.text ?: "x;x;x"))
                element.lastChild.delete()
            runWriteAction {
                element.document?.let {
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(it)
                    CodeStyleManager.getInstance(project).reformat(element, true)
                }
            }
            return pointer.element!!
        }

        private fun replaceWithSpaceOrComma(nextIn: PsiElement?, collapseChar: CollapseChar) {
            if (nextIn == null)
                return
            val pointer = SmartPointerManager.createPointer(nextIn)
            var previous = nextIn.previous
            while (previous != null && previous.isWhitespaceOrComma()) {
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
            var nextPointer: SmartPsiElementPointer<PsiElement>?
            while (superNext.isWhitespaceOrComma()) {
                val toDelete = superNext
                nextPointer = superNext.next?.let {
                    SmartPointerManager.createPointer(it)
                }
                toDelete.delete()
                superNext = nextPointer?.element ?: return
            }
        }

        /**
         * Check if PSI element is blank or made up of whitespace and/or commas
         */
        private fun PsiElement.isWhitespaceOrComma() : Boolean {
            return this is PsiWhiteSpace || whitespaceOrComma.matches(text) || text.isBlank()
        }
    }
}

enum class CollapseChar(internal val text: String, internal val char: String) {
    SPACE(CaosBundle.message("caos.intentions.collapse-lines-with", "spaces"), " "),
    COMMA(CaosBundle.message("caos.intentions.collapse-lines-with", "commas"), ",")
}