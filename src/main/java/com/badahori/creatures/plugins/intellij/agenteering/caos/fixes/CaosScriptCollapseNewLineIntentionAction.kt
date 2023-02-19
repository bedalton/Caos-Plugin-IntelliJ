package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil


class CaosScriptCollapseNewLineWithCommasIntentionAction: CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA) {
    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (element.variant?.isOld != true)
            return false
        return super.isAvailable(project, editor, element)
    }
}
class CaosScriptCollapseNewLineWithSpacesIntentionAction: CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE)

abstract class CaosScriptCollapseNewLineIntentionAction(
    private val collapseChar: CollapseChar
): PsiElementBaseIntentionAction(), LocalQuickFix {
    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CAOSScript

    open override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val file = element.containingFile
        if (file !is CaosScriptFile)
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
        val isMultiline = file.text.length > 3 && file.lastChild.lineNumber != 0
        return hasNewlines || isMultiline
    }

    override fun getText(): String = collapseChar.text

    override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
        val file = problemDescriptor.psiElement?.containingFile ?: return
        invoke(project, file)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = element.containingFile
            ?: return
        invoke(project, file)
    }

    private fun invoke(project: Project, file: PsiFile) {
        if (!ApplicationManager.getApplication().isDispatchThread) {
            try {
                collapseLines(file, collapseChar)
            } catch (_: Exception) {

            }
            return
        }
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
            try {
                CaosNotifications.showError(project, "Collapse Script Error", message)
            } catch (_: Exception) {

            }
        }
    }


    companion object {
        private val QUOTE_STRING =
            "(\"([^\"\\\\\\n\\r]|\\\\[\"|\\\\])*\")|('([^\\r\\n'\\\\]|\\\\['\\\\])*')".toRegex(RegexOption.MULTILINE)
        private val BRACKET_STRING = "\\[[^\\\\]*?]".toRegex(RegexOption.MULTILINE)
        private val AT_DIRECTIVES = "^[*]{2}((([^\\n;]|[;][^;])*;;)|[^\\n]*)".toRegex(RegexOption.MULTILINE)
        val COMMENT = "^[ \\t]*[*][^\\n]*\\n?".toRegex(RegexOption.MULTILINE)

        // Escape strings
        private const val QUOTE_STRING_ESCAPE = "&;;3&2&1&;xX_X_Xx;1&2&3&;;&"
        private const val BRACKET_STRING_ESCAPE = "%;;1%2%3;xX_X_Xx;3%2%1;;%"

        private val WHITESPACE_OR_COMMA = "(\\s|,)+".toRegex(RegexOption.MULTILINE)

        fun collapseLinesInCopy(elementIn: PsiElement, collapseChar: CollapseChar = CollapseChar.COMMA): PsiElement? {
            return if (ApplicationManager.getApplication().isDispatchThread) {
                runWriteAction {
                    collapseLinesEx(elementIn, collapseChar)
                }
            } else {
                runWriteAction {
                    collapseLinesEx(elementIn, collapseChar)
                }
            }
        }

        internal fun collapseLinesInCopyEx(elementIn: PsiElement, collapseChar: CollapseChar = CollapseChar.COMMA): PsiElement? {
            val project = elementIn.project
            val document = PsiDocumentManager.getInstance(project).getCachedDocument(elementIn.containingFile)
                ?: elementIn.document
            if (document != null) {
                PsiDocumentManager.getInstance(project).commitDocument(document)
            }
            if (elementIn is CaosScriptFile) {
                val file = CaosScriptPsiElementFactory.createFileFromText(project, elementIn.text)
                file.setVariant(elementIn.variant, true)
                return collapseLines(file, collapseChar, elementIn.variant)
            } else {
                val element = elementIn.copy()
                return collapseLines(element, collapseChar, elementIn.variant)
            }
        }

        fun collapseLines(elementIn: PsiElement, collapseChar: CollapseChar, variant: CaosVariant? = null): PsiElement? {
            return if (ApplicationManager.getApplication().isDispatchThread) {
                runWriteAction {
                    collapseLinesEx(elementIn, collapseChar, variant)
                }
            } else {
                runWriteAction {
                    collapseLinesEx(elementIn, collapseChar, variant)
                }
            }
        }


        fun collapseLinesEx(elementIn: PsiElement, collapseChar: CollapseChar, variant: CaosVariant? = null): PsiElement? {
            val before = elementIn.text.replace(COLLAPSE_TEST_REGEX, "").replace(WHITESPACE_OR_COMMA, "")
            val project = elementIn.project
            val document = elementIn.document
            val filePointer = SmartPointerManager.createPointer(elementIn.containingFile)
            val elementClass = elementIn.javaClass
            if (document == null) {
                LOGGER.severe("Failed to get document for collapsing")
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
            if (document == null) {
                return try {
                    CaosScriptPsiElementFactory.createAndGet(project, formatted, elementIn::class.java, variant!!)
                        ?.apply {
                            if (!this.isValid) {
                                LOGGER.severe("Document was null. Attempted to create new item with factory, but element was invalid")
                            }
                        }
                } catch (e: Exception) {
                    null
                }.apply {
                    LOGGER.severe("Document was null. Attempting to create new item: ${this?.text}")
                }
            }
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
                    whiteSpace.isValid && whiteSpace.next?.let { next ->
                        next is CaosScriptAtDirectiveComment || next is CaosScriptCodeBlockLine || next is CaosScriptCommandCallLike || next is CaosScriptCodeBlock || next is CaosScriptHasCodeBlock || next is CaosScriptScriptBodyElement
                    } ?: false
                }
                .mapNotNull {
                    if (it.isValid) {
                        SmartPointerManager.createPointer(it)
                    } else {
                        null
                    }
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
                return null
            }
            val after = out.text
                .replace(COLLAPSE_TEST_REGEX, "")
                .replace(WHITESPACE_OR_COMMA, "")
            if (before != after) {
                val error = "Collapsed scripts does not match original script contents. Before(noSpace)<$before>; After(noSpace)<$after>"
                LOGGER.severe(error)
                throw Exception(error)
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
    }
}

enum class CollapseChar(internal val text: String, internal val char: String) {
    SPACE(CaosBundle.message("caos.intentions.collapse-lines-with", "spaces"), " "),
    COMMA(CaosBundle.message("caos.intentions.collapse-lines-with", "commas"), ",")
}

private val COLLAPSE_TEST_REGEX = "(^\\s*[*][^\\n]*\\n*)".toRegex(RegexOption.MULTILINE)
