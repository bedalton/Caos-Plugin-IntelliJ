package com.openc2e.plugins.intellij.caos.def.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTempTextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.openc2e.plugins.intellij.caos.def.lexer.CaosDefLexerAdapter
import com.openc2e.plugins.intellij.caos.def.lexer.CaosDefTypes.*
import com.openc2e.plugins.intellij.caos.def.psi.types.CaosDefTokenSets.Companion.COMMENT_AT_KEYWORDS
import com.openc2e.plugins.intellij.caos.def.psi.types.CaosDefTokenSets.Companion.COMMENT_PARTS
import java.awt.Color

class CaosDefSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer {
        return CaosDefLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        if (tokenType == null) {
            return EMPTY_KEYS
        }
        var attrKey: TextAttributesKey? = null

        if (tokenType in COMMENT_PARTS)
            attrKey = COMMENT
        if (tokenType in COMMENT_AT_KEYWORDS)
            attrKey = COMMENT_STATEMENT
        if (tokenType == CaosDef_WORD_LINK)
            attrKey = WORD_LINK
        if (tokenType == CaosDef_TYPE_LINK || tokenType == CaosDef_TYPE_DEF_NAME)
            attrKey = TYPE_LINK
        if (tokenType == CaosDef_REGION_HEADING_LITERAL)
            attrKey = REGION_HEADER
        return if (attrKey != null) arrayOf(attrKey) else EMPTY_KEYS
    }

    companion object {
        private val EMPTY_KEYS:Array<TextAttributesKey> = arrayOf()
        val ID:TextAttributesKey = createTextAttributesKey("CaosDef_ID", DefaultLanguageHighlighterColors.IDENTIFIER)
        val COMMENT: TextAttributesKey = createTextAttributesKey("CaosDef_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        val COMMENT_STATEMENT:TextAttributesKey = createTextAttributesKey("CaosDef_COMMENT_AT_STATEMENT", DefaultLanguageHighlighterColors.KEYWORD)
        val WORD_LINK:TextAttributesKey = createTextAttributesKey("CaosDef_WORD_LINK", DefaultLanguageHighlighterColors.KEYWORD)
        val TYPE_LINK:TextAttributesKey = createTextAttributesKey("CaosDef_TYPE_LINK", DefaultLanguageHighlighterColors.KEYWORD)
        val REGION_HEADER:TextAttributesKey = createTextAttributesKey("CaosDef_REGION_HEADER", DefaultLanguageHighlighterColors.KEYWORD)
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