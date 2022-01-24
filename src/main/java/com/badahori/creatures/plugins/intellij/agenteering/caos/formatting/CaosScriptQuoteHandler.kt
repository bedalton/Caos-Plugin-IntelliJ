package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.intellij.codeInsight.editorActions.QuoteHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType

class CaosScriptQuoteHandler : QuoteHandler {
    private val openingTokens = listOf(
        CaosScriptTypes.CaosScript_DOUBLE_QUOTE,
        CaosScriptTypes.CaosScript_SINGLE_QUOTE,
        CaosScriptTypes.CaosScript_OPEN_BRACKET
    )

    private val insideTokens = listOf(
        CaosScriptTypes.CaosScript_STRING_TEXT,
        CaosScriptTypes.CaosScript_TEXT_LITERAL,
        CaosScriptTypes.CaosScript_STRING_CHAR,
        CaosScriptTypes.CaosScript_GHOST_QUOTE,
        CaosScriptTypes.CaosScript_ANIM_R,
        CaosScriptTypes.CaosScript_BYTE_STRING_POSE_ELEMENT,
        CaosScriptTypes.CaosScript_INT,
        TokenType.WHITE_SPACE
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
        if (iterator.start != offset && iterator.tokenType !in openingTokens) {
            return false
        }
        val firstToken = iterator.tokenType
        iterator.advance()
        if (iterator.atEnd()) {
            iterator.retreat()
            return true
        }
        while (!iterator.atEnd() && iterator.tokenType in insideTokens && iterator.tokenType !in closingTokens) {
            try {
                iterator.advance()
            } catch (ignored: Exception) {
                break
            }
        }
        val lastToken = iterator.tokenType
        iterator.retreat()
        return lastToken == firstToken || (firstToken == CaosScriptTypes.CaosScript_OPEN_BRACKET && lastToken == CaosScriptTypes.CaosScript_CLOSE_BRACKET)
    }

    override fun hasNonClosedLiteral(editor: Editor, iterator: HighlighterIterator, offset: Int): Boolean {
        val start = iterator.start

        try {
            val doc = editor.document
            //val chars = doc.charsSequence
            val lineEnd = doc.getLineEndOffset(doc.getLineNumber(offset))
            while (!iterator.atEnd() && start < lineEnd) {
                val tokenType = iterator.tokenType
                if (tokenType in closingTokens) {
                    return false
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
        return true
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
        return iterator.tokenType in insideTokens
    }

}