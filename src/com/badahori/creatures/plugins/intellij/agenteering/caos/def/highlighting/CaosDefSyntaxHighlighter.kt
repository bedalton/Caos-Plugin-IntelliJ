package com.badahori.creatures.plugins.intellij.agenteering.caos.def.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTempTextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefLexerAdapter
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefTypes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.types.CaosDefTokenSets.Companion.COMMENT_AT_KEYWORDS
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.types.CaosDefTokenSets.Companion.COMMENT_PARTS
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
        //if (tokenType == CaosDef_AT_TAG)
          //  attrKey = DOC_COMMENT_TAG
        if (tokenType == CaosDef_HASH_TAG)
            attrKey = DOC_COMMENT_HASHTAG
        return if (attrKey != null) arrayOf(attrKey) else EMPTY_KEYS
    }

    companion object {
        private val EMPTY_KEYS:Array<TextAttributesKey> = arrayOf()
        val ID:TextAttributesKey = createTextAttributesKey("CaosDef_ID", DefaultLanguageHighlighterColors.IDENTIFIER)
        val CODE_BLOCK:TextAttributesKey = createTextAttributesKey("CaosDef_CODE_BLOCK", DefaultLanguageHighlighterColors.MARKUP_ENTITY)
        val COMMENT: TextAttributesKey = createTextAttributesKey("CaosDef_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        val COMMENT_STATEMENT:TextAttributesKey = createTextAttributesKey("CaosDef_COMMENT_AT_STATEMENT", DefaultLanguageHighlighterColors.KEYWORD)
        val WORD_LINK:TextAttributesKey = createTextAttributesKey("CaosDef_WORD_LINK", DefaultLanguageHighlighterColors.KEYWORD)
        val TYPE_LINK:TextAttributesKey = createTextAttributesKey("CaosDef_TYPE_LINK", DefaultLanguageHighlighterColors.KEYWORD)
        val TYPE_DEF_NAME:TextAttributesKey = createTextAttributesKey("CaosDef_TYPE_DEF_NAME", DefaultLanguageHighlighterColors.KEYWORD)
        val TYPE_DEF_KEY:TextAttributesKey = createTextAttributesKey("CaosDef_TYPE_DEF_KEY", DefaultLanguageHighlighterColors.CLASS_REFERENCE)
        val TYPE_DEF_WORD_LINK:TextAttributesKey = createTextAttributesKey("CaosDef_TYPE_DEF_WORD_LINK", DefaultLanguageHighlighterColors.KEYWORD)
        val REGION_HEADER:TextAttributesKey = createTextAttributesKey("CaosDef_REGION_HEADER", DefaultLanguageHighlighterColors.KEYWORD)
        val VARIABLE_LINK: TextAttributesKey = createTextAttributesKey("CaosDef_VARIABLE_LINK", DefaultLanguageHighlighterColors.KEYWORD)
        val VARIABLE_TYPE: TextAttributesKey = createTextAttributesKey("CaosDef_VARIABLE_TYPE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)
        val VARIABLE_NAME: TextAttributesKey = createTextAttributesKey("CaosDef_VARIABLE_NAME", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)
        val COMMAND: TextAttributesKey = createTextAttributesKey("CaosDef_COMMAND", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
        val DOC_COMMENT_HASHTAG: TextAttributesKey = createTextAttributesKey("CaosDef_DOC_COMMENT_HASHTAG", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
        val DOC_COMMENT_TAG: TextAttributesKey = createTextAttributesKey("CaosDef_DOC_COMMENT_HASHTAG", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
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