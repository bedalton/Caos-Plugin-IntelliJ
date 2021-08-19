package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.lineNumber
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.badahori.creatures.plugins.intellij.agenteering.utils.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
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
        val message = try {
            if (collapseLines(file, collapseChar) == null)
                "Failed to collapse script to single line"
            else
                null
        } catch (e:Exception) {
            e.message
        }
        if (message != null) {
            CaosNotifications.showError(project, "Collapse Script Error", message)
        }
    }


    companion object {
        val COLLAPSE_WITH_COMMA = CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA)
        val COLLAPSE_WITH_SPACE = CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE)
        private val QUOTE_STRING =
            "(\"([^\"\\\\\\n\\r]|\\\\[\"|\\\\])*\")|('([^\\r\\n'\\\\]|\\\\['\\\\])*')".toRegex(RegexOption.MULTILINE)
        private val BRACKET_STRING = "\\[[^\\\\]*?]".toRegex(RegexOption.MULTILINE)
        private val AT_DIRECTIVES = "\\*{2}((([^\\n;]|[;][^;])*;;)|[^\\n]*)".toRegex(RegexOption.MULTILINE)
        val COMMENT = "^[ \t]*\\*[^\\n]+\\n?\$".toRegex(RegexOption.MULTILINE)

        // Escape strings
        private const val QUOTE_STRING_ESCAPE = "&;;3&2&1&;xX_X_Xx;1&2&3&;;&"
        private const val BRACKET_STRING_ESCAPE = "%;;1%2%3;xX_X_Xx;3%2%1;;%"

        private val WHITESPACE_OR_COMMA = "(\\s|,)+".toRegex(RegexOption.MULTILINE)

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
            val document = elementIn.document
            val filePointer = SmartPointerManager.createPointer(elementIn.containingFile)
            val elementClass = elementIn.javaClass
            if (document == null) {
                LOGGER.severe("Failed to get document for collapsing")
                return null
            }
            val text = elementIn.text
            var formatted = text

            // Remove comments
            formatted = AT_DIRECTIVES.replace(formatted, " ")
            formatted = COMMENT.replace(formatted, " ")

            // Escape quote strings
            val quoteStrings = QUOTE_STRING.findAll(formatted).asSequence().toList().map {
                it.groupValues[0]
            }
            formatted = QUOTE_STRING.replace(formatted, QUOTE_STRING_ESCAPE)

            // Escape Bracket Strings
            val bracketStrings = BRACKET_STRING.findAll(formatted).asSequence().toList().map {
                it.groupValues[0]
            }
            formatted = BRACKET_STRING.replace(formatted, BRACKET_STRING_ESCAPE)

            if (formatted.contains('\n')) {
                formatted = formatted.replace('\n', ' ')
            }
            // Replace all whitespace with a single space
            formatted = formatted.replace(WHITESPACE_OR_COMMA, " ")

            // Unescape Quotes
            formatted = replaceQuotes(
                project = project,
                type = "String",
                formattedIn = formatted,
                escape = QUOTE_STRING_ESCAPE,
                strings = quoteStrings
            )
                ?: return null

            // Unescape Bracket strings
            formatted = replaceQuotes(
                project = project,
                type = "[string] or [animation]",
                formattedIn = formatted,
                escape = BRACKET_STRING_ESCAPE,
                strings = bracketStrings
            )?.trim()
                ?: return null

            val range = elementIn.textRange
            document.replaceString(range.startOffset, range.endOffset, formatted.trim())

            val file = filePointer.element
            if (file?.isValid != true) {
                LOGGER.severe("Failed to commit document after collapsing")
                return null
            }

            filePointer.element?.let { theFile ->
                PsiDocumentManager.getInstance(project).getCachedDocument(theFile)?.let { doc ->
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(doc)
                }
            }

            val newElement = file.findElementAt(range.startOffset + 1)?.getParentOfType(elementClass)
            if (newElement == null) {
                document.replaceString(range.startOffset, range.startOffset + formatted.length, text)
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
                LOGGER.severe("Failed to relocate file element after replace")
                return null
            }


            filePointer.element?.let { theFile ->
                PsiDocumentManager.getInstance(project).getCachedDocument(theFile)?.let { doc ->
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(doc)
                }
            }
            if (collapseChar.char == " ")
                return newElement
            val newElementPointer = SmartPointerManager.createPointer(newElement)

            var lastIndex = 0

            filePointer.element?.let { theFile ->
                PsiDocumentManager.getInstance(project).getCachedDocument(theFile)?.let { doc ->
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(doc)
                    PsiDocumentManager.getInstance(project).commitDocument(doc)
                }
            } ?: throw Exception("File invalidated during replace with comma")

            val didReplaceAll = PsiTreeUtil.collectElementsOfType(newElement, PsiWhiteSpace::class.java)
                .filter { whiteSpace ->
                    whiteSpace.next?.let { next ->
                        next is CaosScriptCodeBlockLine || next is CaosScriptCommandCallLike || next is CaosScriptCodeBlock || next is CaosScriptHasCodeBlock || next is CaosScriptScriptBodyElement
                    } ?: false
                }
                .map {
                    SmartPointerManager.createPointer(it)
                }
                .all { pointer ->
                    val whiteSpaceElement = pointer.element
                        ?: throw Exception("Whitespace was null after: $lastIndex")
                    lastIndex = whiteSpaceElement.endOffset
                    // If this point is reached, we know it is a comma replacement char
                    val comma = CaosScriptPsiElementFactory.comma(project)
                    whiteSpaceElement.replace(comma) != null
                }
            if (!didReplaceAll) {
                throw Exception("Failed to replace all new lines with commas")
            }
            val out = newElementPointer.element
            if (out == null) {
                LOGGER.severe("Failed to reload document after collapsing")
            }
            return out
        }

        private fun replaceQuotes(
            project: Project,
            type: String,
            formattedIn: String,
            escape: String,
            strings: List<String>
        ): String? {
            var formatted = formattedIn
            for (string in strings) {
                formatted = formatted.replaceFirst(escape, string, false)
            }
            if (formatted.contains(escape)) {
                CaosNotifications.showError(
                    project,
                    "CAOS Formatting Error",
                    "Failed to unescape $type after flattening"
                )
                return null
            }
            return formatted
        }


