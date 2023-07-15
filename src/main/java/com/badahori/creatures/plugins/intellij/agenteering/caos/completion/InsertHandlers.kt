package com.badahori.creatures.plugins.intellij.agenteering.caos.completion


import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqualityExpressionPrime
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringText
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement


object SpaceAfterInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        if (EditorUtil.isTextAtOffset(context, " ") || EditorUtil.isTextAtOffset(context, "\n") || EditorUtil.isTextAtOffset(context, "\t"))
            return
        if (lookupElement.psiElement?.getParentOfType(CaosScriptEqualityExpressionPrime::class.java)?.eqOp != null)
            return
        EditorUtil.insertText(context, " ", true)
    }
}

object ReplaceTextWithValueInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupEl: LookupElement) {
        context.document.replaceString(context.startOffset, context.tailOffset, lookupEl.lookupString)
    }
}

object ReplaceContentsWithValueInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupEl: LookupElement) {
        val element = lookupEl.psiElement
        val lookupString = lookupEl.lookupString
        if (element != null) {
            val stringLike = element.getSelfOrParentOfType(CaosScriptStringLike::class.java)?.let {
                if (it.firstChild is CaosScriptStringText) {
                    it.firstChild
                } else {
                    it
                }
            } ?: element
            val text = element.text
            context.document.replaceString(element.startOffset, element.endOffset, lookupString)
        } else {
            context.document.replaceString(context.startOffset, context.tailOffset, lookupString)
        }
    }
}

object ReplaceStringContentsWithValueInsertHandler : InsertHandler<LookupElement> {

    private val DUMMY_IDENTIFIER_WITH_TRAILING_SPACE = "$DUMMY_IDENTIFIER_TRIMMED ?".toRegex()
    override fun handleInsert(context: InsertionContext, lookupEl: LookupElement) {
        val element = lookupEl.psiElement
        val lookupString = lookupEl.lookupString
        if (element == null) {
            LOGGER.info("ReplaceStringInsertHandler: Element is null")
            context.document.replaceString(context.startOffset, context.tailOffset, lookupString)
            return
        }
        LOGGER.info("ReplaceStringInsertHandler: Element: ${element.tokenType}; Text: ${element.text}")
        val stringLike = element.getSelfOrParentOfType(CaosScriptStringLike::class.java)?.let {
            if (it.firstChild is CaosScriptStringText) {
                it.firstChild
            } else {
                it
            }
        } ?: element


        var start = stringLike.startOffset
        val text = stringLike.text.replace(DUMMY_IDENTIFIER_WITH_TRAILING_SPACE, "")

        LOGGER.info("ReplaceStringInsertHandler: StringLikeElement: ${stringLike.tokenType}; Text: <${stringLike.text}>; TrueText: <$text>; LookupString: <${lookupString}>")

        val firstChar = text.firstOrNull()
        val lastChar = if (text.length > 1) {
            text.last()
        } else {
            null
        }
        if (firstChar == '"') {
            start += 1
        } else if (firstChar == '\'') {
            start += 1
        }

        val endMod = if (lastChar == '"') {
            -1
        } else if (lastChar == '\'' && firstChar == '\'') {
            -1
        } else {
            0
        }

        context.document.replaceString(start, context.tailOffset, lookupString)
        val nextStart = start + lookupString.length
        val tail = stringLike.text.split(DUMMY_IDENTIFIER_WITH_TRAILING_SPACE).getOrNull(1)
            ?: return
        context.document.replaceString(nextStart, nextStart + tail.length + endMod, "")
    }
}


class ReplaceFromStartInsertHandler(private val start: Int, private val afterInsert: InsertHandler<LookupElement>? = null) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupEl: LookupElement) {
        context.document.replaceString(start, context.tailOffset, lookupEl.lookupString)
        afterInsert?.handleInsert(context, lookupEl)
    }
}

class InsertToknBeforeToken(private val afterInsert: InsertHandler<LookupElement>? = null) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupEl: LookupElement) {
        if (replace(lookupEl.psiElement, lookupEl)) {
            return
        }
        context.document.insertString(context.startOffset, "tokn " + lookupEl.lookupString)
        afterInsert?.handleInsert(context, lookupEl)
    }

    private fun replace(element: PsiElement?, lookupElement: LookupElement): Boolean {
        if (element == null) {
            return false
        }
        val token = lookupElement.lookupString
        if (token.length != 4) {
            return false
        }
        val toknRvalue = try {
            CaosScriptPsiElementFactory.createToknCommandWithToken(element.project, token)
        } catch (_: Exception) {
            null
        } ?: return false
        return try {
            element.replace(toknRvalue) != null
        } catch (_: Exception) {
            false
        }
    }
}

class ReplaceUntilWithValueInsertHandler(private val length: Int) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupEl: LookupElement) {
        context.document.replaceString(context.startOffset, context.startOffset + length, lookupEl.lookupString)
    }
}

class InsertInsideQuoteHandler(private val openQuote: Char, private val closeQuote: Char) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        var elementText: String = lookupElement.psiElement
            ?.textWithoutCompletionIdString
            ?: return
        val insertionText = lookupElement.lookupString
        if (elementText.isEmpty())
            return
        val hasOpenQuote = elementText[0] == openQuote
        val hasCloseQuote = elementText.last() == closeQuote
        if (hasOpenQuote && hasCloseQuote)
            return
        val replaceWith = if (!hasOpenQuote && !hasCloseQuote) {
            "$openQuote$insertionText$closeQuote"
        } else if (!hasOpenQuote) {
            "$openQuote$insertionText"
        } else if (!hasCloseQuote) {
            "$insertionText$closeQuote"
        } else {
            insertionText
        }
        val editor = context.editor
        EditorUtil.replaceText(editor, TextRange.create(context.startOffset, context.selectionEndOffset), replaceWith, true)
    }
}
class CloseQuoteInsertHandler(private val closeQuote:String) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        if (EditorUtil.isTextAtOffset(context, closeQuote))
            return
        EditorUtil.insertText(context, closeQuote, true)
    }
}
object EqualSignInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        try {
            if (EditorUtil.isTextAtOffset(context, "=") || EditorUtil.isTextAtOffset(context, " ="))
                return
            val text = if (EditorUtil.isTextAtOffset(context, " "))
                "= "
            else
                " = "
            EditorUtil.insertText(context, text, true)
        } catch (e: Exception) {

        }
    }
}

class OffsetCursorInsertHandler(val offset:Int) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        val editor = context.editor
        EditorUtil.offsetCaret(editor, offset)
    }
}