package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.intellij.codeInsight.editorActions.QuoteHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator

class CaosScriptQuoteHandler : QuoteHandler {
    private val openingTokens = listOf(
            CaosScriptTypes.CaosScript_DOUBLE_QUOTE,
            CaosScriptTypes.CaosScript_SINGLE_QUOTE,
            CaosScriptTypes.CaosScript_OPEN_BRACKET
    )

    private val closingTokens = listOf(
            CaosScriptTypes.CaosScript_DOUBLE_QUOTE,
            CaosScriptTypes.CaosScript_SINGLE_QUOTE,
            CaosScriptTypes.CaosScript_CLOSE_BRACKET
    )

    private val closingChars = listOf(
            '"',
            '\'',
            ']'
    )

    override fun isOpeningQuote(iterator: HighlighterIterator, offset: Int): Boolean {
       return iterator.start == offset && iterator.tokenType in openingTokens
    }

    override fun hasNonClosedLiteral(editor: Editor, iterator: HighlighterIterator, offset: Int): Boolean {
        val start = iterator.start
        try {
            val doc = editor.document
            val chars = doc.charsSequence
            val lineEnd = doc.getLineEndOffset(doc.getLineNumber(offset))
            while (!iterator.atEnd() && iterator.start < lineEnd) {
                val tokenType = iterator.tokenType
                if (tokenType in closingTokens && isNonClosedLiteral(iterator, chars)) {
                    return true
                }
                iterator.advance()
            }
        } finally {
            while (true) {
                if (iterator.atEnd() || iterator.start != start) {
                    iterator.retreat()
                } else {
                    break
                }
            }
        }
        return false
    }

    override fun isClosingQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        val tokenType = iterator.tokenType
        return if (tokenType !in closingTokens) {
            false
        } else {
            val start = iterator.start
            val end = iterator.end
            end - start >= 1 && offset == end - 1
        }
    }


    private fun isNonClosedLiteral(iterator: HighlighterIterator, chars: CharSequence): Boolean {
        return iterator.start >= iterator.end - 1 || chars[iterator.end - 1] !in closingChars
    }

    override fun isInsideLiteral(iterator: HighlighterIterator): Boolean {
        return iterator.tokenType in openingTokens
    }

}