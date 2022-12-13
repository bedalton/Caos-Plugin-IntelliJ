package com.badahori.creatures.plugins.intellij.agenteering.catalogue.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer.CatalogueLexerAdapter
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer.CatalogueTypes.*
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.types.CatalogueTokenSets
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class  CatalogueSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer {
        return CatalogueLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        if (tokenType == null) {
            return EMPTY_KEYS
        }
        val attrKey: TextAttributesKey? = when (tokenType) {
            CATALOGUE_INT -> NUMBER
            CATALOGUE_INT_STRING_LITERAL -> INT_STRING
            CATALOGUE_ARRAY_KW, CATALOGUE_TAG_KW -> KEYWORD
            CATALOGUE_OVERRIDE_KW -> MODIFIER
            CATALOGUE_COMMENT_LITERAL -> COMMENT
            in CatalogueTokenSets.STRINGS -> STRING
            else -> null
        }
        return if (attrKey != null) arrayOf(attrKey) else EMPTY_KEYS
    }

    companion object {
        private val EMPTY_KEYS:Array<TextAttributesKey> = arrayOf()
        @JvmStatic
        val ID:TextAttributesKey = createTextAttributesKey("Catalogue_ID", DefaultLanguageHighlighterColors.IDENTIFIER)

        @JvmStatic
        val KEYWORD = createTextAttributesKey("Catalogue_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

        @JvmStatic
        val MODIFIER = createTextAttributesKey("Catalogue_MODIFIER", DefaultLanguageHighlighterColors.KEYWORD)

        @JvmStatic
        val NUMBER = createTextAttributesKey("Catalogue_INT", DefaultLanguageHighlighterColors.NUMBER)

        @JvmStatic
        val NAME = createTextAttributesKey("Catalogue_NAME", DefaultLanguageHighlighterColors.CLASS_NAME)

        @JvmStatic
        val TAG_NAME = createTextAttributesKey("Catalogue_TAG_NAME", NAME)

        @JvmStatic
        val ARRAY_NAME = createTextAttributesKey("Catalogue_ARRAY_NAME", NAME)

        @JvmStatic
        val STRING:TextAttributesKey = createTextAttributesKey("Catalogue_STRING", DefaultLanguageHighlighterColors.STRING)

        // Highlights strings that contain only a number
        @JvmStatic
        val INT_STRING = createTextAttributesKey("Catalogue_INT_STRING", STRING)

        @JvmStatic
        val COMMENT:TextAttributesKey = createTextAttributesKey("Catalogue_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)

    }
}