/*
        private fun commitDocument(project: Project, pointer: SmartPsiElementPointer<PsiElement>) {
            val element = pointer.element
                ?: return
            element.document?.let {
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(it)
                CodeStyleManager.getInstance(project).reformat(element, false)
            }
        }
        fun collapseLinesPSI(elementIn: PsiElement, collapseChar: CollapseChar): PsiElement? {
            val project = elementIn.project
            val pointer = SmartPointerManager.createPointer(elementIn.copy())
            val didDeleteComments = stripComments(elementIn)
            if (!didDeleteComments) {
                CaosNotifications.showError(
                    project,
                    "CAOS Formatting Error",
                    "Failed to remove comments from document for flattening"
                )
                return null
            }

            commitDocument(project, pointer)
            val didDeleteSecondaryWhiteSpace = PsiTreeUtil.collectElementsOfType(elementIn, PsiWhiteSpace::class.java)
                .filterNot { element -> keepSpaces(element) }
                .filter { it.next?.tokenType in CaosScriptTokenSets.WHITESPACES }
                .map { SmartPointerManager.createPointer(it) }
                .all {
                    val element = it.element
                        ?: return@all false
                    if (element.isValid) {
                        element.delete()
                        true
                    } else {
                        false
                    }
                }
            if (!didDeleteSecondaryWhiteSpace) {
                CaosNotifications.showError(
                    project,
                    "CAOS Formatting Error",
                    "Failed to delete duplicate whitespace elements"
                )
                return null
            }
            commitDocument(project, pointer)
            val didReplacePrimaryWhitespace = PsiTreeUtil.collectElementsOfType(elementIn, PsiWhiteSpace::class.java)
                .filterNot { element -> keepSpaces(element) }
                .map { SmartPointerManager.createPointer(it) }
                .all {
                    val element = it.element
                        ?: return@all false
                    if (element.isValid) {
                        if (element.text != collapseChar.text) {
                            val charElement = if (collapseChar == CollapseChar.SPACE)
                                CaosScriptPsiElementFactory.spaceLikeOrNewlineSpace(project)
                            else
                                CaosScriptPsiElementFactory.comma(project)
                            element.replace(charElement) != null
                        } else {
                            true
                        }
                    } else {
                        false
                    }
                }
            if (!didReplacePrimaryWhitespace) {
                CaosNotifications.showError(
                    project,
                    "CAOS Formatting Error",
                    "Failed to delete duplicate whitespace elements"
                )
                return null
            }
            val element = pointer.element
            if (element == null) {
                CaosNotifications.showError(
                    project,
                    "CAOS Formatting Error",
                    "Script invalidated unexpectedly"
                )
                return null
            }
            commitDocument(project, pointer)
            val whiteSpaces = PsiTreeUtil.collectElementsOfType(elementIn, PsiWhiteSpace::class.java)
                .map { whiteSpace -> SmartPointerManager.createPointer(whiteSpace) }
            var trimmed = true
            for (whiteSpacePointer in whiteSpaces) {
                val whiteSpace = whiteSpacePointer.element
                    ?: break
                if (whiteSpace.previous != null)
                    break
                if (whiteSpace.isValid)
                    whiteSpace.delete()
                else {
                    trimmed = false
                    break
                }
            }
            for (whiteSpacePointer in whiteSpaces.reversed()) {
                val whiteSpace = whiteSpacePointer.element
                    ?: break
                if (whiteSpace.next != null)
                    break
                if (whiteSpace.isValid)
                    whiteSpace.delete()
                else {
                    trimmed = false
                    break
                }
            }
            if (!trimmed) {
                CaosNotifications.showError(
                    project,
                    "CAOS Formatting Error",
                    "Failed to trim leading and trailing whitespace"
                )
                return null
            }

            return elementIn.replace(element)
        }

        private fun stripComments(element: PsiElement): Boolean {
            PsiTreeUtil.collectElementsOfType(element, CaosScriptCaos2Block::class.java).firstOrNull()
                ?.let { caos2Block ->
                    val next = caos2Block.next
                    val spacePointer = if (next != null && next.tokenType in CaosScriptTokenSets.WHITESPACES)
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
            val comments = PsiTreeUtil.collectElementsOfType(element, PsiComment::class.java).map {
                SmartPointerManager.createPointer(it)
            }
            for (commentPointer in comments) {
                val comment = commentPointer.element
                didDeleteComments = if (comment != null && comment.isValid) {
                    var next = comment.next?.next
                        ?.let {
                            SmartPointerManager.createPointer(it)
                        }
                    comment.delete()
                    while (next != null) {
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
                    didDeleteComments
                } else {
                    false
                }
            }
            return didDeleteComments
        }
/*
        fun collapseLines(elementIn: PsiElement, collapseChar: CollapseChar): PsiElement? {
            val project = elementIn.project
            val pointer = SmartPointerManager.createPointer(elementIn)
            var element = pointer.element!!
            PsiTreeUtil.collectElementsOfType(element, CaosScriptCaos2Block::class.java).firstOrNull()
                ?.let { caos2Block ->
                    val next = caos2Block.next
                    val spacePointer = if (next != null && next.tokenType in CaosScriptTokenSets.WHITESPACES)
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



            pointer.element?.document?.let {
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(it)
                CodeStyleManager.getInstance(project).reformat(element, false)
            }
            element = pointer.element!!
            val newLines = PsiTreeUtil.collectElementsOfType(element, PsiWhiteSpace::class.java)
                .filter { it.textContains('\n') }
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
            if (PsiTreeUtil
                    .collectElementsOfType(element, PsiWhiteSpace::class.java)
                    .any { it.textContains('\n') }
            ) {
                CaosInjectorNotifications.showError(
                    project,
                    "CAOS Formatting Error",
                    "New lines exists after deletion"
                )
                return null
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
            if (!next.text.contains('\n') || (next.text == collapseChar.char && previousIsNotCollapseChar))
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
*/
        /**
         * Check if PSI element is blank or made up of whitespace and/or commas
         */
        private fun PsiElement.isWhitespaceOrComma(): Boolean {
            return this is PsiWhiteSpace || WHITESPACE_OR_COMMA.matches(text) || text.isBlank()
        }*/
    }
}

enum class CollapseChar(internal val text: String, internal val char: String) {
    SPACE(CaosBundle.message("caos.intentions.collapse-lines-with", "spaces"), " "),
    COMMA(CaosBundle.message("caos.intentions.collapse-lines-with", "commas"), ",")
}
