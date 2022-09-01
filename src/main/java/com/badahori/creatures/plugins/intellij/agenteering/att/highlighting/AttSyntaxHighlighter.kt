package com.badahori.creatures.plugins.intellij.agenteering.att.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.att.lexer.AttLexerAdapter
import com.badahori.creatures.plugins.intellij.agenteering.att.lexer.AttTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class AttSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer {
        return AttLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        if (tokenType == null) {
            return EMPTY_KEYS
        }
        val attrKey: TextAttributesKey? = when (tokenType) {
            AttTypes.ATT_INT_LITERAL -> NUMBER
            AttTypes.ATT_ERROR_VALUE_LITERAL -> ERROR_COMMAND_TOKEN
            else -> null
        }
        return if (attrKey != null) arrayOf(attrKey) else EMPTY_KEYS
    }

    companion object {
        private val EMPTY_KEYS: Array<TextAttributesKey> = arrayOf()

        @JvmStatic
        val ERROR_COMMAND_TOKEN: TextAttributesKey =
            createTextAttributesKey("Att Error Element", DefaultLanguageHighlighterColors.IDENTIFIER)

        @JvmStatic
        val NUMBER: TextAttributesKey =
            createTextAttributesKey("Att Number", DefaultLanguageHighlighterColors.IDENTIFIER)

        val X1: TextAttributesKey = createTextAttributesKey("ATT_X_1", NUMBER)
        val Y1: TextAttributesKey = createTextAttributesKey("ATT_Y_1", X1)
        val X2: TextAttributesKey = createTextAttributesKey("ATT_X_2", NUMBER)
        val Y2: TextAttributesKey = createTextAttributesKey("ATT_Y_2", X2)
        val X3: TextAttributesKey = createTextAttributesKey("ATT_X_3", NUMBER)
        val Y3: TextAttributesKey = createTextAttributesKey("ATT_Y_3", X3)
        val X4: TextAttributesKey = createTextAttributesKey("ATT_X_4", NUMBER)
        val Y4: TextAttributesKey = createTextAttributesKey("ATT_Y_4", X4)
        val X5: TextAttributesKey = createTextAttributesKey("ATT_X_5", NUMBER)
        val Y5: TextAttributesKey = createTextAttributesKey("ATT_Y_5", X5)
        val X6: TextAttributesKey = createTextAttributesKey("ATT_X_6", NUMBER)
        val Y6: TextAttributesKey = createTextAttributesKey("ATT_Y_6", X6)
    }
}