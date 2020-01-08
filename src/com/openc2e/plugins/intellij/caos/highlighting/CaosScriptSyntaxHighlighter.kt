package com.openc2e.plugins.intellij.caos.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTempTextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.openc2e.plugins.intellij.caos.lexer.CaosScriptLexerAdapter
import com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptTokenSets
import java.awt.Color

class CaosScriptSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer {
        return CaosScriptLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        if (tokenType == null) {
            return EMPTY_KEYS
        }
        val attrKey: TextAttributesKey? = when (tokenType) {
            in CaosScriptTokenSets.COMMENTS -> COMMENT
            in CaosScriptTokenSets.ANIMATION_STRING -> ANIMATION
            in CaosScriptTokenSets.STRING_LIKE -> STRING
            in CaosScriptTokenSets.Variables -> VAR_TOKEN
            in CaosScriptTokenSets.NUMBER_LITERALS -> NUMBER
            in CaosScriptTokenSets.KEYWORDS -> KEYWORDS
            CaosScriptTypes.CaosScript_WORD -> WORD_TOKEN
            else -> null
        }
        return if (attrKey != null) arrayOf(attrKey) else EMPTY_KEYS
    }

    companion object {
        private val EMPTY_KEYS:Array<TextAttributesKey> = arrayOf()
        @JvmStatic
        val ID:TextAttributesKey = createTextAttributesKey("CaosScript_ID", DefaultLanguageHighlighterColors.IDENTIFIER)
        @JvmStatic
        val KEYWORDS:TextAttributesKey = createTextAttributesKey("CaosScript_KEYWORDS", DefaultLanguageHighlighterColors.IDENTIFIER)
        @JvmStatic
        val COMMENT: TextAttributesKey = createTextAttributesKey("CaosScript_LINE_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        @JvmStatic
        val VAR_TOKEN:TextAttributesKey = createTextAttributesKey("CaosScript_VAR_TOKEN", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
        @JvmStatic
        val WORD_TOKEN:TextAttributesKey = createTextAttributesKey("CaosScript_WORD_TOKEN", DefaultLanguageHighlighterColors.FUNCTION_CALL)
        @JvmStatic
        val NUMBER:TextAttributesKey = createTextAttributesKey("CaosScript_NUMBER_LITERAL", DefaultLanguageHighlighterColors.NUMBER)
        @JvmStatic
        val ANIMATION:TextAttributesKey = createTextAttributesKey("CaosScript_ANIMATION", DefaultLanguageHighlighterColors.STRING)
        @JvmStatic
        val STRING:TextAttributesKey = createTextAttributesKey("CaosScript_STRING_LITERAL", DefaultLanguageHighlighterColors.STRING)
    }
}

private val TextAttributesKey.asErrorAttribute:TextAttributesKey get() {
    val attributes = this.defaultAttributes.clone()
    attributes.effectType = EffectType.WAVE_UNDERSCORE
    attributes.effectColor = Color.RED.brighter()
    attributes.errorStripeColor = Color.RED.brighter()
    val name = this.externalName + "_ERROR"
    val temp = createTempTextAttributesKey(name, attributes)
    return createTextAttributesKey(name, temp)
}