package com.badahori.creatures.plugins.intellij.agenteering.caos.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTempTextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptLexerAdapter
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
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
            in CaosScriptTokenSets.PREFIX_KEYWORDS -> PREFIX_TOKEN
            CaosScriptTypes.CaosScript_SUBROUTINE_NAME -> SUBROUTINE_NAME
            in CaosScriptTokenSets.COMMENTS -> COMMENT
            in CaosScriptTokenSets.ANIMATION_STRING -> ANIMATION
            in CaosScriptTokenSets.STRING_LIKE -> STRING
            CaosScriptTypes.CaosScript_VA_XX, CaosScriptTypes.CaosScript_VAR_X -> VAR_TOKEN_VA
            CaosScriptTypes.CaosScript_OV_XX, CaosScriptTypes.CaosScript_OBV_X -> VAR_TOKEN_OV
            CaosScriptTypes.CaosScript_MV_XX -> VAR_TOKEN_MV
            in CaosScriptTokenSets.NUMBER_LITERALS -> NUMBER
            in CaosScriptTokenSets.KEYWORDS -> KEYWORDS
            CaosScriptTypes.CaosScript_WORD -> TOKEN
            CaosScriptTypes.CaosScript_K_GSUB -> COMMAND_TOKEN
            in CaosScriptTokenSets.ALL_COMMANDS -> COMMAND_TOKEN
            CaosScriptTypes.CaosScript_EQ_OP_OLD_ -> EQ_OP_KEYWORD
            CaosScriptTypes.CaosScript_EQ_OP_NEW_ -> SYMBOL
            CaosScriptTypes.CaosScript_PICT_DIMENSION -> NUMBER
            else -> null
        }
        return if (attrKey != null) arrayOf(attrKey) else EMPTY_KEYS
    }

    companion object {
        private val EMPTY_KEYS:Array<TextAttributesKey> = arrayOf()
        @JvmStatic
        val ID:TextAttributesKey = createTextAttributesKey("CaosScript_ID", DefaultLanguageHighlighterColors.IDENTIFIER)
        @JvmStatic
        val KEYWORDS:TextAttributesKey = createTextAttributesKey("CaosScript_KEYWORDS", DefaultLanguageHighlighterColors.KEYWORD)
        @JvmStatic
        val COMMENT: TextAttributesKey = createTextAttributesKey("CaosScript_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        @JvmStatic
        val VAR_TOKEN: TextAttributesKey = createTextAttributesKey("CaosScript_VAR_TOKEN", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
                @JvmStatic
        val VAR_TOKEN_VA:TextAttributesKey = createTextAttributesKey("CaosScript_VAR_TOKEN_VA", VAR_TOKEN)
        @JvmStatic
        val VAR_TOKEN_OV:TextAttributesKey = createTextAttributesKey("CaosScript_VAR_TOKEN_OV", VAR_TOKEN)
        @JvmStatic
        val VAR_TOKEN_MV:TextAttributesKey = createTextAttributesKey("CaosScript_VAR_TOKEN_MV", VAR_TOKEN)
        @JvmStatic
        val COMMAND_TOKEN:TextAttributesKey = createTextAttributesKey("CaosScript_COMMAND_TOKEN", DefaultLanguageHighlighterColors.FUNCTION_CALL)
        @JvmStatic
        val RVALUE_TOKEN: TextAttributesKey = createTextAttributesKey("CaosScript_RVALUE", COMMAND_TOKEN)
        @JvmStatic
        val LVALUE_TOKEN: TextAttributesKey = createTextAttributesKey("CaosScript_LVALUE", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
        @JvmStatic
        val PREFIX_TOKEN:TextAttributesKey = createTextAttributesKey("CaosScript_PREFIX_TOKEN", COMMAND_TOKEN)
        @JvmStatic
        val SUFFIX_TOKEN:TextAttributesKey = createTextAttributesKey("CaosScript_SUFFIX_TOKEN", RVALUE_TOKEN)
        @JvmStatic
        val NUMBER:TextAttributesKey = createTextAttributesKey("CaosScript_NUMBER_LITERAL", DefaultLanguageHighlighterColors.NUMBER)
        @JvmStatic
        val ANIMATION:TextAttributesKey = createTextAttributesKey("CaosScript_ANIMATION", DefaultLanguageHighlighterColors.CONSTANT)
        @JvmStatic
        val BYTE_STRING:TextAttributesKey = ANIMATION
        @JvmStatic
        val STRING:TextAttributesKey = createTextAttributesKey("CaosScript_STRING_LITERAL", DefaultLanguageHighlighterColors.STRING)
        @JvmStatic
        val TOKEN:TextAttributesKey = createTextAttributesKey("CaosScript_TOKEN", DefaultLanguageHighlighterColors.STRING)
        @JvmStatic
        val EQ_OP_KEYWORD:TextAttributesKey = createTextAttributesKey("CaosScript_EQ_OP_KEYWORD", KEYWORDS)
        @JvmStatic
        val SYMBOL:TextAttributesKey = createTextAttributesKey("CaosScript_SYMBOLS", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)
       @JvmStatic
        val SUBROUTINE_NAME: TextAttributesKey = createTextAttributesKey("CaosScript_SUBROUTINE_NAME", DefaultLanguageHighlighterColors.INSTANCE_METHOD)
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