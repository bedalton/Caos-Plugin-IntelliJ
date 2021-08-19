package com.badahori.creatures.plugins.intellij.agenteering.caos.def.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefLexerAdapter
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefTypes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.types.CaosDefTokenSets.Companion.COMMENT_AT_KEYWORDS
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.types.CaosDefTokenSets.Companion.COMMENT_PARTS
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTempTextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import java.awt.Color

class CaosDefSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer {
        return CaosDefLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        if (tokenType == null) {
            return EMPTY_KEYS
        }
        val attrKey = when (tokenType) {
            in COMMENT_PARTS -> DOC_COMMENT
            in COMMENT_AT_KEYWORDS -> DOC_COMMENT_TAG
            CaosDef_WORD_LINK -> DOC_COMMENT_WORD_LINK
            CaosDef_TYPE_LINK, CaosDef_VALUES_LIST_NAME -> DOC_COMMENT_TYPE_LINK
            CaosDef_REGION_HEADING_LITERAL -> REGION_HEADER
            CaosDef_VALUES_LIST_VALUE -> VALUES_LIST_VALUE_NAME
            CaosDef_TEXT_LITERAL -> VALUES_LIST_VALUE_DESCRIPTION
            CaosDef_AT_FILE -> DOC_COMMENT_TYPE_LINK
            CaosDef_COMMA -> DefaultLanguageHighlighterColors.COMMA
            CaosDef_DASH -> DefaultLanguageHighlighterColors.OPERATION_SIGN
            CaosDef_EQ -> DefaultLanguageHighlighterColors.OPERATION_SIGN
            CaosDef_SEMI -> DefaultLanguageHighlighterColors.SEMICOLON
            CaosDef_HASH_TAG -> DOC_COMMENT_HASHTAG
            else -> return EMPTY_KEYS
        }
        return arrayOf(attrKey)
    }

    companion object {
        private val EMPTY_KEYS:Array<TextAttributesKey> = arrayOf()
        val COMMAND: TextAttributesKey = createTextAttributesKey("CaosDef_COMMAND", DefaultLanguageHighlighterColors.CLASS_NAME)
        val ID:TextAttributesKey = createTextAttributesKey("CaosDef_ID", DefaultLanguageHighlighterColors.IDENTIFIER)
        val CODE_BLOCK:TextAttributesKey = createTextAttributesKey("CaosDef_CODE_BLOCK", DefaultLanguageHighlighterColors.MARKUP_ENTITY)
        val DOC_COMMENT: TextAttributesKey = createTextAttributesKey("CaosDef_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT)
        val DOC_COMMENT_TAG:TextAttributesKey = createTextAttributesKey("CaosDef_DOC_COMMENT_AT_STATEMENT", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
        val DOC_COMMENT_VARIABLE_TYPE: TextAttributesKey = createTextAttributesKey("CaosDef_DOC_COMMENT_VARIABLE_TYPE", DOC_COMMENT)
        val DOC_COMMENT_VARIABLE_LINK: TextAttributesKey = createTextAttributesKey("CaosDef_DOC_COMMENT_VARIABLE_LINK", DOC_COMMENT)
        val DOC_COMMENT_HASHTAG: TextAttributesKey = createTextAttributesKey("CaosDef_DOC_COMMENT_HASHTAG", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
        val DOC_COMMENT_WORD_LINK:TextAttributesKey = createTextAttributesKey("CaosDef_DOC_COMMENT_WORD_LINK", DOC_COMMENT)
        val DOC_COMMENT_TYPE_LINK:TextAttributesKey = createTextAttributesKey("CaosDef_DOC_COMMENT_TYPE_LINK", DOC_COMMENT_WORD_LINK)
        val VALUES_LIST_NAME:TextAttributesKey = createTextAttributesKey("CaosDef_VALUES_LIST_NAME", COMMAND)
        val VALUES_LIST_TYPE:TextAttributesKey = createTextAttributesKey("CaosDef_VALUES_LIST_TYPE", DefaultLanguageHighlighterColors.LABEL)
        val VALUES_LIST_VALUE_KEY:TextAttributesKey = createTextAttributesKey("CaosDef_VALUES_LIST_KEY", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
        val VALUES_LIST_VALUE_NAME:TextAttributesKey = createTextAttributesKey("CaosDef_VALUES_LIST_VALUE_NAME", VALUES_LIST_VALUE_KEY)
        val VALUES_LIST_VALUE_DESCRIPTION:TextAttributesKey = createTextAttributesKey("CaosDef_VALUES_LIST_VALUE_DESCRIPTION", VALUES_LIST_VALUE_NAME)
        val VALUES_LIST_WORD_LINK:TextAttributesKey = createTextAttributesKey("CaosDef_VALUES_LIST_WORD_LINK", DefaultLanguageHighlighterColors.KEYWORD)
        val REGION_HEADER:TextAttributesKey = createTextAttributesKey("CaosDef_REGION_HEADER", DefaultLanguageHighlighterColors.KEYWORD)
        val VARIABLE_TYPE: TextAttributesKey = createTextAttributesKey("CaosDef_VARIABLE_TYPE", DefaultLanguageHighlighterColors.CONSTANT)
        val VARIABLE_NAME: TextAttributesKey = createTextAttributesKey("CaosDef_VARIABLE_NAME", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)
    }
}

@Suppress("unused")
private val TextAttributesKey.asErrorAttribute:TextAttributesKey get() {
    val attributes = this.defaultAttributes.clone()
    attributes.effectType = EffectType.WAVE_UNDERSCORE
    attributes.effectColor = Color.RED.brighter()
    attributes.errorStripeColor = Color.RED.brighter()
    val name = this.externalName + "_ERROR"
    val temp = createTempTextAttributesKey(name, attributes)
    return createTextAttributesKey(name, temp)
}